package com.stipess.youplay.music;

import android.graphics.Bitmap;

import java.io.Serializable;

/**
 * Created by Stjepan Stjepanovic on 27.11.2017..
 * <p>
 * Copyright (C) Stjepan Stjepanovic 2017 <stipess@youplayandroid.com>
 * Music.java is part of YouPlay.
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

public class Music implements Serializable{

    private String title;
    private String author;
    private String duration;
    private String id;
    private String views;
    private Bitmap image;
    private String url;
    private String path;
    private int downloaded;
    private String timeAgo;
    private String viewsSearch;

    public Music(String title, String author, String duration, String id, String views, Bitmap image)
    {
        this.title = title;
        this.author = author;
        this.duration = duration;
        this.views = views;
        this.id = id;
        this.image = image;
    }

    public Music(String title, String author, String duration, String id, String views, int downloaded)
    {
        this.title = title;
        this.author = author;
        this.duration = duration;
        this.views = views;
        this.id = id;
        this.downloaded = downloaded;
    }

    public Music(){}

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    // Samo se koristi u searchu
    public String getTimeAgo() {
        return timeAgo;
    }

    public void setTimeAgo(String timeAgo) {
        this.timeAgo = timeAgo;
    }

    public String getViewsSearch() {
        return viewsSearch;
    }

    public void setViewsSearch(String viewsSearch) {
        this.viewsSearch = viewsSearch;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getViews() {
        return views;
    }

    public void setViews(String views)
    {
        this.views = views;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getId()
    {
        return id;
    }

    @Deprecated
    public Bitmap getImage() {
        return image;
    }

    @Deprecated
    public void setImage(Bitmap image) {
        this.image = image;
    }

    public void setUrlImage(String url)
    {
        this.url = url;
    }

    public String getUrlImage()
    {
        return url;
    }

    public void setDownloaded(int downloaded)
    {
        this.downloaded = downloaded;
    }

    public int getDownloaded()
    {
        return downloaded;
    }

    public String getPath()
    {
        return path;
    }

    public void setPath(String path)
    {
        this.path = path;
    }

}
