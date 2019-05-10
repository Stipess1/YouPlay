package com.hfad.youplay.extractor;

import com.hfad.youplay.database.YouPlayDatabase;
import com.hfad.youplay.music.Music;
import com.hfad.youplay.utils.FileManager;
import com.hfad.youplay.utils.Utils;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SearchExtractor {
    private static final String SEARCH_QUERY = "https://www.youtube.com/results?search_query=";
    private static final String PAGE = "&page=";
    private int page = 1;
    private List<Music> musicList = new ArrayList<>();
    private List<Music> checkList = new ArrayList<>();

    public List<Music> getMusics()
    {
        return musicList;
    }

    public void setSearchQuery(String searchQuery) throws Exception
    {
        checkList.addAll(YouPlayDatabase.getInstance().getData());
        Connection connect = Jsoup.connect(SEARCH_QUERY+searchQuery+PAGE+page);
        Document document = null;
        try{
            document = connect.get();
        }catch (Exception e)
        {
            throw new IOException(e.getMessage());
        }
        if(document != null)
        {
            Element list = document.select("ol[class=\"item-section\"]").first();
            for(Element item : list.children())
            {
                Element el;

                if ((item.select("div[class*=\"search-message\"]").first()) != null)
                    return;
                else if((el = item.select("div[class*=\"yt-lockup-video\"]").first()) != null)
                {
                    try{
                        if(!isLiveStream(el))
                        {
                            Element title = el.select("h3").first().select("a").first(); // Naslov
                            Element duration = item.select("span[class*=\"video-time\"]").first(); // Dužina pjesme
                            Element autor = item.select("div[class=\"yt-lockup-byline\"]").first() // Autor pjesme
                                    .select("a").first();
                            Element url = el.select("h3")
                                    .first().select("a").first(); // Url pesme
                            String id = url.attr("abs:href").substring(32);
                            Music music = new Music();
                            music.setAuthor(autor.text());
                            music.setTitle(title.text());
                            music.setDownloaded(0);
                            music.setDuration(duration.text());
                            music.setId(id);
                            music.setViews(Utils.convertViewsToString(getViewCount(el)));
                            music.setUrlImage(getThumbnailUrl(id));
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
                            musicList.add(music);
                        }
                    }
                    catch (Exception e)
                    {
                        throw new IOException(e.getMessage());
                    }
                }
            }

        }

    }

    private long getViewCount(Element element)
    {
        Element view = element.select("div[class=\"yt-lockup-meta\"]").first();
        if(view == null) return -1;

        if(view.select("li").size() < 2) return -1;

        String input;

        input = view.select("li").get(1).text();
        return Long.parseLong(Utils.removeNonDigitCharacters(input));
    }

    private String getThumbnailUrl(String id)
    {
        return "http://img.youtube.com/vi/" + id +"/0.jpg";
    }

    private static boolean isLiveStream(Element item)
    {
        return !item.select("span[class*=\"yt-badge-live\"]").isEmpty()
                || !item.select("span[class*=\"video-time-overlay-live\"]").isEmpty();
    }
}
