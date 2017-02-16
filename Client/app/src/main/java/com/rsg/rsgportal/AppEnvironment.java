package com.rsg.rsgportal;

import android.os.Environment;

import java.io.File;

/**
 * Created by adamsingle on 22/01/2016.
 */
public class AppEnvironment {
    /**
     * Simple wrapper for the Environment to allow mocking this interaction
     * @return
     */
    public File getExternalStorageDirectory() {
        return Environment.getExternalStorageDirectory();
    }

    /**
     * Retrieve the downloads directory
     * @return
     */
    public File getExternalPublicDownloadDirectory() {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    }
}
