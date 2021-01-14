package com.stipess.youplay.youtube.loaders;


import android.content.Context;

import androidx.loader.content.AsyncTaskLoader;

import com.stipess.youplay.R;
import com.stipess.youplay.extractor.DownloaderTestImpl;
import com.stipess.youplay.music.Music;
import com.stipess.youplay.utils.FileManager;
import com.stipess.youplay.utils.Utils;

import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.ListExtractor;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem;
import org.schabi.newpipe.extractor.search.SearchExtractor;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by Stjepan Stjepanovic on 10.12.2017..
 * <p>
 * Copyright (C) Stjepan Stjepanovic 2017 <stipess@youplayandroid.com>
 * YoutubeMusicLoader.java is part of YouPlay.
 * <p>
 * YouPlay is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * YouPlay is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with YouPlay.  If not, see <http://www.gnu.org/licenses/>.
 */
public class YoutubeMusicLoader extends AsyncTaskLoader<List<Music>> {

    private static final String TAG = "YoutubeMusicLoader";
    private String query;
    private ArrayList<Music> checkList;
    private Context context;
    private String[] c;
    private ListExtractor.InfoItemsPage<InfoItem> page;
    private List<Music> list;
    private boolean nextPage;

    public YoutubeMusicLoader(Context context, String query, ArrayList<Music> checkList)
    {
        super(context);
        this.query = query;
        this.context = context;
        this.checkList = checkList;
        c = new String[] {context.getResources().getString(R.string.short_thousand),
                context.getResources().getString(R.string.short_million),
                context.getResources().getString(R.string.short_billion)};
    }

    public YoutubeMusicLoader(Context context, ArrayList<Music> checkList, ListExtractor.InfoItemsPage<InfoItem> page, boolean nextPage, String query) {
        super(context);
        this.nextPage = nextPage;
        this.context = context;
        this.checkList = checkList;
        this.page = page;
        this.query = query;
        c = new String[] {context.getResources().getString(R.string.short_thousand),
                context.getResources().getString(R.string.short_million),
                context.getResources().getString(R.string.short_billion)};
    }

    public ListExtractor.InfoItemsPage<InfoItem> getPage() {
        return page;
    }

    @Override
    public List<Music> loadInBackground() {
        list = new ArrayList<>();

        if(NewPipe.getDownloader() == null)
            NewPipe.init(DownloaderTestImpl.getInstance());
        try{
            if(nextPage) {
                SearchExtractor extractor = ServiceList.YouTube.getSearchExtractor(query);
                extractor.fetchPage();

                page = extractor.getPage(page.getNextPage());
                addToList(page);

            } else {
                SearchExtractor extractor = ServiceList.YouTube.getSearchExtractor(query);
                extractor.fetchPage();
                ListExtractor.InfoItemsPage<InfoItem> page = extractor.getInitialPage();
                this.page = page;

                addToList(page);
            }
            return list;
        }
        catch (Exception e) {
            e.printStackTrace();
        }


        return list;
    }

    private void addToList(ListExtractor.InfoItemsPage<InfoItem> page) {
        for(InfoItem item : page.getItems()) {
            // moramo pogledat dali je dobiveni item video ili nesto drugo (channel, playlista...)
            if(item instanceof StreamInfoItem) {
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
                if(stream.getTextualUploadDate() != null)
                    music.setTimeAgo(convertToTimeAgo(Objects.requireNonNull(stream.getTextualUploadDate())));
                music.setViewsSearch(coolFormat(stream.getViewCount(), 0));

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
            } else if(item instanceof PlaylistInfoItem) {
                PlaylistInfoItem playlist = (PlaylistInfoItem) item;
            }
        }
    }

    private  String convertToTimeAgo(String time) {
        String[] vrijeme = time.split(" ");
        int num = Integer.parseInt(vrijeme[0]);

        if(time.contains("month ago") || time.contains("months ago")) {
            time = context.getResources().getQuantityString(R.plurals.months, num, num);
        }  else if(time.contains("week ago") || time.contains("weeks ago")) {
            time = context.getResources().getQuantityString(R.plurals.weeks, num, num);
        }  else if(time.contains("year ago") || time.contains("years ago")) {
            time = context.getResources().getQuantityString(R.plurals.years, num, num);
        } else if(time.contains("day ago") ||time.contains("days ago")) {
            time = context.getResources().getQuantityString(R.plurals.days, num, num);
        }

        return time;
    }

    private String coolFormat(double n, int iteration) {

        double d = ((long) n / 100) / 10.0;
        boolean isRound = (d * 10) %10 == 0;//true if the decimal part is equal to 0 (then it's trimmed anyway)
        return (d < 1000? //this determines the class, i.e. 'k', 'm' etc
                ((d > 99.9 || isRound || (!isRound && d > 9.99)? //this decides whether to trim the decimals
                        (int) d * 10 / 10 : d + "" // (int) d * 10 / 10 drops the decimal
                ) + "" + c[iteration])
                : coolFormat(d, iteration+1));

    }


}
