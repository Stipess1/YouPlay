package com.hfad.youplay.youtube.loaders;


import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

import androidx.loader.content.AsyncTaskLoader;

import com.hfad.youplay.database.YouPlayDatabase;
import com.hfad.youplay.extractor.SearchExtractor;
import com.hfad.youplay.music.Music;

import org.apache.commons.lang.exception.ExceptionUtils;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by Stjepan on 10.12.2017..
 */

public class YoutubeMusicLoader extends AsyncTaskLoader<List<Music>> {

    private static final String TAG = "YoutubeMusicLoader";
    private String query;
    private boolean relatedVideos;
    private ArrayList<Music> checkList;
    private String id;

    public YoutubeMusicLoader(Context context, String query, ArrayList<Music> checkList)
    {
        super(context);
        this.query = query;
        this.checkList = checkList;
    }

    public YoutubeMusicLoader(Context context, String id, boolean relatedVideos)
    {
        super(context);
        this.relatedVideos = relatedVideos;
        this.id = id;
    }

    @Override
    public List<Music> loadInBackground() {

        if(!relatedVideos)
        {
            SearchExtractor searchExtractor = new SearchExtractor();
            try{
                searchExtractor.setCheckList(checkList);
                searchExtractor.setSearchQuery(query);
                return searchExtractor.getMusics();
            }
            catch (Exception ignored) {

            }
        }

        return null;
    }
}
