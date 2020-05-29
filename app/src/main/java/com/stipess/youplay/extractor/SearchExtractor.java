package com.stipess.youplay.extractor;

import android.util.Log;

import com.stipess.youplay.music.Music;
import com.stipess.youplay.utils.FileManager;
import com.stipess.youplay.utils.Utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

public class SearchExtractor {
    private static final String TAG = SearchExtractor.class.getSimpleName();
    private static final String SEARCH_QUERY = "https://www.youtube.com/results?search_query=";
    private static final String PAGE = "&page=";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/40.0.2214.115 Safari/537.36";
    private int page = 1;
    private List<Music> musicList = new ArrayList<>();
    private List<Music> checkList = new ArrayList<>();

    private Document document;

    public List<Music> getMusics()
    {
        return musicList;
    }

    public void setCheckList(ArrayList<Music> checkList) {
        this.checkList.addAll(checkList);
    }

    public void setSearchQuery(String searchQuery) throws Exception
    {
        final String site;
        searchQuery = URLEncoder.encode(searchQuery, "UTF-8");
        final String url = SEARCH_QUERY+searchQuery;
        Log.d(TAG, "URL: " + url);
        site = download(url);
        document = Jsoup.parse(site, url);
        if(document != null) {
            Element list = document.select("ol[class=\"item-section\"]").first();
            for(Element item : list.children()) {
                Element el;

                if ((item.select("div[class*=\"search-message\"]").first()) != null)
                    return;
                else if((el = item.select("div[class*=\"yt-lockup-video\"]").first()) != null)
                {
                    try{
                        if(!isLiveStream(el)) {
                            Element autor = item.select("div[class=\"yt-lockup-byline\"]").first() // Autor pjesme
                                    .select("a").first();
                            if(!autor.text().toLowerCase().contains("saregama")) {
                                Element title = el.select("h3").first().select("a").first(); // Naslov
                                Element duration = item.select("span[class*=\"video-time\"]").first(); // Dužina pjesme
                                Element link = el.select("h3")
                                        .first().select("a").first(); // Url pesme
                                String id = link.attr("abs:href").substring(32);
                                Music music = new Music();
                                music.setAuthor(autor.text());
                                music.setTitle(title.text());
                                music.setDownloaded(0);
                                music.setDuration(duration.text());
                                music.setId(id);
                                music.setViews(Utils.convertViewsToString(getViewCount(el)));
                                music.setUrlImage(getThumbnailUrl(id));
                                checkMusic(music);
                            }
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

    /**
     * Ovo je bolji nacin za provjeravanje pjesama dali su vec skinuti, nezelimo ako nade pjesmu
     * da nastavi pregledavat ostale pjesme nego samo iskocimo iz loopa jer smo pjesmu vec pronsali
     * @param music pjesma koja se mora provjerit dali je skinuta
     */
    private void checkMusic(Music music) {
        for(Music pjesma : checkList)
        {
            if(pjesma.getId().equals(music.getId()))
            {
                if(pjesma.getDownloaded() == 1)
                {
                    music.setPath(FileManager.getMediaPath(music.getId()));
                    music.setDownloaded(1);
                    break;
                }
            }
        }
        musicList.add(music);
    }

    private String download(String siteUrl) throws IOException
    {
        URL url = new URL(siteUrl);
        HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
        BufferedReader in = null;
        StringBuilder response = new StringBuilder();
        try {
            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", USER_AGENT);
            in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            while((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
        } catch(Exception e) {
            throw new IOException(e);
        } finally {
            if(in != null) {
                in.close();
            }
        }

        return response.toString();
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
