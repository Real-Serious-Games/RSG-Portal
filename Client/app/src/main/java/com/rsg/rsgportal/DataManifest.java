package com.rsg.rsgportal;

import java.util.List;

/**
 * Created by rorydungan on 2/03/2016.
 *
 * Definition for deserialising data manifest file
 */
public class DataManifest {
    private String destination;

    private List<DataFileDefinition> files;

    /**
     * The version of the apk that needs to be installed with this version of the application
     */
    private int apkVersionCode;

    /**
     * The size in bytes of the apk
     */
    private int apkSize;

    /**
     * The md5 generated checksum for the apk
     */
    private String apkmd5;

    public String getDestination() {
        return destination;
    }

    public List<DataFileDefinition> getFileDefinitions() {
        return files;
    }

    /**
     * return the "apkVersionCode" property deserialised from the manifest. Used to verify the
     * version of the APK that should be installed.
     * @return
     */
    public int requiredApkVersion() {
        return apkVersionCode;
    }

    /**
     * Return the "apkSize" property deserialised from the manifest.
     * @return
     */
    public int getApkSize() {
        return apkSize;
    }

    /**
     * Return the "apkmd5" property deserialised from the manifest.
     * @return
     */
    public String apkChecksum() {
        return apkmd5;
    }

    /**
     * Definition for deserialising data manifest file
     */
    public class DataFileDefinition {

        /**
         * the name of the file
         */
        private String source;

        /**
         * File size in bytes
         */
        private int size;

        /**
         * The md5 generated checksum for the file
         */
        private String md5;

        /**
         * Return the "source" property deserialised from the manifest
         * @return
         */
        public String filename() {
            return source;
        }

        /**
         * Return the "size" property deserialised from the manifest
         * @return
         */
        public int fileSize() {
            return size;
        }

        /**
         * Return the "md5" property deserialised from the manifest
         * @return
         */
        public String checksum() {
            return md5;
        }
    }
}
