package com.hfad.youplay.youtube.loaders;


import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.Thumbnail;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;
import com.hfad.youplay.database.YouPlayDatabase;
import com.hfad.youplay.music.Music;
import com.hfad.youplay.utils.FileManager;
import com.hfad.youplay.utils.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.hfad.youplay.utils.Constants.API_KEY;

/**
 * Created by Stjepan on 10.12.2017..
 */

public class YoutubeMusicLoader extends AsyncTaskLoader<List<Music>> {

    private static final String TAG = "YoutubeMusicLoader";
    private static final long NUMBER_OF_VIDEOS_RETURNED = 20;
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
        try{
            YouTube youTube = new YouTube.Builder(new NetHttpTransport(), new JacksonFactory(), new HttpRequestInitializer() {
                @Override
                public void initialize(HttpRequest httpRequest) throws IOException {

                }
            }).setApplicationName("YouPlay").build();

            YouTube.Search.List search = youTube.search().list("id");
            YouTube.Videos.List video = youTube.videos().list("id,contentDetails,statistics,snippet");

            search.setKey(API_KEY);
            if(!relatedVideos)
            {
                search.setQ(query);
                search.setMaxResults(NUMBER_OF_VIDEOS_RETURNED);
            }
            else
            {
                search.setRelatedToVideoId(id);
                search.setMaxResults((long) 10);
            }
            search.setType("video");
            video.setKey(API_KEY);
            video.setFields("items(id,contentDetails/duration,statistics/viewCount,snippet(title,channelTitle),snippet/thumbnails/medium/url)");
            search.setFields("items(id/kind,id/videoId)");


            SearchListResponse searchListResponse = search.execute();
            List<SearchResult> searchResults = searchListResponse.getItems();
            video.setId(Utils.getID(searchResults));
            VideoListResponse videoListResponse = video.execute();
            List<Video> videoList = videoListResponse.getItems();

            for(Video videos: videoList)
            {
                // Provjeravamo je li dobiveni video zapravo stream
                if(!videos.getContentDetails().getDuration().equals("PT0S"))
                {
                    Thumbnail thumbnail = videos.getSnippet().getThumbnails().getMedium();

                    String url = thumbnail.getUrl();
                    Music music = new Music();
                    music.setAuthor(videos.getSnippet().getChannelTitle());
                    music.setDuration(Utils.getDuration(videos.getContentDetails().getDuration()));
                    music.setTitle(videos.getSnippet().getTitle());
                    music.setViews(Utils.convertViewsToString(videos.getStatistics().getViewCount().longValue()));
                    music.setUrlImage(url);
                    music.setId(videos.getId());
                    music.setDownloaded(0);
                    for(Music pjesma : musicList)
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
                    youtubeVideoList.add(music);
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return youtubeVideoList;
    }
}
