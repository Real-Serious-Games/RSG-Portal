package com.rsg.rsgportal;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.Mockito.*;

/**
 * Created by PhilipWarren on 11/01/2016.
 */
public class PortalUpdaterTests {

    private RemoteManifestDownloader mockRemoteManifestDownloader;
    private AppDownloader mockAppDownloader;
    private ApkInstaller mockApkInstaller;

    private PortalUpdater testObject;

    @Before
    public void init() {
        mockRemoteManifestDownloader = mock(RemoteManifestDownloader.class);
        mockAppDownloader = mock(AppDownloader.class);
        mockApkInstaller = mock(ApkInstaller.class);

        testObject = new PortalUpdater();
        testObject.RemoteManifestDownloader = mockRemoteManifestDownloader;
        testObject.AppDownloader = mockAppDownloader;
        testObject.ApkInstaller = mockApkInstaller;
    }

    //TODO Need to test this, but out of the box Mockito can't mock static methods, and hence can't mock Singletons. PowerMock can sit on top of Mockito to do this though

//    @Test
//    public void triggers_notify_with_the_exception_message_when_unable_to_get_update()
//            throws Exception {
//
//        final String testMessage = "TestMessage";
//
//        PortalAppInfo.AppVersion = 1;
//
//        doAnswer(new Answer<Void>() {
//                @Override
//                public Void answer(InvocationOnMock invocation) throws Throwable {
//                    Object[] args = invocation.getArguments();
//                    GenericCallback genericCallback = (GenericCallback)args[0];
//                    genericCallback.fail(new Exception(testMessage));
//                    return null;
//                }
//            })
//            .when(mockRemoteManifestDownloader)
//            .getLatestPortalManifest(isA(GenericCallback.class));
//
//        testObject.update();
//
//        verify(mockNotify, times(1)).post(testMessage);
//    }
//
//    @Test
//    public void does_not_trigger_notification_when_the_latest_update_is_currently_installed()
//            throws Exception {
//
//        PortalAppInfo.AppVersion = 1;
//
//        final PortalManifest testManifest = new PortalManifest();
//        testManifest.VersionNumber = 1;
//
//        doAnswer(new Answer<Void>() {
//                @Override
//                public Void answer(InvocationOnMock invocation) throws Throwable {
//                    Object[] args = invocation.getArguments();
//                    GenericCallback genericCallback = (GenericCallback)args[0];
//                    genericCallback.success(testManifest);
//                    return null;
//                }
//            })
//            .when(mockRemoteManifestDownloader)
//            .getLatestPortalManifest(isA(GenericCallback.class));
//
//        testObject.update();
//
//        verify(mockNotify, never()).post(anyString());
//    }
//
//    @Test
//    public void triggers_notification_if_there_is_an_update_available()
//        throws Exception {
//
//        PortalAppInfo.AppVersion = 1;
//
//        final PortalManifest testManifest = new PortalManifest();
//        testManifest.VersionNumber = 2;
//
//        doAnswer(new Answer<Void>() {
//                @Override
//                public Void answer(InvocationOnMock invocation) throws Throwable {
//                    Object[] args = invocation.getArguments();
//                    GenericCallback genericCallback = (GenericCallback)args[0];
//                    genericCallback.success(testManifest);
//                    return null;
//                }
//            })
//            .when(mockRemoteManifestDownloader)
//            .getLatestPortalManifest(isA(GenericCallback.class));
//
//        testObject.update();
//
//        verify(mockNotify, times(1)).post(anyString());
//    }

    @Test
    public void downloads_app_from_server_when_there_is_a_update()
        throws Exception {

        PortalAppInfo.AppVersion = 1;

        final PortalManifest testManifest = new PortalManifest();
        testManifest.VersionNumber = 2;

        doAnswer(new Answer<Void>() {
                @Override
                public Void answer(InvocationOnMock invocation) throws Throwable {
                    Object[] args = invocation.getArguments();
                    GenericCallback genericCallback = (GenericCallback)args[0];
                    genericCallback.success(testManifest);
                    return null;
                }
            })
            .when(mockRemoteManifestDownloader)
            .getLatestPortalManifest(isA(GenericCallback.class));

        GenericCallback<Boolean> mockCallback = mock(GenericCallback.class);

        testObject.update(mockCallback);

        verify(mockAppDownloader, times(1)).downloadPortalApp(eq(testManifest), isA(Callback.class));
    }

    @Test
    public void installs_the_apk_after_it_is_downloaded()
        throws Exception {

        PortalAppInfo.AppVersion = 1;

        final PortalManifest testManifest = new PortalManifest();
        testManifest.VersionNumber = 2;
        testManifest.ApkName = "Frank";

        doAnswer(new Answer<PortalManifest>() {
            @Override
            public PortalManifest answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                GenericCallback<PortalManifest> genericCallback = (GenericCallback<PortalManifest>)args[0];
                genericCallback.success(testManifest);
                return null;
            }
        })
        .when(mockRemoteManifestDownloader)
        .getLatestPortalManifest(isA(GenericCallback.class));

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                Callback callback = (Callback)args[1];
                callback.success();
                return null;
            }
        })
        .when(mockAppDownloader)
                .downloadPortalApp(eq(testManifest), any(Callback.class));

        GenericCallback<Boolean> mockCallback = mock(GenericCallback.class);

        testObject.update(mockCallback);

        verify(mockApkInstaller, times(1)).install(testManifest.ApkName);
    }
}
