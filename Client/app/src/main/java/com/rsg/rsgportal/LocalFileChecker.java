package com.rsg.rsgportal;

import android.util.Log;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by adamsingle on 16/02/2016.
 * ToTest: Test getting files found locally
 * ToTest: Test handling no files
 */
public class LocalFileChecker implements ILocalFileChecker {

    public LocalStagingDeserialiser LocalStagingDeserialiser;
    public FileSystem FileSystem;
    public MD5Checker MD5;

    /**
     * Attempt to retrieve a file from staging directory
     * @param fileName
     * @param fileSize
     * @param fileChecksum
     * @param completeCallback
     */
    public void TryGetFileLocally(String fileName, int fileSize, String fileChecksum, GenericCallback<String> completeCallback) {
        File stagingDirectory = LocalStagingDeserialiser.getStagingDirectory();

        //file is yet to be found.
        AtomicReference<Boolean> fileFound = new AtomicReference<>(false);


        AtomicReference<Integer> totalFiles = new AtomicReference<>(0);

        getFileCount(stagingDirectory, totalFiles);

        //if there aren't any files in the app folder we don't have an installed version. Just return
        if (totalFiles.get() == 0) {
            completeCallback.success(null);
            return;
        }

        AtomicReference<String> fullpath = new AtomicReference<>();

        findFileRecursively(stagingDirectory, fileName, fileSize, fileChecksum, fullpath, fileFound, totalFiles, new Callback() {
            @Override
            public void success() {
                completeCallback.success(fullpath.get());
            }

            @Override
            public void fail(Exception e) {
                completeCallback.fail(e);
            }
        });
    }


    /**
     * Attempt to retrieve a file from the staging directory
     * @param definition
     * @return
     */
    @Override
    public void TryGetFileLocally(DataManifest.DataFileDefinition definition, GenericCallback<String> completeCallback) {
        TryGetFileLocally(definition.filename(), definition.fileSize(), definition.checksum(), completeCallback);
    }

    private void getFileCount(File file, AtomicReference<Integer> fileCount) {
        File[] files = file.listFiles();
        if (files == null) {
            return;
        }

        fileCount.set(fileCount.get() + files.length);

        for (File f: files) {
            if (f.isDirectory()) {
                getFileCount(f, fileCount);
            }
        }
    }

    private void findFileRecursively(
            File rootDirectory,
            String fileName,
            int fileSize,
            String fileChecksum,
            AtomicReference<String> fullpath,
            AtomicReference<Boolean> fileFound,
            AtomicReference<Integer> numberOfFilesLeftToCheck,
            Callback recursionCompleteCallback) {
        for (File file : rootDirectory.listFiles()) {

            if (file.isDirectory()) {
                numberOfFilesLeftToCheck.set(numberOfFilesLeftToCheck.get() - 1);
                findFileRecursively(file, fileName, fileSize, fileChecksum, fullpath, fileFound, numberOfFilesLeftToCheck, recursionCompleteCallback);
            }
            else {
                checkFileMatch(fileName, fileSize, fileChecksum, file, new GenericProgressFeedbackCallback<Boolean>() {
                    @Override
                    public void success(Boolean result) {

                        numberOfFilesLeftToCheck.set(numberOfFilesLeftToCheck.get() - 1);

                        if (result) {
                            fileFound.set(result);
                            fullpath.set(file.getAbsolutePath());
                            recursionCompleteCallback.success();
                        }
                        else if (numberOfFilesLeftToCheck.get() <= 0) {
                            recursionCompleteCallback.success();
                        }
                    }

                    @Override
                    public void fail(Exception ex) {
                        Log.println(Log.ERROR, "FileChecker", "an exception occurred attempting to compare two files");
                    }

                    @Override
                    public void progress(float progress) {

                    }
                });
            }
        }
    }

    private void checkFileMatch(String fileName, int fileSize, String fileChecksum, File file, GenericProgressFeedbackCallback<Boolean> callback) {
        if (fileName.toLowerCase().equals(file.getName().toLowerCase())) {
            //matching filename. Further verification needed here
            if (compareFileSize(fileSize, file)) {
                compareChecksum(fileChecksum, file, callback);
            }
            else {
                callback.success(false);
            }
        }
        else {
            callback.success(false);
        }
    }

    private boolean compareFileSize(int fileSize, File file) {
        return file.length() == fileSize;
    }

    private void compareChecksum(String fileChecksum, File file, GenericProgressFeedbackCallback<Boolean> callback) {
        MD5.checkFileMD5(file.getAbsolutePath(), fileChecksum, callback);
    }
}
