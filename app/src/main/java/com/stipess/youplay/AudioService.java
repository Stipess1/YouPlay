package com.stipess.youplay;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;

import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.JobIntentService;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.media.session.MediaButtonReceiver;

import com.liulishuo.filedownloader.FileDownloader;
import com.stipess.youplay.listeners.AudioOutputListener;
import com.stipess.youplay.listeners.ButtonListener;
import com.stipess.youplay.listeners.NetworkStateListener;
import com.stipess.youplay.music.Music;
import com.stipess.youplay.player.AudioPlayer;
import com.stipess.youplay.radio.Station;
import com.stipess.youplay.utils.FileManager;
import com.stipess.youplay.utils.NotificationId;
import com.stipess.youplay.utils.Utils;
//import com.liulishuo.filedownloader.FileDownloader;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import static com.stipess.youplay.utils.Constants.*;

public class AudioService extends JobIntentService implements AudioManager.OnAudioFocusChangeListener
{

    private static final String TAG = AudioService.class.getSimpleName();

    private final IBinder binder = new AudioBinder();
    public static String SONG = "song";
    public static String ACTION = "action";

    private AudioPlayer audioPlayer;
    private AudioManager audioManager;
    private static final int NOTIFICATION_ID = NotificationId.getID();

    public RemoteViews remoteViews;
    private ArrayList<Station> stations;
    private Music music;
    private Station station;
    private Notification notification;
    private NotificationManager manager;
    private ServiceCallback serviceCallback;
    private MediaSessionCompat mediaSessionCompat;

    private AudioOutputListener outputListener;
    private NetworkStateListener networkStateListener;

    private boolean wasPlaying = false;
    private boolean isLoss = false;
    private static AudioService instance;
    private boolean isDestroyed;

    private String image = "";
    private String title = "";
    private String author = "";
    private boolean firstTime = true;

    public AudioService()
    {

    }

    public static AudioService getInstance()
    {
        return instance;
    }

    @Override
    public IBinder onBind(@NonNull Intent intent)
    {
        return binder;
    }

    @Override
    public void onCreate()
    {
        instance = this;

        audioPlayer = new AudioPlayer(this);
//        audioPlayer.setMusicList(realMusic);

        mediaSessionCompat = new MediaSessionCompat(this, getPackageName());
        mediaSessionCompat.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS);

        try {
            MediaControllerCompat mediaControllerCompat = new MediaControllerCompat(getApplicationContext(), mediaSessionCompat.getSessionToken());
//            MediaControllerCompat.setMediaController(getApplication(), mediaSessionCompat.getController());

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
                updateNotification("", "", "");
            }

            @Override
            public void onPause() {
                setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED);
                if (serviceCallback != null)
                    serviceCallback.callback(PLAY_PAUSE);
                else
                    audioPlayer.playWhenReady();

                Log.d(TAG, "PAUSE song");
                updateNotification("", "", "");
            }

            @Override
            public void onSkipToNext() {
                setMediaPlaybackState(PlaybackStateCompat.STATE_SKIPPING_TO_NEXT);
                if (serviceCallback != null)
                    serviceCallback.callback(NEXT);
                else {
                    audioPlayer.nextSong();
                    updateNotification(audioPlayer.getCurrentlyPlaying().getTitle(),
                            FileManager.getPicturePath(audioPlayer.getCurrentlyPlaying().getId()),
                            audioPlayer.getCurrentlyPlaying().getAuthor());
                }

            }

            @Override
            public void onSkipToPrevious() {
                setMediaPlaybackState(PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS);
                if (serviceCallback != null)
                    serviceCallback.callback(PREVIOUS);
                else {
                    audioPlayer.previousSong();
                    updateNotification(audioPlayer.getCurrentlyPlaying().getTitle(),
                            FileManager.getPicturePath(audioPlayer.getCurrentlyPlaying().getId()),
                            audioPlayer.getCurrentlyPlaying().getAuthor());
                }

            }

            @Override
            public void onSeekTo(long pos) {
                audioPlayer.seekTo(pos);

                PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
                        .setState(PlaybackStateCompat.STATE_PLAYING, audioPlayer.getCurrentPosition(), 1.0f)
                        .setActions(PlaybackStateCompat.ACTION_PLAY |
                                PlaybackStateCompat.ACTION_PAUSE |
                                PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                                PlaybackStateCompat.ACTION_SEEK_TO |
                                PlaybackStateCompat.ACTION_PLAY_PAUSE);
                mediaSessionCompat.setPlaybackState(stateBuilder.build());
            }

        };
        mediaSessionCompat.setCallback(mController);

        mediaSessionCompat.setMediaButtonReceiver(null);
        mediaSessionCompat.setActive(true);

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

//        remoteViews = new RemoteViews(getApplication().getPackageName(), R.layout.custom_notification);
//        remoteViews.setImageViewResource(R.id.notification_image, R.mipmap.ic_launcher_round);
//        remoteViews.setInt(R.id.notification_layout, "setBackgroundColor", getResources().getColor(R.color.light_black));

        outputListener       = new AudioOutputListener();
        networkStateListener = new NetworkStateListener();

        registerReceiver(outputListener, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
        registerReceiver(networkStateListener, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        startForegroundService();

    }


    public interface ServiceCallback
    {
        void callback(String callback);
    }

    public void setCallback(ServiceCallback serviceCallback)
    {
        this.serviceCallback = serviceCallback;
    }

    public AudioPlayer getAudioPlayer() {
        return audioPlayer;
    }

    public void setDestroyed(boolean isDestroyed)
    {
        this.isDestroyed = isDestroyed;
    }

    public boolean isDestroyed()
    {
        return isDestroyed;
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

    public int getCurrentStreamPos()
    {
        for(Station station : stations)
            if(station.getId().equals(this.station.getId()))
                return stations.indexOf(station);

        return 0;
    }

    public void startForegroundService() {
        startForeground(NOTIFICATION_ID, initNotification("", "", ""));
        notification = initNotification("", "", "");
    }

    public void setPlaybackState(int statePlaying) {
        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
                .setState(statePlaying, audioPlayer.getCurrentPosition(), 1.0f)
                .setActions(PlaybackStateCompat.ACTION_PLAY |
                        PlaybackStateCompat.ACTION_PAUSE |
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                        PlaybackStateCompat.ACTION_SEEK_TO |
                        PlaybackStateCompat.ACTION_PLAY_PAUSE);
        mediaSessionCompat.setPlaybackState(stateBuilder.build());
    }


    /**
     * Funckija se prvi put izvrsi pri pokretanju aplikacije, te svaki puta kada se izabere pjesma ili postaja.
     * @param title ime postaje/pjesme
     * @param image URL od slike
     * @return Notifikacija
     */
    public Notification initNotification(String title, String image, String author) {
        File fileImage = FileManager.getPictureFile(image);
        if(!title.equals("") && !image.equals("")) {
            Log.d(TAG, "Author: " + author + " , " + image + " , " + title);
            this.author = author;
            this.image = image;
            this.title = title;
        }
        int drawable;
        if(audioPlayer.getPlayWhenReady())
        {
            drawable = R.drawable.pause;
            audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            if(!this.title.equals("") && !this.image.equals("")) {
                PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
                        .setState(PlaybackStateCompat.STATE_PLAYING, audioPlayer.getCurrentPosition(), 1.0f)
                        .setActions(PlaybackStateCompat.ACTION_PLAY |
                                PlaybackStateCompat.ACTION_PAUSE |
                                PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                                PlaybackStateCompat.ACTION_SEEK_TO |
                                PlaybackStateCompat.ACTION_PLAY_PAUSE);
                mediaSessionCompat.setPlaybackState(stateBuilder.build());
            }
        }
        else {
            drawable = R.drawable.play;
            if(!this.title.equals("") && !this.image.equals("")) {
                PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
                        .setState(PlaybackStateCompat.STATE_PAUSED, audioPlayer.getCurrentPosition(), 0)
                        .setActions(PlaybackStateCompat.ACTION_PLAY |
                                PlaybackStateCompat.ACTION_PAUSE |
                                PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                                PlaybackStateCompat.ACTION_SEEK_TO |
                                PlaybackStateCompat.ACTION_PLAY_PAUSE);
                mediaSessionCompat.setPlaybackState(stateBuilder.build());
            }
        }

        Intent play_pause = new Intent(this, ButtonListener.class);
        play_pause.putExtra(ButtonListener.BUTTON, PLAY_PAUSE);
        PendingIntent play_pause_btn = PendingIntent.getBroadcast(getApplicationContext(), 111, play_pause, 0);

        Intent next = new Intent(this, ButtonListener.class);
        next.putExtra(ButtonListener.BUTTON, NEXT);
        PendingIntent next_btn = PendingIntent.getBroadcast(getApplicationContext(), 112, next, 0);

        Intent previous = new Intent(this, ButtonListener.class);
        previous.putExtra(ButtonListener.BUTTON, PREVIOUS);
        PendingIntent previous_btn = PendingIntent.getBroadcast(getApplicationContext(), 113, previous, 0);

        Intent cancel = new Intent(this, ButtonListener.class);
        cancel.putExtra(ButtonListener.BUTTON, EXIT);
        PendingIntent cancel_btn = PendingIntent.getBroadcast(getApplicationContext(), 114, cancel, 0);

        String id = "";
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            int importance = NotificationManager.IMPORTANCE_LOW;

            id = "5385";
            NotificationChannel channel = new NotificationChannel(id, "YouPlay", importance);
            channel.setDescription("Music player");
            channel.setShowBadge(false);

            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);

        }

        Bitmap bitmap = BitmapFactory.decodeFile(FileManager.getPicturePath(this.image));
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "222")
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentTitle(this.title)
                .setContentText(this.author)
                .setLargeIcon(bitmap)
                .addAction(R.drawable.previous, "previous", previous_btn)
                .addAction(drawable, "Play-Pause", play_pause_btn)
                .addAction(R.drawable.next , "next", next_btn)
                .addAction(R.drawable.cancel, "cancel", cancel_btn)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle().setMediaSession(mediaSessionCompat.getSessionToken()).setShowActionsInCompactView(0,1,2))
                .setOngoing(true);
        if(id.equals("5385"))
            builder.setChannelId(id);

        Intent actionIntent = new Intent(this, MainActivity.class);
        PendingIntent actionPendingIntent = PendingIntent.getActivity(this, 0, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(actionPendingIntent);

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
                    .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmapImage);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                metadataCompat.putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, audioPlayer.getMusicList().size());

            if(music.getDownloaded() == 1)
                metadataCompat.putLong(MediaMetadataCompat.METADATA_KEY_DOWNLOAD_STATUS, MediaDescriptionCompat.STATUS_DOWNLOADED);
            else
                metadataCompat.putLong(MediaMetadataCompat.METADATA_KEY_DOWNLOAD_STATUS, MediaDescriptionCompat.STATUS_NOT_DOWNLOADED);

            audioPlayer.playSong(music);

            mediaSessionCompat.setMetadata(metadataCompat.build());

            setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);
        } else {
            
            MediaMetadataCompat.Builder metadataCompat = new MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, station.getName())
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, station.getCountry());
            mediaSessionCompat.setMetadata(metadataCompat.build());

            setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);
            Log.d(TAG, "SET STATION");
            audioPlayer.playSong(station);
        }
    }

    public void updateNotification(String title, String image, String author)
    {
        isLoss = false;
        notification = initNotification(title, image, author);
        manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if(manager != null)
            manager.notify(NOTIFICATION_ID, notification);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {

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
                    playSong(music, station);
                    break;
                case NEXT_SONG:
                    if(serviceCallback != null) {
                        serviceCallback.callback(NEXT);
                    }
                    else {
                        audioPlayer.nextSong();
                        if(audioPlayer.getCurrentlyPlaying() != null)
                            updateNotification(audioPlayer.getCurrentlyPlaying().getTitle(),
                                    audioPlayer.getCurrentlyPlaying().getId(),
                                    audioPlayer.getCurrentlyPlaying().getAuthor());
                    }
                    break;
                case PREVIOUS_SONG:
                    if(serviceCallback != null) {
                        serviceCallback.callback(PREVIOUS);
                    }
                    else {
                        audioPlayer.previousSong();
                        if(audioPlayer.getCurrentlyPlaying() != null)
                            updateNotification(audioPlayer.getCurrentlyPlaying().getTitle(),
                                    audioPlayer.getCurrentlyPlaying().getId(),
                                    audioPlayer.getCurrentlyPlaying().getAuthor());
                    }
                    break;
                case PLAY_PAUSE_SONG:

                    if(serviceCallback != null)
                        serviceCallback.callback(PLAY_PAUSE);
                    else
                        audioPlayer.playWhenReady();

                    updateNotification("", "", "");
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
                    updateNotification("","", "");
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
        Log.d(TAG, "UNPLUGGED PLUGGED " + focus);
        switch (focus)
        {
            case AudioManager.AUDIOFOCUS_GAIN:
                Log.d(TAG, "AUDIOFOCUS GAIN");
                if(!audioPlayer.getPlayWhenReady() && wasPlaying && !isLoss)
                {

                    mediaSessionCompat.setActive(true);
                    if(serviceCallback != null)
                        serviceCallback.callback(PLAY_PAUSE);
                    else
                        audioPlayer.playWhenReady();

                    updateNotification("","", "");
                    wasPlaying = false;
                }
                    break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                Log.d(TAG, "AUDIOFOCUS LOSS TRANSIENT");
                if(audioPlayer.getPlayWhenReady())
                {
                    wasPlaying = true;
                    if(serviceCallback != null)
                        serviceCallback.callback(PLAY_PAUSE);
                    else
                        audioPlayer.playWhenReady();

                    updateNotification("","", "");
//                    Log.d(TAG, audioManager.getMode() +" MODE");
//                    switch (audioManager.getMode())
//                    {
//                        case AudioManager.MODE_RINGTONE:
//                            case AudioManager.MODE_IN_CALL:
//                                wasPlaying = true;
//                                break;
//                    }
                    if(preferences.getBoolean("sound_mode", false))
                        wasPlaying = false;
                }
                break;

                case AudioManager.AUDIOFOCUS_LOSS:
                    Log.d(TAG, "AUDIOFOCUS LOSS 1");
                    if(audioPlayer.getPlayWhenReady())
                    {
                        if(serviceCallback != null)
                            serviceCallback.callback(PLAY_PAUSE);
                        else
                            audioPlayer.playWhenReady();

                        updateNotification("","", "");
                        Log.d(TAG, "AUDIOFOCUS LOSS ");
                        wasPlaying = true;
                        isLoss = true;
                    }
                    break;
        }
    }

    public void playSong(Music music, Station station)
    {
        this.music = music;
        this.station = station;

        setMusic();

        if(audioPlayer.isStream())
            updateNotification(station.getName(), station.getIcon(), "");
        else
            updateNotification(music.getTitle(), music.getId(), music.getAuthor());

    }


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
        FileDownloader.getImpl().unBindService();
        int pid = android.os.Process.myPid();
        android.os.Process.killProcess(pid);
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
