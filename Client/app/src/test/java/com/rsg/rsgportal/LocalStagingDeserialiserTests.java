package com.rsg.rsgportal;

import com.google.gson.Gson;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;

/**
 * Created by rorydungan on 20/01/2016.
 */
public class LocalStagingDeserialiserTests {

    @Mock
    private FileSystem mockFileSystem;

    @Mock
    private AppEnvironment mockAppEnvironment;

    LocalStagingDeserialiser testObject;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);

        testObject = new LocalStagingDeserialiser();
        testObject.FileSystem = mockFileSystem;
        testObject.AppEnvironment = mockAppEnvironment;
    }

    @Test
    public void check_local_version_returns_existing_version_from_manifest_file() throws Exception {
        int expectedVersion = 1;
        mockManifestLoading(expectedVersion);

        int actualVersion = testObject.getLocalApplicationVersion();

        Assert.assertEquals(expectedVersion, actualVersion);
    }

    /**
     * setup mock manifest
     * @throws IOException
     */
    private void mockManifestLoading(int versionNumber) throws IOException {
        String expectedManifest = "{ \"VersionNumber\": " + versionNumber + "}";

        when(mockFileSystem.loadFile("com.RSG.MyApp/manifest"))
                .thenReturn(expectedManifest.getBytes());
    }

    @Test
    public void check_local_version_returns_zero_when_no_manifest_is_present() throws Exception {
        when(mockFileSystem.loadFile("com.RSG.MyApp/manifest"))
            .thenThrow(new IOException());

        int actualVersion = testObject.getLocalApplicationVersion();

        Assert.assertEquals(0, actualVersion);
    }

    @Test
    public void can_save_new_local_version_number() throws Exception {
        int newVersionNumber = 2;

        LocalApplicationManifest expectedManifest = new LocalApplicationManifest();
        expectedManifest.setVersionNumber(newVersionNumber);
        byte[] expectedByteArray = new Gson().toJson(expectedManifest).getBytes();

        testObject.updateLocalAppVersionNumber(newVersionNumber);

        //No easy way to test what is being sent to save though
        verify(mockFileSystem, times(1))
            .saveFile(isA(String.class), eq(expectedByteArray));
    }

    @Test
    public void can_get_data_filepaths() throws Exception {
        int versionNumber = 1;

        File testDownloadsDir = new File("downloads");

        String testFilename1 = "testOne";
        String testFilename2 = "testTwo";

        List<String> testFilePaths = Arrays.asList(testFilename1, testFilename2);

        when(mockAppEnvironment.getExternalPublicDownloadDirectory())
                .thenReturn(testDownloadsDir);

        //Can't mock this out properly because it is new'd up in the function...
        when(mockFileSystem.getAllFilepathsInDirectory(isA(File.class)))
            .thenReturn(testFilePaths);

        List<String> actualFilePaths = testObject.getAppDataFilepaths(versionNumber);

        Assert.assertEquals(testFilePaths.get(0), actualFilePaths.get(0));
    }

    @Test
    public void can_intall_apk() throws Exception {
        //TODO
    }
}