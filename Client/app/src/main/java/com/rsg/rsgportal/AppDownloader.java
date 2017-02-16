package com.rsg.rsgportal;

import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Downloads a app and stores it on the local device.
 *
 * Created by PhilipWarren on 15/01/2016.
 */
public class AppDownloader implements IAppDownloader {

    public Http Http;

    public FileSystem FileSystem;

    public ILocalFileChecker FileChecker;

    public LocalStagingDeserialiser LocalStagingDeserialiser;

    public void downloadPortalApp(final PortalManifest manifest, final Callback callback) {

        String apkUrl = "http://" + PortalAppInfo.RemoteServerAddress + "/" +
                "v" + PortalAppInfo.ApiVersion + "/" + manifest.ApkName;

        Http.downloadFile(apkUrl, new GenericCallback<byte[]>() {
            @Override
            public void success(byte[] data) {

                try {
                    FileSystem.ensureDirectoriesForFile(manifest.ApkName);
                    FileSystem.saveFile(manifest.ApkName, data);
                } catch (IOException e) {
                    callback.fail(e);
                    return;
                }

                callback.success();
            }

            @Override
            public void fail(Exception e) {

                callback.fail(e);
            }
        });
    }

    public void downloadAppUpdates(final int version,
                                   final DataManifest manifest,
                                   final GenericCallback<Integer> downloadSizeCallback,
                                   final GenericProgressFeedbackCallback<List<String>> progressCallback) {
        final String baseUrl = "http://" + PortalAppInfo.RemoteServerAddress + "/" +
                "v" + PortalAppInfo.ApiVersion + "/android/" + AppUpdater.APP_ID;
        final String apkUrl = version + "/" + AppUpdater.APP_ID + ".apk";
        final String dataManifestUrl = version + "/manifest";

        // Read data manifest and build list of files
        final List<DataManifest.DataFileDefinition> dataFiles;
        try {
            dataFiles = manifest.getFileDefinitions();
        } catch (JsonSyntaxException ex) {
            progressCallback.fail(ex);
            return;
        }

        //A list of file paths that have been found locally and don't need to be downloaded
        List<String> localFiles = new ArrayList<>();

        //The list of file paths needed to be requested from teh server
        List<String> serverRequests = new ArrayList<>();


        Callback allFilesPathsFoundCallback = new Callback() {
            @Override
            public void success() {
                downloadNecessaryFilesFromServer(localFiles, serverRequests);
            }

            @Override
            public void fail(Exception ex) {
                //What the hell to do here?
            }

            private void downloadNecessaryFilesFromServer(final List<String> localFiles, List<String> serverRequests) {
                final Hashtable<String, Integer> fileSizes = new Hashtable<>();

                final Stack<Callback> calls = new Stack<>();
                for (final String file : serverRequests) {
                    calls.push(new Callback() {
                        @Override
                        public void success() {
                            Http.getContentLength(baseUrl + "/" + file, new GenericCallback<Integer>() {
                                @Override
                                public void success(Integer data) {
                                    fileSizes.put(file, data);

                                    if (calls.size() > 0) {
                                        Callback call = calls.pop();
                                        call.success();
                                    } else {
                                        // We know know the size of all the files
                                        int totalSize = getTotalDownloadSize(fileSizes);
                                        downloadSizeCallback.success(totalSize);
                                        downloadAllFiles(baseUrl, fileSizes, totalSize, new ProgressFeedbackCallback() {
                                            @Override
                                            public void success() {
                                                //return a list of the filepaths for the files that already exist locally.
                                                //Ths installer already pulls all the files out of the current versions directory
                                                progressCallback.success(localFiles);
                                            }

                                            @Override
                                            public void fail(Exception ex) {
                                                progressCallback.fail(ex);
                                            }

                                            @Override
                                            public void progress(float progress) {
                                                progressCallback.progress(progress);
                                            }
                                        });
                                    }
                                }

                                @Override
                                public void fail(Exception e) {
                                    progressCallback.fail(e);
                                }
                            });
                        }

                        @Override
                        public void fail(Exception e) {
                            progressCallback.fail(e);
                        }
                    });
                }

                // Run the first callback to start the chain down the stack.
                calls.pop().success();
            }
        };

        //This is the count for the total number of files that need to be
        //checked for local versions. It is all of the files in the manifest plus the
        //apk file
        final int totalFileCount = dataFiles.size() + 1;

        //This is the running count of files that have been checked.
        AtomicReference<Integer> fileCount = new AtomicReference<>(0);

        //check if the apk file has already been downloaded in a previous version.
        FileChecker.TryGetFileLocally(AppUpdater.APP_ID + ".apk", manifest.getApkSize(), manifest.apkChecksum(), new GenericCallback<String>() {
            @Override
            public void success(String apkFilePath) {
                if (apkFilePath != null) {
                    localFiles.add(apkFilePath);
                    LocalStagingDeserialiser.addNewVersionAPKPath(version, apkFilePath);

                } else {
                    serverRequests.add(apkUrl);
                }

                fileCount.set(fileCount.get() + 1);
                if (fileCount.get() >= totalFileCount) {
                    allFilesPathsFoundCallback.success();
                }
            }

            @Override
            public void fail(Exception e) {
                allFilesPathsFoundCallback.fail(e);
            }
        });

        //Add the data manifest url to the download list
        serverRequests.add(dataManifestUrl);

        //check each file needed for this version in previously installed versions.
        for (DataManifest.DataFileDefinition file : dataFiles) {
            FileChecker.TryGetFileLocally(file, new GenericCallback<String>() {
                @Override
                public void success(String fullpath) {
                    if (fullpath != null) {
                        localFiles.add(fullpath);
                    } else {
                        serverRequests.add(version + "/data/" + file.filename());
                    }
                    fileCount.set(fileCount.get() + 1);
                    if (fileCount.get() >= totalFileCount) {
                        allFilesPathsFoundCallback.success();
                    }
                }

                @Override
                public void fail(Exception e) {
                    //what the hell to do here?
                }
            });
        }
    }

    private void downloadAllFiles(final String downloadUrl, Hashtable<String, Integer> files, final int totalSize, final ProgressFeedbackCallback progressCallback) {
        final Stack<Callback> filesToDownload = new Stack<>();

        final AtomicReference<Float> currentProgress = new AtomicReference<>(0f);

        Iterator<Map.Entry<String, Integer>> it = files.entrySet().iterator();
        while (it.hasNext()) {
            final Map.Entry<String, Integer> file = it.next();
            final String source = downloadUrl + "/" + file.getKey();
            final String destination = AppUpdater.APP_ID + "/" + file.getKey();
            final int size = file.getValue();

            filesToDownload.push(new Callback() {
                @Override
                public void success() {
                    try {
                        FileSystem.ensureDirectoriesForFile(destination);
                        Http.downloadFile(source, FileSystem.getNewFileStream(destination),
                                new ProgressFeedbackCallback() {
                                    @Override
                                    public void success() {
                                        currentProgress.set(currentProgress.get() + (float) size / (float) totalSize);

                                        // Download next file
                                        if (filesToDownload.size() > 0) {
                                            Callback call = filesToDownload.pop();
                                            call.success();
                                        } else {
                                            // All files have been successfully downloaded!
                                            progressCallback.success();
                                        }
                                    }

                                    @Override
                                    public void fail(Exception ex) {
                                        progressCallback.fail(ex);
                                    }

                                    @Override
                                    public void progress(float progress) {
                                        // Scale progress for this file to fit with overall progress.
                                        final float scaledProgress = progress * ((float) size / (float) totalSize);

                                        // Add to current overall progress to show total overall progress
                                        progressCallback.progress(currentProgress.get() + scaledProgress);
                                    }
                                });
                    } catch (Exception ex) {
                        progressCallback.fail(ex);
                    }
                }

                @Override
                public void fail(Exception e) {
                    progressCallback.fail(e);
                }
            });
        }

        // Run the first callback to start the chain down the stack.
        filesToDownload.pop().success();
    }

    /**
     * Finds the total download size by iterating through the hashtable of individual file names
     * and sizes.
     */
    private int getTotalDownloadSize(Hashtable<String, Integer> fileSizes) {
        Iterator<Map.Entry<String, Integer>> it = fileSizes.entrySet().iterator();
        int total = 0;

        while (it.hasNext()) {
            final Map.Entry<String, Integer> file = it.next();
            total += file.getValue();
        }

        return total;
    }

}
