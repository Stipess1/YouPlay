package com.hfad.youplay.fragments;


import android.content.Context;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import com.hfad.youplay.Ilisteners.OnItemClicked;
import com.hfad.youplay.Ilisteners.OnMusicSelected;
import com.hfad.youplay.R;
import com.hfad.youplay.adapter.VideoAdapter;
import com.hfad.youplay.database.YouPlayDatabase;
import com.hfad.youplay.music.Music;
import com.hfad.youplay.utils.FileManager;
import com.hfad.youplay.utils.ThemeManager;

import java.util.ArrayList;
import java.util.List;

import static com.hfad.youplay.utils.Constants.DIALOG_NOW_PLAYING;
import static com.hfad.youplay.utils.Constants.DIALOG_TABLE_DELETE;

/**
 * A simple {@link Fragment} subclass.
 */
public class PlaylistTableFragment extends BaseFragment implements OnMusicSelected {

    private static final String TAG = PlaylistTableFragment.class.getSimpleName();

    private RecyclerView recyclerView;
    private String title = "";
    private List<Music> data;
    private DividerItemDecoration dividerItemDecoration;
    private OnItemClicked onItemClicked;
    public VideoAdapter videoAdapter;

    public PlaylistTableFragment() {
        // Required empty public constructor
    }

    @Override
    public void initAudioService() {

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        onItemClicked = (OnItemClicked) getActivity();
    }

    @Override
    public void buildAlertDialog(final int position,final View view) {

        final Music pjesma = data.get(position);
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(pjesma.getTitle())
                .setItems(R.array.you_playlist_table_dialog, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        switch (i) {
                            case DIALOG_NOW_PLAYING:
                                PlaylistTableFragment.this.setCurrentSong(pjesma, position);
                                dialogInterface.cancel();
                                break;
                            case DIALOG_TABLE_DELETE:
                                view.startAnimation(AnimationUtils.loadAnimation(PlaylistTableFragment.this.getContext(), android.R.anim.fade_out));
                                data.remove(pjesma);
                                videoAdapter.deleteMusic(position);
                                YouPlayDatabase.getInstance(PlaylistTableFragment.this.getContext()).deleteTableMusic(title, position);
                                Snackbar snackbar = Snackbar.make(PlaylistTableFragment.this.getView(), PlaylistTableFragment.this.getResources().getString(R.string.song_deleted), Snackbar.LENGTH_SHORT);
                                View view = snackbar.getView();
                                TextView textView = view.findViewById(android.support.design.R.id.snackbar_text);
                                textView.setTextColor(ContextCompat.getColor(getContext(), ThemeManager.getSnackbarFont()));
                                snackbar.show();
                                dialogInterface.cancel();
                                break;
                        }
                    }
                });
        builder.create().show();
    }

    @Override
    public void setupActionBar() {

    }

    private void setCurrentSong(Music pjesma, int position)
    {
        YouPlayDatabase database = YouPlayDatabase.getInstance(getContext());
        if(database.ifItemExists(pjesma.getId()) && database.isDownloaded(pjesma.getId()))
        {
            pjesma.setPath(FileManager.getMediaPath(pjesma.getId()));
            onItemClicked.onMusicClick(pjesma, data, title, false);
        }
        else
        {
            data.remove(position);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if(getView() != null)
            getView().setBackgroundColor(getResources().getColor(ThemeManager.getTheme()));

        dividerItemDecoration.setDrawable(getActivity().getResources().getDrawable(ThemeManager.getDividerColor()));
        recyclerView.removeItemDecoration(dividerItemDecoration);
        recyclerView.addItemDecoration(dividerItemDecoration);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_playlist_table, container, false);
        recyclerView = view.findViewById(R.id.playlist_recyclerView);
        try{
            data = YouPlayDatabase.getInstance(getContext()).getDataTable(title);
            for(int i = 0; i < data.size(); i++)
            {
                Music pjesma = data.get(i);
                if(!FileManager.getMediaFile(pjesma.getId()).exists())
                {
                    pjesma.setDownloaded(0);
                    data.remove(i);
                    data.add(i, pjesma);
                }
            }
        }catch (SQLiteException e)
        {
            Toast.makeText(getContext(), getString(R.string.playlist_error, title), Toast.LENGTH_SHORT).show();
        }

        recyclerView.setBackgroundColor(getResources().getColor(ThemeManager.getTheme()));
        view.setBackgroundColor(getResources().getColor(ThemeManager.getTheme()));

        videoAdapter = new VideoAdapter(getContext(), R.layout.play_adapter_view, data, false);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(linearLayoutManager);
        dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(), linearLayoutManager.getOrientation());
        dividerItemDecoration.setDrawable(getActivity().getResources().getDrawable(ThemeManager.getDividerColor()));
        recyclerView.addItemDecoration(dividerItemDecoration);
        recyclerView.setAdapter(videoAdapter);
        videoAdapter.setListener(this);

        return view;
    }

    public void setData(String title)
    {
        this.title = title;
    }

    @Override
    public void onClick(Music pjesma, View view)
    {
        onItemClicked.onMusicClick(pjesma, data, title, false);
        setPlayScreen();
    }

    @Override
    public void onShuffle() {
        onItemClicked.onMusicClick(data.get(0), data, title, true);
        setPlayScreen();
    }

    public void refreshAdapter()
    {
        data.clear();
        try{
            data.addAll(YouPlayDatabase.getInstance(getContext()).getDataTable(title));
            for(int i = 0; i < data.size(); i++)
            {
                Music pjesma = data.get(i);
                if(!FileManager.getMediaFile(pjesma.getId()).exists())
                {
                    pjesma.setDownloaded(0);
                    data.remove(i);
                    data.add(i, pjesma);
                }
            }
        }
        catch (SQLiteException e)
        {
            if(getContext() != null)
                Toast.makeText(getContext(), getResources().getString(R.string.cannot_find_table), Toast.LENGTH_SHORT).show();
        }
        videoAdapter.notifyDataSetChanged();
    }

    public void resetAdapter()
    {
        videoAdapter.disableEdit();
    }

    @Override
    public void onLongClick(Music pjesma, View v)
    {
        FragmentTransaction fragmentTransaction = getActivity().getSupportFragmentManager().beginTransaction();
        fragmentTransaction.addToBackStack("edit");
        fragmentTransaction.commit();
        videoAdapter.setEdit("["+title+"]", pjesma);
    }

    @Override
    public void onInfoClicked(int position, View v)
    {
        buildAlertDialog(position, v);
    }
}
