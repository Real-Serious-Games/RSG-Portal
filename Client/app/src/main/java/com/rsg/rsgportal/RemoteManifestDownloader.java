package com.rsg.rsgportal;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.jdeferred.Deferred;
import org.jdeferred.Promise;
import org.jdeferred.impl.DeferredObject;

/**
 * This class will query the remote server for the information on the latest app version.
 *
 * Created by PhilipWarren on 12/01/2016.
 */
public class RemoteManifestDownloader {

    public Http Http;

    public GsonDeserialiser Gson;

    //
    // Gets the latest manifest from the server and return it.
    //
    public void getLatestPortalManifest(final GenericCallback genericCallback) {

        String url = "http://" + PortalAppInfo.RemoteServerAddress + "/" +
                "v" + PortalAppInfo.ApiVersion + "/portal_manifest.json";

        Http.downloadFile(url, new GenericCallback<byte[]>() {

            @Override
            public void success(byte[] downloadedFile) {

                String downloadedString = new String(downloadedFile);
                try {
                    PortalManifest manifest = new Gson().fromJson(downloadedString, PortalManifest.class);

                    genericCallback.success(manifest);
                } catch (JsonSyntaxException ex) {
                    fail(ex);
                }
            }

            @Override
            public void fail(Exception e) {

                genericCallback.fail(e);
            }
        });
    }

    /**
     * Requests the latest manifest for the application from the server
     */
    public void getLatestApplicationManifest(final GenericCallback<RemoteApplicationManifest> callback)
    {
        //TODO: write unit tests for this method
        String url = "http://" + PortalAppInfo.RemoteServerAddress + "/" +
                "v" + PortalAppInfo.ApiVersion + "/android/" + AppUpdater.APP_ID + "/manifest";

        Http.downloadFile(url, new GenericCallback<byte[]>() {
            @Override
            public void success(byte[] data) {
                try {
                    RemoteApplicationManifest manifest = new Gson().fromJson(new String(data), RemoteApplicationManifest.class);
                    callback.success(manifest);
                } catch (JsonSyntaxException ex) {
                    fail(ex);
                }
            }

            @Override
            public void fail(Exception e) {
                callback.fail(e);
            }
        });
    }

    /**
     * Request the data manifest for the specified version of the app from the server.
     * @param version Version to request
     * @return A promise for the asynchronous request.
     */
    public Promise<DataManifest, Exception, Void> getDataManifest(final int version) {
        Deferred<DataManifest, Exception, Void> deferred = new DeferredObject<>();

        final String baseUrl = "http://" + PortalAppInfo.RemoteServerAddress + "/" +
                "v" + PortalAppInfo.ApiVersion + "/android/" + AppUpdater.APP_ID;
        final String fileToDownload = baseUrl + "/" + version + "/manifest";

        Http.downloadFile(fileToDownload, new GenericCallback<byte[]>() {
            @Override
            public void success(byte[] data) {
                // Read the data manifest
                try {
                    DataManifest manifest = readDataManifest(data);
                    deferred.resolve(manifest);
                } catch (Exception e) {
                    deferred.reject(e);
                }
            }

            @Override
            public void fail(Exception e) {
                deferred.reject(e);
            }
        });

        return deferred.promise();
    }

    /**
     * Reads the data manifest and returns a list of DataFileDefinitions it includes.
     */
    private DataManifest readDataManifest(byte[] data) throws JsonSyntaxException {

        DataManifest manifest = Gson.fromJson(new String(data), DataManifest.class);

        return manifest;
    }
}
