package com.rsg.rsgportal;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

/**
 * Created by adamsingle on 20/01/2016.
 */
public class LocalStagingDeserialiser {

    public FileSystem FileSystem;

    public AppEnvironment AppEnvironment;

    HashMap<Integer, String> apkVersionPaths = new HashMap<>();

    /**
     * Load the application manifest
     * @return
     * @throws IOException
     */
    private LocalApplicationManifest getApplicationManifest() throws IOException, JsonSyntaxException {
        byte[] manifestJson = FileSystem.loadFile(AppUpdater.APP_ID + "/manifest");
        String manifestString = new String(manifestJson);

        LocalApplicationManifest manifest = new Gson().fromJson(manifestString, LocalApplicationManifest.class);
        return manifest;
    }

    /**
     * Return the data manifest for a specific version of the app. used to verify the files.
     * @param version
     * @return
     * @throws IOException
     * @throws JsonSyntaxException
     */
    public DataManifest getDataManifest(int version) throws IOException, JsonSyntaxException {
        byte[] manifestJson = FileSystem.loadFile(AppUpdater.APP_ID + "/" + Integer.toString(version) + "/manifest");
        String manifestString = new String(manifestJson);

        DataManifest manifest = new Gson().fromJson(manifestString, DataManifest.class);
        return manifest;
    }

    /**
     * Helper function to get the data manifest for the currently expected installation.
     * @return
     * @throws IOException
     * @throws JSONException
     */
    public DataManifest getCurrentDataManifest() throws IOException, JSONException {
        return getDataManifest(getLocalApplicationVersion());
    }

    /**
     * Check the local storage to find the version number for the installed version of
     * the app.
     */
    public int getLocalApplicationVersion()
    {
        int version = 0;
        LocalApplicationManifest manifest = null;
        try {
            manifest = getApplicationManifest();
        } catch (IOException ex) {
            return version;
        }

        if (manifest != null) {
            version = manifest.getVersionNumber();
        }
        return version;
    }

    /**
     * Update the local app manifest with the new version
     * @param newVersionNumber
     */
    public void updateLocalAppVersionNumber(int newVersionNumber) throws IOException {

        LocalApplicationManifest manifest = new LocalApplicationManifest();

        manifest.setVersionNumber(newVersionNumber);

        String json = new Gson().toJson(manifest);

        final String fileName = AppUpdater.APP_ID + "/manifest";
        FileSystem.ensureDirectoriesForFile(fileName);
        FileSystem.saveFile(fileName, json.getBytes());
    }

    /**
     * Retrieve a list of all the file paths for the data files for a specific version
     * @param versionNumber
     */
    public List<String> getAppDataFilepaths(int versionNumber) throws Exception{

        //Given the version number we just need to inspect all files that are within
        //DOWNLOADS/<APP_IDENTIFIER>/<VersionNumber>/data
        File downloadDirectory = new File(AppEnvironment.getExternalPublicDownloadDirectory(),
                AppUpdater.APP_ID + "/" + versionNumber + "/data");
        return FileSystem.getAllFilepathsInDirectory(downloadDirectory);
    }

    /**
     * @param versionNumber
     * @return
     * @throws Exception
     */
    public String getAPKPath(int versionNumber) throws Exception {

        if (!apkVersionPaths.containsKey(versionNumber)) {
            //if we don't have a record of where to find this, we can expect it to be in the downloads folder
            //just like it was when we downloaded new each time.
            File downloadDirectory = new File(AppEnvironment.getExternalPublicDownloadDirectory(),
                    AppUpdater.APP_ID + "/" + versionNumber);
            String path = downloadDirectory + "/" + AppUpdater.APP_ID + ".apk";
            //add it to the hashmap just for completeness
            addNewVersionAPKPath(versionNumber, path);
            return path;
        }

        return apkVersionPaths.get(versionNumber);
    }

    /**
     * Return the directory that all versions of the app are downloaded to
     * @return
     */
    public File getStagingDirectory() {
        return new File(AppEnvironment.getExternalPublicDownloadDirectory(), AppUpdater.APP_ID);
    }

    /**
     * Since we don't copy the apk's every time there is an update if the one needed already exists
     * on the device in a previous version, we need to have a place for the installer to find the
     * path it needs for the version it's supposed to be installing. Here we can add a version
     * after the downloader parses the manifest and knows where to get it from.
     * @param version
     * @param path
     */
    public void addNewVersionAPKPath(int version, String path) {
        apkVersionPaths.put(version, path);
    }
}
