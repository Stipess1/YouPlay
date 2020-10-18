package com.stipess.youplay.fragments;


import android.content.Context;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.stipess.youplay.AudioService;
import com.stipess.youplay.Ilisteners.OnItemClicked;
import com.stipess.youplay.Ilisteners.OnMusicSelected;
import com.stipess.youplay.R;
import com.stipess.youplay.adapter.VideoAdapter;
import com.stipess.youplay.database.DatabaseHandler;
import com.stipess.youplay.database.YouPlayDatabase;
import com.stipess.youplay.music.Music;
import com.stipess.youplay.player.AudioPlayer;
import com.stipess.youplay.utils.FileManager;
import com.stipess.youplay.utils.ThemeManager;

import java.util.ArrayList;

import static com.stipess.youplay.utils.Constants.DIALOG_NOW_PLAYING;
import static com.stipess.youplay.utils.Constants.DIALOG_TABLE_DELETE;
import static com.stipess.youplay.utils.Constants.TABLE_NAME;

/**
 * Created by Stjepan Stjepanovic
 * <p>
 * Copyright (C) Stjepan Stjepanovic 2017 <stipess@youplayandroid.com>
 * PlaylistTableFragment.java is part of YouPlay.
 * <p>
 * YouPlay is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * YouPlay is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with YouPlay.  If not, see <http://www.gnu.org/licenses/>.
 */
public class PlaylistTableFragment extends BaseFragment implements OnMusicSelected {

    private static final String TAG = PlaylistTableFragment.class.getSimpleName();

    private RecyclerView recyclerView;
    private String title = "";
    private ArrayList<Music> data;
    private DividerItemDecoration dividerItemDecoration;
    private OnItemClicked onItemClicked;
    private AudioPlayer audioPlayer;
    public VideoAdapter videoAdapter;

    public PlaylistTableFragment() {
        // Required empty public constructor
    }

    @Override
    public void initAudioService() {
        audioPlayer = AudioService.getInstance().getAudioPlayer();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        onItemClicked = (OnItemClicked) getActivity();
        initAudioService();
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
                                YouPlayDatabase.getInstance(getContext()).deleteTableMusic(title, position);
                                Snackbar snackbar = Snackbar.make(getView(), PlaylistTableFragment.this.getResources().getString(R.string.song_deleted), Snackbar.LENGTH_SHORT);
                                View view = snackbar.getView();
                                TextView textView = view.findViewById(com.google.android.material.R.id.snackbar_text);
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
                    data.remove(pjesma);
                    YouPlayDatabase.getInstance(getContext()).deleteTableMusic(title, i);
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
        audioPlayer.setMusicList(data);
        audioPlayer.setPosition(data.indexOf(pjesma));
        onItemClicked.onMusicClick(pjesma, data, title, false);
        setPlayScreen();
    }

    @Override
    public void onShuffle() {
        audioPlayer.setMusicList(data);
        audioPlayer.setPosition(0);
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
                    data.remove(pjesma);
                    videoAdapter.deleteMusic(i);
                    YouPlayDatabase.getInstance(getContext()).deleteTableMusic(title, i);
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
