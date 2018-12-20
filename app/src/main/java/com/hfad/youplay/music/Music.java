package com.hfad.youplay.music;

import android.graphics.Bitmap;

import java.io.Serializable;

/**
 * Created by Stjepan on 27.11.2017..
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
