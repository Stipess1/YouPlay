package com.hfad.youplay.utils;

import android.os.Environment;

import java.io.File;

import static com.hfad.youplay.utils.Constants.APP_NAME;
import static com.hfad.youplay.utils.Constants.DATABASE;
import static com.hfad.youplay.utils.Constants.NO_MEDIA;

public class FileManager
{
    private static final String JPG = ".jpg";
    private static final String MP3 = ".mp3";

    private FileManager()
    {

    }

    public static String getPicturePath(String id)
    {
        return new File(Environment.getExternalStorageDirectory() + File.separator + Constants.APP_NAME + File.separator +
                Constants.NO_MEDIA, id + JPG).getPath();
    }

    public static File getRootPath()
    {
        return new File(Environment.getExternalStorageDirectory() + File.separator + APP_NAME);
    }

    public static String getMediaPath(String id)
    {
        return new File(Environment.getExternalStorageDirectory() + File.separator + APP_NAME, id + MP3).getPath();
    }

    public static File getMediaFile(String id)
    {
        return new File(Environment.getExternalStorageDirectory() + File.separator + APP_NAME, id + MP3);
    }

    public static File getPictureFile(String id)
    {
        return new File(Environment.getExternalStorageDirectory() + File.separator + APP_NAME + File.separator + NO_MEDIA, id + JPG);
    }

    public static File getDownloadFolder()
    {
        return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "YouPlay.apk");
    }

    public static File getPictureFolder()
    {
        return new File(Environment.getExternalStorageDirectory() + File.separator + APP_NAME + File.separator + NO_MEDIA);
    }

    public static File getDatabaseFolder()
    {
        return new File(Environment.getExternalStorageDirectory() + File.separator + APP_NAME + File.separator + DATABASE);
    }
}
