package com.rsg.rsgportal;

import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpMessage;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpHead;

import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A wrapper class for http requests.
 *
 * Created by PhilipWarren on 12/01/2016.
 */
public class Http {

    public void downloadFile(String urlString, GenericCallback<byte[]> genericCallback) {

        URL url;

        try {

            url = new URL(urlString);

        } catch (MalformedURLException e) {

            genericCallback.fail(e);
            return;
        }

        HttpDownloader httpDownloader = new HttpDownloader(genericCallback);
        httpDownloader.execute(url);
    }

    /**
     * Download a file from the specified URL, with a callback for progress.
     * @param urlString URL to download the file from
     * @param dataCallback Callback for returning the data once the download is complete
     * @param progressCallback Callback for returning current progress
     */
    public void downloadFile(String urlString, GenericCallback<byte[]> dataCallback, ProgressFeedbackCallback progressCallback) {

        URL url;

        try {

            url = new URL(urlString);

        } catch (MalformedURLException e) {

            dataCallback.fail(e);
            return;
        }

        HttpDownloader httpDownloader = new HttpDownloader(dataCallback, progressCallback);
        httpDownloader.execute(url);
    }

    /**
     * Download a file from the specified URL, streaming data to the specified data output as it is
     * downloaded.
     * @param urlString URL to download from
     * @param output Stream to write data to
     * @param callback
     */
    public void downloadFile(String urlString, DataOutput output, ProgressFeedbackCallback callback) {

        URL url;

        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            callback.fail(e);
            return;
        }

        HttpStreamDownloader httpDownloader = new HttpStreamDownloader(output, callback);
        httpDownloader.execute(url);
    }

    /**
     * Sends a HEAD request to the given URL and returns the Content-Length
     * property through a callback.
     *
     * Used for determining the size of a file before downloading it.
     */
    public void getContentLength(String urlString, GenericCallback<Integer> callback) {
        URL url;

        try {

            url = new URL(urlString);

        } catch (MalformedURLException e) {

            callback.fail(e);
            return;
        }

        HttpContentLengthQuery request = new HttpContentLengthQuery(callback);
        request.execute(url);
    }

    private class HttpDownloader extends AsyncTask<URL, Float, byte[]> {

        final private GenericCallback genericCallback;
        final private ProgressFeedbackCallback progressCallback;

        final private static int MAX_BUFFER_SIZE = 1024;

        // Store exception thrown in background thread so that we can pass it to our callback in the
        // main thread.
        final private AtomicReference<Exception> cachedException = new AtomicReference<Exception>();

        public HttpDownloader(GenericCallback genericCallback) {

            this.genericCallback = genericCallback;
            this.progressCallback = null;
        }

        public HttpDownloader(GenericCallback genericCallback, ProgressFeedbackCallback progressCallback) {

            this.genericCallback = genericCallback;
            this.progressCallback = progressCallback;
        }

        @Override
        protected byte[] doInBackground(URL... urls) {

            URL url = urls[0];
            String urlString = url.toString();
            String fileName = urlString.substring(urlString.lastIndexOf("/") + 1, urlString.length());
            HttpURLConnection httpURLConnection = null;
            int responseCode = 0;

            try {

                httpURLConnection = (HttpURLConnection) url.openConnection();
                responseCode = httpURLConnection.getResponseCode();

            }
            catch (IOException e) {

                cachedException.set(e);
                return null;
            }

            byte[] bytes = new byte[0];

            if (responseCode == HttpURLConnection.HTTP_OK)
            {
                /* Rory 20160125: this doesn't appear to be used anywhere

                String disposition = httpURLConnection.getHeaderField("Content-Disposition");
                if (disposition != null)
                {
                    // extracts file name from header field
                    int index = disposition.indexOf("filename=");
                    if (index > 0)
                    {
                        fileName = disposition.substring(index + 10, disposition.length() - 1);
                    }
                }*/

                try {
                    // TODO: update progress callback
                    InputStream inputStream = httpURLConnection.getInputStream();
                    bytes = IOUtils.toByteArray(inputStream);

                }
                catch (IOException e) {

                    cachedException.set(e);
                    return null;
                }
            }
            else
            {
                cachedException.set(new Exception("Received a " + responseCode + " response code."));
                return null;
            }

            return bytes;
        }

        @Override
        protected void onProgressUpdate(Float... progress) {
            if (progressCallback != null) {
                progressCallback.progress(progress[0]);
            }
        }

        @Override
        protected void onPostExecute(byte[] downloadedFile) {
            Exception ex = cachedException.get();

            // Download successful
            if (ex == null) {
                if (progressCallback != null) {
                    progressCallback.progress(1f);
                    progressCallback.success();
                }
                genericCallback.success(downloadedFile);
            } else {
                genericCallback.fail(ex);
                if (progressCallback != null) {
                    progressCallback.fail(ex);
                }
            }
        }
    }

    private class HttpStreamDownloader extends AsyncTask<URL, Float, Void> {

        private final AtomicReference<Exception> cachedException = new AtomicReference<>();

        private final int MAX_BUFFER_SIZE = 8192;

        private final ProgressFeedbackCallback callback;
        private final DataOutput dataOutput;

        public HttpStreamDownloader(DataOutput dataOutput, ProgressFeedbackCallback callback) {
            this.callback = callback;
            this.dataOutput = dataOutput;
        }

        @Override
        protected Void doInBackground(URL... urls) {
            try {
                // Open a connection to the URL
                HttpURLConnection connection = (HttpURLConnection) urls[0].openConnection();

                connection.connect();

                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw new Exception("HTTP GET on " + urls[0].toString() + " returned code " + responseCode);
                }

                int contentLength = connection.getContentLength();

                // Get download stream
                InputStream stream = connection.getInputStream();
                int downloaded = 0;
                while (downloaded < contentLength) {
                    byte buffer[];
                    if (contentLength - downloaded > MAX_BUFFER_SIZE) {
                        buffer = new byte[MAX_BUFFER_SIZE];
                    } else {
                        buffer = new byte[contentLength - downloaded];
                    }

                    // Read from the server into the buffer
                    int read = stream.read(buffer);
                    if (read == -1) {
                        break;
                    }

                    dataOutput.write(buffer, 0, read);
                    downloaded += read;

                    publishProgress((float) downloaded / (float) contentLength);
                }

            } catch (Exception ex) {
                cachedException.set(ex);
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Float... progress) {
            callback.progress(progress[0]);
        }

        @Override
        protected void onPostExecute(Void arg) {
            Exception ex = cachedException.get();

            if (ex == null) {
                callback.progress(1f);
                callback.success();
            } else {
                callback.fail(ex);
            }
        }
    }

    private class HttpContentLengthQuery extends AsyncTask<URL, Void, Integer> {

        private GenericCallback<Integer> callback;

        // Store exception thrown in background thread so that we can pass it to our callback in the
        // main thread.
        final private AtomicReference<Exception> cachedException = new AtomicReference<Exception>();

        public HttpContentLengthQuery(GenericCallback<Integer> callback) {
            this.callback = callback;
        }

        @Override
        protected Integer doInBackground(URL... urls) {
            String urlString = urls[0].toString();

            // We have to use the legacy Apache HTTP client because HttpUrlConnection does not
            // correctly support HTTP HEAD requests.
            AndroidHttpClient httpClient = AndroidHttpClient.newInstance("com.rsg.rsgportal");
            try {
                HttpResponse response = httpClient.execute(new HttpHead(urlString));

                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == 200) {
                    Header[] contentLength = response.getHeaders("Content-Length");
                    if (contentLength.length > 0) {
                        return Integer.decode(contentLength[0].getValue());
                    } else {
                        throw new NoSuchFieldException("Content-Length field not present in HTTP header.");
                    }
                } else {
                    throw new IOException("HTTP HEAD on " + urlString + " returned status " + statusCode);
                }
            } catch (Exception ex) {
                cachedException.set(ex);

                return null;
            } finally {
                httpClient.close();
            }
        }

        @Override
        protected void onPostExecute(Integer downloadSize) {
            if (cachedException.get() == null) {
                callback.success(downloadSize);
            } else {
                callback.fail(cachedException.get());
            }
        }
    }
}
