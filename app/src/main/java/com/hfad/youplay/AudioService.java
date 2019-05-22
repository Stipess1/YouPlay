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
import android.graphics.BitmapFactory;
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
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.RemoteViews;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.NotificationTarget;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
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
import com.hfad.youplay.player.AudioPlayer;
import com.hfad.youplay.radio.Station;
import com.hfad.youplay.utils.FileManager;
import com.hfad.youplay.utils.NotificationId;
import com.hfad.youplay.utils.Utils;

import java.io.File;
import java.util.ArrayList;

import static com.hfad.youplay.utils.Constants.*;

public class AudioService extends Service implements AudioManager.OnAudioFocusChangeListener
{

    private static final String TAG = AudioService.class.getSimpleName();

    private final IBinder binder = new AudioBinder();
    public static String SONG = "song";
    public static String ACTION = "action";
    public static String LIST = "list";

    private AudioPlayer audioPlayer;
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
    private MediaSessionCompat mediaSessionCompat;
    private int alarmCount = 0;
    private String currentTable = "";

    private AudioOutputListener outputListener;
    private NetworkStateListener networkStateListener;

    private boolean wasPlaying = false;
    private boolean isLoss = false;
    private static AudioService instance;
    private boolean isDestroyed;
    private boolean listenerAdded = false;

    private boolean replay = false;
    private boolean autoPlaybool = true;
    private boolean shuffled = false;
    private boolean alarm = false;
    private boolean alarmEnded = false;
    private boolean replaySong = false;


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
        audioPlayer = new AudioPlayer(this);
//        audioPlayer.setMusicList(realMusic);

        mediaSessionCompat = new MediaSessionCompat(this, getPackageName());
        mediaSessionCompat.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS);

        try {
            MediaControllerCompat mediaControllerCompat = new MediaControllerCompat(getApplicationContext(), mediaSessionCompat.getSessionToken());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        MediaSessionCompat.Callback mController = new MediaSessionCompat.Callback() {

            @Override
            public void onPlay() {
                mediaSessionCompat.setActive(true);
                setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);
                if (serviceCallback != null)
                    serviceCallback.callback(PLAY_PAUSE);
                else
                    audioPlayer.playWhenReady();
                updateNotification("", "");
            }

            @Override
            public void onPause() {
                setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED);
                if (serviceCallback != null)
                    serviceCallback.callback(PLAY_PAUSE);
                else
                    audioPlayer.playWhenReady();

                updateNotification("", "");
            }

            @Override
            public void onSkipToNext() {
                setMediaPlaybackState(PlaybackStateCompat.STATE_SKIPPING_TO_NEXT);
                if (serviceCallback != null)
                    serviceCallback.callback(NEXT);
                else
                    audioPlayer.nextSong();

                updateNotification("", "");
            }

            @Override
            public void onSkipToPrevious() {
                setMediaPlaybackState(PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS);
                if (serviceCallback != null)
                    serviceCallback.callback(PREVIOUS);
                else
                    audioPlayer.previousSong();

                updateNotification("", "");
            }

        };
        mediaSessionCompat.setCallback(mController);

        mediaSessionCompat.setMediaButtonReceiver(null);
        mediaSessionCompat.setActive(true);

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

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

    public AudioPlayer getAudioPlayer() {
        return audioPlayer;
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

    public void setReplaySong(boolean replaySong)
    {
        this.replaySong = replaySong;
    }

    public boolean isReplaySong()
    {
        return replaySong;
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


    /**
     * Funckija se prvi put izvrsi pri pokretanju aplikacije, te svaki puta kada se izabere pjesma ili postaja.
     * @param text ime postaje/pjesme
     * @param image URL od slike
     * @return Notifikacija
     */
    public Notification initNotification(String text, String image)
    {
        if(audioPlayer.getPlayWhenReady())
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

//        builder.setCustomContentView(remoteViews);
        return builder.build();
    }

    private void setMediaPlaybackState(int state) {
        PlaybackStateCompat.Builder playbackstateBuilder = new PlaybackStateCompat.Builder();
        switch (state) {
            case PlaybackStateCompat.STATE_PLAYING: {
                playbackstateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PAUSE
                        | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS);
                break;
            }
            default: {
                playbackstateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PLAY
                        | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS);
                break;
            }
        }

        playbackstateBuilder.setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 0);
        mediaSessionCompat.setPlaybackState(playbackstateBuilder.build());
    }

    private void setMusic()
    {
        if(!audioPlayer.isStream() && music.getPath() != null) {
            Bitmap bitmapImage = BitmapFactory.decodeFile(FileManager.getPicturePath(music.getId()));
            MediaMetadataCompat.Builder metadataCompat = new MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, music.getTitle())
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, music.getAuthor())
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, Utils.convertToMilis(music.getDuration()))
                    .putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, bitmapImage);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                metadataCompat.putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, audioPlayer.getMusicList().size());

            mediaSessionCompat.setMetadata(metadataCompat.build());

            setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);

            audioPlayer.playSong(music);

            Answers.getInstance().logCustom(new CustomEvent("Songs played"));
        } else {
            Bitmap bitmap = BitmapFactory.decodeFile(station.getIcon());
            MediaMetadataCompat.Builder metadataCompat = new MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, station.getName())
                    .putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, bitmap)
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, station.getCountry());
            mediaSessionCompat.setMetadata(metadataCompat.build());

            setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);

//            audioPlayer.playSong(station);
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
            if(intent.getSerializableExtra(SONG) != null && !audioPlayer.isStream())
            {
//                musicList = (ArrayList<Music>) intent.getSerializableExtra(LIST);
                music = (Music) intent.getSerializableExtra(SONG);
            }
            else if(audioPlayer.isStream() && intent.getSerializableExtra(SONG) != null)
                station = (Station) intent.getSerializableExtra(SONG);


            switch (intent.getIntExtra(ACTION,0))
            {
                case PLAY_SONG:
                    playSong();
                    break;
                case NEXT_SONG:
                    if(serviceCallback != null)
                        serviceCallback.callback(NEXT);
                    else {
                        audioPlayer.nextSong();
                        updateNotification(audioPlayer.getCurrentlyPlaying().getTitle(), FileManager.getPicturePath(audioPlayer.getCurrentlyPlaying().getId()));
                    }
                    break;
                case PREVIOUS_SONG:
                    if(serviceCallback != null)
                        serviceCallback.callback(PREVIOUS);
                    else {
                        audioPlayer.previousSong();
                        updateNotification(audioPlayer.getCurrentlyPlaying().getTitle(), FileManager.getPicturePath(audioPlayer.getCurrentlyPlaying().getId()));
                    }
                    break;
                case PLAY_PAUSE_SONG:

                    if(serviceCallback != null)
                        serviceCallback.callback(PLAY_PAUSE);
                    else
                        audioPlayer.playWhenReady();

                    updateNotification("", "");
                    break;
                case EXIT_APP:
                    stopSelf();
                    if(serviceCallback != null)
                        serviceCallback.callback(EXIT);
                    break;
                case ADS:
                    if(serviceCallback != null)
                        serviceCallback.callback(AD);
                    break;
                default:
                    updateNotification("","");
                    break;
            }

            MediaButtonReceiver.handleIntent(mediaSessionCompat, intent);

        }
        return START_NOT_STICKY;
    }

    @Override
    public void onAudioFocusChange(int focus)
    {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        switch (focus)
        {
            case AudioManager.AUDIOFOCUS_GAIN:
                if(!audioPlayer.getPlayWhenReady() && wasPlaying && !isLoss)
                {
                    mediaSessionCompat.setActive(true);
                    if(serviceCallback != null)
                        serviceCallback.callback(PLAY_PAUSE);
                    else
                        audioPlayer.playWhenReady();

                    updateNotification("","");
                    wasPlaying = false;
                }
                    break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                if(audioPlayer.getPlayWhenReady())
                {
                    if(serviceCallback != null)
                        serviceCallback.callback(PLAY_PAUSE);
                    else
                        audioPlayer.playWhenReady();

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
                    if(audioPlayer.getPlayWhenReady())
                    {
                        if(serviceCallback != null)
                            serviceCallback.callback(PLAY_PAUSE);
                        else
                            audioPlayer.playWhenReady();

                        updateNotification("","");
                        wasPlaying = true;
                        isLoss = true;
                    }
                    break;
        }
    }

    private void playSong()
    {
        if(audioPlayer.isStream())
            updateNotification(station.getName(), station.getIcon());
        else
            updateNotification(music.getTitle(), music.getId());

        setMusic();
    }

//    public void nextSong()
//    {
//        if(!isStream && music != null)
//        {
//            int pos;
//            for(Music pjesma : musicList)
//                if(pjesma.getId().equals(music.getId()))
//                {
//                    pos = musicList.indexOf(pjesma);
//                    if(pos+1 < musicList.size())
//                    {
//                        music = musicList.get(pos+1);
//                        if(music != null && music.getPath() != null)
//                            playSong();
//                        break;
//                    }
//                }
//        }
//        else if(isStream)
//        {
//            int pos;
//            for(Station station : stations)
//                if(station.getId().equals(this.station.getId()))
//                {
//                    pos = stations.indexOf(station);
//                    if(pos+1 < stations.size())
//                    {
//                        this.station = stations.get(pos+1);
//                        playSong();
//                        break;
//                    }
//                }
//        }
//    }
//
//    private void previousSong()
//    {
//        if(!isStream && music != null)
//        {
//            int pos;
//            for(Music pjesma : musicList)
//                if(pjesma.getId().equals(music.getId()))
//                {
//                    pos = musicList.indexOf(pjesma);
//                    if(pos-1 >= 0)
//                    {
//                        music = musicList.get(pos-1);
//                        if(music != null)
//                            playSong();
//                        break;
//                    }
//                }
//        }
//        else if(isStream)
//        {
//            int pos;
//            for(Station station : stations)
//                if(station.getId().equals(this.station.getId()))
//                {
//                    pos = stations.indexOf(station);
//                    if(pos-1 >= 0)
//                    {
//                        this.station = stations.get(pos-1);
//                        playSong();
//                        break;
//                    }
//                }
//        }
//    }


    @Override
    public void onDestroy()
    {
        if(manager != null)
            manager.cancelAll();

        audioManager.abandonAudioFocus(this);
        audioPlayer.stop();
        audioPlayer.release();
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
