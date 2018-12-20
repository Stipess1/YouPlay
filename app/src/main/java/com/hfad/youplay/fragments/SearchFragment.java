package com.hfad.youplay.fragments;


import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
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

import com.hfad.youplay.AudioService;
import com.hfad.youplay.Ilisteners.OnItemClicked;
import com.hfad.youplay.Ilisteners.OnSuggestionSelected;
import com.hfad.youplay.R;
import com.hfad.youplay.adapter.SearchAdapter;
import com.hfad.youplay.adapter.SuggestionAdapter;
import com.hfad.youplay.database.YouPlayDatabase;
import com.hfad.youplay.Ilisteners.OnMusicSelected;
import com.hfad.youplay.music.Music;

import com.hfad.youplay.utils.FileManager;
import com.hfad.youplay.utils.ThemeManager;
import com.hfad.youplay.utils.Utils;
import com.hfad.youplay.youtube.loaders.SuggestionLoader;
import com.hfad.youplay.youtube.loaders.YoutubeMusicLoader;
import com.liulishuo.okdownload.OkDownload;

import java.util.ArrayList;
import java.util.List;

import static com.hfad.youplay.utils.Constants.*;

/**
 * A simple {@link Fragment} subclass.
 */
public class SearchFragment extends BaseFragment implements OnMusicSelected, OnSuggestionSelected{

    public SearchFragment() {
        // Required empty public constructor
    }

    private static final String TAG = SearchFragment.class.getSimpleName();

    public RecyclerView recyclerView;
    public ProgressBar progressBar;
    private SearchAdapter videoAdapter;
    private DividerItemDecoration dividerItemDecoration;
    private ArrayList<Music> musicList = new ArrayList<>();
    private Context context;
    private TextView internet, noResult;
    private SearchView searchView;
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

        videoAdapter = new SearchAdapter(getContext(), R.layout.play_adapter_view, musicList);
        suggestionAdapter = new SuggestionAdapter(getContext(), R.layout.suggestion, suggestions);

        suggestionAdapter.setListener(this);
        videoAdapter.setListener(this);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(linearLayoutManager);
        dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(), linearLayoutManager.getOrientation());
        dividerItemDecoration.setDrawable(getActivity().getResources().getDrawable(ThemeManager.getDividerColor()));
//        recyclerView.addItemDecoration(dividerItemDecoration);


        Toolbar toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity)getActivity()).setSupportActionBar(toolbar);
        setupActionBar();

        db = YouPlayDatabase.getInstance(getContext());
        return view;
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

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(final String query) {
                if(ifInternetConnection())
                {
                    swapAdapter = false;

                    recyclerView.swapAdapter(null, true);
                    recyclerView.setAdapter(videoAdapter);
                    recyclerView.addItemDecoration(dividerItemDecoration);

                    getLoaderManager().restartLoader(1, null, new android.support.v4.app.LoaderManager.LoaderCallbacks<List<Music>>() {

                        @Override
                        public android.support.v4.content.Loader<List<Music>> onCreateLoader(int id, Bundle args) {
                            return new YoutubeMusicLoader(getContext(), query);
                        }

                        @Override
                        public void onLoadFinished(android.support.v4.content.Loader<List<Music>> loader, List<Music> data) {
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
                        }

                        @Override
                        public void onLoaderReset(android.support.v4.content.Loader<List<Music>> loader) {

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
                if(ifInternetConnection())
                {
                    if(query.length() > 2)
                    {
                        if(!swapAdapter)
                        {
                            recyclerView.swapAdapter(null, true);
                            recyclerView.setAdapter(suggestionAdapter);
                            recyclerView.removeItemDecoration(dividerItemDecoration);
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
                            }

                            @Override
                            public void onLoaderReset(Loader<List<String>> loader) {
                                suggestions.clear();
                            }
                        }).forceLoad();
                    }
                    else if(query.length() < 2 && !suggestions.isEmpty())
                    {
                        suggestions.clear();
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
                EditText searchEditText = searchView.findViewById(android.support.v7.appcompat.R.id.search_src_text);
                searchEditText.setTextColor(getResources().getColor(R.color.white));
                searchEditText.setHintTextColor(getResources().getColor(R.color.white));
            }

//            dividerItemDecoration.setDrawable(getActivity().getResources().getDrawable(ThemeManager.getDividerColor()));
//            recyclerView.removeItemDecoration(dividerItemDecoration);
//            recyclerView.addItemDecoration(dividerItemDecoration);
            internet.setTextColor(ContextCompat.getColor(getContext(), ThemeManager.getFontTheme()));
            noResult.setTextColor(ContextCompat.getColor(getContext(), ThemeManager.getFontTheme()));

            getView().setBackgroundColor(getResources().getColor(ThemeManager.getTheme()));
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
        AudioService.getInstance().isStream(false);
        initAudioService();
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

    public void setPlayingIfNotDownloaded(Music pjesma)
    {
        // Pogledat jel pjesma vec postoji i da li je live stream
        // Ako se neka pjesma skida, otkazi preuzimanje i postavi drugu pjesma da svira ili skida.
        OkDownload.with().downloadDispatcher().cancelAll();
        if(!db.ifItemExists(pjesma.getId()) && !db.isDownloaded(pjesma.getId()))
        {
            audioService.exoPlayer.setPlayWhenReady(false);
            musicClicked.onMusicClick(pjesma, null);
        }
        else
        {
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
    public void buildAlertDialog(int position, View view)
    {
        final Music pjesma = musicList.get(position);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), ThemeManager.getDialogTheme());
        builder.setTitle(pjesma.getTitle())
                .setItems(R.array.you_search_dialog, (dialogInterface, i) -> {
                    switch (i)
                    {
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
                });
        builder.create().show();
    }

    @Override
    public void onClick(String query)
    {
        searchView.setQuery(query, true);
        searchView.clearFocus();
    }

    @Override
    public void onAutoClick(String query)
    {
        searchView.setQuery(query, false);
    }
}
