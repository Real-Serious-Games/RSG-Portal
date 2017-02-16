package com.rsg.rsgportal;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class CheckForUpdatesTestbed extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_check_for_updates);

        Http http = new Http();

        RemoteManifestDownloader remoteManifestDownloader = new RemoteManifestDownloader();
        remoteManifestDownloader.Http = http;

        PortalAppInfo.AppContext = this;

        FileSystem fileSystem = new FileSystem();

        AppDownloader appDownloader = new AppDownloader();
        appDownloader.FileSystem = fileSystem;
        appDownloader.Http = http;

        ApkInstaller apkInstaller = new ApkInstaller();

        PortalUpdater updater = new PortalUpdater();
        updater.RemoteManifestDownloader = remoteManifestDownloader;
        updater.AppDownloader = appDownloader;
        updater.ApkInstaller = apkInstaller;

        updater.update(new GenericCallback<Boolean>() {
            @Override
            public void success(Boolean data) {

            }

            @Override
            public void fail(Exception e) {

            }
        });

        //set up the notify instance
        Notify.Instance().SetView(findViewById(R.id.application_title));
    }
}
