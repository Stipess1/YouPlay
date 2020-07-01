package com.stipess.youplay.youtube.loaders;


import android.content.Context;
import android.util.Log;

import androidx.loader.content.AsyncTaskLoader;

import com.stipess.youplay.extractor.DownloaderTestImpl;
import com.stipess.youplay.music.Music;
import com.stipess.youplay.utils.FileManager;
import com.stipess.youplay.utils.Utils;


import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.ListExtractor;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.search.SearchExtractor;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;

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

    @Override
    public List<Music> loadInBackground() {
//        SearchExtractor searchExtractor = new SearchExtractor();
        List<Music> list = new ArrayList<>();
        if(!relatedVideos)
        {
            if(NewPipe.getDownloader() == null)
                NewPipe.init(DownloaderTestImpl.getInstance());
            try{

                SearchExtractor extractor = ServiceList.YouTube.getSearchExtractor(query);
                extractor.fetchPage();
//                extractor.fetchPage();

                for(InfoItem item : extractor.getInitialPage().getItems()) {
                    StreamInfoItem stream = (StreamInfoItem) item;
                    Music music = new Music();
                    music.setAuthor(stream.getUploaderName());
                    music.setViews(Utils.convertViewsToString(stream.getViewCount()));
                    music.setUrlImage(stream.getThumbnailUrl());
                    music.setTitle(stream.getName());
                    String tempUrl = stream.getUrl();
                    if(tempUrl.contains("https://youtu.be/")) {
                        tempUrl = tempUrl.substring(17, tempUrl.length());
                    } else if(tempUrl.contains("https://www.youtube.com/watch?v=")) {
                        tempUrl = tempUrl.substring(32, tempUrl.length());
                    } else if(tempUrl.contains("https://m.youtube.com/watch?v=")) {
                        tempUrl = tempUrl.substring(30, tempUrl.length());
                    } else if(tempUrl.contains("http://www.youtube.com/v/")) {
                        tempUrl = tempUrl.substring(25, tempUrl.length());
                    }
                    music.setId(tempUrl);
                    music.setDuration(Utils.convertDuration(stream.getDuration()*1000));
                    Log.d(TAG,"URL: " + stream.getDuration());

                    for(Music pjesma : checkList)
                    {
                        if(pjesma.getId().equals(music.getId()))
                        {
                            if(pjesma.getDownloaded() == 1)
                            {
                                music.setPath(FileManager.getMediaPath(music.getId()));
                                music.setDownloaded(1);
                            }
                        }
                    }

                    list.add(music);
                }


                return list;
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }



        return list;
    }


}
