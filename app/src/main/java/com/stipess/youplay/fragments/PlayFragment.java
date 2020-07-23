package com.stipess.youplay.fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.collection.ArrayMap;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import android.support.v4.media.session.PlaybackStateCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.webkit.URLUtil;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.google.android.material.snackbar.Snackbar;
import com.stipess.youplay.AudioService;
import com.stipess.youplay.Ilisteners.OnCommentClicked;
import com.stipess.youplay.Ilisteners.OnDataChanged;
import com.stipess.youplay.Ilisteners.OnItemClicked;
import com.stipess.youplay.Ilisteners.OnMusicSelected;
import com.stipess.youplay.Ilisteners.OnRadioSelected;
import com.stipess.youplay.MainActivity;
import com.stipess.youplay.R;
import com.stipess.youplay.adapter.CommentAdapter;
import com.stipess.youplay.adapter.PlaylistAdapter;
import com.stipess.youplay.database.DatabaseHandler;
import com.stipess.youplay.database.YouPlayDatabase;
import com.stipess.youplay.music.Music;
import com.stipess.youplay.player.AudioPlayer;
import com.stipess.youplay.radio.Country;
import com.stipess.youplay.radio.Station;
import com.stipess.youplay.utils.Constants;
import com.stipess.youplay.utils.FileManager;
import com.stipess.youplay.utils.Utils;
import com.stipess.youplay.youtube.loaders.CommentsLoader;
import com.stipess.youplay.youtube.loaders.UrlLoader;
import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.FileDownloadListener;
import com.liulishuo.filedownloader.FileDownloadQueueSet;
import com.liulishuo.filedownloader.FileDownloader;

import org.schabi.newpipe.extractor.ListExtractor;
import org.schabi.newpipe.extractor.comments.CommentsInfoItem;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import pub.devrel.easypermissions.EasyPermissions;

import static com.stipess.youplay.utils.Constants.APP_NAME;
import static com.stipess.youplay.utils.Constants.FORMAT;
import static com.stipess.youplay.utils.Constants.PLAY_PAUSE_SONG;
import static com.stipess.youplay.utils.Constants.TABLE_NAME;
import static com.stipess.youplay.utils.Utils.*;


public class PlayFragment extends BaseFragment implements View.OnClickListener,
        OnMusicSelected, OnRadioSelected, OnDataChanged, AudioPlayer.PlayerListener, OnCommentClicked {

    public static final String TAG = PlayFragment.class.getSimpleName();
    private static final String YOUTUBELINK = "https://www.youtube.com/watch?v=";

    private AudioPlayer audioPlayer;
    private ImageView currentlyPlaying;
    private FrameLayout play_pause;
    private FrameLayout shuffle;
    private FrameLayout replayF;
    private FrameLayout volume;
    private FrameLayout alarm;
    private ProgressBar bar;
    private FrameLayout comments;
    private FrameLayout playlist;
    private TextView commentError;
    private TextView commentResult;
    private ProgressBar suggestionBar;
    public TextView currentlyTitle, durationTime, durationTimeCurrent, autoPlay;
    public SeekBar seekbar;
    public static Runnable runnable;
    public static Handler handler;
    private ArrayList<Music> tempList;
    private int lastPost;
    private int currentProgress;
    private boolean wasPlaying = false;
    private PlaylistAdapter adapter;
    private ArrayList<Station> stations;
    private OnItemClicked onItemClicked;
    // funkcija zadrzava komentare od trenutne pjesme
    private List<CommentsInfoItem> list = new ArrayList<>();
    private UrlLoader urlLoader;

    private Context context;
    private RecyclerView recyclerView;
    private boolean isCommentsClick = false;
    private boolean isSuggestionClick = true;

    private boolean slided = false;
    private float currentHeight;
    private float layoutHeight;
    private boolean mediaCompleted = false;
    private AudioService audioService;
    private YouPlayDatabase db;
    private boolean deleteMedia;
    private LinearLayoutManager linearLayoutManager;
    private ArrayMap<String, List<CommentsInfoItem>> cache = new ArrayMap<>();
    private ArrayMap<String, ListExtractor.InfoItemsPage<CommentsInfoItem>> lastPage = new ArrayMap<>();
    private ListExtractor.InfoItemsPage<CommentsInfoItem> page;
    private boolean loading;
    private CommentAdapter commentAdapter;
    private boolean isMoreComments = false;

    public static Music currentlyPlayingSong;

    public PlayFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
        onItemClicked = (OnItemClicked) getActivity();
    }

    public boolean isSlided() {
        return slided;
    }


    public ArrayList<Station> getStations()
    {
        return stations;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_play, container, false);
        handler = new Handler();
        play_pause = view.findViewById(R.id.play_pause_layout);
        play_pause.setOnClickListener(this);
        FrameLayout next = view.findViewById(R.id.you_next);
        next.setOnClickListener(this);
        FrameLayout previous = view.findViewById(R.id.you_previous);
        previous.setOnClickListener(this);
        shuffle = view.findViewById(R.id.you_shuffle);
        shuffle.setOnClickListener(this);
        replayF = view.findViewById(R.id.replay);
        replayF.setOnClickListener(this);
        autoPlay = view.findViewById(R.id.autoplay);
        autoPlay.setOnClickListener(this);
        volume   = view.findViewById(R.id.volume);
        volume.setOnClickListener(this);
        comments = view.findViewById(R.id.comments);
        comments.setOnClickListener(this);
        playlist = view.findViewById(R.id.suggestions);
        playlist.setOnClickListener(this);
        FrameLayout addToPlaylist = view.findViewById(R.id.add_playlist);
        addToPlaylist.setOnClickListener(this);
        alarm = view.findViewById(R.id.alarm);
        alarm.setOnClickListener(this);
        commentError = view.findViewById(R.id.comment_no_result);
        commentResult = view.findViewById(R.id.no_result);

        AudioManager audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        int streamVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        if(streamVolume == 0)
            volume.setForeground(getResources().getDrawable(R.drawable.volume_mute));

        ConstraintLayout playFragment = view.findViewById(R.id.play_fragment);
        playFragment.setOnClickListener(this);

        stations = new ArrayList<>();
        tempList = new ArrayList<>();

        audioService = AudioService.getInstance();
        if(audioService != null)
            audioPlayer = audioService.getAudioPlayer();

        if(audioPlayer != null)
            if(audioService != null && audioPlayer.getMusicList() != null && !audioPlayer.isStream())
            {
                if(audioPlayer.isShuffled())
                    shuffle.setForeground(getResources().getDrawable(R.drawable.shuffle_pressed));

                tempList.clear();
                if(audioPlayer.getCurrentlyPlaying() != null)
                    tempList.addAll(audioPlayer.getMusicList());
            }

        adapter = new PlaylistAdapter(getContext(), R.layout.play_fragment_list, tempList, PlaylistAdapter.ListType.SUGGESTIONS);

        if(audioService != null && audioService.getStations() != null && audioPlayer.isStream())
            refreshList(audioService.getStations());

        durationTime        = view.findViewById(R.id.duration_time);
        durationTimeCurrent = view.findViewById(R.id.duration_time_current);
        currentlyPlaying    = view.findViewById(R.id.currently_playing_image);
        currentlyTitle      = view.findViewById(R.id.currently_playing_title);
        seekbar             = view.findViewById(R.id.currently_playing_duration);
        recyclerView        = view.findViewById(R.id.play_suggestion_list);
        bar                 = view.findViewById(R.id.play_loading_bar);
        suggestionBar       = view.findViewById(R.id.suggestion_loading_bar);

        currentlyTitle.setSelected(true);
        adapter.setListener(this, this);
        adapter.setOnSwipeListener(new PlaylistAdapter.OnSwipeListener() {
            @Override
            public void onSwipe(int position) {
                setCurrentDeleted(position);
//                audioPlayer.setMusicList(new ArrayList<>(tempList));
                audioPlayer.getMusicList().remove(position);
            }
        });
        recyclerView.setAdapter(adapter);
        linearLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(linearLayoutManager);

        ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if(!isCommentsClick) return;

                int visibleItemCount = linearLayoutManager.getChildCount();
                int totalItemCount = linearLayoutManager.getItemCount();
                int pastVisibleItems = linearLayoutManager.findFirstVisibleItemPosition();
                if(pastVisibleItems + visibleItemCount >= totalItemCount && !loading && isMoreComments) {
                    loading = true;

                    Log.d(TAG, "KRAJ LISTE");
                    loadMoreSongs();
                }
            }
        });
        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean input)
            {
                if(input && !audioPlayer.isStream())
                {
                    currentProgress = progress;
                    durationTimeCurrent.setText(convertDuration(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar)
            {
                if(audioService.getAudioPlayer().getPlayWhenReady() && !audioPlayer.isStream())
                {
                    audioService.setPlaybackState(PlaybackStateCompat.STATE_PAUSED);
                    audioService.getAudioPlayer().setPlayWhenReady(false);
                    wasPlaying = true;
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {
                if(!audioPlayer.isStream())
                {
                    audioService.getAudioPlayer().seekTo(currentProgress);
                    audioService.setPlaybackState(PlaybackStateCompat.STATE_PLAYING);
                    if(wasPlaying)
                    {
                        audioService.getAudioPlayer().setPlayWhenReady(true);
                        wasPlaying = false;
                        playCycle();
                    }
                }
            }
        });
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if(!slided && newState == RecyclerView.SCROLL_STATE_DRAGGING)
                    slide();
            }
        });


        if(audioService != null && audioPlayer != null && !audioPlayer.isStream() && audioPlayer.getCurrentlyPlaying() != null)
        {
            setCurrent(audioPlayer.getPosition());
            Log.d(TAG, "AudioService listener musiclist");
            setDestroyedScreenSong();
        }
        else if(audioService != null && audioService.getStations() != null)
        {
            setCurrent(audioService.getCurrentStreamPos());
            adapter.setPlay(PlaylistAdapter.ListType.STATIONS);
            setDestroyedScreenStream();
        }

//        ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
        return view;
    }

    private void loadMoreSongs() {
        recyclerView.post(new Runnable() {
            @Override
            public void run() {
                list.add(null);
                commentAdapter.notifyItemInserted(list.size() - 1);
            }
        });
        String url = Constants.YOUTUBELINK+audioPlayer.getCurrentlyPlaying().getId();
        CommentsLoader commentsLoader = new CommentsLoader(getContext(), url, page, true);
        getLoaderManager().restartLoader(77, null, new LoaderManager.LoaderCallbacks<List<CommentsInfoItem>>() {
            @NonNull
            @Override
            public Loader<List<CommentsInfoItem>> onCreateLoader(int id, @Nullable Bundle args) {
                return commentsLoader;
            }

            @Override
            public void onLoadFinished(@NonNull Loader<List<CommentsInfoItem>> loader, List<CommentsInfoItem> data) {

                Log.d(TAG, "list size: " + list.size());
                list.remove(list.size()-1);
                commentAdapter.notifyItemRemoved(list.size() - 1);

                list.addAll(data);
                if(data.size() > 0)
                    cache.put(audioPlayer.getCurrentlyPlaying().getId(), new ArrayList<>(list));
                commentAdapter.notifyDataSetChanged();
                suggestionBar.setVisibility(View.GONE);
                page = commentsLoader.getPage();
                if(data.size() > 0)
                    lastPage.put(audioPlayer.getCurrentlyPlaying().getId(), page);

                if(data.size() == 0)
                    isMoreComments = false;

                loading = false;
            }

            @Override
            public void onLoaderReset(@NonNull Loader<List<CommentsInfoItem>> loader) {

            }
        }).forceLoad();
    }

    /*
        Nakon što se activity unisti pjesma dalje moze nastavit svirat, ova funkcija ce postavit
        sliku, ime pjesme itd. nakon što se ponovno pokrene aplikacija.
         */
    private void setDestroyedScreenSong()
    {
        currentlyPlayingSong = audioPlayer.getCurrentlyPlaying();
        Log.d(TAG, "setDestroyedScreenSong");

        Glide.with(this).load(FileManager.getPictureFile(currentlyPlayingSong.getId())).apply(new RequestOptions().skipMemoryCache(true).error(R.mipmap.ic_launcher)).into(currentlyPlaying);

        currentlyTitle.setText(currentlyPlayingSong.getTitle());
        durationTime.setText(currentlyPlayingSong.getDuration());
        durationTimeCurrent.setText(R.string.you_temp_time);
        ((MainActivity)getActivity()).pager.setCurrentItem(0);

        if(audioService.getAudioPlayer().getPlayWhenReady())
            play_pause.setForeground(getResources().getDrawable(R.drawable.pause));

        else
            play_pause.setForeground(getResources().getDrawable(R.drawable.play));


        AudioPlayer.Replay replay = audioPlayer.getReplay();

        if(replay == AudioPlayer.Replay.REPLAY_ALL)
            replayF.setForeground(getResources().getDrawable(R.drawable.replay_pressed));
        else if(replay == AudioPlayer.Replay.REPLAY_ONE)
            replayF.setForeground(getResources().getDrawable(R.drawable.replay_all));
        else
            replayF.setForeground(getResources().getDrawable(R.drawable.replay));

        if(audioPlayer.isAutoplay())
            autoPlay.setTextColor(getResources().getColor(R.color.seekbar_progress));
        else
            autoPlay.setTextColor(getResources().getColor(R.color.suggestions));

        seekbar.setMax((int)audioService.getAudioPlayer().getDuration());
        seekbar.setProgress((int) audioService.getAudioPlayer().getCurrentPosition());
        if(audioService.getAudioPlayer().isAlarm())
            alarm.setForeground(getResources().getDrawable(R.drawable.alarm_add));
        if(currentlyPlayingSong.getDownloaded() == 1)
            seekbar.setSecondaryProgress(seekbar.getMax());

        durationTimeCurrent.setText(convertDuration(audioService.getAudioPlayer().getCurrentPosition()));
        playCycle();
    }

    /*
    Nakon što se activity unisti stream dalje moze nastavit svirat, ova funkcija ce postavit
    sliku, ime streama itd. nakon što se ponovno pokrene aplikacija.
     */
    private void setDestroyedScreenStream()
    {
        Station station = audioService.getStation();
        Glide.with(this).load(station.getIcon()).apply(new RequestOptions().skipMemoryCache(true).error(R.mipmap.ic_launcher)).into(currentlyPlaying);
        currentlyTitle.setText(station.getName());

        durationTimeCurrent.setText(R.string.you_temp_time);
        seekbar.setProgress(0);

        ((MainActivity)getActivity()).pager.setCurrentItem(0);

        if(audioService.getAudioPlayer().isAlarm())
            alarm.setForeground(getResources().getDrawable(R.drawable.alarm_add));

        if(audioService.getAudioPlayer().getPlayWhenReady())
            play_pause.setForeground(getResources().getDrawable(R.drawable.pause));
        else
            play_pause.setForeground(getResources().getDrawable(R.drawable.play));

        durationTimeCurrent.setText(convertDuration(audioService.getAudioPlayer().getCurrentPosition()));
        playCycle();
    }

    /**
     * Povecaj ili smanji jacinu zvuka pjesme
     * @param code keyCode broj
     */
    public void setVolume(int code)
    {
        AudioManager audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);

        if(code == KeyEvent.KEYCODE_VOLUME_UP)
        {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
            this.volume.setForeground(getResources().getDrawable(R.drawable.volume_up));
        }
        else
        {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
            int volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            if(volume == 0)
                this.volume.setForeground(getResources().getDrawable(R.drawable.volume_mute));
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setRetainInstance(true);
        db = YouPlayDatabase.getInstance();
    }

    @Override
    public void onClickStation(Station station, View v)
    {
        final int position = stations.indexOf(station);

        if(audioService.getAudioPlayer().getPlayWhenReady())
            audioService.getAudioPlayer().stop();

        audioPlayer.setStream(true);
        audioPlayer.playSong(station);
        setSong(station);
        setCurrent(position);

        if(slided)
            slide();
    }

    @Override
    public void initAudioService() {
        audioService = AudioService.getInstance();
        audioPlayer = audioService.getAudioPlayer();
        audioPlayer.setPlayerState(this);
    }


    @Override
    public void onInfoClicked(Station station) {

    }

    @Override
    public void onClickCountry(Country country, View v) {

    }

    public void setMusic(Music pjesma, List<Music> pjesme)
    {

        if(!db.ifItemExists(pjesma.getId()) && pjesma.getDownloaded() == 0)
        {
            Log.d(TAG, "Prvi if");
            downloadSong(pjesma, true);
        }
        // Kad dodamo iz url valid i kad pritisenmo na ne skinutu pjesmu radi samo jedanput.
        else if(db.ifItemExists(pjesma.getId()) && pjesma.getDownloaded() == 0 && !URLUtil.isValidUrl(pjesma.getPath()))
        {
            Log.d(TAG, "Drugi if " + " path: " + pjesma.getPath());
            deleteAndDownload(pjesma);
            if(!tempList.equals(pjesme))
                adapter.reloadList(pjesme);

        }
        else
        {
            if(!tempList.equals(pjesme))
                adapter.reloadList(pjesme);
            setSong(pjesma);
        }
    }

    public void setStream(Station pjesma, ArrayList<Station> stations)
    {
        if(!this.stations.equals(stations))
        {
            this.stations.clear();
            this.stations.addAll(stations);
            adapter.setStations(stations);
            audioService.setStations(stations);
        }
        setCurrent(getStationPos(pjesma, stations));
        audioPlayer.playSong(pjesma);
        setSong(pjesma);
    }

    private int getStationPos(Station pjesma, ArrayList<Station> stations)
    {
        for(Station station : stations)
            if(station.getId().equals(pjesma.getId()))
            {
                return stations.indexOf(station);
            }
            return 0;
    }

    @Override
    public void onClick(Music pjesma, View view)
    {
        audioPlayer.setPosition(tempList.indexOf(pjesma));

        if(audioService.getAudioPlayer().getPlayWhenReady())
            audioService.getAudioPlayer().stop();

        FileDownloader.getImpl().pauseAll();
        audioPlayer.setStream(false);
        if(pjesma.getDownloaded() == 1){
            audioPlayer.playSong(pjesma);
            setSong(pjesma);
        }
        else
            downloadSong(pjesma, false);

        if(slided)
            slide();
    }

    @Override
    public void onInfoClicked(int position, View v)
    {

    }

    @Override
    public void onLongClick(Music pjesma, View v)
    {

        // Dodati nesto drugo posto koristimo sad swipe da uklonimo pjesmu
//        if(tempList.size() > 0 && !pjesma.equals(currentlyPlayingSong))
//        {
//            int position = tempList.indexOf(pjesma);
//            tempList.remove(pjesma);
//            adapter.notifyItemRemoved(position);
//            adapter.notifyItemRangeChanged(position, tempList.size());
//
//            setCurrentDeleted(tempList.indexOf(currentlyPlayingSong));
//        }

    }

    @Override
    public void onShuffle() {

    }

    /**
     * Kada obrisemo pjesmu prilikom swipe u ekranu za reproduciranje
     * moramo obavjestit adapter i time ponovno istaknuti koja pjesma se
     * reproducira
     * @param position pozicija na kojoj se pjesma nalazi
     */
    private void setCurrentDeleted(int position){
        audioPlayer.setPosition(position);

        adapter.setCurrent(position);

        if(position >= 0)
        {
            adapter.notifyItemChanged(lastPost);
            // tako da mozemo postavit grey BG na trenutnu pjesmu
            adapter.notifyItemChanged(position);
        }

        lastPost = position;
        audioPlayer.setLastPost(lastPost);
        adapter.setLastCurrent(lastPost);
    }

    private void setCurrent(final int position)
    {
        audioPlayer.setPosition(position);

        Log.d(TAG, "SCroll " + position) ;
        if(!slided && isSuggestionClick)
            recyclerView.scrollToPosition(position);

        if(isCommentsClick)
            onCommentClick(false);
        adapter.setCurrent(position);

        if(position >= 0)
        {
            Log.d(TAG, "SetCurrent postiion ");
            adapter.notifyItemChanged(audioPlayer.getLastPost());
            // tako da mozemo postavit grey BG na trenutnu pjesmu
            adapter.notifyItemChanged(position);
        }

        lastPost = position;
        audioPlayer.setLastPost(lastPost);
        adapter.setLastCurrent(lastPost);
    }

    public void refreshList(List<Station> postaje)
    {
        tempList.clear();
        adapter.setStations(postaje);
    }

    public void refreshList(ArrayList<Music> pjesme, boolean queue)
    {
        adapter.reloadList(pjesme);
        audioPlayer.setMusicList(pjesme);

        if(!queue)
            setCurrent(0);
        else
        {
            // Posto nece nac currentlyplaying song u dobivenoj listi moramo usporedivat id !
            Log.d(TAG, "queue je true");
            if(currentlyPlayingSong != null)
            {
                for(Music data : pjesme)
                {
                    if(data.getId().equals(currentlyPlayingSong.getId()))
                    {
                        setCurrent(pjesme.indexOf(data));
                    }
                }
            }
            else
            {
                Log.d(TAG, "queue je true setsong");
                setSong(pjesme.get(0));
            }
        }

    }

    @Override
    public void onClick(View view) {
        switch (view.getId())
        {
            case R.id.play_pause_layout:
                playPauseSong(true);
                break;
            case R.id.you_next:
                nextSong();
                break;
            case R.id.you_previous:
                previousSong();
                break;
            case R.id.replay:
                replay();
                break;
            case R.id.you_shuffle:
                // Ako korisnik nije izabro pjesmu iz search-a
                if(currentlyPlayingSong != null &&
                        adapter.getPlay() != PlaylistAdapter.ListType.STATIONS)
                {

                    if(!audioPlayer.isShuffled()) {
                        audioPlayer.setLastPost(audioPlayer.getPosition());
                        audioPlayer.setPosition(0);
                        setCurrent(0);
                        audioPlayer.shuffle();
                        tempList.clear();
                        tempList.addAll(audioPlayer.getMusicList());
                        adapter.notifyDataSetChanged();
                        shuffle.setForeground(getResources().getDrawable(R.drawable.shuffle_pressed));
                    }
                    else
                    {
                        audioPlayer.unShuffle();
                        tempList.clear();
                        tempList.addAll(audioPlayer.getMusicList());
                        adapter.notifyDataSetChanged();
                        this.shuffle.setForeground(getResources().getDrawable(R.drawable.shuffle));
                        setCurrent(audioPlayer.getPosition());
                    }
                }
                break;
            case R.id.play_fragment:
                if(slided)
                    slide();
                break;
            case R.id.autoplay:
                autoPlay();
                break;
            case R.id.volume:
                if(getContext() != null)
                {
                    AudioManager audio = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
                    audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI);
                }
                break;
            case R.id.add_playlist:
                if(currentlyPlayingSong != null)
                    buildPlaylistDialog(currentlyPlayingSong);
                break;
            case R.id.alarm:
                if(audioService.getAudioPlayer().getAlarm() > 0 && audioService.getAudioPlayer().isAlarm())
                {
                    audioService.getAudioPlayer().setAlarm(false);
                    alarm.setForeground(getResources().getDrawable(R.drawable.alarm_add));
                    Toast.makeText(getContext(), getResources().getString(R.string.alarm_disabled), Toast.LENGTH_SHORT).show();
                }
                else
                    buildAlarmDialog();
                break;
            case R.id.suggestions:
                onSuggestionClick();
                break;
            case R.id.comments:
                onCommentClick(true);
                break;
        }
    }

    private void onSuggestionClick() {
        if(isSuggestionClick) return;

        if(isCommentsClick) {
            isCommentsClick = false;
            comments.setForeground(getResources().getDrawable(R.drawable.ic_comment));
        }
        commentError.setVisibility(View.GONE);
        commentResult.setVisibility(View.GONE);
        isSuggestionClick = true;
        playlist.setForeground(getResources().getDrawable(R.drawable.ic_playlist_pressed));
        recyclerView.swapAdapter(null, true);
        recyclerView.setAdapter(adapter);

    }

    @Override
    public void onCommentClicked(CommentsInfoItem item, View view) {
//        View tekst = linearLayoutManager.findViewByPosition(list.indexOf(item));
        if(view != null) {
            TextView textView = view.findViewById(R.id.comment_text);
            if(textView.getMaxLines() == 2 && textView.getText().equals(item.getCommentText()))
                textView.setMaxLines(Integer.MAX_VALUE);
            else if(textView.getText().equals(item.getCommentText()))
                textView.setMaxLines(2);
        }
    }

    private void onCommentClick(boolean click) {
        if(isCommentsClick && click) return;

        if(isSuggestionClick) {
            isSuggestionClick = false;
            playlist.setForeground(getResources().getDrawable(R.drawable.ic_playlist));
        }
        isCommentsClick = true;
        comments.setForeground(getResources().getDrawable(R.drawable.ic_comment_pressed));
        recyclerView.swapAdapter(null, true);

        if(audioPlayer.getCurrentlyPlaying() != null ) {
            if(!internetConnection()) {
                commentError.setVisibility(View.VISIBLE);
                return;
            }

            if(cache.containsKey(audioPlayer.getCurrentlyPlaying().getId())) {
                String id = audioPlayer.getCurrentlyPlaying().getId();
                list.clear();
                list.addAll(Objects.requireNonNull(cache.get(id)));
                this.page = lastPage.get(id);
                if(commentAdapter == null) {
                    commentAdapter = new CommentAdapter(getContext(), R.layout.comment_adapter_view, list);
                    commentAdapter.setListener(this);
                }
                recyclerView.setAdapter(commentAdapter);
                commentAdapter.notifyDataSetChanged();
                return;
            }

            String url = Constants.YOUTUBELINK+audioPlayer.getCurrentlyPlaying().getId();
            suggestionBar.setVisibility(View.VISIBLE);
            CommentsLoader commentsLoader = new CommentsLoader(context, url);
            getLoaderManager().restartLoader(77, null, new LoaderManager.LoaderCallbacks<List<CommentsInfoItem>>() {
                @NonNull
                @Override
                public Loader<List<CommentsInfoItem>> onCreateLoader(int id, @Nullable Bundle args) {
                    return commentsLoader;
                }

                @Override
                public void onLoadFinished(@NonNull Loader<List<CommentsInfoItem>> loader, List<CommentsInfoItem> data) {

                    list.clear();
                    list.addAll(data);

                    commentAdapter = new CommentAdapter(getContext(), R.layout.comment_adapter_view, list);
                    commentAdapter.setListener(PlayFragment.this);
                    // ako korisnik ponovno stisne na komentare ucitat ce iz liste umjesto da se ponovno ucitava.
                    if(data.size() > 0) {
                        cache.put(audioPlayer.getCurrentlyPlaying().getId(), data);
                        isMoreComments = true;
                    }

                    recyclerView.setAdapter(commentAdapter);
                    suggestionBar.setVisibility(View.GONE);
                    page = commentsLoader.getPage();

                    if(data.size() > 0)
                        lastPage.put(audioPlayer.getCurrentlyPlaying().getId(), page);

                    if(data.size() == 0)
                        commentResult.setVisibility(View.VISIBLE);
                    else
                        commentResult.setVisibility(View.GONE);

                    loading = false;
                }

                @Override
                public void onLoaderReset(@NonNull Loader<List<CommentsInfoItem>> loader) {

                }
            }).forceLoad();
        }
    }

    private boolean internetConnection() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
    }

    private void buildPlaylistDialog(final Music pjesma)
    {
        final List<String> titles = YouPlayDatabase.getInstance(getContext()).getAllPlaylists();
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(getResources().getString(R.string.add_to_playlist))
                .setItems(titles.toArray(new CharSequence[titles.size()]), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String title = titles.get(i);
                        YouPlayDatabase.getInstance(PlayFragment.this.getContext()).insertInTable(pjesma, title);
                        Snackbar.make(getView(), PlayFragment.this.getResources().getString(R.string.playlist_added), Snackbar.LENGTH_SHORT).show();
                        onItemClicked.refreshPlaylist();
                    }
                });
        builder.create().show();
    }

    private void buildAlarmDialog()
    {
        NumberPicker numberPicker = new NumberPicker(getContext());
        numberPicker.setMaxValue(20);
        numberPicker.setMinValue(1);

        FrameLayout frameLayout = new FrameLayout(getActivity());
        FrameLayout.LayoutParams params = new  FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        params.leftMargin  = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
        params.rightMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);

        numberPicker.setLayoutParams(params);
        frameLayout.addView(numberPicker);

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(getResources().getString(R.string.set_alarm))
                .setMessage(getResources().getString(R.string.alarm_pick))
                .setPositiveButton(getResources().getString(R.string.rationale_ok), null)
                .setNegativeButton(getResources().getString(R.string.rationale_cancel), null);

        builder.setView(frameLayout);

        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(audioService != null)
                {
//                    audioService.setAlarmCount(numberPicker.getValue());
                    audioPlayer.setAlarm(numberPicker.getValue());
                    audioPlayer.setAlarm(true);
//                    audioService.setAlarm(true);
                    alarm.setForeground(getResources().getDrawable(R.drawable.alarm_set));
                    dialog.dismiss();
                    Toast.makeText(getContext(), getResources().getString(R.string.alarm_set), Toast.LENGTH_SHORT).show();
                }
            }
        });


    }

    @Override
    public void setupActionBar()
    {
    }

    public void replay()
    {
        if(audioPlayer.getReplay() == AudioPlayer.Replay.REPLAY_ONE)
        {
            replayF.setForeground(getResources().getDrawable(R.drawable.replay));
            audioPlayer.setReplay(AudioPlayer.Replay.REPLAY_OFF);
        }
        else if (audioPlayer.getReplay() == AudioPlayer.Replay.REPLAY_ALL)
        {
            replayF.setForeground(getResources().getDrawable(R.drawable.replay_all));
            audioPlayer.setReplay(AudioPlayer.Replay.REPLAY_ONE);
        }
        else {
            replayF.setForeground(getResources().getDrawable(R.drawable.replay_pressed));
            audioPlayer.setReplay(AudioPlayer.Replay.REPLAY_ALL);
        }

    }

    private void autoPlay()
    {
        if(!audioPlayer.isAutoplay())
        {
            autoPlay.setTextColor(getResources().getColor(R.color.seekbar_progress));
            audioPlayer.setAutoplay(true);
            if(mediaCompleted) {
                audioService.getAudioPlayer().nextSong();

            }
        }
        else
        {
            autoPlay.setTextColor(getResources().getColor(R.color.suggestions));
            audioPlayer.setAutoplay(false);
        }

    }

    public void slide()
    {

        final ConstraintLayout layout = getView().findViewById(R.id.play_list_layout);
        final ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) layout.getLayoutParams();

        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);

        if(!slided)
        {
            currentHeight = layout.getHeight();

            float heightPixels = (float) metrics.heightPixels;
            layoutHeight = 0.75f*heightPixels;

            params.width = layout.getWidth();
            params.height = (int) currentHeight;

            params.topToBottom = ConstraintLayout.LayoutParams.UNSET;
            layout.setLayoutParams(params);

            ValueAnimator va = ValueAnimator.ofFloat(currentHeight, layoutHeight);
            va.setDuration(400);
            va.setInterpolator(new AccelerateDecelerateInterpolator());
            va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    float height = (float) valueAnimator.getAnimatedValue();
                    params.height = (int) height;
                    layout.requestLayout();
                }
            });
            va.start();
            slided = true;
        }
        else
        {
            if(MainActivity.size > 0 && MainActivity.adLoaded)
            {
                layoutHeight = layout.getLayoutParams().height;
                currentHeight -= MainActivity.size;
                MainActivity.adLoaded = false;
                MainActivity.size = -1;
            }

            ValueAnimator va = ValueAnimator.ofFloat(layoutHeight, currentHeight);
            va.setDuration(400);
            va.setInterpolator(new AccelerateDecelerateInterpolator());
            va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    float height = (float) valueAnimator.getAnimatedValue();
                    params.height = (int) height;
                    layout.requestLayout();
                }
            });
            va.addListener(new AnimatorListenerAdapter() {

                @Override
                public void onAnimationEnd(Animator animation) {

                    params.height = 0;
                    params.width = 0;
                    params.topToBottom = R.id.play_pause_layout;

                    layout.setLayoutParams(params);
                    if(isSuggestionClick)
                        recyclerView.scrollToPosition(audioPlayer.getPosition());
                }
            });
            va.start();
            slided = false;

        }

    }

    public void setShuffled()
    {
        this.shuffle.setForeground(getResources().getDrawable(R.drawable.shuffle_pressed));
        audioPlayer.setShuffled(true);
    }

    public void setUnshuffled() {
        this.shuffle.setForeground(getResources().getDrawable(R.drawable.shuffle));
        audioPlayer.setShuffled(false);
    }

    public void nextSong()
    {
        audioPlayer.nextSong();
        if(audioPlayer.isStream())
            setCurrent(audioPlayer.getPosition());
    }

    public void previousSong()
    {
        audioPlayer.previousSong();
        if(audioPlayer.isStream())
            setCurrent(audioPlayer.getPosition());
    }

    public void playPauseSong(boolean update)
    {
        if(audioPlayer.getPlayWhenReady() && !update)
        {
            audioPlayer.setPlayWhenReady(false);
            if(isAdded())
                play_pause.setForeground(getResources().getDrawable(R.drawable.play));
        }
        else if(!audioPlayer.getPlayWhenReady() && !update)
        {
            audioPlayer.setPlayWhenReady(true);
            if(isAdded())
                play_pause.setForeground(getResources().getDrawable(R.drawable.pause));
            playCycle();
        }
        else
        {
            Intent newIntent = new Intent(getContext(), AudioService.class);
            newIntent.putExtra(AudioService.ACTION, PLAY_PAUSE_SONG);
            getContext().startService(newIntent);
        }
    }

    private void setSong(Station pjesma)
    {
        Log.d(TAG, "SetSong station");
        bar.setVisibility(View.VISIBLE);
        seekbar.setProgress(0);

        play_pause.setForeground(getResources().getDrawable(R.drawable.pause));
        currentlyTitle.setText(pjesma.getName());

        if(!pjesma.getIcon().isEmpty())
            Glide.with(this).load(pjesma.getIcon()).apply(new RequestOptions().skipMemoryCache(true).error(R.drawable.image_holder)).into(currentlyPlaying);
        else
            Glide.with(this).load(R.drawable.image_holder).into(currentlyPlaying);

        durationTimeCurrent.setText(R.string.you_temp_time);

        audioService.playSong(null, pjesma);
        setPlayScreen();
    }

    private void playSong(Music pjesma)
    {
        currentlyPlayingSong = pjesma;
        if(isAdded())
            Glide.with(this).load(FileManager.getPictureFile(pjesma.getId())).apply(new RequestOptions().skipMemoryCache(true).error(R.mipmap.ic_launcher)).into(currentlyPlaying);

        seekbar.setProgress(0);
        suggestionBar.setVisibility(View.GONE);
        play_pause.setForeground(getResources().getDrawable(R.drawable.pause));
        currentlyTitle.setText(pjesma.getTitle());
        durationTime.setText(pjesma.getDuration());
        durationTimeCurrent.setText(R.string.you_temp_time);
        if(!audioPlayer.isShuffled()) {
            this.shuffle.setForeground(getResources().getDrawable(R.drawable.shuffle));
        }
        audioService.playSong(pjesma, null);
    }

    private void setSong(final Music pjesma)
    {
        setCurrent(audioPlayer.getPosition());
        playSong(pjesma);
    }

    private void playCycle()
    {
        if(audioService != null && handler != null )
        {
            if(audioService.getAudioPlayer().getPlayWhenReady())
            {
                seekbar.setProgress((int) audioService.getAudioPlayer().getCurrentPosition());
                runnable = new Runnable() {
                    @Override
                    public void run() {
                        if (durationTimeCurrent != null && audioService != null && audioService.getAudioPlayer().getPlayWhenReady())
                            playCycle();

                    }
                };
                handler.postDelayed(runnable, 100);

                runnable = new Runnable() {
                    @Override
                    public void run() {
                        if (durationTimeCurrent != null && audioService != null && audioService.getAudioPlayer().getPlayWhenReady())
                            durationTimeCurrent.setText(convertDuration(audioService.getAudioPlayer().getCurrentPosition()));

                    }
                };
                handler.postDelayed(runnable, 1000);
            }

        }
    }

    /*
    Ako je korisnik zatvori aplikaciju prije nego sto se pjesma skinula
    u folderu ostane pola skinut fajl, taj fajl ce se obrisat i ponovno ce pocet
    skidat pjesmu
     */
    private void deleteAndDownload(Music pjesma)
    {
        deleteMedia = true;

        File check = FileManager.getMediaFile(pjesma.getId());
        if(check.exists())
            check.delete();

        check = new File(Environment.getExternalStorageDirectory() + File.separator + APP_NAME, pjesma.getId() + FORMAT + ".temp");
        if(check.exists())
            check.delete();

        downloadSong(pjesma, false);
    }

    private void downloadSong(final Music pjesma, final boolean relatedVideos) {
        Activity activity = getActivity();
        if (activity instanceof MainActivity)
            ((MainActivity) getActivity()).pager.setCurrentItem(0);

        bar.setVisibility(View.VISIBLE);
        String getYoutubeLink = YOUTUBELINK + pjesma.getId();

        if(urlLoader != null)
            urlLoader.cancel(true);

        urlLoader = new UrlLoader(getYoutubeLink, relatedVideos, audioService.getAudioPlayer().getMusicList());
        urlLoader.setListener(new UrlLoader.Listener() {
            @Override
            public void postExecute(List<String> data) {
                if (data != null && data.get(1) != null && isAdded()) {
                    pjesma.setPath(data.get(1));
                    pjesma.setUrlImage(data.get(0));
                    urlExists(pjesma);
                    if(relatedVideos)
                    {
                        tempList.clear();
                        tempList.addAll(urlLoader.getMusicList());
                        tempList.add(0, pjesma);
                        suggestionBar.setVisibility(View.GONE);
                        refreshList(tempList, false);
//                        audioPlayer.setMusicList(tempList);
                    }
                } else {
                    if(getContext() != null)
                    {
                        Toast.makeText(getContext(), getResources().getString(R.string.cant_extract), Toast.LENGTH_SHORT).show();
                        bar.setVisibility(View.GONE);
                    }
                }
            }
        });
        urlLoader.execute();
    }

    private void updateTable(Music pjesma)
    {
        new DatabaseHandler(pjesma, TABLE_NAME, YouPlayDatabase.YOUPLAY_DB, DatabaseHandler.UpdateType.ADD).
                setDataChangedListener(this).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
    }

    @Override
    public void dataChanged(DatabaseHandler.UpdateType type, String databaseName, Music pjesma)
    {
        if(pjesma.getDownloaded() == 0 && isAdded())
            setMusic(pjesma, tempList);

        if(isAdded())
            onItemClicked.refresh(pjesma);
    }

    @Override
    public void deleteProgress(int length, String title) {

    }

    @Override
    public void dataChanged(DatabaseHandler.UpdateType type, ArrayList<Music> pjesme) {

    }

    private void download(final Music pjesma)
    {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean cacheMode = preferences.getBoolean(Constants.CACHE_MODE, true);
//        boolean cacheMode = false;
        if(cacheMode)
        {
            BaseDownloadTask songTask = FileDownloader.getImpl().create(pjesma.getPath()).setPath(FileManager.getMediaPath(pjesma.getId()));
            FileDownloadQueueSet queueSet = new FileDownloadQueueSet(new FileDownloadListener() {
                @Override
                protected void pending(BaseDownloadTask task, int soFarBytes, int totalBytes) {

                }

                @Override
                protected void progress(BaseDownloadTask task, int soFarBytes, int totalBytes) {
                    double divide = (double) soFarBytes / totalBytes;
                    double math = (double) seekbar.getMax() * divide;
                    seekbar.setSecondaryProgress((int) math);
                }

                @Override
                protected void completed(BaseDownloadTask task) {
                    if(isAdded())
                    {
//                        Toast.makeText(getContext(), getResources().getString(R.string.you_downloaded), Toast.LENGTH_SHORT).show();
                        seekbar.setSecondaryProgress(seekbar.getMax());
                        pjesma.setDownloaded(1);
                        pjesma.setPath(FileManager.getMediaPath(pjesma.getId()));
                        updateTable(pjesma);
                        onItemClicked.refreshSearchList(pjesma);
                        onItemClicked.refreshPlaylist();
                        Answers.getInstance().logCustom(new CustomEvent("Songs downloaded"));
                    }
                }

                @Override
                protected void paused(BaseDownloadTask task, int soFarBytes, int totalBytes) {

                }

                @Override
                protected void error(BaseDownloadTask task, Throwable e) {
                    if(isAdded()) {
                        bar.setVisibility(View.GONE);
                        if(Utils.freeSpace(true) < 20)
                            Toast.makeText(getContext(), getResources().getString(R.string.no_space), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                protected void warn(BaseDownloadTask task) {

                }
            });
            queueSet.downloadSequentially(songTask);
            queueSet.start();
        }
    }

    private void urlExists(final Music pjesma)
    {
        seekbar.setSecondaryProgress(0);
        if(context == null) return;
        if(!deleteMedia)
        {
            Log.d(TAG, "URL EXISTS " + pjesma.getTitle());
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            boolean cacheMode = preferences.getBoolean(Constants.CACHE_MODE, true);
//            boolean cacheMode = false;
            BaseDownloadTask songTask = FileDownloader.getImpl().create(pjesma.getPath()).setPath(FileManager.getMediaPath(pjesma.getId()));
            BaseDownloadTask picTask = FileDownloader.getImpl().create(pjesma.getUrlImage()).setPath(FileManager.getPicturePath(pjesma.getId()));


            final int songId = songTask.getId();
            final int picId = picTask.getId();

            Log.d(TAG, "URL id " + songId);
            FileDownloadListener downloadListener = new FileDownloadListener() {
                @Override
                protected void pending(BaseDownloadTask task, int soFarBytes, int totalBytes) {
                    Log.d(TAG, "URL PENDING id " + task.getId());
                }

                @Override
                protected void started(BaseDownloadTask task) {
                    if(task.getId() == songId)
                        bar.setVisibility(View.GONE);
                }

                @Override
                protected void progress(BaseDownloadTask task, int soFarBytes, int totalBytes) {
                    if(task.getId() == songId)
                    {
                        double divide = (double) soFarBytes / totalBytes;
                        double math = (double) seekbar.getMax() * divide;
                        seekbar.setSecondaryProgress((int) math);
                    }
                }

                @Override
                protected void completed(BaseDownloadTask task) {
                    if (task.getId() == picId)
                    {
                        Log.d(TAG, "EndCause.COMPLETED IMAGE: " + pjesma.getTitle());
                        pjesma.setDownloaded(0);
                        updateTable(pjesma);
                    }
                    else if(task.getId() == songId && getContext() != null)
                    {
                        Log.d(TAG, "EndCause.COMPLETED: " + pjesma.getTitle());
//                        Toast.makeText(getContext(), getResources().getString(R.string.you_downloaded), Toast.LENGTH_SHORT).show();
                        seekbar.setSecondaryProgress(seekbar.getMax());
                        pjesma.setDownloaded(1);
                        pjesma.setPath(FileManager.getMediaPath(pjesma.getId()));
                        updateTable(pjesma);
                        onItemClicked.refreshSearchList(pjesma);
                        Answers.getInstance().logCustom(new CustomEvent("Songs downloaded"));
                    }
                }

                @Override
                protected void paused(BaseDownloadTask task, int soFarBytes, int totalBytes) {

                }

                @Override
                protected void error(BaseDownloadTask task, Throwable e) {
                        bar.setVisibility(View.GONE);
                        if(Utils.freeSpace(true) < 20)
                            Toast.makeText(getContext(), getResources().getString(R.string.no_space), Toast.LENGTH_SHORT).show();
                }

                @Override
                protected void warn(BaseDownloadTask task) {

                }
            };

            FileDownloadQueueSet queueSet = new FileDownloadQueueSet(downloadListener);

            final List<BaseDownloadTask> tasks = new ArrayList<>();
            tasks.add(picTask);
            if(cacheMode)
                tasks.add(songTask);

            queueSet.downloadSequentially(tasks);
            queueSet.start();
        }
        else
        {
            bar.setVisibility(View.GONE);
            download(pjesma);
            setMusic(pjesma, tempList);
        }
        deleteMedia = false;
    }

    @Override
    public void buildAlertDialog(int position, View view)
    {

    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        currentlyPlayingSong = null;
    }

    @Override
    public void onDetach()
    {
        handler = null;
        runnable = null;
        super.onDetach();
    }

    @Override
    public void onReady() {
        Log.d(TAG, "Play ready");
        bar.setVisibility(View.GONE);
        if(audioPlayer.isStream())
        {
            bar.setVisibility(View.GONE);
            durationTime.setText(R.string.you_temp_time);
            seekbar.setMax(0);
            seekbar.setProgress(0);
        }
        else
        {
            seekbar.setMax((int)audioPlayer.getDuration());
            if(currentlyPlayingSong != null && currentlyPlayingSong.getDownloaded() == 1)
                seekbar.setSecondaryProgress(seekbar.getMax());
        }
        playCycle();
        mediaCompleted = false;
    }

    @Override
    public void onBuffering() {
        if(isAdded())
            bar.setVisibility(View.VISIBLE);
    }

    @Override
    public void onSetSong(Music music) {
        setSong(music);
    }

    @Override
    public void onSetStation(Station station) {
        setSong(station);
    }
    @Override
    public void downloadSong(Music music) {
        downloadSong(music, false);
    }
}
