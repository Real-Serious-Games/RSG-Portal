package com.rsg.rsgportal;

import android.os.AsyncTask;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by rorydungan on 18/02/2016.
 *
 * Helper class for checking MD5 sums.
 */
public class MD5Checker {

    public FileSystem FileSystem;

    /**
     * Asynchronously calculate the MD5 sum of the given file and check it against a given checksum.
     */
    public void checkFileMD5(String path, String expectedMD5, GenericProgressFeedbackCallback<Boolean> callback) {
        GenerateMD5Task generateMD5 = new GenerateMD5Task(expectedMD5, callback);
        generateMD5.execute(path);
    }

    private class GenerateMD5Task extends AsyncTask<String, Float, String> {

        private final AtomicReference<Exception> cachedException = new AtomicReference<>();

        private final int MAX_BUFFER_SIZE = 8192;

        private final GenericProgressFeedbackCallback<Boolean> callback;
        private final String expectedMD5;

        public GenerateMD5Task(String expectedMD5, GenericProgressFeedbackCallback<Boolean> callback) {
            this.expectedMD5 = expectedMD5;
            this.callback = callback;
        }

        @Override
        protected String doInBackground(String... paths) {
            String path = paths[0];

            RandomAccessFile file = null;
            try {
                MessageDigest digest = MessageDigest.getInstance("MD5");

                file = FileSystem.getNewFileStreamFromAbsolutePath(path);
                long length = file.length();
                byte[] buffer = new byte[MAX_BUFFER_SIZE];

                int read;

                while((read = file.read(buffer)) > 0) {
                    digest.update(buffer, 0, read);

                    publishProgress((float)length / (float) read);
                }
                byte[] md5sum = digest.digest();
                BigInteger bigInt = new BigInteger(1, md5sum);
                String output = bigInt.toString(16);
                // Fill to 32 chars
                output = String.format("%32s", output).replace(' ', '0');
                return output;
            } catch (Exception ex) {
                cachedException.set(ex);
                return null;
            } finally {
                if (file != null) {
                    try {
                        file.close();
                    } catch (IOException ex) {
                        cachedException.set(ex);
                    }
                }
            }
        }

        @Override
        protected void onProgressUpdate(Float... progress) {
            callback.progress(progress[0]);
        }

        @Override
        protected void onPostExecute(String md5) {
            if (cachedException.get() == null) {
                callback.success(md5.equals(expectedMD5));
            } else {
                callback.fail(cachedException.get());
            }
        }
    }
}
