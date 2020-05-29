package com.stipess.youplay.fragments;


import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.stipess.youplay.AudioService;
import com.stipess.youplay.BuildConfig;
import com.stipess.youplay.Ilisteners.OnItemClicked;
import com.stipess.youplay.Ilisteners.OnSuggestionSelected;
import com.stipess.youplay.MainActivity;
import com.stipess.youplay.R;
import com.stipess.youplay.adapter.SearchAdapter;
import com.stipess.youplay.adapter.SuggestionAdapter;
import com.stipess.youplay.database.YouPlayDatabase;
import com.stipess.youplay.Ilisteners.OnMusicSelected;
import com.stipess.youplay.music.Music;

import com.stipess.youplay.utils.Constants;
import com.stipess.youplay.utils.FileManager;
import com.stipess.youplay.utils.SeparatorDecoration;
import com.stipess.youplay.utils.ThemeManager;
import com.stipess.youplay.utils.Utils;
import com.stipess.youplay.youtube.loaders.SuggestionLoader;
import com.stipess.youplay.youtube.loaders.YoutubeMusicLoader;
import com.liulishuo.filedownloader.FileDownloader;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.stipess.youplay.utils.Constants.*;

/**
 * A simple {@link Fragment} subclass.
 */
public class SearchFragment extends BaseFragment implements OnMusicSelected, OnSuggestionSelected{

    public SearchFragment() {
        // Required empty public constructor
    }

    private static final String TAG = SearchFragment.class.getSimpleName();

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private SearchAdapter videoAdapter;
    private SeparatorDecoration dividerItemDecoration;
    private ArrayList<Music> musicList = new ArrayList<>();
    private Context context;
    private TextView internet, noResult;
    private SearchView searchView;
    private ConstraintLayout clear;
    private OnItemClicked musicClicked;
    private final List<String> suggestions = new ArrayList<>();

    private SuggestionAdapter suggestionAdapter;
    private boolean swapAdapter;
    private YouPlayDatabase db;
    private AudioService audioService;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_search, container, false);
        this.context = getContext();
        musicList = new ArrayList<>();
        // ako budemo dodovali iteme u toolbar potrebno nam je ovo.
        setHasOptionsMenu(true);

        internet     = view.findViewById(R.id.internet_connection);
        recyclerView = view.findViewById(R.id.recycler_view);
        noResult     = view.findViewById(R.id.search_no_result);
        progressBar  = view.findViewById(R.id.progress);
        searchView   = view.findViewById(R.id.search_song);
        clear        = view.findViewById(R.id.constraintLayout2);



        videoAdapter = new SearchAdapter(getContext(), R.layout.play_adapter_view, musicList);
        suggestionAdapter = new SuggestionAdapter(getContext(), R.layout.suggestion, suggestions);

        suggestionAdapter.setListener(this);
        videoAdapter.setListener(this);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(linearLayoutManager);
        dividerItemDecoration = new SeparatorDecoration(getResources().getColor(ThemeManager.getDividerColorSearch()), 2);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
        Set <String> set = settings.getStringSet(SEARCH_LIST, new HashSet<>());

        suggestions.addAll(new ArrayList<>(set));
        recyclerView.swapAdapter(null, true);
        recyclerView.setAdapter(suggestionAdapter);

        suggestionAdapter.notifyDataSetChanged();

        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
                SharedPreferences.Editor editor = settings.edit();

                editor.putStringSet(Constants.SEARCH_LIST, new HashSet<>());
                editor.apply();

                suggestions.clear();
                suggestionAdapter.notifyDataSetChanged();

                Toast.makeText(getContext(), getString(R.string.clear_done), Toast.LENGTH_SHORT).show();
            }
        });


        Toolbar toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity)getActivity()).setSupportActionBar(toolbar);
        setupActionBar();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(final String query) {
                if(ifInternetConnection())
                {
                    PreferenceManager.setDefaultValues(getContext(), R.xml.preference, true);

                    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
                    SharedPreferences.Editor editor = settings.edit();
                    if(!set.contains(query)) {
                        set.add(query);
                        editor.putStringSet(Constants.SEARCH_LIST, set);
                        editor.apply();
                    }

                    swapAdapter = false;

                    recyclerView.swapAdapter(null, true);
                    recyclerView.setAdapter(videoAdapter);
                    recyclerView.addItemDecoration(dividerItemDecoration);
                    if(audioService == null)
                        initAudioService();

                    getLoaderManager().restartLoader(1, null, new LoaderManager.LoaderCallbacks<List<Music>>() {

                        @Override
                        public Loader<List<Music>> onCreateLoader(int id, Bundle args) {
                            return new YoutubeMusicLoader(getContext(), query, audioService.getAudioPlayer().getSearchList());
                        }

                        @Override
                        public void onLoadFinished(Loader<List<Music>> loader, List<Music> data) {
                            if(data.size() > 0)
                                noResult.setVisibility(View.GONE);
                            else
                                noResult.setVisibility(View.VISIBLE);

                            progressBar.setVisibility(View.GONE);

                            musicList.clear();
                            musicList.addAll(data);

                            videoAdapter.notifyDataSetChanged();
                            recyclerView.smoothScrollToPosition(0);

                            recyclerView.setVisibility(View.VISIBLE);
                            internet.setVisibility(View.GONE);
//                            if(MainActivity.isGooglePlay)
//                                buildInfoDialog();
                        }

                        @Override
                        public void onLoaderReset(Loader<List<Music>> loader) {

                        }
                    }).forceLoad();
                    noResult.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.GONE);
                    progressBar.setVisibility(View.VISIBLE);

                    searchView.clearFocus();
                }
                else
                {
                    recyclerView.setVisibility(View.GONE);
                }

                return true;
            }

            @Override
            public boolean onQueryTextChange(final String query)
            {
                Log.d(TAG, "QueryTextChange");
                if(ifInternetConnection())
                {
                    if(query.length() > 2)
                    {
                        clear.setVisibility(View.GONE);
                        noResult.setVisibility(View.GONE);
                        if(!swapAdapter)
                        {
                            recyclerView.swapAdapter(null, true);
                            recyclerView.setAdapter(suggestionAdapter);
                            recyclerView.removeItemDecoration(dividerItemDecoration);
                            recyclerView.setLayoutAnimation(null);
                            swapAdapter = true;
                        }

                        getActivity().getSupportLoaderManager().restartLoader(5, null, new LoaderManager.LoaderCallbacks<List<String>>() {

                            @Override
                            public Loader<List<String>> onCreateLoader(int id, Bundle args) {
                                return new SuggestionLoader(getContext(), query);
                            }

                            @Override
                            public void onLoadFinished(Loader<List<String>> loader, List<String> data) {
                                suggestions.clear();
                                suggestions.addAll(data);
                                suggestionAdapter.notifyDataSetChanged();
                                Log.d(TAG, "Query notify " + suggestions.size());
                            }

                            @Override
                            public void onLoaderReset(Loader<List<String>> loader) {
                                suggestions.clear();
                            }
                        }).forceLoad();
                    }
                    else if(query.length() < 2 && !suggestions.isEmpty())
                    {
                        clear.setVisibility(View.VISIBLE);
                        suggestions.clear();
                        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
                        Set <String> set = settings.getStringSet(SEARCH_LIST, new HashSet<>());
                        suggestions.addAll(new ArrayList<>(set));
                        suggestionAdapter.notifyDataSetChanged();
                        musicList.clear();
                        videoAdapter.notifyDataSetChanged();
                    }
                }
                else
                {
                    musicList.clear();
                    recyclerView.setVisibility(View.GONE);
                }
                return false;
            }
        });
        db = YouPlayDatabase.getInstance(getContext());
        return view;
    }

    private void buildInfoDialog() {
        SharedPreferences settings = getActivity().getSharedPreferences("website", 0);
        SharedPreferences.Editor editor = settings.edit();

        if(settings.getBoolean("skipMessage", true)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(getResources().getString(R.string.alert_title))
                    .setMessage(getResources().getString(R.string.cache_summary));
            builder.setPositiveButton(R.string.rationale_ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.cancel();
                    dialogInterface.dismiss();
                }
            });
            builder.setNeutralButton(R.string.website, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                    Intent website1 = new Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.YOUPLAY_WEBSITE+"#download"));
                    startActivity(website1);
                }
            });
            builder.setNegativeButton(R.string.rationale_dont_ask_again, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    editor.putBoolean("skipMessage", false);
                    editor.apply();
                    dialogInterface.dismiss();
                    dialogInterface.cancel();

                }
            });
            builder.create().show();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        musicClicked = (OnItemClicked) getActivity();
    }

    public void initAudioService()
    {
        audioService = AudioService.getInstance();
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState)
    {
        recyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState)
            {
                if(recyclerView.getScrollState() == RecyclerView.SCROLL_STATE_DRAGGING)
                    searchView.clearFocus();

            }
        });

    }

    /**
     * Ova se funkcija zove nakon što je pjesma obrisana iz history
     * Funkcija osvježava search listu sa history listom.
     * @param pjesma pjesma koja se osvježava
     */
    public void refreshMusicList(Music pjesma)
    {
        if(musicList.size() > 0)
        {
            int pos = getPosition(pjesma);
            if(pos >= 0)
            {
                musicList.remove(pos);
                musicList.add(pos, pjesma);
                videoAdapter.notifyDataSetChanged();
            }
        }
    }

    private int getPosition(Music pjesma)
    {
        if(pjesma != null)
            for(Music pjes : musicList)
                if(pjes.getId().equals(pjesma.getId()))
                    return musicList.indexOf(pjes);


        return -1;
    }

    public void refreshFragment()
    {
        if(getActivity() != null)
            getActivity().getSupportFragmentManager().beginTransaction().detach(this).attach(this).commitAllowingStateLoss();
    }


    @Override
    public void onResume() {
        super.onResume();

        if(getView() != null)
        {
            if(ThemeManager.getDebug().equals("Dark"))
            {
                EditText searchEditText = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
                searchEditText.setTextColor(getResources().getColor(R.color.white));
                searchEditText.setHintTextColor(getResources().getColor(R.color.white));
            }
        }
    }

    @Override
    public void onStop() {
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Service.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(searchView.getWindowToken(), 0);

        super.onStop();
    }

    @Override
    public void setupActionBar() {

    }

    public boolean ifInternetConnection() {
        if(getActivity() != null)
        {
            ConnectivityManager connectivityManager
                    = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            if(activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting())
            {
                animate(internet);
                internet.setVisibility(View.GONE);
                return true;
            }
            if(internet.getVisibility() != View.VISIBLE)
            {
                animate(internet);
                internet.setVisibility(View.VISIBLE);
            }
        }
        return false;
    }

    private void animate(View view)
    {
        Animation animation;
        if(view.getVisibility() == View.GONE)
            animation = AnimationUtils.loadAnimation(context, android.R.anim.fade_in);
        else
            animation = AnimationUtils.loadAnimation(context, android.R.anim.fade_out);

        view.startAnimation(animation);
    }

    @Override
    public void onClick(Music pjesma, View view)
    {
        int position = musicList.indexOf(pjesma);
        if(position == -1)
        {
            Toast.makeText(context, getResources().getString(R.string.error), Toast.LENGTH_SHORT).show();
            return;
        }

        Music pjes = musicList.get(position);
        initAudioService();
        audioService.getAudioPlayer().setStream(false);
        if(Utils.freeSpace(true) > 20)
            setPlayingIfNotDownloaded(pjes);
        else
            Toast.makeText(context, getResources().getString(R.string.no_space), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onInfoClicked(int position, View v)
    {
        buildAlertDialog(position,v);
    }

    private void setPlayingIfNotDownloaded(Music pjesma)
    {
        // Pogledat jel pjesma vec postoji i da li je live stream
        // Ako se neka pjesma skida, otkazi preuzimanje i postavi drugu pjesma da svira ili skida.
        FileDownloader.getImpl().pauseAll();
//        OkDownload.with().downloadDispatcher().cancelAll();
        if(!db.ifItemExists(pjesma.getId()) && !db.isDownloaded(pjesma.getId())) {
            audioService.getAudioPlayer().setPlayWhenReady(false);
            musicClicked.onMusicClick(pjesma, null, "---", false);
        }
        else {
            pjesma.setPath(FileManager.getMediaPath(pjesma.getId()));
            musicClicked.onMusicClick(pjesma);
        }

    }

    @Override
    public void onLongClick(Music pjesma, View view)
    {
        int position = musicList.indexOf(pjesma);
        buildAlertDialog(position, view);
    }

    @Override
    public void onShuffle() {

    }

    @Override
    public void buildAlertDialog(int position, View view)
    {
        final Music pjesma = musicList.get(position);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(pjesma.getTitle())
                .setItems(R.array.you_search_dialog, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        switch (i) {
                            case DIALOG_NOW_PLAYING:
                                setPlayingIfNotDownloaded(pjesma);
                                break;
                            case 1:
                                Intent appIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:" + pjesma.getId()));
                                Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.youtube.com/watch?v=" + pjesma.getId()));
                                try {
                                    getContext().startActivity(appIntent);
                                } catch (ActivityNotFoundException ex) {
                                    getContext().startActivity(webIntent);
                                }
                        }
                    }
                });
        builder.create().show();
    }

    @Override
    public void onClick(String query) {
        searchView.setQuery(query, true);
        searchView.clearFocus();
    }

    @Override
    public void onAutoClick(String query)
    {
        searchView.setQuery(query, false);
    }
}