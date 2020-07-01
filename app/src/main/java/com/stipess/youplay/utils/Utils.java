package com.stipess.youplay.utils;

import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import com.stipess.youplay.BuildConfig;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

/**
 * Created by Stjepan on 2.12.2017..
 */

public class Utils {

    private Utils(){}

    private static DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
    private static DecimalFormat formatter = new DecimalFormat("#,###", symbols);
    private static final long MEGA_BYTE = 1048576;


    public static String convertViewsToString(long views)
    {
        return formatter.format(views);
    }




    public static long convertToMilis(String duration)
    {
        Log.d("Utils", "duration: "  + duration);
        String[] regex;
        if(duration.contains(":"))
            regex = duration.split(":");
        else
            regex = duration.split("\\.");

        if(regex.length >= 3)
        {
            long hours = Integer.parseInt(regex[0]);
            long mins  = Integer.parseInt(regex[1]);
            long secs  = Integer.parseInt(regex[2]);

            hours = (hours * 3600) * 1000;
            mins  = (mins  * 60) * 1000;
            secs  = secs * 1000;

            return hours + mins + secs;
        }
        else if(regex.length >= 2)
        {
            long mins = Integer.parseInt(regex[0]);
            long secs = Integer.parseInt(regex[1]);

            mins  = (mins  * 60) * 1000;
            secs  = secs * 1000;

            return mins + secs;
        }
        else
        {
            long secs = Integer.parseInt(regex[0]);

            return secs * 1000;
        }
    }

    public static String convertDuration(long milis)
    {
        int seconds = (int) (milis / 1000) % 60;
        int minutes = (int) ((milis / (1000*60)) % 60);
        int hours   = (int) ((milis / (1000*60*60)));
        if(hours == 0)
        {
            return String.format("%d:%02d", minutes, seconds);
        }
        return String.format("%d:%02d:%02d",hours ,minutes, seconds);
    }

    public static int freeSpace(boolean external)
    {
        StatFs statFs = getStats(external);
        long availableBlocks = statFs.getAvailableBlocksLong();
        long blockSize = statFs.getBlockSizeLong();
        long freeBytes = availableBlocks * blockSize;

        return (int) (freeBytes / MEGA_BYTE);
    }

    private static StatFs getStats(boolean external){
        String path;

        if (external){
            path = Environment.getExternalStorageDirectory().getAbsolutePath();
        }
        else{
            path = Environment.getRootDirectory().getAbsolutePath();
        }

        return new StatFs(path);
    }

    public static String removeNonDigitCharacters(String remove)
    {
        return remove.replaceAll("\\D+","");
    }

    public static boolean needsUpdate(String webVersion)
    {
        webVersion = webVersion.replaceAll("\\.","");
        String localVersion = BuildConfig.VERSION_NAME.replaceAll("\\.", "");

        return Integer.parseInt(webVersion) > Integer.parseInt(localVersion);
    }

//    public static String concatenateIDs(List<SearchResult> searchResults) {
//
//        StringBuilder contentDetails = new StringBuilder();
//        for (SearchResult result : searchResults) {
//            String id = result.getId().getVideoId();
//            if (id != null) {
//                contentDetails.append(id);
//                contentDetails.append(",");
//            }
//        }
//
//        if (contentDetails.length() == 0) {
//            return null;
//        }
//
//        if (contentDetails.toString().endsWith(",")) {
//            contentDetails.setLength(contentDetails.length() - 1); //remove last ,
//        }
//        return contentDetails.toString();
//    }

    public static String convertISO8601DurationToNormalTime(String isoTime) {
        String formattedTime = new String();

        if (isoTime.contains("H") && isoTime.contains("M") && isoTime.contains("S")) {
            String hours = isoTime.substring(isoTime.indexOf('T') + 1, isoTime.indexOf('H'));
            String minutes = isoTime.substring(isoTime.indexOf('H') + 1, isoTime.indexOf('M'));
            String seconds = isoTime.substring(isoTime.indexOf('M') + 1, isoTime.indexOf('S'));
            formattedTime = hours + ":" + formatTo2Digits(minutes) + ":" + formatTo2Digits(seconds);
        } else if (!isoTime.contains("H") && isoTime.contains("M") && isoTime.contains("S")) {
            String minutes = isoTime.substring(isoTime.indexOf('T') + 1, isoTime.indexOf('M'));
            String seconds = isoTime.substring(isoTime.indexOf('M') + 1, isoTime.indexOf('S'));
            formattedTime = minutes + ":" + formatTo2Digits(seconds);
        } else if (isoTime.contains("H") && !isoTime.contains("M") && isoTime.contains("S")) {
            String hours = isoTime.substring(isoTime.indexOf('T') + 1, isoTime.indexOf('H'));
            String seconds = isoTime.substring(isoTime.indexOf('H') + 1, isoTime.indexOf('S'));
            formattedTime = hours + ":00:" + formatTo2Digits(seconds);
        } else if (isoTime.contains("H") && isoTime.contains("M") && !isoTime.contains("S")) {
            String hours = isoTime.substring(isoTime.indexOf('T') + 1, isoTime.indexOf('H'));
            String minutes = isoTime.substring(isoTime.indexOf('H') + 1, isoTime.indexOf('M'));
            formattedTime = hours + ":" + formatTo2Digits(minutes) + ":00";
        } else if (!isoTime.contains("H") && !isoTime.contains("M") && isoTime.contains("S")) {
            String seconds = isoTime.substring(isoTime.indexOf('T') + 1, isoTime.indexOf('S'));
            formattedTime = "0:" + formatTo2Digits(seconds);
        } else if (!isoTime.contains("H") && isoTime.contains("M") && !isoTime.contains("S")) {
            String minutes = isoTime.substring(isoTime.indexOf('T') + 1, isoTime.indexOf('M'));
            formattedTime = minutes + ":00";
        } else if (isoTime.contains("H") && !isoTime.contains("M") && !isoTime.contains("S")) {
            String hours = isoTime.substring(isoTime.indexOf('T') + 1, isoTime.indexOf('H'));
            formattedTime = hours + ":00:00";
        }

        return formattedTime;
    }

    private static String formatTo2Digits(String str) {
        if (str.length() < 2) {
            str = "0" + str;
        }
        return str;
    }
}
