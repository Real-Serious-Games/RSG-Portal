package com.rsg.rsgportal;

import java.util.List;

/**
 * Created by adamsingle on 21/01/2016.
 */
public interface IAppDownloader {

    void downloadPortalApp(final PortalManifest manifest, final Callback callback);

    void downloadAppUpdates(final int version,
                            final DataManifest dataManifest,
                            final GenericCallback<Integer> downloadSizeCallback,
                            final GenericProgressFeedbackCallback<List<String>> progressCallback);
}
