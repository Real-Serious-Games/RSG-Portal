package com.rsg.rsgportal;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Pair;

import org.jdeferred.Deferred;
import org.jdeferred.DoneFilter;
import org.jdeferred.Promise;
import org.jdeferred.impl.DeferredObject;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by adamsingle on 22/01/2016.
 */
public class AppInstaller implements IAppInstaller {

    public ApkInstaller ApkInstaller;

    public FileSystem FileSystem;

    public LocalStagingDeserialiser LocalStagingDeserialiser;

    public AppEnvironment AppEnvironment;

    @Override
    public void installApp(final int versionNumber,
                           final List<String> localFilePaths,
                           final GenericProgressFeedbackCallback<Boolean> progressCallback) {

        try {
            List<String> filepaths = LocalStagingDeserialiser.getAppDataFilepaths(versionNumber);

            //Add the already existing local paths
            filepaths.addAll(localFilePaths);

            if (filepaths.isEmpty()) {
                progressCallback.fail(new Exception("No local files were found for version " + versionNumber));
                return;
            }

            File destinationFilePath = new File(AppEnvironment.getExternalStorageDirectory(), AppUpdater.APP_ID);
            int totalFiles = filepaths.size();
            //A fake percentage. This simply indicates how many files have been copied or installed
            final float percentagePerFile = 1f / totalFiles;

            final AtomicReference<Float> progress = new AtomicReference<>(0f);

            final LocalStagingDeserialiser localStagingDeserialiser = this.LocalStagingDeserialiser;

            Deferred<Void, Exception, Pair<Integer, Integer>> pendingPromise = new DeferredObject<>();
            Promise<Void, Exception, Pair<Integer, Integer>> promise = pendingPromise.promise();

            //copy files
            for (String filepath : filepaths) {
                final File sourceFile = new File(filepath);
                final File destFile = new File(destinationFilePath, sourceFile.getName());

                promise.then((DoneFilter<Void, Promise>) args -> {
                    progress.set(progress.get() + percentagePerFile);
                    progressCallback.progress(progress.get());
                    return FileSystem.copyFile(sourceFile, destFile);
                });
            }

            promise.done(result -> {
                    try {
                        String apkPath = localStagingDeserialiser.getAPKPath(versionNumber);
                        boolean installingApk = installAPK(apkPath);
                        progressCallback.success(installingApk);
                    } catch (Exception e) {
                        progressCallback.fail(e);
                    }
                })
                .fail(progressCallback::fail);

            pendingPromise.resolve(null);
        }
        catch (Exception ex) {
            progressCallback.fail(ex);
        }
    }

    /**
     * Taking a filepath to an apk, this function will figure out if the currently installed version
     * is the same version number as the one at the provided filepath.
     * @param filepath
     * @return True if we're installing a new apk, false if we don't have to.
     */
    private boolean installAPK(String filepath) {
        final PackageManager pm = PortalAppInfo.AppContext.getPackageManager();

        //Get the version of the file
        PackageInfo info = pm.getPackageArchiveInfo(filepath, 0);
        int newAPKVersion = info.versionCode;

        //get the version of the apk installed, if any
        boolean installNewAPK = false;

        try{
            PackageInfo installedInfo = pm.getPackageInfo(info.packageName, 0);
            if (installedInfo.versionCode != newAPKVersion) { //do the versions match? If not, install the new one.
                installNewAPK = true;
            }
        }
        catch(PackageManager.NameNotFoundException ex) {
            //the application isn't installed.
            installNewAPK = true;
        }

        //If we need to, install the new APK. Otherwise the one we have is already valid.
        if (installNewAPK) {
            ApkInstaller.installFromFullpath(filepath);
        }

        return installNewAPK;
    }

    /**
     * Check if the specified app is installed and if it is a higher version than the specified version,
     * uninstall it.
     */
    public boolean uninstallAppIfNecessary(String packageName, int newVersion) {
        final PackageManager pm = PortalAppInfo.AppContext.getPackageManager();

        boolean uninstall = false;

        // Get the version info of the app
        try {
            PackageInfo info = pm.getPackageInfo(packageName, 0);
            if (info.versionCode > newVersion) {
                uninstall = true;

                ApkInstaller.uninstall(packageName);
            }
        } catch (PackageManager.NameNotFoundException ex) {
            uninstall = false;
        }

        return uninstall;
    }
}
