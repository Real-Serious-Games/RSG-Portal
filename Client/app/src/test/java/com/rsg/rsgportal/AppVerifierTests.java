package com.rsg.rsgportal;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.google.gson.Gson;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

/**
 * Created by rorydungan on 2/03/2016.
 */
public class AppVerifierTests {

    private AppVerifier testObject;

    private int calls;

    @Mock
    PackageManager mockPackageManager;

    @Mock
    MD5Checker mockMd5Checker;

    @Mock
    AppEnvironment mockAppEnvironment;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);

        calls = 0;

        testObject = new AppVerifier();
        testObject.PackageManager = mockPackageManager;
        testObject.MD5Checker = mockMd5Checker;
        testObject.AppEnvironment = mockAppEnvironment;

        when(mockAppEnvironment.getExternalStorageDirectory()).thenReturn(new File("/"));
    }

    @Test
    public void succeeds_if_package_version_is_correct() throws Exception {
        final String packageName = "com.RSG.SomeApp";
        final int expectedVersion = 5;
        final String manifestString =
            "{" +
            "    \"apkVersionCode\": " + expectedVersion + "," +
            "    \"apkSize\": 123," +
            "    \"apkmd5\": \"f00\"," +
            "    \"destination\": \"test\"," +
            "    \"files\": []" +
            "}";

        DataManifest manifest = new Gson().fromJson(manifestString, DataManifest.class);

        PackageInfo packageInfo = new PackageInfo();
        packageInfo.versionCode = expectedVersion;
        packageInfo.packageName = packageName;

        when(mockPackageManager.getPackageInfo(packageName, 0))
            .thenReturn(packageInfo);

        testObject.verifyApp(packageName, manifest)
            .then(valid -> {
                calls++;
                assertEquals(true, valid);
            });

        assertEquals(1, calls);
    }

    @Test
    public void fails_if_package_not_installed() throws Exception {
        final String packageName = "com.RSG.SomeApp";
        final int expectedVersion = 5;
        final String manifestString =
                "{" +
                "    \"apkVersionCode\": " + expectedVersion + "," +
                "    \"apkSize\": 123," +
                "    \"apkmd5\": \"f00\"," +
                "    \"destination\": \"test\"," +
                "    \"files\": []" +
                "}";

        DataManifest manifest = new Gson().fromJson(manifestString, DataManifest.class);

        when(mockPackageManager.getPackageInfo(packageName, 0))
                .thenThrow(new PackageManager.NameNotFoundException());

        testObject.verifyApp(packageName, manifest)
                .then(valid -> {
                    calls++;
                    assertEquals(false, valid);
                });

        assertEquals(1, calls);
    }

    @Test
    public void fails_if_version_code_too_low() throws Exception {
        final String packageName = "com.RSG.SomeApp";
        final int expectedVersion = 5;
        final int actualVersion = 3;
        final String manifestString =
                "{" +
                "    \"apkVersionCode\": " + expectedVersion + "," +
                "    \"apkSize\": 123," +
                "    \"apkmd5\": \"f00\"," +
                "    \"destination\": \"test\"," +
                "    \"files\": []" +
                "}";

        DataManifest manifest = new Gson().fromJson(manifestString, DataManifest.class);

        PackageInfo packageInfo = new PackageInfo();
        packageInfo.versionCode = actualVersion;
        packageInfo.packageName = packageName;

        when(mockPackageManager.getPackageInfo(packageName, 0))
                .thenReturn(packageInfo);

        testObject.verifyApp(packageName, manifest)
                .then(valid -> {
                    calls++;
                    assertEquals(false, valid);
                });

        assertEquals(1, calls);
    }

    @Test
    public void fails_if_version_code_too_high() throws Exception {
        final String packageName = "com.RSG.SomeApp";
        final int expectedVersion = 5;
        final int actualVersion = 7;
        final String manifestString =
                "{" +
                "    \"apkVersionCode\": " + expectedVersion + "," +
                "    \"apkSize\": 123," +
                "    \"apkmd5\": \"f00\"," +
                "    \"destination\": \"test\"," +
                "    \"files\": []" +
                "}";

        DataManifest manifest = new Gson().fromJson(manifestString, DataManifest.class);

        PackageInfo packageInfo = new PackageInfo();
        packageInfo.versionCode = actualVersion;
        packageInfo.packageName = packageName;

        when(mockPackageManager.getPackageInfo(packageName, 0))
                .thenReturn(packageInfo);

        testObject.verifyApp(packageName, manifest)
                .then(valid -> {
                    calls++;
                    assertEquals(false, valid);
                });

        assertEquals(1, calls);
    }

    @Test
    public void succeeds_if_data_file_md5_is_correct() throws Exception {
        final String packageName = "com.RSG.SomeApp";
        final int expectedVersion = 5;
        final String testFile = "test";
        final String expectedMd5 = "8726b044c56ad948e7dc94d46c9e3c13";
        final String manifestString =
                "{" +
                "    \"apkVersionCode\": " + expectedVersion + "," +
                "    \"apkSize\": 123," +
                "    \"apkmd5\": \"f00\"," +
                "    \"destination\": \"test\"," +
                "    \"files\": [" +
                "       {" +
                "           \"source\": \"" + testFile + "\"," +
                "           \"size\": 1," +
                "           \"md5\": \"" + expectedMd5 + "\"" +
                "       }" +
                "   ]" +
                "}";

        DataManifest manifest = new Gson().fromJson(manifestString, DataManifest.class);

        final File testFileDir = new File("/", manifest.getDestination());
        final String testFilePath = new File(testFileDir, testFile).getAbsolutePath();

        PackageInfo packageInfo = new PackageInfo();
        packageInfo.versionCode = expectedVersion;
        packageInfo.packageName = packageName;

        when(mockPackageManager.getPackageInfo(packageName, 0))
                .thenReturn(packageInfo);

        doAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            GenericProgressFeedbackCallback<Boolean> callback = (GenericProgressFeedbackCallback<Boolean>)args[2];
            callback.success(true);
            return null;
        })
        .when(mockMd5Checker)
        .checkFileMD5(eq(testFilePath), eq(expectedMd5), isA(GenericProgressFeedbackCallback.class));

        testObject.verifyApp(packageName, manifest)
                .then(valid -> {
                    calls++;
                    assertEquals(true, valid);
                });

        assertEquals(1, calls);
    }

    @Test
    public void fails_if_data_file_does_not_exist() throws Exception {
        final String packageName = "com.RSG.SomeApp";
        final int expectedVersion = 5;
        final String testFile1 = "test1";
        final String testFile2 = "test2";
        final String expectedMd5 = "8726b044c56ad948e7dc94d46c9e3c13";
        final String manifestString =
                "{" +
                        "    \"apkVersionCode\": " + expectedVersion + "," +
                        "    \"apkSize\": 123," +
                        "    \"apkmd5\": \"f00\"," +
                        "    \"destination\": \"test\"," +
                        "    \"files\": [" +
                        "       {" +
                        "           \"source\": \"" + testFile1 + "\"," +
                        "           \"size\": 1," +
                        "           \"md5\": \"" + expectedMd5 + "\"" +
                        "       }," +
                        "       {" +
                        "           \"source\": \"" + testFile2 + "\"," +
                        "           \"size\": 1," +
                        "           \"md5\": \"" + expectedMd5 + "\"" +
                        "       }" +
                        "   ]" +
                        "}";

        DataManifest manifest = new Gson().fromJson(manifestString, DataManifest.class);

        final File testFileDir = new File("/", manifest.getDestination());
        final String testFile1Path = new File(testFileDir, testFile1).getAbsolutePath();
        final String testFile2Path = new File(testFileDir, testFile2).getAbsolutePath();

        PackageInfo packageInfo = new PackageInfo();
        packageInfo.versionCode = expectedVersion;
        packageInfo.packageName = packageName;

        when(mockPackageManager.getPackageInfo(packageName, 0))
                .thenReturn(packageInfo);

        doAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            GenericProgressFeedbackCallback<Boolean> callback = (GenericProgressFeedbackCallback<Boolean>)args[2];
            callback.fail(new IOException());
            return null;
        })
        .when(mockMd5Checker)
        .checkFileMD5(eq(testFile1Path), anyString(), isA(GenericProgressFeedbackCallback.class));

        doAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            GenericProgressFeedbackCallback<Boolean> callback = (GenericProgressFeedbackCallback<Boolean>)args[2];
            callback.success(true);
            return null;
        })
        .when(mockMd5Checker)
        .checkFileMD5(eq(testFile2Path), anyString(), isA(GenericProgressFeedbackCallback.class));

        testObject.verifyApp(packageName, manifest)
                .then(valid -> {
                    calls++;
                    assertEquals(false, valid);
                });

        assertEquals(1, calls);
    }

    @Test
    public void fails_if_data_file_md5_does_not_match_manifest() throws Exception {
        final String packageName = "com.RSG.SomeApp";
        final int expectedVersion = 5;
        final String testFile = "test";
        final String expectedMd5 = "8726b044c56ad948e7dc94d46c9e3c13";
        final String manifestString =
                "{" +
                        "    \"apkVersionCode\": " + expectedVersion + "," +
                        "    \"apkSize\": 123," +
                        "    \"apkmd5\": \"f00\"," +
                        "    \"destination\": \"test\"," +
                        "    \"files\": [" +
                        "       {" +
                        "           \"source\": \"" + testFile + "\"," +
                        "           \"size\": 1," +
                        "           \"md5\": \"" + expectedMd5 + "\"" +
                        "       }" +
                        "   ]" +
                        "}";

        DataManifest manifest = new Gson().fromJson(manifestString, DataManifest.class);

        final File testFileDir = new File("/", manifest.getDestination());
        final String testFilePath = new File(testFileDir, testFile).getAbsolutePath();

        PackageInfo packageInfo = new PackageInfo();
        packageInfo.versionCode = expectedVersion;
        packageInfo.packageName = packageName;

        when(mockPackageManager.getPackageInfo(packageName, 0))
                .thenReturn(packageInfo);

        doAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            GenericProgressFeedbackCallback<Boolean> callback = (GenericProgressFeedbackCallback<Boolean>)args[2];
            callback.success(false);
            return null;
        })
        .when(mockMd5Checker)
        .checkFileMD5(eq(testFilePath), eq(expectedMd5), isA(GenericProgressFeedbackCallback.class));

        testObject.verifyApp(packageName, manifest)
                .then(valid -> {
                    calls++;
                    assertEquals(false, valid);
                });

        assertEquals(1, calls);
    }
}
