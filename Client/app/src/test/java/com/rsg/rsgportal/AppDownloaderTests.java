package com.rsg.rsgportal;

import com.google.gson.Gson;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;

import java.io.DataOutput;
import java.io.RandomAccessFile;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.AdditionalMatchers.not;

/**
 * Created by PhilipWarren on 15/01/2016.
 */
public class AppDownloaderTests {

    private  PortalAppInfo testPortalAppInfo;

    private Http mockHttp;
    private FileSystem mockFileSystem;
    private ILocalFileChecker mockFileChecker;
    private LocalStagingDeserialiser mockLocalStagingDeserialiser;

    private AppDownloader testObject;

    private String getBaseUrl() {
        return "http://" + PortalAppInfo.RemoteServerAddress + "/" +
                "v" + PortalAppInfo.ApiVersion + "/android/" + AppUpdater.APP_ID;
    }

    private void setUpDefaultContentLengthCallback() {
        doAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            GenericCallback genericCallback = (GenericCallback<Integer>)args[1];
            genericCallback.success(1);

            return null;
        })
        .when(mockHttp)
        .getContentLength(anyString(), isA(GenericCallback.class));
    }

    private void setUpDefaultDownloadCallbacks() {
        doAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            GenericCallback genericCallback = (GenericCallback<byte[]>)args[1];
            genericCallback.success(new byte[1]);
            return null;
        })
        .when(mockHttp)
        .downloadFile(anyString(), isA(GenericCallback.class), isA(ProgressFeedbackCallback.class));
    }

    /**
     * Make TryGetFileLocally resolve with null to indicate that the files couldn't be found locally.
     */
    private void setUpDefaultTryGetfileLocallyCallbacks() {
        doAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            GenericCallback<String> callback = (GenericCallback<String>)args[1];
            callback.success(null);
            return null;
        })
        .when(mockFileChecker)
        .TryGetFileLocally(isA(DataManifest.DataFileDefinition.class), isA(GenericCallback.class));

        doAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            GenericCallback<String> callback = (GenericCallback<String>)args[3];
            callback.success(null);
            return null;
        })
        .when(mockFileChecker)
        .TryGetFileLocally(anyString(), anyInt(), anyString(), isA(GenericCallback.class));
    }

    @Before
    public void init() {
        testPortalAppInfo = new PortalAppInfo();

        mockHttp = mock(Http.class);
        mockFileSystem = mock(FileSystem.class);
        mockFileChecker = mock(LocalFileChecker.class);
        mockLocalStagingDeserialiser = mock(LocalStagingDeserialiser.class);

        testObject = new AppDownloader();
        testObject.Http = mockHttp;
        testObject.FileSystem = mockFileSystem;
        testObject.FileChecker = mockFileChecker;
        testObject.LocalStagingDeserialiser = mockLocalStagingDeserialiser;
    }

    @Test
    public void downloads_Apps_Apk_file() throws Exception {

        PortalManifest testPortalManifest = new PortalManifest();
        testPortalManifest.ApkName = "Frank.apk";
        String expectedUrlString =
                "http://" + testPortalAppInfo.RemoteServerAddress + "/" + "v" + PortalAppInfo.ApiVersion + "/" + testPortalManifest.ApkName;

        testObject.downloadPortalApp(testPortalManifest, new Callback() {
            @Override
            public void success() {

            }

            @Override
            public void fail(Exception e) {

            }
        });

        verify(mockHttp, times(1)).downloadFile(Matchers.eq(expectedUrlString), Matchers.<GenericCallback<byte[]>>any());
    }

    @Test
    public void saves_file_when_downloaded() throws Exception {

        PortalManifest testManifest = new PortalManifest();
        testManifest.ApkName = "Frank.apk";
        final byte[] testBytes = "Herbert".getBytes();

        doAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            GenericCallback genericCallback = (GenericCallback<byte[]>)args[1];
            genericCallback.success(testBytes);
            return null;
        })
        .when(mockHttp)
        .downloadFile(anyString(), Matchers.<GenericCallback<byte[]>>any());

        testObject.downloadPortalApp(testManifest, new Callback() {
            @Override
            public void success() {

            }

            @Override
            public void fail(Exception e) {

            }
        });

        verify(mockFileSystem).saveFile(testManifest.ApkName, testBytes);
    }

//    @Test
//    public void progress_callback_returns_success_when_download_complete() {
//
//    }

    @Test
    public void size_callback_returns_total_download_size() {
        final String baseUrl = getBaseUrl();

        final int firstFileSize = 100;
        final int secondFileSize = 200;
        final int manifestFileSize = 50;
        final String firstFileName = baseUrl + "/1/" + AppUpdater.APP_ID + ".apk";
        final String secondFileName = baseUrl + "/1/data/Foo.txt";
        final String dataManifestFileName = baseUrl + "/1/manifest";
        final byte[] dataManifest = (
                "{ " +
                "   files: [ " +
                "       { " +
                "           \"source\": \"Foo.txt\", " +
                "           \"latestVersion\": \"1\"" +
                "       }" +
                "   ]" +
                "}").getBytes();

        DataManifest testManifest = new Gson().fromJson(new String(dataManifest), DataManifest.class);

        setUpDefaultTryGetfileLocallyCallbacks();

        doAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            GenericCallback genericCallback = (GenericCallback<Integer>) args[1];
            genericCallback.success(firstFileSize);

            return null;
        })
        .when(mockHttp)
        .getContentLength(eq(firstFileName), isA(GenericCallback.class));

        doAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            GenericCallback genericCallback = (GenericCallback<Integer>)args[1];
            genericCallback.success(secondFileSize);

            return null;
        })
        .when(mockHttp)
        .getContentLength(eq(secondFileName), isA(GenericCallback.class));

        doAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            GenericCallback genericCallback = (GenericCallback<Integer>)args[1];
            genericCallback.success(manifestFileSize);

            return null;
        })
        .when(mockHttp)
        .getContentLength(eq(dataManifestFileName), isA(GenericCallback.class));

        final int expectedSize = firstFileSize + secondFileSize + manifestFileSize;

        GenericCallback<Integer> mockCallback = mock(GenericCallback.class);

        testObject.downloadAppUpdates(1,
                testManifest,
                mockCallback,
                mock(GenericProgressFeedbackCallback.class));

        verify(mockCallback, times(1))
            .success(expectedSize);
    }

    @Test
    public void downloads_apk_file() throws Exception {
        final String fileName = getBaseUrl() + "/1/" + AppUpdater.APP_ID + ".apk";

        setUpDefaultDownloadCallbacks();

        setUpDefaultContentLengthCallback();

        setUpDefaultTryGetfileLocallyCallbacks();

        when(mockFileSystem.getNewFileStream(anyString()))
                .thenReturn(mock(RandomAccessFile.class));

        testObject.downloadAppUpdates(1,
                mock(DataManifest.class),
                mock(GenericCallback.class),
                mock(GenericProgressFeedbackCallback.class));

        verify(mockFileSystem, times(1))
                .getNewFileStream(AppUpdater.APP_ID + "/1/" + AppUpdater.APP_ID + ".apk");

        verify(mockHttp, times(1))
                .downloadFile(eq(fileName), isA(DataOutput.class), isA(ProgressFeedbackCallback.class));
    }

    @Test
    public void downloads_data_file() throws Exception {
        final String baseUrl = getBaseUrl();

        final String dataFileName = baseUrl + "/1/data/Foo.txt";
        final String dataManifestFileName = baseUrl + "/1/data/manifest";
        final byte[] dataManifest = (
                "{ " +
                "   files: [ " +
                "       { " +
                "           \"source\": \"Foo.txt\", " +
                "           \"size\": \"1\"," +
                "           \"md5\": \"testChecksum\" " +
                "       }" +
                "   ]" +
                "}").getBytes();

        DataManifest testManifest = new Gson().fromJson(new String(dataManifest), DataManifest.class);

        when(mockFileSystem.getNewFileStream(anyString()))
                .thenReturn(mock(RandomAccessFile.class));

        setUpDefaultDownloadCallbacks();

        setUpDefaultContentLengthCallback();

        setUpDefaultTryGetfileLocallyCallbacks();

        testObject.downloadAppUpdates(1,
                testManifest,
                mock(GenericCallback.class),
                mock(GenericProgressFeedbackCallback.class));

        verify(mockFileSystem, times(1))
                .getNewFileStream(AppUpdater.APP_ID + "/1/data/Foo.txt");

        verify(mockHttp, times(1))
                .downloadFile(eq(dataFileName), isA(DataOutput.class), isA(ProgressFeedbackCallback.class));
    }

    @Test
    public void only_downloads_data_files_not_found_locally() throws Exception {

        final String baseUrl = getBaseUrl();

        final String secondFilePath = AppUpdater.APP_ID + "/1/data/AlreadyOnDevice.txt";

        final String dataManifestUrl = baseUrl + "/1/manifest";
        final byte[] dataManifest = (
                "{ " +
                        "   files: [ " +
                        "       { " +
                        "           \"source\": \"FileNotOnDevice.txt\", " +
                        "           \"latestVersion\": \"1\"" +
                        "       }," +
                        "       { " +
                        "           \"source\": \"AlreadyOnDevice.txt\", " +
                        "           \"latestVersion\": \"1\"" +
                        "       }" +
                        "   ]" +
                        "}").getBytes();

        DataManifest testManifest = new Gson().fromJson(new String(dataManifest), DataManifest.class);

        when(mockFileSystem.getNewFileStream(anyString()))
                .thenReturn(mock(RandomAccessFile.class));

        setUpDefaultDownloadCallbacks();

        setUpDefaultContentLengthCallback();

        setUpDefaultTryGetfileLocallyCallbacks();

        doAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            GenericCallback<String> callback = (GenericCallback<String>) args[1];
            callback.success(secondFilePath);
            return null;
        })
        .when(mockFileChecker)
        .TryGetFileLocally(eq(testManifest.getFileDefinitions().get(1)), isA(GenericCallback.class));

        doAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            GenericCallback<String> cb = (GenericCallback<String>)args[3];
            cb.success(AppUpdater.APP_ID + "/1/" + AppUpdater.APP_ID + ".apk");
            return null;
        })
        .when(mockFileChecker)
        .TryGetFileLocally(eq(AppUpdater.APP_ID + ".apk"), anyInt(), anyString(), isA(GenericCallback.class));

        testObject.downloadAppUpdates(1,
                testManifest,
                mock(GenericCallback.class),
                mock(GenericProgressFeedbackCallback.class));

        verify(mockHttp, times(1))
            .downloadFile(not(eq(dataManifestUrl)), isA(DataOutput.class), isA(ProgressFeedbackCallback.class));
    }

    @Test
    public void progress_gets_passed_to_callback() {
        // TODO: Write test
    }

    @Test
    public void progress_callback_returns_success_when_all_downloads_complete() {
        // TODO: Write test
    }

    @Test
    public void progress_callback_fails_if_download_fails() {
        // TODO: Write test
    }
}
