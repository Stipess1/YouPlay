package com.hfad.youplay.youtube.loaders;

import android.os.AsyncTask;

import com.crashlytics.android.Crashlytics;
import com.hfad.youplay.AudioService;
import com.hfad.youplay.extractor.YoutubeExtractor;


import java.util.ArrayList;
import java.util.List;


public class UrlLoader extends AsyncTask<Void,Void,List<String>>
{
    private static final String TAG = UrlLoader.class.getSimpleName();
    private String getYoutubeLink;
    private Listener listener;

    public UrlLoader(String getYoutubeLink)
    {
        this.getYoutubeLink = getYoutubeLink;
    }

    public interface Listener{
        void postExecute(List<String> list);
    }

    @Override
    protected List<String> doInBackground(Void... voids) {
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

    @Override
    protected void onPostExecute(List<String> strings) {
        AudioService audioService = AudioService.getInstance();
        if(listener != null && audioService != null && !audioService.isDestroyed())
            listener.postExecute(strings);
    }

    public void setListener(Listener listener){
        this.listener = listener;
    }
}
