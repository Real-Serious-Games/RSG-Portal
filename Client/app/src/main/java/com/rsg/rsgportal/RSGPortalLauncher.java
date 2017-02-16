package com.rsg.rsgportal;

/**
 * Created by adamsingle on 22/01/2016.
 */
public class RSGPortalLauncher {
    /**
     * Singleton accessor
     */
    private static RSGPortalLauncher instance;

    public static RSGPortalLauncher Instance() {

        if (instance == null) {
            instance = new RSGPortalLauncher();
            SetUpDependencies();
        }

        return instance;
    }

    /**
     * Accessor for the portal updater. This is needed by the UI
     */
    private static PortalUpdater portalUpdater;

    public static PortalUpdater PortalUpdater() { return portalUpdater; }

    /**
     * Accessor for the updater. This is needed by the UI
     */
    private static AppUpdater appUpdater;

    public static AppUpdater Updater() {
        return appUpdater;
    }

    private static void SetUpDependencies() {
        //new up HTTP
        Http http = new Http();

        //set up GsonDeserialiser
        GsonDeserialiser gsonDeserialiser = new GsonDeserialiser();

        //Set up the RemoteManifestDownloader
        RemoteManifestDownloader remoteManifestDownloader = new RemoteManifestDownloader();
        remoteManifestDownloader.Http = http;
        remoteManifestDownloader.Gson = gsonDeserialiser;

        //set up FileSystem
        FileSystem fileSystem = new FileSystem();

        //set up MD5
        MD5Checker md5 = new MD5Checker();
        md5.FileSystem = fileSystem;

        //set up AppEnvironment
        AppEnvironment appEnvironment = new AppEnvironment();

        //set up LocalStagingDeserialiser
        LocalStagingDeserialiser localStagingDeserialiser = new LocalStagingDeserialiser();
        localStagingDeserialiser.FileSystem = fileSystem;
        localStagingDeserialiser.AppEnvironment = appEnvironment;

        //set up AppVerifier
        AppVerifier appVerifier = new AppVerifier();
        appVerifier.AppEnvironment = appEnvironment;
        appVerifier.MD5Checker = md5;
        appVerifier.PackageManager = PortalAppInfo.AppContext.getPackageManager();

        //set up LocalFileChecker
        LocalFileChecker localFileChecker = new LocalFileChecker();
        localFileChecker.FileSystem = fileSystem;
        localFileChecker.LocalStagingDeserialiser = localStagingDeserialiser;
        localFileChecker.MD5 = md5;

        //set up AppDownloader
        AppDownloader appDownloader = new AppDownloader();
        appDownloader.FileSystem = fileSystem;
        appDownloader.Http = http;
        appDownloader.FileChecker = localFileChecker;
        appDownloader.LocalStagingDeserialiser = localStagingDeserialiser;

        //set up ApkInstaller
        ApkInstaller apkInstaller = new ApkInstaller();

        //Set up PortalUpdater
        portalUpdater = new PortalUpdater();
        portalUpdater.RemoteManifestDownloader = remoteManifestDownloader;
        portalUpdater.AppDownloader = appDownloader;
        portalUpdater.ApkInstaller = apkInstaller;

        //set up AppInstaller
        AppInstaller appInstaller = new AppInstaller();
        appInstaller.ApkInstaller = apkInstaller;
        appInstaller.AppEnvironment = appEnvironment;
        appInstaller.FileSystem = fileSystem;
        appInstaller.LocalStagingDeserialiser = localStagingDeserialiser;

        //set up Updater
        appUpdater = new AppUpdater();
        appUpdater.AppInstaller = appInstaller;
        appUpdater.ApplicationDownloader = appDownloader;
        appUpdater.LocalStagingDeserialiser = localStagingDeserialiser;
        appUpdater.RemoteManifestDownloader = remoteManifestDownloader;
        appUpdater.AppVerifier = appVerifier;
    }
}
