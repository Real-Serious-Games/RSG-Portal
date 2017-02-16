package com.rsg.rsgportal;

import android.content.Intent;
import android.net.Uri;
import android.os.Environment;

import java.io.File;

/**
 * This class is will install a APK.
 *
 * Created by PhilipWarren on 15/01/2016.
 */
public class ApkInstaller {

    public void install(String fileName) {

        File filesLocation = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse("file://" + filesLocation + File.separator + fileName), "application/vnd.android.package-archive");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PortalAppInfo.AppContext.startActivity(intent);
    }

    public void installFromFullpath(String filePath) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse("file://" + filePath), "application/vnd.android.package-archive");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PortalAppInfo.AppContext.startActivity(intent);
    }

    /**
     * Launch an intent to uninstall the specified app.
     */
    public void uninstall(String appName) {
        Uri packageUri = Uri.parse("package:" + appName);
        Intent intent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageUri);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PortalAppInfo.AppContext.startActivity(intent);
    }
}
