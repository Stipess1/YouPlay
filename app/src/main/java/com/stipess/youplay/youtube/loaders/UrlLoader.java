package com.stipess.youplay.youtube.loaders;

import android.os.AsyncTask;
import android.util.Log;

import com.stipess.youplay.AudioService;
import com.stipess.youplay.extractor.DownloaderTestImpl;
import com.stipess.youplay.music.Music;
import com.stipess.youplay.utils.FileManager;
import com.stipess.youplay.utils.Utils;

import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeStreamExtractor;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.stream.StreamInfoItemsCollector;

import java.util.ArrayList;
import java.util.List;


public class UrlLoader extends AsyncTask<Void,Void,List<String>>
{
    private static final String TAG = UrlLoader.class.getSimpleName();
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:43.0) Gecko/20100101 Firefox/43.0";
    private static final String DEFAULT_HTTP_ACCEPT_LANGUAGE = "de";

    private String getYoutubeLink;
    private Listener listener;
    private List<Music> musicList = new ArrayList<>();
    private List<Music> checkList = new ArrayList<>();
    private boolean relatedVideos;

    public UrlLoader(String getYoutubeLink, boolean relatedVideos, List<Music> list)
    {
        this.getYoutubeLink = getYoutubeLink;
        this.relatedVideos = relatedVideos;
        checkList.addAll(list);
    }

    public interface Listener{
        void postExecute(List<String> list);
    }

    public List<Music> getMusicList()
    {
        return musicList;
    }

    @Override
    protected List<String> doInBackground(Void... voids) {
        List<String> data = new ArrayList<>();
        try
        {
            if(NewPipe.getDownloader() == null)
                NewPipe.init(DownloaderTestImpl.getInstance());

            StreamingService service = NewPipe.getService("YouTube");
            YoutubeStreamExtractor extractor1 = (YoutubeStreamExtractor) service.getStreamExtractor(getYoutubeLink);

            extractor1.fetchPage();

            StreamInfoItemsCollector relatedVideos = extractor1.getRelatedStreams();

            data.add(extractor1.getThumbnailUrl());
            data.add(extractor1.getAudioStreams().get(0).getUrl());

            Log.d(TAG, "Extracted");

            if(this.relatedVideos) {

                for(StreamInfoItem stream : relatedVideos.getItems()) {
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
                    Log.d(TAG, "Pjesma; "+music.getId());
                    Log.d(TAG, "CheckList: " + checkList.size());
                    for(Music pjesma : checkList)
                    {
                        if(pjesma.getId().equals(music.getId()))
                        {
                            Log.d(TAG, "Pjesme: " + pjesma.getId()+"-"+music.getId());
                            if(pjesma.getDownloaded() == 1)
                            {
                                music.setPath(FileManager.getMediaPath(music.getId()));
                                music.setDownloaded(1);
                            }
                        }
                    }
                    musicList.add(music);
                }

            }


            return data;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
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
