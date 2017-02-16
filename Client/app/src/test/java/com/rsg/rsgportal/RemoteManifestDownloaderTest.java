package com.rsg.rsgportal;

import com.google.gson.Gson;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.Mockito.*;

/**
 * Created by PhilipWarren on 13/01/2016.
 */
public class RemoteManifestDownloaderTest {

    private Http mockHttp;

    private GsonDeserialiser mockGson;

    private RemoteManifestDownloader testObject;

    @Before
    public void init() {

        mockHttp = mock(Http.class);
        mockGson = mock(GsonDeserialiser.class);

        testObject = new RemoteManifestDownloader();
        testObject.Http = mockHttp;
        testObject.Gson = mockGson;

    }

    @Test
    public void calls_fail_callback_when_no_manifest_available()
        throws Exception {

        final Exception testException = new Exception("Could not download file.");

        doAnswer(new Answer<Void>() {
                @Override
                public Void answer(InvocationOnMock invocation) throws Throwable {
                GenericCallback genericCallback = (GenericCallback)invocation.getArguments()[1];
                genericCallback.fail(testException);
                return null;
                }
            })
            .when(mockHttp).downloadFile(anyString(), isA(GenericCallback.class));

        GenericCallback mockGenericCallback = mock(GenericCallback.class);

        testObject.getLatestPortalManifest(mockGenericCallback);

        verify(mockGenericCallback, never()).success(any(PortalManifest.class));
        verify(mockGenericCallback, times(1)).fail(testException);
    }

    @Test
    public void deserializes_a_manifest_from_http()
        throws Exception {

        final int testVersionNumber = 5;
        PortalManifest testManifest = new PortalManifest();
        testManifest.VersionNumber = testVersionNumber;
        final String testSerializedManifest = new Gson().toJson(testManifest);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                GenericCallback genericCallback = (GenericCallback)invocation.getArguments()[1];
                genericCallback.success(testSerializedManifest.getBytes());
                return null;
                }
            })
            .when(mockHttp).downloadFile(anyString(), isA(GenericCallback.class));

        GenericCallback mockGenericCallback = mock(GenericCallback.class);

        doAnswer(new Answer<Void>() {
                @Override
                public Void answer(InvocationOnMock invocation) throws Throwable {
                    PortalManifest manifest = (PortalManifest)invocation.getArguments()[0];
                    Assert.assertEquals(testVersionNumber, manifest.VersionNumber);
                    return null;
                }
            })
            .when(mockGenericCallback).success(isA(PortalManifest.class));

        testObject.getLatestPortalManifest(mockGenericCallback);

        verify(mockGenericCallback, never()).fail(any(Exception.class));
        verify(mockGenericCallback, times(1)).success(any(PortalManifest.class));
    }

    // Todo: Test that when deserialize fails the fail callback is called and not the success.
}
