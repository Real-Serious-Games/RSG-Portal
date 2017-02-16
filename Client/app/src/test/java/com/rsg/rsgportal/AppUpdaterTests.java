package com.rsg.rsgportal;

import junit.framework.Assert;

import org.jdeferred.impl.DeferredObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

/**
 * Created by adamsingle on 19/01/2016.
 */
public class AppUpdaterTests {

    @Mock
    private RemoteManifestDownloader mockRemoteManifestDownloader;

    @Mock
    private LocalStagingDeserialiser mockLocalStagingDeserialiser;

    @Mock
    private IAppDownloader mockAppDownloader;

    @Mock
    private IAppInstaller mockAppInstaller;

    private AppUpdater testObject;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);

        when(mockRemoteManifestDownloader.getDataManifest(anyInt()))
            .thenReturn(new DeferredObject<DataManifest, Exception, Void>().resolve(mock(DataManifest.class)));

        testObject = new AppUpdater();
        testObject.RemoteManifestDownloader = mockRemoteManifestDownloader;
        testObject.LocalStagingDeserialiser = mockLocalStagingDeserialiser;
        testObject.ApplicationDownloader = mockAppDownloader;
        testObject.AppInstaller = mockAppInstaller;
    }

    @Test
    public void fails_callback_when_checking_for_updates_errors_out()
            throws Exception {

        final String testExceptionMessage = "Error: Failed to retrieve the manifest from the server.";

        //Mock out the RemoteManifestDownloader to fail the callback passed in
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                GenericCallback callback = (GenericCallback) args[0];
                callback.fail(new Exception(testExceptionMessage));
                return null;
            }
        })
        .when(mockRemoteManifestDownloader)
        .getLatestApplicationManifest(isA(GenericCallback.class));

        GenericCallback<RemoteApplicationManifest> mockCheckUpdatesCallback = mock(GenericCallback.class);

        testObject.checkForUpdates(mockCheckUpdatesCallback);

        verify(mockCheckUpdatesCallback, times(1)).fail(isA(Exception.class));
    }

    @Test
    public void sends_app_manifest_from_server() {
        final int testVersionNumber = 1;

        final RemoteApplicationManifest mockManifest = mock(RemoteApplicationManifest.class);
        when(mockManifest.getCurrentVersion())
            .thenReturn(testVersionNumber);

        //mock out the LocalStagingDeserialiser to return the current version number

        when(mockLocalStagingDeserialiser.getLocalApplicationVersion())
                .thenReturn(testVersionNumber);

        //mock out the RemoteManifestDownloader to return the testManifest through the callback passed to it
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                GenericCallback callback = (GenericCallback) args[0];
                callback.success(mockManifest);
                return null;
            }
        })
        .when(mockRemoteManifestDownloader)
        .getLatestApplicationManifest(isA(GenericCallback.class));

        //mock out the callback that will be passed to us from the UI when Check For Updates is pressed.
        GenericCallback<RemoteApplicationManifest> mockCheckUpdatesCallback = mock(GenericCallback.class);

        testObject.checkForUpdates(mockCheckUpdatesCallback);

        //Verify that under these conditions we recognise no update is needed and send that back through
        //the UI's callback.
        verify(mockCheckUpdatesCallback, times(1))
                .success(mockManifest);
    }

    @Test
    public void updates_known_remote_manifest_when_check_for_version_is_successful() {
        final int testRemoteVersionNumber = 2;

        final RemoteApplicationManifest mockManifest = mock(RemoteApplicationManifest.class);
        when(mockManifest.getCurrentVersion())
            .thenReturn(testRemoteVersionNumber);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                GenericCallback callback = (GenericCallback) args[0];
                callback.success(mockManifest);
                return null;
            }
        })
        .when(mockRemoteManifestDownloader)
        .getLatestApplicationManifest(isA(GenericCallback.class));

        testObject.checkForUpdates(mock(GenericCallback.class));

        Assert.assertEquals(mockManifest, testObject.getKnownRemoteAppManifest());
    }

    @Test
    public void fails_download_callback_on_appDownloader_error() {
        final Exception testException = new Exception("Error downloading application");

        //mock out the AppDownloader error
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                GenericProgressFeedbackCallback callback = (GenericProgressFeedbackCallback) args[3];
                callback.fail(testException);
                return null;
            }
        })
        .when(mockAppDownloader)
        .downloadAppUpdates(isA(Integer.class), isA(DataManifest.class), isA(GenericCallback.class), isA(GenericProgressFeedbackCallback.class));

        //mock out the callback we were passed
        ProgressFeedbackCallback mockDownloadCallback = mock(ProgressFeedbackCallback.class);

        testObject.downloadUpdates(1, mock(GenericCallback.class), mockDownloadCallback);

        verify(mockDownloadCallback, times(1))
                .fail(testException);
    }

    @Test
    public void succeeds_download_callback_on_appdownloader_success() {

        List<String> files = new ArrayList<>();
        //mock out the success callback on the AppDownloader
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                GenericProgressFeedbackCallback progressCallback = (GenericProgressFeedbackCallback)args[3];
                progressCallback.success(files);
                return null;
            }
        })
        .when(mockAppDownloader)
        .downloadAppUpdates(isA(Integer.class), isA(DataManifest.class), isA(GenericCallback.class), isA(GenericProgressFeedbackCallback.class));

        //mock out the progress callback passed into the Updater
        ProgressFeedbackCallback mockProgressCallback = mock(ProgressFeedbackCallback.class);

        testObject.downloadUpdates(1, mock(GenericCallback.class), mockProgressCallback);

        verify(mockProgressCallback, times(1))
            .success();
    }

    //TODO: Test that the files returned from the download are passed to the install

    @Test
    public void progress_is_passed_to_progress_callback() {
        final float testProgress = 0.5f;

        //mock out the progress callback
        //This doesn't mock accurately the asynchronous nature since this will fire a
        //50% progress as soon as the function is called, but we only need to test
        //that the Updater responds appropriately when that progress is fed back
        //to it, no matter when that might be.
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                GenericProgressFeedbackCallback progressCallback = (GenericProgressFeedbackCallback)args[3];
                progressCallback.progress(testProgress);
                return null;
            }
        })
        .when(mockAppDownloader)
        .downloadAppUpdates(isA(Integer.class), isA(DataManifest.class), isA(GenericCallback.class), isA(GenericProgressFeedbackCallback.class));

        //mock out the progress callback passed into the Updater
        ProgressFeedbackCallback mockProgressCallback = mock(ProgressFeedbackCallback.class);

        testObject.downloadUpdates(1, mock(GenericCallback.class), mockProgressCallback);

        verify(mockProgressCallback, times(1))
           .progress(testProgress);
    }

    @Test
    public void progress_less_than_0_is_capped_before_passing_on() {
        final float testProgress = -0.5f;

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                GenericProgressFeedbackCallback progressCallback = (GenericProgressFeedbackCallback) args[3];
                progressCallback.progress(testProgress);
                return null;
            }
        })
        .when(mockAppDownloader)
        .downloadAppUpdates(isA(Integer.class), isA(DataManifest.class), isA(GenericCallback.class), isA(GenericProgressFeedbackCallback.class));

        //mock out the progress callback passsed into the Updater
        ProgressFeedbackCallback mockProgressCallback = mock(ProgressFeedbackCallback.class);

        testObject.downloadUpdates(1, mock(GenericCallback.class), mockProgressCallback);

        verify(mockProgressCallback, times(1))
            .progress(0f);
    }

    @Test
    public void progress_greater_than_1_is_capped_before_passing_on() {
        final float testProgress = 1.5f;

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                GenericProgressFeedbackCallback progressCallback = (GenericProgressFeedbackCallback)args[3];
                progressCallback.progress(testProgress);
                return null;
            }
        })
        .when(mockAppDownloader)
        .downloadAppUpdates(isA(Integer.class), isA(DataManifest.class), isA(GenericCallback.class), isA(GenericProgressFeedbackCallback.class));

        //mock out the progress callback passed into the Updater
        ProgressFeedbackCallback mockProgressCallback = mock(ProgressFeedbackCallback.class);

        testObject.downloadUpdates(1, mock(GenericCallback.class), mockProgressCallback);

        verify(mockProgressCallback, times(1))
            .progress(1f);
    }

    @Test
    public void fails_size_callback_on_app_downloader_size_callback_error() {

        final Exception testException = new Exception("Failed to retrieve the size of the download");

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                GenericCallback<Integer> sizeCallback = (GenericCallback<Integer>) args[2];
                sizeCallback.fail(testException);
                return null;
            }
        })
        .when(mockAppDownloader)
        .downloadAppUpdates(isA(Integer.class), isA(DataManifest.class), isA(GenericCallback.class), isA(GenericProgressFeedbackCallback.class));

        //mock out the download size callback passed into Updater
        GenericCallback<Integer> mockSizeCallback = mock(GenericCallback.class);

        testObject.downloadUpdates(1, mockSizeCallback, mock(ProgressFeedbackCallback.class));

        verify(mockSizeCallback, times(1))
            .fail(testException);
    }

    @Test
    public void size_is_passed_up_on_app_downloader_callback_success() {
        final int testDownloadSize = 1000;

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                GenericCallback<Integer> sizeCallback = (GenericCallback<Integer>) args[2];
                sizeCallback.success(testDownloadSize);
                return null;
            }
        })
        .when(mockAppDownloader)
        .downloadAppUpdates(isA(Integer.class), isA(DataManifest.class), isA(GenericCallback.class), isA(GenericProgressFeedbackCallback.class));

        //mock out the download size callback passed into Updater
        GenericCallback<Integer> mockSizeCallback = mock(GenericCallback.class);

        testObject.downloadUpdates(1, mockSizeCallback, mock(ProgressFeedbackCallback.class));

        verify(mockSizeCallback, times(1))
            .success(testDownloadSize);
    }

    @Test
    public void size_of_0_is_handled_as_a_fail(){

        final int testDownloadSize = 0;

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                GenericCallback<Integer> sizeCallback = (GenericCallback<Integer>)args[2];
                sizeCallback.success(testDownloadSize);
                return null;
            }
        })
        .when(mockAppDownloader)
        .downloadAppUpdates(isA(Integer.class), isA(DataManifest.class), isA(GenericCallback.class), isA(GenericProgressFeedbackCallback.class));

        //mock out the download size callback passed to Updater
        GenericCallback<Integer> mockSizeCallback = mock(GenericCallback.class);

        testObject.downloadUpdates(1, mockSizeCallback, mock(ProgressFeedbackCallback.class));

        verify(mockSizeCallback, times(1))
            .fail(isA(Exception.class));
    }

    @Test
    public void size_less_than_0_is_handled_as_a_fail(){

        final int testDownloadSize = -1000;

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                GenericCallback<Integer> sizeCallback = (GenericCallback<Integer>)args[2];
                sizeCallback.success(testDownloadSize);
                return null;
            }
        })
        .when(mockAppDownloader)
        .downloadAppUpdates(isA(Integer.class), isA(DataManifest.class), isA(GenericCallback.class), isA(GenericProgressFeedbackCallback.class));

        //mock out the download size callback passed to Updater
        GenericCallback<Integer> mockSizeCallback = mock(GenericCallback.class);

        testObject.downloadUpdates(1, mockSizeCallback, mock(ProgressFeedbackCallback.class));

        verify(mockSizeCallback, times(1))
                .fail(isA(Exception.class));
    }

    @Test
    public void fail_install_callback_if_app_install_fails_progress_callback() {
        final Exception testException = new Exception("Failed to install application");

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                GenericProgressFeedbackCallback<Boolean> progressCallback = (GenericProgressFeedbackCallback<Boolean>)args[2];
                progressCallback.fail(testException);
                return null;
            }
        })
        .when(mockAppInstaller)
        .installApp(isA(Integer.class), isA(List.class), isA(GenericProgressFeedbackCallback.class));

        //mock out the passed in callback
        GenericProgressFeedbackCallback<Boolean> mockProgressCallback = mock(GenericProgressFeedbackCallback.class);

        testObject.installApp(1, mockProgressCallback);

        verify(mockProgressCallback, times(1))
            .fail(testException);
    }

    @Test
    public void success_from_installer_succeeds_progress_callback() {

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                GenericProgressFeedbackCallback<Boolean> progressCallback = (GenericProgressFeedbackCallback<Boolean>)args[2];
                progressCallback.success(true);
                return null;
            }
        })
        .when(mockAppInstaller)
        .installApp(isA(Integer.class), isA(List.class), isA(GenericProgressFeedbackCallback.class));

        //mock out the passed in callback
        GenericProgressFeedbackCallback<Boolean> mockProgressCallback = mock(GenericProgressFeedbackCallback.class);

        testObject.installApp(1, mockProgressCallback);

        verify(mockProgressCallback, times(1))
                .success(true);
    }

    @Test
    public void progress_from_installer_is_sent_to_progress_callback() {
        final float testProgress = 0.5f;

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                GenericProgressFeedbackCallback<Boolean> progressCallback = (GenericProgressFeedbackCallback)args[2];
                progressCallback.progress(testProgress);
                return null;
            }
        })
                .when(mockAppInstaller)
                .installApp(isA(Integer.class), isA(List.class), isA(GenericProgressFeedbackCallback.class));

        //mock out the passed in callback
        GenericProgressFeedbackCallback<Boolean> mockProgressCallback = mock(GenericProgressFeedbackCallback.class);

        testObject.installApp(1, mockProgressCallback);

        verify(mockProgressCallback, times(1))
                .progress(testProgress);
    }

    @Test
    public void install_progress_less_than_0_is_capped_before_passing_on() {
        final float testProgress = -0.5f;

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                GenericProgressFeedbackCallback<Boolean> progressCallback = (GenericProgressFeedbackCallback<Boolean>) args[2];
                progressCallback.progress(testProgress);
                return null;
            }
        })
        .when(mockAppInstaller)
        .installApp(isA(Integer.class), isA(List.class), isA(GenericProgressFeedbackCallback.class));

        //mock out the progress callback passsed into the Updater
        GenericProgressFeedbackCallback<Boolean> mockProgressCallback = mock(GenericProgressFeedbackCallback.class);

        testObject.installApp(1, mockProgressCallback);

        verify(mockProgressCallback, times(1))
                .progress(0f);
    }

    @Test
    public void install_progress_greater_than_1_is_capped_before_passing_on() {
        final float testProgress = 1.5f;

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                GenericProgressFeedbackCallback<Boolean> progressCallback = (GenericProgressFeedbackCallback<Boolean>)args[2];
                progressCallback.progress(testProgress);
                return null;
            }
        })
        .when(mockAppInstaller)
        .installApp(isA(Integer.class), isA(List.class), isA(GenericProgressFeedbackCallback.class));

        //mock out the progress callback passed into the Updater
        GenericProgressFeedbackCallback<Boolean> mockProgressCallback = mock(GenericProgressFeedbackCallback.class);

        testObject.installApp(1, mockProgressCallback);

        verify(mockProgressCallback, times(1))
                .progress(1f);
    }

    @Test
    public void after_successful_installation_local_version_number_is_adjusted() throws Exception{
        final int remoteVersionNumber = 3;

        final RemoteApplicationManifest mockManifest = mock(RemoteApplicationManifest.class);
        when(mockManifest.getCurrentVersion())
                .thenReturn(remoteVersionNumber);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                GenericCallback callback = (GenericCallback)args[0];
                callback.success(mockManifest);
                return null;
            }
        })
        .when(mockRemoteManifestDownloader)
        .getLatestApplicationManifest(isA(GenericCallback.class));

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                GenericProgressFeedbackCallback<Boolean> progressCallback = (GenericProgressFeedbackCallback<Boolean>) args[2];
                progressCallback.success(true);
                return null;
            }
        })
        .when(mockAppInstaller)
        .installApp(isA(Integer.class), isA(List.class), isA(GenericProgressFeedbackCallback.class));

        testObject.checkForUpdates(mock(GenericCallback.class));

        testObject.installApp(remoteVersionNumber, mock(GenericProgressFeedbackCallback.class));

        verify(mockLocalStagingDeserialiser, times(1))
                .updateLocalAppVersionNumber(remoteVersionNumber);
    }
}
