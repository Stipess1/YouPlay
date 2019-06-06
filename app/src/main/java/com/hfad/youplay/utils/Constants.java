package com.hfad.youplay.utils;



/**
 * Created by Stjepan on 7.12.2017..
 */

public class Constants {

    private Constants(){}

    public static final String APP_NAME = "YouPlay";
    public static final int PERMISSION_ALL = 77;
    public static final String FORMAT = ".mp3";
    public static final String MEDIA_FORMAT = ".jpg";
    public static final String NO_MEDIA = ".nomedia";
    public static final String DATABASE = "database";

    public static final String TABLE_NAME = "SONGS";
    public static final String ID = "ID";
    public static final String TITLE = "TITLE";
    public static final String AUTHOR = "AUTHOR";
    public static final String DURATION = "DURATION";
    public static final String DOWNLOADED = "DOWNLOADED";
    public static final String VIEWS = "VIEWS";

    public static final String NEXT = "next";
    public static final String PREVIOUS = "previous";
    public static final String PLAY_PAUSE = "play_pause";
    public static final String EXIT = "exit";
    public static final String AD = "ad";
    public static final int NEXT_SONG = 2;
    public static final int PREVIOUS_SONG = 3;
    public static final int PLAY_PAUSE_SONG = 1;
    public static final int PLAY_SONG = 6;
    public static final int EXIT_APP = 4;
    public static final int ADS = 5;

    // History Fragment
    public static final int DIALOG_NOW_PLAYING = 0;
    public static final int DIALOG_ADD_QUEUE = 1;
    public static final int DIALOG_PLAYLIST = 2;
    public static final int DIALOG_YOUTUBE = 3;
    public static final int DIALOG_DELETE = 4;

    //Playlist Fragment
    public static final int DIALOG_PLAYLIST_DELETE = 0;
    public static final int DIALOG_PLAYLIST_RENAME = 1;

    public static final int DIALOG_TABLE_DELETE = 1;

    public static final String API_KEY = "AIzaSyCT6umUAVwA62xDL0_u6RAX7Xne8YLWjW8";

    public static final String SUGESS_URL_FIRST = "http://suggestqueries.google.com/complete/search?hl=en&ds=yt&client=youtube&hjson=t&cp=1&q=";
    public static final String SUGESS_URL_SECON = "&format=5&alt=json";
    public static final String RQST_METHOD = "GET";

    // Fragments name
    public static final String PLAY_FRAGMENT = "PlayFragment";
    public static final String HISTORY_FRAGMENT = "HistoryFragment";
    public static final String RADIO_FRAGMENT = "RadioFragment";

    public static final String CACHE_MODE = "cache_mode";
    public static final String WEBSITE = "website";
    public static final String CONTRIBUTE = "contribute";
    public static final String VERSION_CHECK = "version_check";
    public static final String SEND_EMAIL = "send_email";

    public static final String DOWNLOAD_LINK = "https://youplayandroid.com/download/YouPlay.apk";


//    public static final String[] FRAGMENTS = {
//            "History",
//            "Playlist",
//            "Radio_Country",
//            "Search_Country"
//    };

}
