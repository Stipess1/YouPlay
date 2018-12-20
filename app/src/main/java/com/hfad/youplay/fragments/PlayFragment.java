package com.hfad.youplay.fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.webkit.URLUtil;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.hfad.youplay.AudioService;
import com.hfad.youplay.Ilisteners.OnDataChanged;
import com.hfad.youplay.Ilisteners.OnItemClicked;
import com.hfad.youplay.Ilisteners.OnMusicSelected;
import com.hfad.youplay.Ilisteners.OnRadioSelected;
import com.hfad.youplay.MainActivity;
import com.hfad.youplay.R;
import com.hfad.youplay.adapter.PlaylistAdapter;
import com.hfad.youplay.database.DatabaseHandler;
import com.hfad.youplay.database.YouPlayDatabase;
import com.hfad.youplay.music.Music;
import com.hfad.youplay.radio.Country;
import com.hfad.youplay.radio.Station;
import com.hfad.youplay.utils.Constants;
import com.hfad.youplay.utils.FileManager;
import com.hfad.youplay.utils.Utils;
import com.hfad.youplay.youtube.loaders.UrlLoader;
import com.hfad.youplay.youtube.loaders.YoutubeMusicLoader;
import com.liulishuo.okdownload.DownloadListener;
import com.liulishuo.okdownload.DownloadSerialQueue;
import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.OkDownload;
import com.liulishuo.okdownload.core.cause.EndCause;
import com.liulishuo.okdownload.core.cause.ResumeFailedCause;
import com.liulishuo.okdownload.core.listener.DownloadListener1;
import com.liulishuo.okdownload.core.listener.assist.Listener1Assist;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.hfad.youplay.utils.Constants.APP_NAME;
import static com.hfad.youplay.utils.Constants.FORMAT;
import static com.hfad.youplay.utils.Constants.PLAY_PAUSE_SONG;
import static com.hfad.youplay.utils.Constants.TABLE_NAME;
import static com.hfad.youplay.utils.Utils.*;


public class PlayFragment extends BaseFragment implements View.OnClickListener,
        OnMusicSelected, OnRadioSelected, OnDataChanged {

    public static final String TAG = PlayFragment.class.getSimpleName();
    private static final String YOUTUBELINK = "https://www.youtube.com/watch?v=";

    private boolean shuffled = false;
    private ImageView currentlyPlaying;
    private FrameLayout play_pause;
    private FrameLayout shuffle;
    private FrameLayout replayF;
    private FrameLayout slide_up;
    private ProgressBar bar;
    private ProgressBar suggestionBar;
    public TextView currentlyTitle, durationTime, durationTimeCurrent, autoPlay;
    public SeekBar seekbar;
    public static Runnable runnable;
    public static Handler handler;
    private ArrayList<Music> musicList;
    private ArrayList<Music> tempList;
    public int position;
    private int lastPost;
    private int currentProgress;
    private boolean userClick = true;
    private boolean wasPlaying = false;
    private boolean replay = false;
    private PlaylistAdapter adapter;
    private ArrayList<Station> stations;
    private LinearLayoutManager linearLayoutManager;
    private OnItemClicked onItemClicked;

    private String getYoutubeLink;
    private RecyclerView recyclerView;

    private boolean slided = false;
    private float currentHeight;
    private float layoutHeight;
    private boolean autoPlaybool = true;
    private boolean mediaCompleted = false;
    private AudioService audioService;
    private YouPlayDatabase db;
    private boolean deleteMedia;

    public static Music currentlyPlayingSong;

    public PlayFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        onItemClicked = (OnItemClicked) getActivity();
    }

    public boolean getShuffled()
    {
        return shuffled;
    }

    public void setUserClick(boolean userClick)
    {
        this.userClick = userClick;
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

        ConstraintLayout playFragment = view.findViewById(R.id.play_fragment);
        playFragment.setOnClickListener(this);

        stations = new ArrayList<>();
        musicList = new ArrayList<>();
        tempList = new ArrayList<>();

        audioService = AudioService.getInstance();

        if(audioService != null && audioService.getMusicList() != null && !audioService.getIsStream())
        {
            if(audioService.isShuffled())
            {
                shuffled = true;
                shuffle.setForeground(getResources().getDrawable(R.drawable.shuffle_pressed));
            }
            tempList.clear();
            tempList.addAll(audioService.getMusicList());

            musicList.clear();
            musicList.addAll(audioService.getRealMusic());
        }


        adapter = new PlaylistAdapter(getContext(), R.layout.play_fragment_list, tempList, PlaylistAdapter.ListType.SUGGESTIONS);

        if(audioService != null && audioService.getStations() != null && audioService.getIsStream())
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
        slide_up = view.findViewById(R.id.slide_up_list);
        slide_up.setOnClickListener(this);
        adapter.setListner(this, this);
        recyclerView.setAdapter(adapter);
        linearLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(linearLayoutManager);

        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean input)
            {
                if(input && !audioService.getIsStream())
                {
                    currentProgress = progress;
                    durationTimeCurrent.setText(convertDuration(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar)
            {
                if(audioService.exoPlayer.getPlayWhenReady() && !audioService.getIsStream())
                {
                    audioService.exoPlayer.setPlayWhenReady(false);
                    wasPlaying = true;
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {
                if(!audioService.getIsStream())
                {
                    audioService.exoPlayer.seekTo(currentProgress);
                    if(wasPlaying)
                    {
                        audioService.exoPlayer.setPlayWhenReady(true);
                        wasPlaying = false;
                        playCycle();
                    }
                }
            }
        });
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if(!slided && newState == RecyclerView.SCROLL_STATE_DRAGGING &&
                        linearLayoutManager.findFirstVisibleItemPosition() != 0)
                    slide();
            }
        });


        if(audioService != null && audioService.getEventListener() != null)
        {
            Log.d(TAG, "AudioService listener");
            AudioService.getInstance().exoPlayer.removeListener(audioService.getEventListener());
            AudioService.getInstance().setListenerAdded(false);
            setListeners();
        }

        if(audioService != null && audioService.getMusicList() != null && !audioService.getIsStream())
        {
            setCurrent(AudioService.getInstance().getCurrentSongPos());
            Log.d(TAG, "AudioService listener musiclist");
            setDestroyedScreenSong();
        }
        else if(audioService != null && audioService.getIsStream() && audioService.getStations() != null)
        {
            setCurrent(AudioService.getInstance().getCurrentStreamPos());
            setDestroyedScreenStream();
        }

//        ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
        return view;
    }

    /*
    Nakon što se activity unisti pjesma dalje moze nastavit svirat, ova funkcija ce postavit
    sliku, ime pjesme itd. nakon što se ponovno pokrene aplikacija.
     */
    private void setDestroyedScreenSong()
    {
        currentlyPlayingSong = audioService.getMusic();

        Glide.with(this).load(FileManager.getPictureFile(currentlyPlayingSong.getId())).apply(new RequestOptions().skipMemoryCache(true).error(R.mipmap.ic_launcher)).into(currentlyPlaying);

        currentlyTitle.setText(currentlyPlayingSong.getTitle());
        durationTime.setText(currentlyPlayingSong.getDuration());
        durationTimeCurrent.setText(R.string.you_temp_time);
        ((MainActivity)getActivity()).pager.setCurrentItem(0);

        if(audioService.exoPlayer.getPlayWhenReady())
            play_pause.setForeground(getResources().getDrawable(R.drawable.pause));

        else
            play_pause.setForeground(getResources().getDrawable(R.drawable.play));

        replay = audioService.isReplay();
        autoPlaybool = audioService.isAutoPlaybool();

        if(replay)
            replayF.setForeground(getResources().getDrawable(R.drawable.replay_pressed));
        else
            replayF.setForeground(getResources().getDrawable(R.drawable.replay));

        if(autoPlaybool)
            autoPlay.setTextColor(getResources().getColor(R.color.seekbar_progress));
        else
            autoPlay.setTextColor(getResources().getColor(R.color.suggestions));

        seekbar.setMax((int)audioService.exoPlayer.getDuration());
        if(currentlyPlayingSong.getDownloaded() == 1)
            seekbar.setSecondaryProgress(seekbar.getMax());

        durationTimeCurrent.setText(convertDuration(audioService.exoPlayer.getCurrentPosition()));
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

        if(audioService.exoPlayer.getPlayWhenReady())
            play_pause.setForeground(getResources().getDrawable(R.drawable.pause));
        else
            play_pause.setForeground(getResources().getDrawable(R.drawable.play));

        durationTimeCurrent.setText(convertDuration(audioService.exoPlayer.getCurrentPosition()));
        playCycle();
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

        if(audioService.exoPlayer.getPlayWhenReady())
            audioService.exoPlayer.stop();

        audioService.isStream(true);
        setSong(station);
        setCurrent(position);

        if(slided)
            slide();
    }

    @Override
    public void initAudioService() {
        audioService = AudioService.getInstance();
    }

    public ArrayList<Music> getTempList()
    {
        return tempList;
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
        this.position = tempList.indexOf(pjesma);

        if(audioService.exoPlayer.getPlayWhenReady())
            audioService.exoPlayer.stop();

        OkDownload.with().downloadDispatcher().cancelAll();
        audioService.isStream(false);
        if(pjesma.getDownloaded() == 1)
            setSong(pjesma);
        else
            downloadSong(pjesma, false);
        setCurrent(position);

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

        if(tempList.size() > 0 && !pjesma.equals(currentlyPlayingSong))
        {
            int position = tempList.indexOf(pjesma);
            tempList.remove(pjesma);
            adapter.notifyItemRemoved(position);
            adapter.notifyItemRangeChanged(position, tempList.size());

            setCurrent(tempList.indexOf(currentlyPlayingSong));
        }

    }

    public void setCurrent(final int position)
    {
        this.position = position;

        recyclerView.scrollToPosition(position);
        adapter.setCurrent(position);

        if(position >= 0)
        {
            adapter.notifyItemChanged(lastPost);
            // tako da mozemo postavit grey BG na trenutnu pjesmu
            adapter.notifyItemChanged(position);
        }

        lastPost = position;
    }

    public void refreshList(List<Station> postaje)
    {
        tempList.clear();
        adapter.setStations(postaje);
    }

    public void refreshList(List<Music> pjesme, boolean queue)
    {
        Log.d(TAG, "refreshList size: " + pjesme.size());
        adapter.reloadList(pjesme);

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
                if(musicList != null && currentlyPlayingSong != null &&
                        adapter.getPlay() != PlaylistAdapter.ListType.STATIONS)
                {
                    if(!shuffled)
                    {
                        lastPost = position;
                        position = 0;
                        setCurrent(position);
                        Log.d(TAG, "Lists1: " + musicList.size() + " " + tempList.size());
                        musicList.clear();
                        Log.d(TAG, "Lists2: " + musicList.size() + " " + tempList.size());
                        musicList.addAll(getTempList());
                        Log.d(TAG, "Lists3: " + musicList.size() + " " + tempList.size());
                        Collections.shuffle(getTempList());
                        checkShuffle();
                    }
                    else
                    {
                        checkShuffle();
                        setCurrent(getPosition());
                    }

                    audioService.setMusicList(getTempList());
                }
                break;
            case R.id.slide_up_list:
                slide();
                break;
            case R.id.play_fragment:
                if(slided)
                    slide();
                break;
            case R.id.autoplay:
                autoPlay();
                break;
        }
    }

    private int getPosition()
    {
        for(Music pjesma : musicList)
            if(pjesma.getId().equals(currentlyPlayingSong.getId()))
                return musicList.indexOf(pjesma);
        return 0;
    }

    @Override
    public void setupActionBar()
    {
    }

    public void replay()
    {
        if(replay)
        {
            replay = false;
            replayF.setForeground(getResources().getDrawable(R.drawable.replay));
        }
        else
        {
            replay = true;
            replayF.setForeground(getResources().getDrawable(R.drawable.replay_pressed));
        }
        audioService.setReplay(replay);
    }

    private void autoPlay()
    {
        if(!autoPlaybool)
        {
            autoPlay.setTextColor(getResources().getColor(R.color.seekbar_progress));
            autoPlaybool = true;
            if(mediaCompleted)
                nextSong();
        }
        else
        {
            autoPlay.setTextColor(getResources().getColor(R.color.suggestions));
            autoPlaybool = false;
        }
        audioService.setAutoPlaybool(autoPlaybool);
    }

    public void slide()
    {

        final ConstraintLayout layout = getView().findViewById(R.id.play_list_layout);
        final ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) layout.getLayoutParams();

        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);

        if(!slided)
        {
            slide_up.setForeground(getResources().getDrawable(R.drawable.ic_expand_more));

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
            va.addUpdateListener(valueAnimator -> {
                float height = (float) valueAnimator.getAnimatedValue();
                params.height = (int) height;
                layout.requestLayout();
            });
            va.start();
            slided = true;
        }
        else
        {
            slide_up.setForeground(getResources().getDrawable(R.drawable.ic_expand_less));

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
            va.addUpdateListener(valueAnimator -> {
                float height = (float) valueAnimator.getAnimatedValue();
                params.height = (int) height;
                layout.requestLayout();
            });
            va.addListener(new AnimatorListenerAdapter() {

                @Override
                public void onAnimationEnd(Animator animation) {

                    params.height = 0;
                    params.width = 0;
                    params.topToBottom = R.id.play_pause_layout;

                    layout.setLayoutParams(params);
                    setCurrent(position);
                }
            });
            va.start();
            slided = false;

        }

    }

    public void checkShuffle()
    {
        if(!shuffled)
        {
            for(Music pjesma : tempList)
                if(pjesma.getId().equals(currentlyPlayingSong.getId()))
                {
                    tempList.remove(pjesma);
                    break;
                }

            tempList.add(0, currentlyPlayingSong);
            adapter.notifyDataSetChanged();
            shuffled = true;
            shuffle.setForeground(getResources().getDrawable(R.drawable.shuffle_pressed));
        }
        else
        {
            tempList.clear();
            tempList.addAll(musicList);
            adapter.notifyDataSetChanged();
            shuffled = false;
            shuffle.setForeground(getResources().getDrawable(R.drawable.shuffle));
        }
        audioService.setShuffled(shuffled);
    }

    public void nextSong()
    {
        // kada korisnik skida next nece radit nema potrebe pregledavat jeli je shuffled ili nije.
        if(musicList != null && !audioService.getIsStream())
        {
            position += 1;
            Music pjesma = null;

            if(replay && position >= tempList.size())
            {
                position = 0;
            }
            if(position < tempList.size())
            {
                pjesma = tempList.get(position);
                setCurrent(position);
            }
            else if(position >= tempList.size())
            {
                position = tempList.size() - 1;
            }
            if(pjesma != null)
            {
                if(audioService.exoPlayer.getPlayWhenReady())
                    audioService.exoPlayer.stop();

                if(pjesma.getDownloaded() == 1)
                    setMusic(pjesma, tempList);
                else
                    downloadSong(pjesma, false);
            }
        }
        else if(audioService.getIsStream())
        {
            position += 1;
            Station pjesma = null;

            if(replay && position >= stations.size())
            {
                position = 0;
            }
            if(position < stations.size())
            {
                pjesma = stations.get(position);
                setCurrent(position);
            }
            else if(position >= stations.size())
            {
                position = stations.size() - 1;
            }
            if(pjesma != null)
            {
                if(audioService.exoPlayer.getPlayWhenReady())
                    audioService.exoPlayer.stop();
                setSong(pjesma);
            }
        }
    }

    public void previousSong()
    {

        if(musicList != null && !audioService.getIsStream())
        {
            position -=1;
            Music pjesma = null;
            if(position >= 0)
            {
                pjesma = tempList.get(position);
                setCurrent(position);
            }
            else if(position <= tempList.size())
            {
                position = 0;
            }
            if(pjesma != null)
            {
                if(audioService.exoPlayer.getPlayWhenReady())
                    audioService.exoPlayer.stop();

                if(pjesma.getDownloaded() == 1)
                    setMusic(pjesma, tempList);
                else
                    downloadSong(pjesma, false);
            }
        }
        else if(audioService.getIsStream())
        {
            position -=1;
            Station pjesma = null;
            if(position >= 0)
            {
                pjesma = stations.get(position);
                setCurrent(position);
            }
            else if(position <= stations.size())
            {
                position = 0;
            }
            if(pjesma != null)
            {
                setSong(pjesma);
            }
        }
    }

    public void playPauseSong(boolean update)
    {
        if(audioService.exoPlayer.getPlayWhenReady() && !update)
        {
            audioService.exoPlayer.setPlayWhenReady(false);
            play_pause.setForeground(getResources().getDrawable(R.drawable.play));
        }
        else if(!audioService.exoPlayer.getPlayWhenReady() && !update)
        {
            audioService.exoPlayer.setPlayWhenReady(true);
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

    public void setListeners()
    {
        if(!AudioService.getInstance().getListenerAdded())
        {
            audioService.setEventListener(new Player.EventListener() {

                @Override
                public void onTimelineChanged(Timeline timeline, Object manifest, int reason) {

                }

                @Override
                public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

                }

                @Override
                public void onLoadingChanged(boolean isLoading) {

                }

                @Override
                public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                    switch (playbackState)
                    {
                        case Player.STATE_ENDED:
                            if(!AudioService.getInstance().isDestroyed())
                            {
                                if(autoPlaybool)
                                {
                                    userClick = false;
                                    Log.d(TAG, "Next song is called");
                                    nextSong();
                                }
                                mediaCompleted = true;
                            }
                            else
                            {
                                if(AudioService.getInstance().isAutoPlaybool())
                                    AudioService.getInstance().nextSong();

                            }
                            break;
                        case Player.STATE_READY:
                            if(!AudioService.getInstance().isDestroyed())
                            {
                                bar.setVisibility(View.GONE);
                                if(audioService.getIsStream())
                                {
                                    bar.setVisibility(View.GONE);
                                    durationTime.setText(R.string.you_temp_time);
                                    seekbar.setMax(0);
                                    seekbar.setProgress(0);
                                }
                                else
                                {
                                    seekbar.setMax((int)audioService.exoPlayer.getDuration());
                                    if(currentlyPlayingSong != null && currentlyPlayingSong.getDownloaded() == 1)
                                        seekbar.setSecondaryProgress(seekbar.getMax());
                                }
                                playCycle();
                                mediaCompleted = false;
                            }
                            break;
                        case Player.STATE_BUFFERING:
                            if(!AudioService.getInstance().isDestroyed())
                                bar.setVisibility(View.VISIBLE);
                            break;
                    }
                }

                @Override
                public void onRepeatModeChanged(int repeatMode) {

                }

                @Override
                public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

                }

                @Override
                public void onPlayerError(ExoPlaybackException error) {

                }

                @Override
                public void onPositionDiscontinuity(int reason) {

                }

                @Override
                public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

                }

                @Override
                public void onSeekProcessed() {

                }
            });
            AudioService.getInstance().exoPlayer.addListener(audioService.getEventListener());
            AudioService.getInstance().setListenerAdded(true);
        }
    }

    public void setSong(Station pjesma)
    {
        Log.d(TAG, "SetSong station");
        bar.setVisibility(View.VISIBLE);
        seekbar.setProgress(0);

        play_pause.setForeground(getResources().getDrawable(R.drawable.pause));
        currentlyTitle.setText(pjesma.getName());

        if(!pjesma.getIcon().isEmpty())
            Glide.with(this).load(pjesma.getIcon()).apply(new RequestOptions().skipMemoryCache(true).error(R.mipmap.ic_launcher)).into(currentlyPlaying);
        else
            Glide.with(this).load(R.mipmap.ic_launcher).into(currentlyPlaying);

        durationTimeCurrent.setText(R.string.you_temp_time);

        onItemClicked.setStation(pjesma);
        setPlayScreen();

    }

    private void playSong(Music pjesma)
    {
        currentlyPlayingSong = pjesma;
        Glide.with(this).load(FileManager.getPictureFile(pjesma.getId())).apply(new RequestOptions().skipMemoryCache(true).error(R.mipmap.ic_launcher)).into(currentlyPlaying);

        seekbar.setProgress(0);
        play_pause.setForeground(getResources().getDrawable(R.drawable.pause));
        currentlyTitle.setText(pjesma.getTitle());
        durationTime.setText(pjesma.getDuration());
        durationTimeCurrent.setText(R.string.you_temp_time);
        onItemClicked.setMusic(pjesma);


//        runnable = () -> {
//            Activity activity = getActivity();
//            if(activity instanceof MainActivity)
//                if(userClick)
//                    ((MainActivity)getActivity()).pager.setCurrentItem(0, true);
//        };
//        handler.postDelayed(runnable, 150);

    }

    public void setSong(final Music pjesma)
    {

        this.position = tempList.indexOf(pjesma);
        for(Music song : tempList)
            if(song.getId().equals(pjesma.getId()))
                setCurrent(tempList.indexOf(song));

        playSong(pjesma);
    }

    public void playCycle()
    {
        if(audioService != null && handler != null )
        {
            if(audioService.exoPlayer.getPlayWhenReady())
            {
                seekbar.setProgress((int) audioService.exoPlayer.getCurrentPosition());
                runnable = () -> {
                    if(durationTimeCurrent != null && audioService != null && audioService.exoPlayer.getPlayWhenReady())
                        playCycle();

                };
                handler.postDelayed(runnable, 100);

                runnable = () -> {
                    if(durationTimeCurrent != null && audioService != null && audioService.exoPlayer.getPlayWhenReady())
                        durationTimeCurrent.setText(convertDuration(audioService.exoPlayer.getCurrentPosition()));

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
    public void deleteAndDownload(Music pjesma)
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

    public void downloadSong(final Music pjesma, final boolean relatedVideos)
    {
        Activity activity = getActivity();
        if(activity instanceof MainActivity)
            ((MainActivity)getActivity()).pager.setCurrentItem(0);

        bar.setVisibility(View.VISIBLE);
        getYoutubeLink = YOUTUBELINK + pjesma.getId();

        getActivity().getSupportLoaderManager().restartLoader(9, null, new LoaderManager.LoaderCallbacks<List<String>>() {
            @NonNull
            @Override
            public Loader<List<String>> onCreateLoader(int id, @Nullable Bundle args)
            {
                return new UrlLoader(getContext(), getYoutubeLink);
            }

            @Override
            public void onLoadFinished(@NonNull Loader<List<String>> loader, List<String> data) {
                if(data != null)
                {
                    pjesma.setPath(data.get(1));
                    pjesma.setUrlImage(data.get(0));
                    urlExists(pjesma, relatedVideos);
                }
                else
                {
                    Toast.makeText(getContext(), getResources().getString(R.string.cant_extract), Toast.LENGTH_SHORT).show();
                    bar.setVisibility(View.GONE);
                }
            }

            @Override
            public void onLoaderReset(@NonNull Loader<List<String>> loader) {

            }

        }).forceLoad();
    }

    private void updateTable(Music pjesma)
    {
        new DatabaseHandler(pjesma, TABLE_NAME, YouPlayDatabase.YOUPLAY_DB, DatabaseHandler.UpdateType.ADD).
                setDataChangedListener(this).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
    }

    @Override
    public void dataChanged(DatabaseHandler.UpdateType type, String databaseName, Music pjesma)
    {
        if(pjesma.getDownloaded() == 0)
            setMusic(pjesma, tempList);

        onItemClicked.refresh(pjesma);
    }

    @Override
    public void deleteProgress(int length, String title) {

    }

    private void download(Music pjesma)
    {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        boolean fastDownload = preferences.getBoolean(getResources().getString(R.string.check_download), false);
        boolean cacheMode = preferences.getBoolean(Constants.CACHE_MODE, true);
        Log.d(TAG, "FastDownload enabled: " + fastDownload);
        if(cacheMode)
        {
            DownloadTask.Builder builder = new DownloadTask.Builder(pjesma.getPath(), FileManager.getMediaFile(pjesma.getId()));
            if(fastDownload)
                builder.setConnectionCount(8);
            DownloadTask task = builder.build();
            task.enqueue(new DownloadListener1() {
                @Override
                public void taskStart(@NonNull DownloadTask task, @NonNull Listener1Assist.Listener1Model model) {

                }

                @Override
                public void retry(@NonNull DownloadTask task, @NonNull ResumeFailedCause cause) {

                }

                @Override
                public void connected(@NonNull DownloadTask task, int blockCount, long currentOffset, long totalLength) {

                }

                @Override
                public void progress(@NonNull DownloadTask task, long currentOffset, long totalLength) {
                    double divide = (double) currentOffset / totalLength;
                    double math = (double) seekbar.getMax() * divide;
                    seekbar.setSecondaryProgress((int) math);
                }

                @Override
                public void taskEnd(@NonNull DownloadTask task, @NonNull EndCause cause, @Nullable Exception realCause, @NonNull Listener1Assist.Listener1Model model) {
                    if(cause == EndCause.COMPLETED)
                    {
                        Toast.makeText(getContext(), getResources().getString(R.string.you_downloaded), Toast.LENGTH_SHORT).show();
                        seekbar.setSecondaryProgress(seekbar.getMax());
                        pjesma.setDownloaded(1);
                        pjesma.setPath(FileManager.getMediaPath(pjesma.getId()));
                        updateTable(pjesma);
                        onItemClicked.refreshSearchList(pjesma);
                        Answers.getInstance().logCustom(new CustomEvent("Songs downloaded"));
                    }
                    else if(cause == EndCause.ERROR)
                    {
                        bar.setVisibility(View.GONE);
                        if(Utils.freeSpace(true) < 20)
                            Toast.makeText(getContext(), getResources().getString(R.string.no_space), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private void urlExists(final Music pjesma, final boolean relatedVideos)
    {
        seekbar.setSecondaryProgress(0);
        if(!deleteMedia)
        {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            boolean fastDownload = preferences.getBoolean(getResources().getString(R.string.check_download), false);
            boolean cacheMode = preferences.getBoolean(Constants.CACHE_MODE, true);

            DownloadSerialQueue serialQueue = new DownloadSerialQueue();
            DownloadTask.Builder builder = new DownloadTask.Builder(pjesma.getUrlImage(), FileManager.getPictureFile(pjesma.getId()));
            DownloadTask task = builder.build();

            DownloadTask.Builder songBuilder = new DownloadTask.Builder(pjesma.getPath(), FileManager.getMediaFile(pjesma.getId()));
            if(fastDownload)
                songBuilder.setConnectionCount(8);

            DownloadTask songTask = songBuilder.build();

            int songId = songTask.getId();
            int imageId = task.getId();
            DownloadListener listener = new DownloadListener1() {
                @Override
                public void taskStart(@NonNull DownloadTask task, @NonNull Listener1Assist.Listener1Model model) {
                    if(task.getId() == songId)
                        bar.setVisibility(View.GONE);
                }

                @Override
                public void retry(@NonNull DownloadTask task, @NonNull ResumeFailedCause cause) {

                }

                @Override
                public void connected(@NonNull DownloadTask task, int blockCount, long currentOffset, long totalLength) {

                }

                @Override
                public void progress(@NonNull DownloadTask task, long currentOffset, long totalLength) {
                    if(task.getId() == songId)
                    {
                        double divide = (double) currentOffset / totalLength;
                        double math = (double) seekbar.getMax() * divide;
                        seekbar.setSecondaryProgress((int) math);
                    }
                }

                @Override
                public void taskEnd(@NonNull DownloadTask task, @NonNull EndCause cause, @Nullable Exception realCause, @NonNull Listener1Assist.Listener1Model model) {
                    if (cause == EndCause.COMPLETED && task.getId() == imageId)
                    {
                        pjesma.setDownloaded(0);
                        updateTable(pjesma);
                    }
                    else if(cause == EndCause.COMPLETED && task.getId() == songId && getContext() != null)
                    {
                        Log.d(TAG, "EndCause.COMPLETED: " + pjesma.getTitle());
                        Toast.makeText(getContext(), getResources().getString(R.string.you_downloaded), Toast.LENGTH_SHORT).show();
                        seekbar.setSecondaryProgress(seekbar.getMax());
                        pjesma.setDownloaded(1);
                        pjesma.setPath(FileManager.getMediaPath(pjesma.getId()));
                        updateTable(pjesma);
                        onItemClicked.refreshSearchList(pjesma);
                        Answers.getInstance().logCustom(new CustomEvent("Songs downloaded"));
                    }
                    else if (cause == EndCause.ERROR)
                    {
                        bar.setVisibility(View.GONE);
                        if(Utils.freeSpace(true) < 20)
                            Toast.makeText(getContext(), getResources().getString(R.string.no_space), Toast.LENGTH_SHORT).show();

                        Log.d(TAG, " exception: " + realCause.toString());
                    }
                }
            };

            serialQueue.setListener(listener);
            serialQueue.enqueue(task);
            if(cacheMode)
                serialQueue.enqueue(songTask);
        }
        else
        {
            bar.setVisibility(View.GONE);
            download(pjesma);
            setMusic(pjesma, tempList);
        }
        deleteMedia = false;

        if(relatedVideos)
        {
            getLoaderManager().restartLoader(1, null, new LoaderManager.LoaderCallbacks<List<Music>>() {
                @Override
                public Loader<List<Music>> onCreateLoader(int id, Bundle args) {
                    suggestionBar.setVisibility(View.VISIBLE);
                    return new YoutubeMusicLoader(getContext(), pjesma.getId(), true);
                }

                @Override
                public void onLoadFinished(Loader<List<Music>> loader, List<Music> data) {
                    tempList.clear();
                    tempList.addAll(data);
                    tempList.add(0, pjesma);
                    suggestionBar.setVisibility(View.GONE);
                    refreshList(tempList, false);
                    audioService.setMusicList(tempList);
                }

                @Override
                public void onLoaderReset(Loader<List<Music>> loader) {

                }
            }).forceLoad();
        }
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

}
