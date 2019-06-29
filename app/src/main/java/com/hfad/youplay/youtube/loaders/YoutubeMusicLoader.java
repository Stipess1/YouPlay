package com.hfad.youplay.youtube.loaders;


import android.content.Context;

import androidx.loader.content.AsyncTaskLoader;

import com.hfad.youplay.database.YouPlayDatabase;
import com.hfad.youplay.extractor.SearchExtractor;
import com.hfad.youplay.music.Music;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by Stjepan on 10.12.2017..
 */

public class YoutubeMusicLoader extends AsyncTaskLoader<List<Music>> {

    private static final String TAG = "YoutubeMusicLoader";
    private String query;
    private boolean relatedVideos;
    private String id;
    private List<Music> musicList = new ArrayList<>();

    public YoutubeMusicLoader(Context context, String query)
    {
        super(context);
        this.query = query;
    }

    public YoutubeMusicLoader(Context context, String id, boolean relatedVideos)
    {
        super(context);
        this.relatedVideos = relatedVideos;
        this.id = id;
    }

    @Override
    public List<Music> loadInBackground() {
        List<Music> youtubeVideoList = new ArrayList<Music>();
        musicList.addAll(YouPlayDatabase.getInstance(getContext()).getData());

        if(!relatedVideos)
        {
            SearchExtractor searchExtractor = new SearchExtractor();
            try{
                searchExtractor.setSearchQuery(query);
                return searchExtractor.getMusics();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        return youtubeVideoList;
    }
}
