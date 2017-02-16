package com.rsg.rsgportal;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by adamsingle on 16/02/2016.
 */
public interface ILocalFileChecker {
    /**
     * Attempt to find the provided filename in a the staging directory. If it
     * finds it, it will return the fullpath
     * @param file
     * @param completeCallback
     * @return
     */
    void TryGetFileLocally(DataManifest.DataFileDefinition file, GenericCallback<String> completeCallback);

    /**
     * Attempt to find the provided filename in a the staging directory. If it
     * finds it, it will return the fullpath
     * @param fileName
     * @param fileSize
     * @param fileChecksum
     * @param completeCallback
     */
    void TryGetFileLocally(String fileName, int fileSize, String fileChecksum, GenericCallback<String> completeCallback);
}
