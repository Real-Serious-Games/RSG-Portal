package com.rsg.rsgportal;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Pair;

import org.apache.commons.io.FileUtils;
import org.jdeferred.Deferred;
import org.jdeferred.Promise;
import org.jdeferred.impl.DeferredObject;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by PhilipWarren on 15/01/2016.
 */
public class FileSystem {

    private FileFilter directoryFilter = new FileFilter() {
        @Override
        public boolean accept(File file) {
            return file.isDirectory();
        }
    };

    public void saveFile(String fileName, byte[] data) throws IOException {

        File fileSaveLocation = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

        File fileToSave = new File(fileSaveLocation, fileName);

        fileToSave.createNewFile();

        FileOutputStream outputStream = new FileOutputStream(fileToSave);
        outputStream.write(data);
        outputStream.close();
    }

    /**
     * Create directories above the specified file path, if they don't already exist.
     * @param fileToSave
     * @throws IOException
     */
    public void ensureDirectoriesForFile(String fileToSave) throws IOException {
        // Create directory if it doesn't exist
        File folder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileToSave).getParentFile();
        if (!folder.exists()) {
            boolean success = folder.mkdirs();
            if (!success) {
                throw new IOException("Failed to create directory " + folder);
            }
        }
    }

    public byte[] loadFile(String fileName) throws IOException {
        File fileOpenLocation = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File fileToLoad = new File(fileOpenLocation, fileName);
        return FileUtils.readFileToByteArray(fileToLoad);
    }

    /**
     * Copy a file from one directory, to another
     * Code from http://stackoverflow.com/questions/16433915/how-to-copy-file-from-one-location-to-another-location
     * @param sourceFile
     * @param destFile
     * @throws IOException
     */
    public Promise<Void, Exception, Pair<Integer, Integer>> copyFile(File sourceFile, File destFile) {
        Deferred<Void, Exception, Pair<Integer, Integer>> deferred = new DeferredObject<>();

        CopyFileTask copyFileTask = new CopyFileTask(sourceFile, destFile, deferred);
        copyFileTask.execute();

        return deferred.promise();
    }

    /**
     * Return a list of the absolute filepaths for all files in a given directory
     * @param directory
     * @return
     */
    public List<String> getAllFilepathsInDirectory(File directory) {
        List<String> filepaths = new ArrayList<>();

        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                filepaths.add(file.getAbsolutePath());
            }
        }

        return filepaths;
    }

    public RandomAccessFile getNewFileStream(String path) throws IOException {
        File fileSaveLocation = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        return getNewFileStreamFromAbsolutePath(new File(fileSaveLocation, path).getAbsolutePath());
    }

    public RandomAccessFile getNewFileStreamFromAbsolutePath(String path) throws IOException {
        File file = new File(path);

        return new RandomAccessFile(file, "rw");
    }

    private class CopyFileTask extends AsyncTask<Void, Void, Void> {

        private final Deferred<Void, Exception, Pair<Integer, Integer>> deferred;

        private final File sourceFile;
        private final File destFile;

        private final AtomicReference<Exception> cachedException = new AtomicReference<>();

        public CopyFileTask(File sourceFile, File destFile, Deferred<Void, Exception, Pair<Integer, Integer>> deferred) {
            this.sourceFile = sourceFile;
            this.destFile = destFile;
            this.deferred = deferred;
        }

        @Override
        protected Void doInBackground(Void... args) {

            try {
                if (sourceFile.isDirectory()) {
                    FileUtils.copyDirectory(sourceFile, destFile);
                } else {
                    FileUtils.copyFile(sourceFile, destFile);
                }
            } catch (Exception ex) {
                cachedException.set(ex);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void arg) {
            Exception ex = cachedException.get();

            if (ex == null) {
                deferred.resolve(null);
            } else {
                deferred.reject(ex);
            }
        }
    }

    /**
     * Return all subdirectories, ignoring files, in the provided root
     * @param root
     * @return
     */
    public File[] getAllSubDirectories(File root) {
        return root.listFiles(directoryFilter);
    }
}
