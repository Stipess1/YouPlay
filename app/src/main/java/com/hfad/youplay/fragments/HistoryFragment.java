package com.hfad.youplay.fragments;


import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteCantOpenDatabaseException;
import android.database.sqlite.SQLiteDiskIOException;
import android.database.sqlite.SQLiteFullException;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SimpleItemAnimator;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.webkit.URLUtil;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.hfad.youplay.AudioService;
import com.hfad.youplay.Ilisteners.OnDataChanged;
import com.hfad.youplay.Ilisteners.OnItemClicked;
import com.hfad.youplay.MainActivity;
import com.hfad.youplay.R;
import com.hfad.youplay.SettingsActivity;
import com.hfad.youplay.adapter.VideoAdapter;
import com.hfad.youplay.database.DatabaseHandler;
import com.hfad.youplay.database.YouPlayDatabase;
import com.hfad.youplay.Ilisteners.OnMusicSelected;
import com.hfad.youplay.music.Music;
import com.hfad.youplay.utils.FileManager;
import com.hfad.youplay.utils.Order;
import com.hfad.youplay.utils.ThemeManager;
import com.hfad.youplay.utils.Utils;
import com.liulishuo.okdownload.OkDownload;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

import static com.hfad.youplay.utils.Constants.*;

/**
 * A simple {@link Fragment} subclass.
 */
public class HistoryFragment extends BaseFragment implements OnMusicSelected,
        View.OnClickListener,
        OnDataChanged
{

    private static final String TAG = "HistoryFragment";

    private RecyclerView recyclerView;
    public List<Music> musicList;
    private List<Music> tempList;
    public VideoAdapter adapter;
    private OnItemClicked onItemClicked;
    private TextView addToQ;
    private TextView addToPlaylist;
    private TextView delete;
    private TextView history;
    private SearchView searchView;
    private AudioService audioService;
    private MenuItem searchItem;
    private ProgressBar barLoading;
    private List<Music> queueList;
    private YouPlayDatabase db;
    private ActionBar actionBar;
    private DividerItemDecoration dividerItemDecoration;
    private boolean queue = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        musicList     = new ArrayList<>();
        queueList     = new ArrayList<>();
        tempList      = new ArrayList<>();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        onItemClicked = (OnItemClicked) getActivity();
        db = YouPlayDatabase.getInstance(context);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_top, container, false);
        recyclerView = view.findViewById(R.id.play_view);
        addToQ       = view.findViewById(R.id.add_to_queue);
        addToPlaylist= view.findViewById(R.id.add_to_playlist);
        delete       = view.findViewById(R.id.delete_selected);
        barLoading   = view.findViewById(R.id.history_list_loading);
        history      = view.findViewById(R.id.empty_history);
        ConstraintLayout layout = view.findViewById(R.id.history_container);

        layout.setBackgroundColor(getResources().getColor(ThemeManager.getTheme()));

        recyclerView.setBackgroundColor(getResources().getColor(ThemeManager.getTheme()));

        addToPlaylist.setOnClickListener(this);
        addToQ.setOnClickListener(this);
        delete.setOnClickListener(this);
        setupAdapter();

        setHasOptionsMenu(true);

        Toolbar toolbar = view.findViewById(R.id.toolbar);
       // searchView = view.findViewById(R.id.history_search_view);

        ((AppCompatActivity)getActivity()).setSupportActionBar(toolbar);

        actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();

        toolbar.inflateMenu(R.menu.history_menu);
        searchItem = toolbar.getMenu().findItem(R.id.action_search);
        searchView = (SearchView) searchItem.getActionView();
        searchView.setQueryHint(getResources().getString(R.string.search_history));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                adapter.filter(s);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                adapter.filter(s);
                return true;
            }
        });

//        setSearchView();
        return view;
    }

    @Override
    public void onResume() {
        // Posto refreshList uzima podatke iz SQL nezelimo svaki put uzet podatke ako nije potrebno
        // npr. ako udemo u postavke i izadem bit ce manji delay zato sto treba dohvatit sve podatke
        // iz SQL-a
        if(musicList.isEmpty())
            refreshList();

        super.onResume();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setRetainInstance(true);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        setupActionBar();
    }

    @Override
    public void initAudioService() {
        audioService = AudioService.getInstance();
    }

    @Override
    public void onClick(View view)
    {
        int id = view.getId();
        if(id == R.id.add_to_queue)
        {
            if(!adapter.getAll().isEmpty())
                addToQueue(adapter.getAll());
            resetAdapter();
        }
        else if(id == R.id.add_to_playlist)
        {
            buildPlaylistDialog(adapter.getAll());
        }
        else if(id == R.id.delete_selected)
        {
            view.startAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_out));
            AlertDialog.Builder dialog = new AlertDialog.Builder(getContext(), ThemeManager.getDialogTheme());
            View dialogView = getLayoutInflater().inflate(R.layout.delete_dialog, null);

            TextView name = dialogView.findViewById(R.id.deleting_title);
            TextView size = dialogView.findViewById(R.id.deleting_size);
            dialog.setView(dialogView);

            int listSize = adapter.getAll().size();

            AlertDialog alert = dialog.create();
            alert.show();

            new DatabaseHandler(adapter.getAll(),
                    TABLE_NAME,
                    YouPlayDatabase.YOUPLAY_DB,
                    DatabaseHandler.UpdateType.REMOVE_LIST).setDataChangedListener(new OnDataChanged() {
                @Override
                public void dataChanged(DatabaseHandler.UpdateType type, String databaseName, Music pjesma) {
                    if(type == DatabaseHandler.UpdateType.REMOVE_LIST)
                    {
                        alert.dismiss();
                        adapter.refreshList();
                        checkIfEmpty();
                        if(PlayFragment.currentlyPlayingSong != null)
                            onItemClicked.refreshSuggestions(musicList, true);

                        resetAdapter();
                    }
                }

                @Override
                public void deleteProgress(int position, String title)
                {
                    name.setText(title);
                    String left = position + "/" + listSize;
                    size.setText(left);
                }

                @Override
                public void dataChanged(DatabaseHandler.UpdateType type, List<Music> pjesme) {

                }
            }).execute();
        }
    }

    @Override
    public void dataChanged(DatabaseHandler.UpdateType type, String databaseName, Music pjesma)
    {
        if(type == DatabaseHandler.UpdateType.REMOVE)
        {
            Snackbar.make(getView(), getResources().getString(R.string.song_deleted), Snackbar.LENGTH_SHORT).show();
            pjesma.setDownloaded(0);
            checkIfEmpty();
            if(PlayFragment.currentlyPlayingSong != null)
                onItemClicked.refreshSuggestions(musicList, true);

            onItemClicked.refreshSearchList(pjesma);
        }
    }

    @Override
    public void deleteProgress(int length, String title)
    {

    }

    @Override
    public void dataChanged(DatabaseHandler.UpdateType type, List<Music> pjesme) {
        if(type == DatabaseHandler.UpdateType.GET && pjesme == null)
            barLoading.setVisibility(View.VISIBLE);
        else
        {
            adapter.refreshList(pjesme);
            barLoading.setVisibility(View.GONE);
            checkIfEmpty();
        }
    }

    //    @Override
//    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
//    {
//        inflater.inflate(R.menu.history_menu, menu);
//
//        MenuItem searchItem = menu.findItem(R.id.action_search);
//        Log.d(TAG, "ON Create OPtions menus");
//
//    }


    public MenuItem getSearchView() {
        return searchItem;
    }

    public void refreshFragment()
    {
        if(getActivity() != null)
            getActivity().getSupportFragmentManager().beginTransaction().detach(this).attach(this).commitAllowingStateLoss();
    }

    public void resetAdapter()
    {
        addToQ.setVisibility(View.GONE);
        addToPlaylist.setVisibility(View.GONE);
        delete.setVisibility(View.GONE);

        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) recyclerView.getLayoutParams();
        params.bottomMargin = 0;

        adapter.disableEdit();
        Snackbar.make(getView(), getResources().getString(R.string.changes_saved), Snackbar.LENGTH_SHORT).show();
    }

    private void addToQueue(Music pjesma)
    {
        if(queueList.isEmpty())
            queue = false;
        else
            queue = true;

        queueList.add(pjesma);
        if(PlayFragment.currentlyPlayingSong != null &&
                !containsSong(PlayFragment.currentlyPlayingSong))
            queueList.add(0,PlayFragment.currentlyPlayingSong);

        tempList.clear();
        tempList.addAll(queueList);

        onItemClicked.refreshSuggestions(tempList, queue);
        Snackbar.make(getView(), getResources().getString(R.string.song_in_queue), Snackbar.LENGTH_SHORT).show();
    }

    private void addToQueue(List<Music> data)
    {
        queue = queueList.isEmpty();

        queueList.addAll(data);
        if(PlayFragment.currentlyPlayingSong != null &&
                !containsSong(PlayFragment.currentlyPlayingSong))
            queueList.add(0,PlayFragment.currentlyPlayingSong);

        tempList.clear();
        tempList.addAll(queueList);

        onItemClicked.refreshSuggestions(tempList, queue);
    }

    private boolean containsSong(Music pjesma)
    {
        for(Music pjes : queueList)
        {
            if(pjes.getId().equals(pjesma.getId()))
                return true;
        }

        return false;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();
        if(id == R.id.latest)
        {
            YouPlayDatabase db = YouPlayDatabase.getInstance();
            if(db.getOrder().equals(Order.ORDER_OLDEST))
            {
                Collections.reverse(musicList);
                adapter.notifyDataSetChanged();
                YouPlayDatabase.getInstance(getContext()).settingsOrderBy(Order.ORDER_LATEST);
                if(PlayFragment.currentlyPlayingSong != null)
                    onItemClicked.refreshSuggestions(musicList, true);

                Snackbar snackbar = Snackbar.make(getView(), getResources().getString(R.string.changes_saved), Snackbar.LENGTH_SHORT);
                View view = snackbar.getView();
                TextView textView = view.findViewById(android.support.design.R.id.snackbar_text);
                textView.setTextColor(ContextCompat.getColor(getContext(), ThemeManager.getSnackbarFont()));
                snackbar.show();
            }
            else
            {
                Toast.makeText(getContext(), getResources().getString(R.string.al_latest), Toast.LENGTH_SHORT).show();
            }
        }
        else if(id == R.id.oldest)
        {
            YouPlayDatabase db = YouPlayDatabase.getInstance();
            if(db.getOrder().equals(Order.ORDER_LATEST))
            {
                Collections.reverse(musicList);
                adapter.notifyDataSetChanged();
                YouPlayDatabase.getInstance(getContext()).settingsOrderBy(Order.ORDER_OLDEST);
                if(PlayFragment.currentlyPlayingSong != null)
                    onItemClicked.refreshSuggestions(musicList, true);

                Snackbar snackbar = Snackbar.make(getView(), getResources().getString(R.string.changes_saved), Snackbar.LENGTH_SHORT);
                View view = snackbar.getView();
                TextView textView = view.findViewById(android.support.design.R.id.snackbar_text);
                textView.setTextColor(ContextCompat.getColor(getContext(), ThemeManager.getSnackbarFont()));
                snackbar.show();
            }
            else
            {
                Toast.makeText(getContext(), getResources().getString(R.string.al_oldest), Toast.LENGTH_SHORT).show();
            }
        }
        else if(id == R.id.settings)
        {
            Intent intent = new Intent(getContext(), SettingsActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void setupActionBar()
    {

        if(actionBar != null)
        {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayShowHomeEnabled(false);
            actionBar.setDisplayShowCustomEnabled(true);

            View customToolbar = LayoutInflater.from(getContext()).inflate(R.layout.toolbar_layout, null);
            actionBar.setCustomView(customToolbar);

            if(getView() != null)
            {
                TextView textView = getView().findViewById(R.id.toolbar_title);
                textView.setTextColor(getResources().getColor(ThemeManager.getFontTheme()));
            }
        }
    }

    public void setupAdapter()
    {
        adapter = new VideoAdapter(getContext(), R.layout.play_adapter_view, musicList, true);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(linearLayoutManager);
        dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(), linearLayoutManager.getOrientation());
        dividerItemDecoration.setDrawable(getActivity().getResources().getDrawable(ThemeManager.getDividerColor()));
        recyclerView.addItemDecoration(dividerItemDecoration);
        adapter.setListener(this);
        recyclerView.setAdapter(adapter);

        // Da se nevidi "blink" kada stisnemo na pjesmu
        ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
    }

    public void refreshList()
    {
        if(audioService == null)
            audioService = AudioService.getInstance();

        if(EasyPermissions.hasPermissions(getContext(), MainActivity.PERMISSIONS))
        {
            try
            {
                YouPlayDatabase.getInstance(getContext()).createPlaylistDatabase();
            }
            catch (SQLiteDiskIOException | SQLiteFullException e)
            {
                Toast.makeText(getContext(), getResources().getString(R.string.no_space), Toast.LENGTH_SHORT).show();
            }
            catch (SQLiteCantOpenDatabaseException e)
            {
                Toast.makeText(getContext(), getResources().getString(R.string.refresh_error), Toast.LENGTH_SHORT).show();
            }

            dividerItemDecoration.setDrawable(getActivity().getResources().getDrawable(ThemeManager.getDividerColor()));
            recyclerView.removeItemDecoration(dividerItemDecoration);
            recyclerView.addItemDecoration(dividerItemDecoration);

            if(Utils.freeSpace(true) > 20 && FileManager.getRootPath().exists())
                adapter.refreshList();
            else if(!FileManager.getRootPath().exists())
                Toast.makeText(getContext(), getResources().getString(R.string.files_dont_exist), Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(getContext(), getResources().getString(R.string.no_space), Toast.LENGTH_SHORT).show();
        }

    }

    /**
     * Posto refreshList uzima podatke iz SQL zna se desit lag. Ovako cemo samo dodat pjesmu u listu
     * i osvjezit adapter i tijekom sljedece pokretanje aplikacije, uzet ce podatke iz SQL-a.
     * @param pjesma
     */
    public void addToList(Music pjesma)
    {
        int position;
        boolean exist = false;
        for(Music temp : musicList)
        {
            if(temp.getId().equals(pjesma.getId()))
            {
                exist = true;
                position = musicList.indexOf(temp);
                musicList.remove(position);
                musicList.add(position, pjesma);
                break;
            }
        }
        if(!exist)
            musicList.add(0, pjesma);

        adapter.notifyDataSetChanged();
        adapter.notifyFilterData(musicList);
        checkIfEmpty();
    }

    public void checkIfEmpty()
    {
        history.setTextColor(getResources().getColor(ThemeManager.getFontTheme()));
        if(musicList.size() > 0)
            history.setVisibility(View.GONE);
        else
            history.setVisibility(View.VISIBLE);

    }

    @Override
    public void onClick(Music pjesma, View view)
    {
        if(!adapter.getState())
        {
            OkDownload.with().downloadDispatcher().cancelAll();
            if(AudioService.getInstance().exoPlayer.getPlayWhenReady())
                AudioService.getInstance().exoPlayer.stop();

            if(URLUtil.isValidUrl(pjesma.getPath()))
                pjesma.setPath("");

            if(searchView.getQuery().length() > 0)
            {
                searchView.setQuery("", true);
                getSearchView().collapseActionView();
            }

            adapter.notifyFilterData(musicList);
            onItemClicked.onMusicClick(pjesma, musicList);
            setPlayScreen();
        }
    }

    @Override
    public void buildAlertDialog(final int position, final View view)
    {
        final Music pjesma = musicList.get(position);

        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), ThemeManager.getDialogTheme());
        builder.setTitle(pjesma.getTitle())
                .setItems(getResources().getStringArray(R.array.you_history_dialog), (dialogInterface, i) -> {
                    switch (i)
                    {
                        case DIALOG_NOW_PLAYING:
                            onItemClicked.onMusicClick(pjesma, musicList);
                            break;
                        case DIALOG_ADD_QUEUE:
                           addToQueue(pjesma);
                            break;
                        case DIALOG_PLAYLIST:
                            buildPlaylistDialog(pjesma);
                            break;
                        case DIALOG_YOUTUBE:
                            Intent appIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:" + pjesma.getId()));
                            Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.youtube.com/watch?v=" + pjesma.getId()));
                            try {
                                getContext().startActivity(appIntent);
                            } catch (ActivityNotFoundException ex) {
                                getContext().startActivity(webIntent);
                            }
                            finally {
                                onItemClicked.pauseSong();
                            }
                            break;
                        case DIALOG_DELETE:
                            view.startAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_out));
                            musicList.remove(pjesma);
                            adapter.deleteMusic(position);
                            adapter.notifyFilterData(musicList);
                            new DatabaseHandler(pjesma,
                                    TABLE_NAME,
                                    YouPlayDatabase.YOUPLAY_DB,
                                    DatabaseHandler.UpdateType.REMOVE).setDataChangedListener(this).execute();
                            checkIfEmpty();
                            break;

                    }
                });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void buildPlaylistDialog(final Music pjesma)
    {
        final List<String> titles = YouPlayDatabase.getInstance(getContext()).getAllPlaylists();
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), ThemeManager.getDialogTheme());
        builder.setTitle(getResources().getString(R.string.add_to_playlist))
                .setItems(titles.toArray(new CharSequence[titles.size()]), (dialogInterface, i) -> {
                    String title = titles.get(i);
                    YouPlayDatabase.getInstance(getContext()).insertInTable(pjesma, title);
                    Snackbar.make(getView(), getResources().getString(R.string.playlist_added), Snackbar.LENGTH_SHORT).show();
                    onItemClicked.refreshPlaylist();
                });
        builder.create().show();
    }

    private void buildPlaylistDialog(final List<Music> data)
    {
        final List<String> titles = db.getAllPlaylists();
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), ThemeManager.getDialogTheme());
        builder.setTitle(getResources().getString(R.string.add_to_playlist))
                .setItems(titles.toArray(new CharSequence[titles.size()]), (dialogInterface, i) -> {
                    String title = titles.get(i);
                    new DatabaseHandler(data, title, YouPlayDatabase.PLAYLIST_DB, DatabaseHandler.UpdateType.ADD).setDataChangedListener(new OnDataChanged() {
                        @Override
                        public void dataChanged(DatabaseHandler.UpdateType type, String databaseName, Music pjesma) {
                            onItemClicked.refreshPlaylist();
                            resetAdapter();
                        }

                        @Override
                        public void deleteProgress(int length, String title) {

                        }

                        @Override
                        public void dataChanged(DatabaseHandler.UpdateType type, List<Music> pjesme) {

                        }
                    }).execute();
                });
        builder.create().show();
    }

    @Override
    public void onInfoClicked(int position, View v)
    {
        buildAlertDialog(position, v);
    }

    @Override
    public void onLongClick(Music pjesma, View view)
    {
        FragmentTransaction fragmentTransaction = getActivity().getSupportFragmentManager().beginTransaction();
        fragmentTransaction.addToBackStack("History");
        fragmentTransaction.commit();

        adapter.setEdit(TABLE_NAME, pjesma);

        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) recyclerView.getLayoutParams();
        params.bottomMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());

        addToQ.setVisibility(View.VISIBLE);
        addToPlaylist.setVisibility(View.VISIBLE);
        delete.setVisibility(View.VISIBLE);
    }

}
