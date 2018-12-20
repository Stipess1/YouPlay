package com.hfad.youplay.utils;

import android.os.Environment;
import android.os.StatFs;

import com.google.api.services.youtube.model.SearchResult;
import com.hfad.youplay.BuildConfig;

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

    public static String getID(List<SearchResult> searchResults) {

        StringBuilder stringBuilder = new StringBuilder();
        for (SearchResult result : searchResults) {
            String id = result.getId().getVideoId();
            if (id != null) {
                stringBuilder.append(id);
                stringBuilder.append(",");
            }
        }

        if (stringBuilder.length() == 0) {
            return null;
        }

        if (stringBuilder.toString().endsWith(",")) {
            stringBuilder.setLength(stringBuilder.length() - 1);
        }
        return stringBuilder.toString();
    }

    public static String convertViewsToString(long views)
    {
        return formatter.format(views);
    }

    public static String getDuration(String duration) {
        duration = duration.substring(2);
        String H, M, S;
        int indOfH = duration.indexOf("H");
        if (indOfH > -1) {
            H = duration.substring(0,indOfH);
            duration = duration.substring(indOfH);
            duration = duration.replace("H","");
        } else {
            H = "";
        }

        int indOfM = duration.indexOf("M");
        if (indOfM > -1) {
            M = duration.substring(0,indOfM);
            duration = duration.substring(indOfM);
            duration = duration.replace("M","");
            if (H.length() > 0 && M.length() == 1) {
                M = "0" + M;
            }
        } else {
            if (H.length() > 0) {
                M = "00";
            } else {
                M = "0";
            }
        }
        int indOfS = duration.indexOf("S");
        if (indOfS > -1) {
            S = duration.substring(0,indOfS);
            duration = duration.substring(indOfS);
            duration = duration.replace("S","");
            if (S.length() == 1) {
                S = "0" + S;
            }
        } else {
            S = "00";
        }
        if (H.length() > 0) {
            return H + ":" +  M + ":" + S;
        } else {
            return M + ":" + S;
        }
    }

    public static long convertToMilis(String duration)
    {
        String[] regex = duration.split(":");
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
            long secs = Integer.parseInt(regex[1]);

            return secs * 1000;
        }
    }

    public static String convertDuration(long milis)
    {
        int seconds = (int) (milis / 1000) % 60;
        int minutes = (int) ((milis / (1000*60)) % 60);
        int hours   = (int) ((milis / (1000*60*60)) % 24);
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

    public static boolean needsUpdate(String webVersion)
    {
        webVersion = webVersion.replaceAll("\\.","");
        String localVersion = BuildConfig.VERSION_NAME.replaceAll("\\.", "");

        return Integer.parseInt(webVersion) > Integer.parseInt(localVersion);
    }
}
