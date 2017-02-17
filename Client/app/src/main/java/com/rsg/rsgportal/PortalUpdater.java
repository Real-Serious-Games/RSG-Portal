package com.rsg.rsgportal;

import android.support.design.widget.Snackbar;

/**
 * Created by PhilipWarren on 11/01/2016.
 */
public class PortalUpdater {

    public RemoteManifestDownloader RemoteManifestDownloader;

    public AppDownloader AppDownloader;

    public ApkInstaller ApkInstaller;

    public void update(final GenericCallback<Boolean> checkCallback) {

        RemoteManifestDownloader.getLatestPortalManifest(new GenericCallback<PortalManifest>() {
            @Override
            public void success(final PortalManifest manifest) {

                if (manifest.VersionNumber > PortalAppInfo.AppVersion) {
                    // TODO Rory Dungan 17/02/17: This should come from a string resource.
                    Notify.Instance().post("There is a new version of the Portal app downloading now.");
                    //notify the caller that the check is complete and the portal app is updating
                    //itself.
                    AppDownloader.downloadPortalApp(manifest, new Callback() {
                        @Override
                        public void success() {
                            checkCallback.success(true);
                            ApkInstaller.install(manifest.ApkName);
                        }

                        @Override
                        public void fail(Exception e) {
                            //pass the failure up to the caller.
                            checkCallback.fail(e);
                            // Todo: Test the fail code path.
                        }
                    });
                }
                else {
                    //notify the caller that the check is complete and there is no update.
                    checkCallback.success(false);
                }
            }

            @Override
            public void fail(Exception e) {
                Notify.Instance().post(e.getMessage(), Snackbar.LENGTH_INDEFINITE);
                checkCallback.fail(e);
            }
        });
    }
}
