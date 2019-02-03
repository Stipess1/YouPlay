package com.hfad.youplay.youtube.loaders;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.hfad.youplay.extractor.YoutubeExtractor;


import java.util.ArrayList;
import java.util.List;


public class UrlLoader extends AsyncTaskLoader<List<String>>
{
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/40.0.2214.115 Safari/537.36";
    private static final String TAG = UrlLoader.class.getSimpleName();
    private String getYoutubeLink;

    public UrlLoader(Context context, String getYoutubeLink)
    {
        super(context);
        this.getYoutubeLink = getYoutubeLink;
    }

    @Nullable
    @Override
    public List<String> loadInBackground()
    {
        List<String> data = new ArrayList<>();
        try
        {
            YoutubeExtractor extractor = new YoutubeExtractor();
            extractor.parse(getYoutubeLink);

            data.add(extractor.getThumbnailUrl());
            data.add(extractor.getAudio().getUrl());
            Crashlytics.setString("last_action", "downloading: " + getYoutubeLink);

            return data;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        Crashlytics.setString("last_action", "downloading failed: " + getYoutubeLink);
        Crashlytics.log("An error has occurred: " + getYoutubeLink);
        return null;
    }
}
