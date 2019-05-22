package com.hfad.youplay.player;

import android.content.Context;
import android.net.Uri;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.hfad.youplay.AudioService;
import com.hfad.youplay.music.Music;
import com.hfad.youplay.radio.Station;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AudioPlayer implements Player.EventListener{

    AudioService audioService = AudioService.getInstance();

    private SimpleExoPlayer exoPlayer;
    // Lista pjesama
    private ArrayList<Music> musicList = new ArrayList<>();
    // Kopija lista pjesama
    private ArrayList<Music> copyList = new ArrayList<>();
    // Lista radio postaji
    private ArrayList<Station> stationList;

    private PlayerListener playerState;

    private boolean autoplay = true;
    private boolean shuffled = false;
    private boolean mediaCompleted = false;

    private Replay replay = Replay.REPLAY_OFF;
    // Pjesma koja trenutno svira
    private Music currentlyPlaying;

    private Station currentlyPlayingStation;
    // Pozicija trenutne pjesme u nizu
    private int position = 0;
    // Zadnja pjesma koja je svirala
    private int lastPost = 0;
    // Koliko ce pjesma svirat prije nego sto se player zaustavi
    private int alarm = 0;
    // Dali je alarm postavljen
    private boolean isAlarm = false;

    private boolean isStream = false;
    // Koju listu player trenutno reproducira
    private String currentTable;

    private Context context;

    public interface PlayerListener {
        // kada je player spreman za reprodukciju
        void onReady();
        // kada player ucitava pjesmu / radio
        void onBuffering();
        // kada sljedeca pjesma pocne svirat
        void onNextSong(Music music);
        // kada prijasnja pjesma pocne svirat
        void onPreviousSong(Music music);
        // pjesma koja se treba skinut
        void downloadSong(Music music);
    }

    public enum Replay {
        // Kada je replay off
        REPLAY_OFF,
        // Replay sve pjesme
        REPLAY_ALL,
        // Replay samo pjesmu koja trenutno svira
        REPLAY_ONE
    }

    public AudioPlayer(Context context) {
        this.context = context;
        exoPlayer = ExoPlayerFactory.newSimpleInstance(context, new DefaultRenderersFactory(context), new DefaultTrackSelector(), new DefaultLoadControl());
        exoPlayer.addListener(this);
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {

        switch (playbackState) {

            case Player.STATE_ENDED:
                if(autoplay) {
                    if(isAlarm){
                        alarm-=1;
                        if(alarm == 0) {
                            isAlarm = false;
                            mediaCompleted = true;
                            break;
                        }
                    } else if (replay == Replay.REPLAY_ONE) {
                        playSong(currentlyPlaying);
                        break;
                    }

                    if(!isAlarm)
                        nextSong();

                    isAlarm = false;

                }
                mediaCompleted = true;
                break;

            case Player.STATE_READY:
                if(playerState != null)
                    playerState.onReady();
                break;
            case Player.STATE_BUFFERING:
                if(playerState != null)
                    playerState.onBuffering();
                break;

        }
    }

    /**
     * Zaustavlja player ili ga pokrece zavisi o booealnu
     */
    public void playWhenReady() {
        if(exoPlayer.getPlayWhenReady())
            exoPlayer.setPlayWhenReady(false);
        else
            exoPlayer.setPlayWhenReady(true);
    }

    public void setPlayWhenReady(boolean ready) {
        exoPlayer.setPlayWhenReady(ready);
    }

    public boolean getPlayWhenReady() {
        return exoPlayer.getPlayWhenReady();
    }

    public void release() {
        exoPlayer.release();
        context = null;
    }

    public void stop() {
        exoPlayer.stop();
    }

    public void seekTo(long milis) {
        exoPlayer.seekTo(milis);
    }

    public long getCurrentPosition() {
        return exoPlayer.getCurrentPosition();
    }

    public long getDuration() {
        return exoPlayer.getDuration();
    }

    /**
     * Postavi listu pjesama koji ce player odsvirat i odma kopiraj u copyListu
     * @param musicList lista pjesama
     */
    public void setMusicList(ArrayList<Music> musicList) {
        Log.d("AudioPlayer" , " set music list");
        this.musicList = musicList;
        copyList.clear();
        copyList.addAll(musicList);
    }

    public void setStationList(ArrayList<Station> stationList) {
        this.stationList = stationList;
    }

    public ArrayList<Station> getStationList() {
        return stationList;
    }

    public Station getCurrentlyPlayingStation() {
        return currentlyPlayingStation;
    }

    public void setCurrentlyPlayingStation(Station currentlyPlayingStation) {
        this.currentlyPlayingStation = currentlyPlayingStation;
    }

    public ArrayList<Music> getMusicList() {
        return musicList;
    }

    /**
     * Postavi pjesmu koja ce trenutno svirat
     * @param music pjesma koja ce svirat
     */
    public void playSong(Music music) {
        Log.d("AudioPlayer", "PLAY SONG");
        currentlyPlaying = music;
        currentlyPlayingStation = null;
        Uri uri = Uri.parse(music.getPath());
        DefaultDataSourceFactory dataSourceFactory = new DefaultDataSourceFactory(context, Util.getUserAgent(context, "YouPlay"), null);
        ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
        MediaSource mediaSource = new ExtractorMediaSource(uri, dataSourceFactory, extractorsFactory, null, null);
        exoPlayer.setAudioStreamType(C.STREAM_TYPE_MUSIC);
        exoPlayer.prepare(mediaSource);
        exoPlayer.setPlayWhenReady(true);
    }

    public void playSong(Station station) {
        currentlyPlayingStation = station;
        currentlyPlaying = null;
        Uri uri = Uri.parse(station.getUrl());
        DefaultDataSourceFactory dataSourceFactory = new DefaultDataSourceFactory(context, Util.getUserAgent(context, "YouPlay"), null);
        ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
        MediaSource mediaSource = new ExtractorMediaSource(uri, dataSourceFactory, extractorsFactory, null, null);
        exoPlayer.setAudioStreamType(C.STREAM_TYPE_MUSIC);
        exoPlayer.prepare(mediaSource);
        exoPlayer.setPlayWhenReady(true);
    }

    public void setPlayerState(PlayerListener playerState) {
        this.playerState = playerState;
    }

    public void setShuffled(boolean shuffled) {
        this.shuffled = shuffled;
    }

    public void setReplay(Replay replay) {
        this.replay = replay;
    }

    public void setAutoplay(boolean autoplay) {
        this.autoplay = autoplay;
    }

    public void setAlarm(boolean isAlarm) {
        this.isAlarm = isAlarm;
    }

    public void setAlarm(int alarm) {
        this.alarm = alarm;
    }

    public void setStream(boolean isStream) {
        this.isStream = isStream;
    }

    public void nextSong() {
        position = position + 1;
        if(!isStream) {
            if(position >= musicList.size()) {
                position = musicList.size() - 1;
                return;
            }
            Music music = musicList.get(position);

            if(autoplay) {
                if(music.getDownloaded() == 1) {
                    currentlyPlaying = music;
                    if(playerState == null)
                        playSong(music);
                } else
                if(playerState != null)
                    playerState.downloadSong(music);
            }
            if(playerState != null)
                playerState.onNextSong(music);
        } else {
            if(position >= stationList.size()) {
                position = stationList.size() - 1;
                return;
            }
            Station station = stationList.get(position);
            playSong(station);
        }

    }

    public void previousSong() {
        position = position - 1;
        if(!isStream) {
            if(position < 0) {
                position = 0;
                return;
            }

            Music music = musicList.get(position);
            if(music.getDownloaded() == 1) {
                currentlyPlaying = music;
                if(playerState == null)
                    playSong(music);
            } else {
                if(playerState != null)
                    playerState.downloadSong(music);
            }

            if(playerState != null)
                playerState.onPreviousSong(music);
        } else {
            if(position < 0) {
                position = 0;
                return;
            }
            Station station = stationList.get(position);
            playSong(station);
        }

    }

    public void shuffle() {
        Collections.shuffle(musicList);
        for(Music pjesma : musicList) {
            if(pjesma.getId().equals(currentlyPlaying.getId())) {
                musicList.remove(pjesma);
                break;
            }
        }
        musicList.add(0, currentlyPlaying);
        position = 0;
        shuffled = true;
        for(Music pjesma : musicList)
            Log.d("AudioPlayer", "title: " + pjesma.getTitle());
    }

    public void unShuffle() {
        musicList.clear();
        musicList.addAll(copyList);
        for(Music pjesma : musicList) {
            if(pjesma.getId().equals(currentlyPlaying.getId())) {
                position = musicList.indexOf(pjesma);
                break;
            }
        }
        shuffled = false;
    }

    public boolean isStream() {
        return isStream;
    }

    public Music getCurrentlyPlaying() {
        return currentlyPlaying;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public void setLastPost(int lastPost) {
        this.lastPost = lastPost;
    }

    public int getLastPost() {
        return lastPost;
    }

    public int getPosition() {
        return position;
    }

    public boolean isAutoplay() {
        return autoplay;
    }

    public boolean isShuffled() {
        return shuffled;
    }

    public boolean isAlarm() {
        return isAlarm;
    }

    public Replay getReplay() {
        return replay;
    }
}
