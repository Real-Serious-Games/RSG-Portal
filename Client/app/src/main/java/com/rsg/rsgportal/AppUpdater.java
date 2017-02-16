package com.rsg.rsgportal;

import android.util.Log;

import org.jdeferred.Deferred;
import org.jdeferred.Promise;
import org.jdeferred.impl.DeferredObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by adamsingle on 19/01/2016.
 */
public class AppUpdater {

    public RemoteManifestDownloader RemoteManifestDownloader;

    public LocalStagingDeserialiser LocalStagingDeserialiser;

    public IAppDownloader ApplicationDownloader;

    public IAppInstaller AppInstaller;

    public AppVerifier AppVerifier;

    private List<String> existingFilePaths = new ArrayList<>();

    private RemoteApplicationManifest currentRemoteManifest;

    public static final String APP_ID = "com.RSG.MyApp";

    public RemoteApplicationManifest getKnownRemoteAppManifest() {
        return currentRemoteManifest;
    }

    public int getCurrentLocalVersionNumber() {
        return LocalStagingDeserialiser.getLocalApplicationVersion();
    }

    ///
    /// Check the local version and the latest version on the server and give a
    /// ServerUpdateCheckResponse or fail if we can't get the latest remote version.
    ///
    public void checkForUpdates(final GenericCallback<RemoteApplicationManifest> callback)
    {
        RemoteManifestDownloader.getLatestApplicationManifest(new GenericCallback<RemoteApplicationManifest>() {
            @Override
            public void success(final RemoteApplicationManifest manifest) {
                currentRemoteManifest = manifest;
                callback.success(manifest);
            }

            @Override
            public void fail(Exception ex) {
                callback.fail(ex);
            }
        });
    }

    public Promise<Boolean, Exception, Float> validateCurrentInstallation() {
        Deferred<Boolean, Exception, Float> deferred = new DeferredObject<>();

        try {
            return AppVerifier.verifyApp(APP_ID, LocalStagingDeserialiser.getCurrentDataManifest());
        }
        catch (Exception ex) {
            return deferred.reject(new Exception("There is a problem with your installation. Please uninstall and reinstall."));
        }
    }

    public void downloadUpdates(final int version, final GenericCallback<Integer> downloadSizeCallback, final ProgressFeedbackCallback progressCallback)
    {
        RemoteManifestDownloader.getDataManifest(version)
            .then(manifest -> {
                ApplicationDownloader.downloadAppUpdates(
                        version,
                        manifest,
                        new GenericCallback<Integer>() {
                            @Override
                            public void success(Integer sizeInBytes) {
                                if (sizeInBytes <= 0) {
                                    downloadSizeCallback.fail(new Exception("An invalid download size was returned: " + sizeInBytes.toString()));
                                } else {
                                    downloadSizeCallback.success(sizeInBytes);
                                }
                            }

                            @Override
                            public void fail(Exception e) {
                                downloadSizeCallback.fail(e);
                            }
                        },
                        new GenericProgressFeedbackCallback<List<String>>() {

                            @Override
                            public void success(List<String> data) {
                                //clear the cache of local file paths then add the new ones to it
                                existingFilePaths.clear();
                                existingFilePaths.addAll(data);
                                progressCallback.success();
                            }

                            @Override
                            public void fail(Exception ex) {
                                progressCallback.fail(ex);
                            }

                            @Override
                            public void progress(float progress) {
                                progress = Math.min(1f, Math.max(0f, progress));
                                progressCallback.progress(progress);
                            }
                        }
                );
            })
            .fail(progressCallback::fail);
    }

    public void installApp(int version, final GenericProgressFeedbackCallback<Boolean> progressCallback) {
        AppInstaller.installApp(
                version,
                existingFilePaths,
                new GenericProgressFeedbackCallback<Boolean>() {
                    @Override
                    public void success(Boolean installed) {

                        try {
                            LocalStagingDeserialiser.updateLocalAppVersionNumber(version);
                            progressCallback.success(installed);
                        } catch (IOException ioex) {
                            progressCallback.fail(ioex);
                        }
                    }

                    @Override
                    public void fail(Exception ex) {
                        progressCallback.fail(ex);
                    }

                    @Override
                    public void progress(float progress) {
                        progress = Math.min(1f, Math.max(0f, progress));
                        progressCallback.progress(progress);
                    }
                }
        );
    }

    /**
     * Android doesn't support installing an app from an apk if a higher version already exists,
     * so check if there's a higher version than the one specified already installed and launch
     * an intent to remove it if necessary.
     * @param version Version to check against.
     * @return True if we need to uninstall the app.
     */
    public boolean uninstallIfNecessary(int version) {
        return AppInstaller.uninstallAppIfNecessary(APP_ID, version);
    }
}
