package com.rsg.rsgportal;

import java.util.List;

/**
 * Class to deserialise manifest from server into.
 *
 * Created by rorydungan on 9/03/2016.
 */
public class RemoteApplicationManifest {

    private int CurrentVersion;

    private List<Integer> AvailableVersions;

    public int getCurrentVersion() {
        return CurrentVersion;
    }

    public List<Integer> getAvailableVersions() {
        return AvailableVersions;
    }
}
