package com.stipess.youplay.youtube.loaders;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.content.AsyncTaskLoader;

import com.stipess.youplay.R;
import com.stipess.youplay.extractor.DownloaderTestImpl;

import org.schabi.newpipe.extractor.ListExtractor;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.comments.CommentsInfoItem;
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeCommentsExtractor;

import java.util.ArrayList;
import java.util.List;
/*
 * Created by Stjepan Stjepanovic
 * <p>
 * Copyright (C) Stjepan Stjepanovic 2017 <stipess@youplayandroid.com>
 * CommentsLoader.java is part of YouPlay.
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
public class CommentsLoader extends AsyncTaskLoader<List<CommentsInfoItem>> {

    private String url;
    private Context context;
    private boolean nextPage;
    private ListExtractor.InfoItemsPage<CommentsInfoItem> page;

    public CommentsLoader(@NonNull Context context, String url) {
        super(context);
        this.context = context;
        this.url = url;
    }

    public CommentsLoader(Context context, String url, ListExtractor.InfoItemsPage<CommentsInfoItem> page, boolean nextPage) {
        super(context);
        this.context = context;
        this.url = url;
        this.nextPage = nextPage;
        this.page = page;
    }

    public ListExtractor.InfoItemsPage<CommentsInfoItem> getPage() {
        return page;
    }

    @Nullable
    @Override
    public List<CommentsInfoItem> loadInBackground() {
        NewPipe.init(DownloaderTestImpl.getInstance());
        List<CommentsInfoItem> list = new ArrayList<>();

        try {
            YoutubeCommentsExtractor extractor = (YoutubeCommentsExtractor) ServiceList.YouTube.getCommentsExtractor(url);
            extractor.fetchPage();
            ListExtractor.InfoItemsPage<CommentsInfoItem> page;
            if(nextPage) {
                page = extractor.getPage(this.page.getNextPage());
            } else {
                page = extractor.getInitialPage();
            }

            this.page = page;

            for(CommentsInfoItem item : page.getItems()) {

                item.setTextualUploadDate(convertToTimeAgo(item.getTextualUploadDate()));
                list.add(item);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
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
}
