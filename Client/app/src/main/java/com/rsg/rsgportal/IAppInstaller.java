package com.rsg.rsgportal;

import java.util.List;

/**
 * Created by adamsingle on 21/01/2016.
 */
public interface IAppInstaller {

    /**
     * Install the apk and copy files to the destination directory.
     * @param versionNumber
     * @param existingFilePaths
     * @param progressCallback
     */
    void installApp(int versionNumber, List<String> existingFilePaths, GenericProgressFeedbackCallback<Boolean> progressCallback);

    /**
     * Check if the specified app is installed and if it is a higher version than the specified version,
     * uninstall it.
     * @param packageName
     * @param newVersion
     * @return
     */
    boolean uninstallAppIfNecessary(String packageName, int newVersion);
}
