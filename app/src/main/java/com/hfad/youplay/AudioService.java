package com.hfad.youplay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.VolumeProviderCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;
import android.widget.RemoteViews;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.NotificationTarget;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.audio.AudioListener;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.hfad.youplay.listeners.AudioOutputListener;
import com.hfad.youplay.listeners.ButtonListener;
import com.hfad.youplay.listeners.NetworkStateListener;
import com.hfad.youplay.music.Music;
import com.hfad.youplay.radio.Station;
import com.hfad.youplay.utils.FileManager;
import com.hfad.youplay.utils.NotificationId;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static com.hfad.youplay.utils.Constants.*;

public class AudioService extends Service implements AudioManager.OnAudioFocusChangeListener
{

    private static final String TAG = AudioService.class.getSimpleName();

    private final IBinder binder = new AudioBinder();
    public static String SONG = "song";
    public static String ACTION = "action";
    public static String LIST = "list";

    public SimpleExoPlayer exoPlayer;
    private AudioManager audioManager;
    private static final int NOTIFICATION_ID = NotificationId.getID();

    public RemoteViews remoteViews;
    private ArrayList<Music> musicList;
    // Ovu listu dohvacamo kada se aplikacija ponovno pokrene ili activity.
    // Zbog toga sto musicList moze biti izmjesan ili sl.
    private ArrayList<Music> realMusic = new ArrayList<>();
    private ArrayList<Station> stations;
    private Music music;
    private Station station;
    private Notification notification;
    private NotificationManager manager;
    private ServiceCallback serviceCallback;
    private Player.EventListener eventListener;
    MediaControllerCompat mediaControllerCompat;
    private int alarmCount = 0;
    private String currentTable = "";

    private AudioOutputListener outputListener;
    private NetworkStateListener networkStateListener;

    private boolean wasPlaying = false;
    private boolean isLoss = false;
    private static AudioService instance;
    private boolean isStream = false;
    private boolean isDestroyed;
    private boolean listenerAdded = false;

    private boolean replay = false;
    private boolean autoPlaybool = true;
    private boolean shuffled = false;
    private boolean alarm = false;
    private boolean alarmEnded = false;


    public AudioService()
    {

    }

    public static AudioService getInstance()
    {
        return instance;
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return binder;
    }

    @Override
    public void onCreate()
    {
        Log.d("MainActivity", "On Service create");
        MediaSessionCompat mediaSessionCompat = new MediaSessionCompat(this, getPackageName());
        mediaSessionCompat.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS);

        try {
            mediaControllerCompat = new MediaControllerCompat(getApplicationContext(), mediaSessionCompat.getSessionToken());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        MediaSessionCompat.Callback mController = new MediaSessionCompat.Callback() {
            @Override
            public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
//                KeyEvent event = mediaButtonEvent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
//                if(event.getAction() == KeyEvent.ACTION_DOWN)
//                {
//                    if(serviceCallback != null)
//                        serviceCallback.callback(PLAY_PAUSE);
//                    else
//                        playPauseSong();
//
//                    updateNotification("", "");
//                    return true;
//                }

                return super.onMediaButtonEvent(mediaButtonEvent);
            }

            @Override
            public void onPlay() {
                playPauseSong();
                updateNotification("", "");
            }

            @Override
            public void onPause() {
                playPauseSong();
                updateNotification("", "");
            }

            @Override
            public void onSkipToNext() {
                nextSong();
            }

            @Override
            public void onSkipToPrevious() {
                previousSong();
            }
        };
        mediaSessionCompat.setCallback(mController);
        mediaSessionCompat.setActive(true);

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        exoPlayer = ExoPlayerFactory.newSimpleInstance(this,new DefaultRenderersFactory(this) ,new DefaultTrackSelector(), new DefaultLoadControl());

        remoteViews = new RemoteViews(getApplication().getPackageName(), R.layout.custom_notification);
        remoteViews.setImageViewResource(R.id.notification_image, R.mipmap.ic_launcher_round);
        remoteViews.setInt(R.id.notification_layout, "setBackgroundColor", getResources().getColor(R.color.light_black));

        outputListener       = new AudioOutputListener();
        networkStateListener = new NetworkStateListener();

        registerReceiver(outputListener, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
        registerReceiver(networkStateListener, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        startForegroundService();

        instance = this;

    }


    public interface ServiceCallback
    {
        void callback(String callback);

    }

    public void setCallback(ServiceCallback serviceCallback)
    {
        this.serviceCallback = serviceCallback;
    }


    public Player.EventListener getEventListener() {
        return eventListener;
    }

    public void setEventListener(Player.EventListener eventListener) {
        this.eventListener = eventListener;
    }

    public boolean isAlarmEnded() {
        return alarmEnded;
    }

    public void setAlarmEnded(boolean alarmEnded) {
        this.alarmEnded = alarmEnded;
    }

    public String getCurrentTable()
    {
        return currentTable;
    }

    public void setCurrentTable(String currentTable)
    {
        this.currentTable = currentTable;
    }

    public int getAlarmCount() {
        return alarmCount;
    }

    public void setAlarmCount(int alarmCount) {
        this.alarmCount = alarmCount;
    }

    public boolean isAlarm() {
        return alarm;
    }

    public void setAlarm(boolean alarm) {
        this.alarm = alarm;
    }

    public boolean isReplay() {
        return replay;
    }

    public void setReplay(boolean replay) {
        this.replay = replay;
    }

    public boolean isAutoPlaybool() {
        return autoPlaybool;
    }

    public void setAutoPlaybool(boolean autoPlaybool) {
        this.autoPlaybool = autoPlaybool;
    }

    public void setListenerAdded(boolean listenerAdded)
    {
        this.listenerAdded = listenerAdded;
    }

    public boolean getListenerAdded()
    {
        return listenerAdded;
    }

    public void setDestroyed(boolean isDestroyed)
    {
        this.isDestroyed = isDestroyed;
    }

    public boolean isDestroyed()
    {
        return isDestroyed;
    }

    public ArrayList<Music> getMusicList()
    {
        return musicList;
    }

    public void setMusicList(ArrayList<Music> musicList)
    {
        this.musicList = musicList;
    }

    public void setRealMusic(ArrayList<Music> realMusic)
    {
        this.realMusic = realMusic;
    }

    public ArrayList<Music> getRealMusic() {
        return realMusic;
    }

    public boolean isShuffled() {
        return shuffled;
    }

    public void setShuffled(boolean shuffled)
    {
        this.shuffled = shuffled;
    }

    public Music getMusic()
    {
        return music;
    }

    public Station getStation() {
        return station;
    }

    public ArrayList<Station> getStations() {
        return stations;
    }

    public void setStations(ArrayList<Station> stations)
    {
        this.stations = stations;
    }

    public int getCurrentSongPos()
    {
        for(Music pjesma : musicList)
            if(pjesma.getId().equals(music.getId()))
                return musicList.indexOf(pjesma);
        return 0;
    }

    public int getCurrentStreamPos()
    {
        for(Station station : stations)
            if(station.getId().equals(this.station.getId()))
                return stations.indexOf(station);

        return 0;
    }

    public void startForegroundService()
    {
        startForeground(NOTIFICATION_ID, initNotification("", ""));
        notification = initNotification("", "");
    }

    public void isStream(boolean isStream)
    {
        this.isStream = isStream;
    }

    public boolean getIsStream()
    {
        return isStream;
    }

    /**
     * Funckija se prvi put izvrsi pri pokretanju aplikacije, te svaki puta kada se izabere pjesma ili postaja.
     * @param text ime postaje/pjesme
     * @param image URL od slike
     * @return Notifikacija
     */
    public Notification initNotification(String text, String image)
    {
        if(exoPlayer.getPlayWhenReady())
        {
            audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            remoteViews.setInt(R.id.play_pause_button, "setBackgroundResource", R.drawable.pause);
        }
        else
            remoteViews.setInt(R.id.play_pause_button, "setBackgroundResource", R.drawable.play);

        // Provjeri da li dobivena pjesma ima put do pjesme (neke postaje nemaju URL od slike pa se ovaj dio preskoci to kasnije pogledati.
        if(image.length() > 0)
        {
            remoteViews.setInt(R.id.notification_image, "setBackgroundColor", ContextCompat.getColor(getApplicationContext(), R.color.black_b));
            File fileImage = FileManager.getPictureFile(image);

            if(!fileImage.exists())
            {
                try{
                    Glide.with(this).asBitmap().load(image).into(new SimpleTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            NotificationTarget target = new NotificationTarget(getApplicationContext()
                                    , R.id.notification_image
                                    , remoteViews
                                    , notification
                                    , NOTIFICATION_ID);
                            Glide.with(getApplicationContext()).asBitmap().apply(new RequestOptions().error(R.mipmap.ic_launcher)).load(resource).into(target);
                        }

                        @Override
                        public void onLoadFailed(@Nullable Drawable errorDrawable) {
                            NotificationTarget target = new NotificationTarget(getApplicationContext()
                                    , R.id.notification_image
                                    , remoteViews
                                    , notification
                                    , NOTIFICATION_ID);
                            Glide.with(getApplicationContext()).asBitmap().load(R.mipmap.ic_launcher).into(target);
                        }
                    });
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
            else
            {

                Glide.with(this).asBitmap().load(fileImage).apply(new RequestOptions().override(60,100)).into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        NotificationTarget target = new NotificationTarget(getApplicationContext()
                                , R.id.notification_image
                                , remoteViews
                                , notification
                                , NOTIFICATION_ID);
                        Glide.with(getApplicationContext()).asBitmap().load(resource).into(target);
                    }
                });
            }

            remoteViews.setTextViewText(R.id.notification_title, text);
        }

        Intent play_pause = new Intent(this, ButtonListener.class);
        play_pause.putExtra(ButtonListener.BUTTON, PLAY_PAUSE);
        PendingIntent play_pause_btn = PendingIntent.getBroadcast(getApplicationContext(), 111, play_pause, 0);
        remoteViews.setOnClickPendingIntent(R.id.play_pause_button, play_pause_btn);

        Intent next = new Intent(this, ButtonListener.class);
        next.putExtra(ButtonListener.BUTTON, NEXT);
        PendingIntent next_btn = PendingIntent.getBroadcast(getApplicationContext(), 112, next, 0);
        remoteViews.setOnClickPendingIntent(R.id.next_button, next_btn);

        Intent previous = new Intent(this, ButtonListener.class);
        previous.putExtra(ButtonListener.BUTTON, PREVIOUS);
        PendingIntent previous_btn = PendingIntent.getBroadcast(getApplicationContext(), 113, previous, 0);
        remoteViews.setOnClickPendingIntent(R.id.previous_button, previous_btn);

        Intent cancel = new Intent(this, ButtonListener.class);
        cancel.putExtra(ButtonListener.BUTTON, EXIT);
        PendingIntent cancel_btn = PendingIntent.getBroadcast(getApplicationContext(), 114, cancel, 0);
        remoteViews.setOnClickPendingIntent(R.id.cancel_button, cancel_btn);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(false)
                .setCustomContentView(remoteViews)
                .setOngoing(true);

        Intent actionIntent = new Intent(this, MainActivity.class);
        PendingIntent actionPendingIntent = PendingIntent.getActivity(this, 0, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(actionPendingIntent);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            int importance = NotificationManager.IMPORTANCE_LOW;

            String id = "222";
            NotificationChannel channel = new NotificationChannel(id, "YouPlay", importance);
            channel.setDescription("Music player");

            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
            builder.setChannelId(id);
        }

        builder.setCustomContentView(remoteViews);
        return builder.build();
    }

    /**
     * Postavlja pjesmu ili postaju po dobivenoj lokaciji pjesme/postaje
     * @param path lokacija pjesme
     */
    private void setMusic(String path)
    {
        // Ako stream zavrsi i prebaci na drugu pjesmu dok je activity unisten
        if(path != null)
        {
            Uri uri = Uri.parse(path);
            DefaultDataSourceFactory dataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, "YouPlay"), null);
            ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
            MediaSource mediaSource = new ExtractorMediaSource(uri, dataSourceFactory, extractorsFactory, null, null);
            exoPlayer.setAudioStreamType(C.STREAM_TYPE_MUSIC);

            exoPlayer.prepare(mediaSource);
            exoPlayer.setPlayWhenReady(true);

            Answers.getInstance().logCustom(new CustomEvent("Songs played"));
        }
    }

    public void updateNotification(String title, String image)
    {
        isLoss = false;
        notification = initNotification(title, image);
        manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if(manager != null)
            manager.notify(NOTIFICATION_ID, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        if(intent != null)
        {
            if(intent.getSerializableExtra(LIST) != null && intent.getSerializableExtra(SONG) != null && !isStream)
            {
                musicList = (ArrayList<Music>) intent.getSerializableExtra(LIST);
                realMusic = (ArrayList<Music>) intent.getSerializableExtra(LIST);
                music = (Music) intent.getSerializableExtra(SONG);
            }
            else if(isStream && intent.getSerializableExtra(SONG) != null)
                station = (Station) intent.getSerializableExtra(SONG);


            switch (intent.getIntExtra(ACTION,0))
            {
                case PLAY_SONG:
                    playSong();
                    break;
                case NEXT_SONG:
                    if(!isDestroyed())
                        serviceCallback.callback(NEXT);
                    else
                        nextSong();
                    break;
                case PREVIOUS_SONG:
                    if(!isDestroyed())
                        serviceCallback.callback(PREVIOUS);
                    else
                        previousSong();
                    break;
                case PLAY_PAUSE_SONG:

                    if(!isDestroyed())
                        serviceCallback.callback(PLAY_PAUSE);
                    else
                        playPauseSong();

                    updateNotification("", "");
                    break;
                case EXIT_APP:
                    stopSelf();
                    if(!isDestroyed())
                        serviceCallback.callback(EXIT);
                    break;
                case ADS:
                    if(!isDestroyed())
                        serviceCallback.callback(AD);
                    break;
                default:
                    updateNotification("","");
                    break;
            }

        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onAudioFocusChange(int focus)
    {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        switch (focus)
        {
            case AudioManager.AUDIOFOCUS_GAIN:
                if(!exoPlayer.getPlayWhenReady() && wasPlaying && !isLoss)
                {
                    if(serviceCallback != null)
                        serviceCallback.callback(PLAY_PAUSE);
                    else
                        playPauseSong();

                    updateNotification("","");
                    wasPlaying = false;
                }
                    break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                if(exoPlayer.getPlayWhenReady())
                {
                    if(serviceCallback != null)
                        serviceCallback.callback(PLAY_PAUSE);
                    else
                        playPauseSong();

                    updateNotification("","");
                    switch (audioManager.getMode())
                    {
                        case AudioManager.MODE_RINGTONE:
                            case AudioManager.MODE_IN_CALL:
                                wasPlaying = true;
                                break;
                    }
                    if(preferences.getBoolean("sound_mode", false))
                        wasPlaying = true;
                }
                break;

                case AudioManager.AUDIOFOCUS_LOSS:
                    if(exoPlayer.getPlayWhenReady())
                    {
                        if(serviceCallback != null)
                            serviceCallback.callback(PLAY_PAUSE);
                        else
                            playPauseSong();

                        updateNotification("","");
                        wasPlaying = true;
                        isLoss = true;
                    }
                    break;
        }
    }

    private void playSong()
    {
        if(isStream)
        {
            setMusic(station.getUrl());
            updateNotification(station.getName(), station.getIcon());
        }
        else
        {
            setMusic(music.getPath());
            updateNotification(music.getTitle(), music.getId());
        }
    }

    public void playPauseSong()
    {
        if(exoPlayer.getPlayWhenReady())
            exoPlayer.setPlayWhenReady(false);
        else
            exoPlayer.setPlayWhenReady(true);
    }

    public void nextSong()
    {
        if(!isStream && music != null)
        {
            int pos;
            for(Music pjesma : musicList)
                if(pjesma.getId().equals(music.getId()))
                {
                    pos = musicList.indexOf(pjesma);
                    if(pos+1 < musicList.size())
                    {
                        music = musicList.get(pos+1);
                        if(music != null && music.getPath() != null)
                            playSong();
                        break;
                    }
                }
        }
        else if(isStream)
        {
            int pos;
            for(Station station : stations)
                if(station.getId().equals(this.station.getId()))
                {
                    pos = stations.indexOf(station);
                    if(pos+1 < stations.size())
                    {
                        this.station = stations.get(pos+1);
                        playSong();
                        break;
                    }
                }
        }
    }

    private void previousSong()
    {
        if(!isStream && music != null)
        {
            int pos;
            for(Music pjesma : musicList)
                if(pjesma.getId().equals(music.getId()))
                {
                    pos = musicList.indexOf(pjesma);
                    if(pos-1 >= 0)
                    {
                        music = musicList.get(pos-1);
                        if(music != null)
                            playSong();
                        break;
                    }
                }
        }
        else if(isStream)
        {
            int pos;
            for(Station station : stations)
                if(station.getId().equals(this.station.getId()))
                {
                    pos = stations.indexOf(station);
                    if(pos-1 >= 0)
                    {
                        this.station = stations.get(pos-1);
                        playSong();
                        break;
                    }
                }
        }
    }


    @Override
    public void onDestroy()
    {
        if(manager != null)
            manager.cancelAll();

        audioManager.abandonAudioFocus(this);
        exoPlayer.stop();
        exoPlayer.release();
        instance = null;
        unregisterReceiver(outputListener);
        unregisterReceiver(networkStateListener);
        super.onDestroy();
    }

    public class AudioBinder extends Binder
    {
        AudioService getAudio()
        {
            return AudioService.this;
        }
    }

}
