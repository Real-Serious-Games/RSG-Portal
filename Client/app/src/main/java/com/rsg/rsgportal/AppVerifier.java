package com.rsg.rsgportal;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import org.jdeferred.Deferred;
import org.jdeferred.Promise;
import org.jdeferred.impl.DeferredObject;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Class for verifying integrity of installed data.
 *
 * Created by rorydungan on 2/03/2016.
 */
public class AppVerifier {

    public PackageManager PackageManager;

    public MD5Checker MD5Checker;

    public AppEnvironment AppEnvironment;

    /**
     * Verify that the app with the specified data manifest has installed correctly.
     *
     * Checks that all files are present, checks MD5 sums of all installed data files, checks
     * whether the specified apk has been installed and if its version code matches the manifest.
     * @param manifest Manifest file to check against.
     * @return Promise that resolves once the app has been verified
     */
    public Promise<Boolean, Exception, Float> verifyApp(String packageName, DataManifest manifest) {
        Deferred<Boolean, Exception, Float> deferred = new DeferredObject<>();
        Promise<Boolean, Exception, Float> promise = deferred.promise();

        // Check the apk is installed and is the correct version.
        if (!verifyInstalledPackage(packageName, manifest.requiredApkVersion())) {
            // Early out if this fails, since it's a quicker test than checking all the data file MD5 sums.
            deferred.notify(1f);
            deferred.resolve(false);
            return promise;
        }

        List<DataManifest.DataFileDefinition> dataFiles = manifest.getFileDefinitions();

        // Early out if there are no data files
        if (dataFiles.size() == 0) {
            deferred.notify(1f);
            deferred.resolve(true);
            return promise;
        }

        // How many files still need to be checked, so we can know when we've checked all of them.
        AtomicReference<Integer> filesRemaining = new AtomicReference<>(dataFiles.size());

        AtomicReference<Boolean> valid = new AtomicReference<>(true);

        // Now check all the files
        for(DataManifest.DataFileDefinition file : dataFiles) {
            File destinationDir = new File(AppEnvironment.getExternalStorageDirectory(), manifest.getDestination());
            String filePath = new File(destinationDir, file.filename()).getAbsolutePath();

            MD5Checker.checkFileMD5(filePath, file.checksum(), new GenericProgressFeedbackCallback<Boolean>() {
                @Override
                public void success(Boolean data) {
                    if (!data) {
                        valid.set(false);
                    }
                    filesRemaining.set(filesRemaining.get() - 1);

                    if (filesRemaining.get() == 0) {
                        deferred.resolve(valid.get());
                    }
                }

                @Override
                public void fail(Exception ex) {
                    filesRemaining.set(filesRemaining.get() - 1);
                    valid.set(false);

                    if (filesRemaining.get() == 0) {
                        deferred.resolve(false);
                    }
                }

                @Override
                public void progress(float progress) {
                    // TODO: Update progress
                }
            });
        }

        return promise;
    }

    /**
     * Check that the specified package is installed, and the installed version is the correct one.
     */
    private boolean verifyInstalledPackage(String name, int versionCode) {
        try {
            PackageInfo info = PackageManager.getPackageInfo(name, 0);
            return info.versionCode == versionCode;
        } catch (PackageManager.NameNotFoundException ex) {
            return false;
        }
    }
}
