package com.rsg.rsgportal;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Pair;

import org.jdeferred.impl.DeferredObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by adamsingle on 22/01/2016.
 */
public class AppInstallerTests {

    private AppInstaller testObject;

    @Mock
    private FileSystem mockFileSystem;

    @Mock
    private LocalStagingDeserialiser mockLocalStagingDeserialiser;

    @Mock
    private AppEnvironment mockAppEnvironment;

    @Mock
    private ApkInstaller mockApkInstaller;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);

        testObject = new AppInstaller();
        testObject.FileSystem = mockFileSystem;
        testObject.LocalStagingDeserialiser = mockLocalStagingDeserialiser;
        testObject.AppEnvironment = mockAppEnvironment;
        testObject.ApkInstaller = mockApkInstaller;

        PortalAppInfo.AppContext = mock(Context.class);
    }

    /**
     * TODO when this is not version specific, this needs to not auto fail, but verify if there should be data
     */
    @Test
    public void fails_callback_if_local_staging_deserialiser_returns_no_filepaths() throws Exception {

        int testVersionNumber = 2;

        List<String> testFilepaths = Arrays.asList();

        List<String> testLocalFilepaths = new ArrayList<>();

        when(mockLocalStagingDeserialiser.getAppDataFilepaths(testVersionNumber))
            .thenReturn(testFilepaths);

        GenericProgressFeedbackCallback<Boolean> mockProgressCallback = mock(GenericProgressFeedbackCallback.class);

        testObject.installApp(testVersionNumber, testLocalFilepaths, mockProgressCallback);

        verify(mockProgressCallback, times(1))
            .fail(isA(Exception.class));
    }

    @Test
    public void passes_exception_up_callback_if_local_staging_deserialiser_throws_an_error() throws Exception {
        int testVersionNumber = 2;

        Exception testException = new Exception("fail");

        List<String> testLocalFilepaths = new ArrayList<>();


        when(mockLocalStagingDeserialiser.getAppDataFilepaths(testVersionNumber))
            .thenThrow(testException);

        GenericProgressFeedbackCallback<Boolean> mockProgressCallback = mock(GenericProgressFeedbackCallback.class);

        testObject.installApp(testVersionNumber, testLocalFilepaths, mockProgressCallback);

        verify(mockProgressCallback, times(1))
            .fail(testException);
    }

    @Test
    public void copies_files_to_correct_folder() throws Exception{
        int testVersionNumber = 2;

        String filename1 = "filenameone";
        String filename2 = "filenametwo";

        List<String> testFilepaths = Arrays.asList(filename1, filename2);

        List<String> testLocalFilepaths = new ArrayList<>();

        when(mockLocalStagingDeserialiser.getAppDataFilepaths(testVersionNumber))
            .thenReturn(testFilepaths);

        when(mockAppEnvironment.getExternalStorageDirectory())
            .thenReturn(new File("public"));

        GenericProgressFeedbackCallback<Boolean> mockProgressCallback = mock(GenericProgressFeedbackCallback.class);

        testObject.installApp(testVersionNumber, testLocalFilepaths, mockProgressCallback);

        //not sure how to tell that the correct file names were passed across
        verify(mockFileSystem, times(2))
            .copyFile(isA(File.class), isA(File.class));

        verify(mockProgressCallback, times(1))
           .progress(0.5f);

        verify(mockProgressCallback, times(1))
            .progress(1f);
    }

    @Test
    public void apk_install_is_passed_on_to_apk_installer() throws Exception{
        int testVersionNumber = 2;

        String testDataFile = "filename";
        List<String> testFilePaths = Arrays.asList(testDataFile);

        List<String> testLocalFilepaths = new ArrayList<>();

        when(mockLocalStagingDeserialiser.getAppDataFilepaths(testVersionNumber))
            .thenReturn(testFilePaths);

        when(mockAppEnvironment.getExternalStorageDirectory())
            .thenReturn(new File("public"));

        String testAPKName = "apk.apk";
        when(mockLocalStagingDeserialiser.getAPKPath(testVersionNumber))
            .thenReturn(testAPKName);

        when(mockFileSystem.copyFile(isA(File.class), isA(File.class)))
                .thenReturn(new DeferredObject<Void, Exception, Pair<Integer, Integer>>().resolve(null).promise());

        PackageManager mockPackageManager = mock(PackageManager.class);
        when(PortalAppInfo.AppContext.getPackageManager())
                .thenReturn(mockPackageManager);

        final String testPackageName = "com.foo.bar";

        PackageInfo mockPackageInfo = mock(PackageInfo.class);
        mockPackageInfo.packageName = testPackageName;
        mockPackageInfo.versionCode += 1;

        when(mockPackageManager.getPackageArchiveInfo(testAPKName, 0))
                .thenReturn(mockPackageInfo);

        when(mockPackageManager.getPackageInfo(testPackageName, 0))
                .thenReturn(mock(PackageInfo.class));

        GenericProgressFeedbackCallback<Boolean> mockProgressCallback = mock(GenericProgressFeedbackCallback.class);

        testObject.installApp(testVersionNumber, testLocalFilepaths, mockProgressCallback);

        verify(mockApkInstaller, times(1))
            .installFromFullpath(testAPKName);
    }
}
