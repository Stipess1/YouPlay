package com.stipess.youplay.fragments;


import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.stipess.youplay.Ilisteners.OnItemClicked;
import com.stipess.youplay.Ilisteners.OnPlaylistSelected;
import com.stipess.youplay.MainActivity;
import com.stipess.youplay.R;
import com.stipess.youplay.adapter.PlaylistAdapter;
import com.stipess.youplay.database.YouPlayDatabase;
import com.stipess.youplay.music.Music;
import com.stipess.youplay.utils.FileManager;
import com.stipess.youplay.utils.ThemeManager;

import java.util.ArrayList;
import java.util.List;

import static com.stipess.youplay.utils.Constants.*;

import pub.devrel.easypermissions.EasyPermissions;

/**
 * A simple {@link Fragment} subclass.
 */
public class PlaylistFragment extends BaseFragment implements OnPlaylistSelected {

    private static final String TAG = PlaylistFragment.class.getSimpleName();

    private List<String> playlists = new ArrayList<>();
    private List<Music> tempList = new ArrayList<>();;
    private PlaylistAdapter playlistAdapter;
    private RecyclerView recyclerView;
    private DividerItemDecoration dividerItemDecoration;
    public PlaylistTableFragment playlistTableFragment;
    private OnItemClicked onItemClicked;

    public PlaylistFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        onItemClicked = (OnItemClicked) getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_playlist, container, false);

        recyclerView = view.findViewById(R.id.playlist_list);
        FloatingActionButton fab = view.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view1) {
                buildAlertDialog(0, view1);
            }
        });

        if(EasyPermissions.hasPermissions(getContext(), MainActivity.PERMISSIONS))
            setupPlaylists();


        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(linearLayoutManager);
        dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(), linearLayoutManager.getOrientation());
        dividerItemDecoration.setDrawable(getActivity().getResources().getDrawable(ThemeManager.getDividerColor()));
        recyclerView.addItemDecoration(dividerItemDecoration);

        return view;
    }

    @Override
    public void initAudioService() {

    }

    @Override
    public void onResume() {
        super.onResume();
        dividerItemDecoration.setDrawable(getActivity().getResources().getDrawable(ThemeManager.getDividerColor()));
        recyclerView.removeItemDecoration(dividerItemDecoration);
        recyclerView.addItemDecoration(dividerItemDecoration);
        if(getView() != null)
            getView().setBackgroundColor(getResources().getColor(ThemeManager.getTheme()));

        if(EasyPermissions.hasPermissions(getContext(), MainActivity.PERMISSIONS) && playlists.isEmpty())
        {
            setupPlaylists();
            refreshAdapter();
        }

    }

    private void refreshAdapter()
    {
        if(playlistAdapter != null)
            playlistAdapter.notifyDataSetChanged();
    }

    public void setupPlaylists()
    {
        playlists.clear();
        if(FileManager.getRootPath().exists())
            playlists.addAll(YouPlayDatabase.getInstance(getContext()).getAllPlaylists());
        else
            Toast.makeText(getContext(), getResources().getString(R.string.files_dont_exist), Toast.LENGTH_LONG).show();

        playlistAdapter = new PlaylistAdapter(getContext(), R.layout.playlist_adapter_view, tempList, PlaylistAdapter.ListType.PLAYLIST_TABLE);
        playlistAdapter.setListner(this);

        if(playlists != null)
            playlistAdapter.setPlaylists(playlists);

        recyclerView.setAdapter(playlistAdapter);
    }

    @Override
    public void buildAlertDialog(final int position, View view)
    {
        final InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Service.INPUT_METHOD_SERVICE);

        if(view.getId() == R.id.fab)
        {
            final EditText playlistTitle = new EditText(getContext());
            playlistTitle.setHint(getResources().getString(R.string.playlist_name));
            playlistTitle.setMaxLines(1);
            playlistTitle.setSingleLine(true);

            playlistTitle.requestFocus();
            // bez delay nezeli radit.
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    imm.showSoftInput(playlistTitle, InputMethodManager.SHOW_IMPLICIT);
                }
            }, 120);

            FrameLayout frameLayout = new FrameLayout(getActivity());
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

            params.leftMargin  = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
            params.rightMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);

            playlistTitle.setLayoutParams(params);
            frameLayout.addView(playlistTitle);

            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle(getResources().getString(R.string.new_playlist))
                    .setView(frameLayout)
                    .setPositiveButton(getResources().getString(R.string.rationale_ok), null)
                    .setNegativeButton(getResources().getString(R.string.rationale_cancel), null);

            final AlertDialog dialog = builder.create();
            dialog.show();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view12) {
                    if (!playlistTitle.toString().isEmpty() && playlistTitle.length() > 1 && playlistTitle.length() <= 25) {
                        if (!YouPlayDatabase.getInstance(PlaylistFragment.this.getContext()).tableExists(playlistTitle.getText().toString())) {
                            dialog.dismiss();
                            imm.hideSoftInputFromWindow(playlistTitle.getWindowToken(), 0);
                            Snackbar snackbar = Snackbar.make(getView(), PlaylistFragment.this.getResources().getString(R.string.playlist_created, playlistTitle.getText()), Snackbar.LENGTH_SHORT);
                            View view = snackbar.getView();
                            TextView textView = view.findViewById(com.google.android.material.R.id.snackbar_text);
                            textView.setTextColor(ContextCompat.getColor(getContext(), ThemeManager.getSnackbarFont()));
                            snackbar.show();
                            YouPlayDatabase.getInstance(getContext()).createPlaylist(playlistTitle.getText().toString());
                            playlists.clear();
                            playlists.addAll(YouPlayDatabase.getInstance(getContext()).getAllPlaylists());
                            playlistAdapter.setPlaylists(playlists);
                            onItemClicked.refreshSpinnerAdapter();
                        } else {
                            Toast.makeText(getContext(), getResources().getString(R.string.playlist_exists), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), getResources().getString(R.string.playlist_enter_name), Toast.LENGTH_SHORT).show();
                    }
                }
            });

            dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialogInterface) {
                    imm.hideSoftInputFromWindow(playlistTitle.getWindowToken(), 0);
                }
            });
        }
        else
        {

            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle(playlists.get(position))
                    .setItems(getResources().getStringArray(R.array.you_playlist_dialog), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if (i == DIALOG_PLAYLIST_DELETE) {
                                YouPlayDatabase.getInstance(PlaylistFragment.this.getContext()).deletePlaylistTable(playlists.get(position), position);
                                playlists.remove(position);
                                playlistAdapter.removePlaylistSong(position);
                                onItemClicked.refreshSpinnerAdapter();
                                Snackbar snackbar = Snackbar.make(getView(), PlaylistFragment.this.getResources().getString(R.string.playlist_deleted), Snackbar.LENGTH_SHORT);
                                View view = snackbar.getView();
                                TextView textView = view.findViewById(com.google.android.material.R.id.snackbar_text);
                                textView.setTextColor(ContextCompat.getColor(getContext(), ThemeManager.getSnackbarFont()));
                                snackbar.show();
                            } else if (i == DIALOG_PLAYLIST_RENAME) {
                                final EditText playlistTitle = new EditText(PlaylistFragment.this.getContext());
                                playlistTitle.setHint(PlaylistFragment.this.getResources().getString(R.string.playlist_name));
                                playlistTitle.setMaxLines(1);
                                playlistTitle.setSingleLine(true);
                                playlistTitle.requestFocus();

                                FrameLayout frameLayout = new FrameLayout(PlaylistFragment.this.getActivity());
                                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

                                params.leftMargin = PlaylistFragment.this.getResources().getDimensionPixelSize(R.dimen.dialog_margin);
                                params.rightMargin = PlaylistFragment.this.getResources().getDimensionPixelSize(R.dimen.dialog_margin);

                                playlistTitle.setLayoutParams(params);
                                frameLayout.addView(playlistTitle);

                                final AlertDialog.Builder rename = new AlertDialog.Builder(PlaylistFragment.this.getContext());
                                rename.setTitle(playlists.get(position))
                                        .setView(frameLayout)
                                        .setPositiveButton(PlaylistFragment.this.getResources().getString(R.string.rationale_ok), null)
                                        .setNegativeButton(PlaylistFragment.this.getResources().getString(R.string.rationale_cancel), null);

                                final AlertDialog renameAlert = rename.create();
                                renameAlert.show();
                                renameAlert.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view1) {
                                        if (!YouPlayDatabase.getInstance(PlaylistFragment.this.getContext()).tableExists(playlistTitle.getText().toString())) {
                                            YouPlayDatabase.getInstance(PlaylistFragment.this.getContext()).renamePlaylist(playlists.get(position), playlistTitle.getText().toString());
                                            playlists.clear();
                                            playlists.addAll(YouPlayDatabase.getInstance(PlaylistFragment.this.getContext()).getAllPlaylists());
                                            playlistAdapter.setPlaylists(playlists);
                                            onItemClicked.refreshSpinnerAdapter();
                                            renameAlert.dismiss();
                                        } else {
                                            Toast.makeText(PlaylistFragment.this.getContext(), PlaylistFragment.this.getResources().getString(R.string.playlist_exists), Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });

                            }
                        }
                    });
            builder.create().show();

        }
    }

    @Override
    public void setupActionBar() {

    }

    @Override
    public void onClick(String title, View view)
    {
        int position = playlists.indexOf(title);
        title = playlists.get(position);

        playlistTableFragment = new PlaylistTableFragment();
        playlistTableFragment.setData(title);

        Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                FragmentTransaction ft = PlaylistFragment.this.getActivity().getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.fragment_container, playlistTableFragment);
                ft.addToBackStack("playlistTable");
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                ft.commit();
            }
        };
        handler.postDelayed(runnable, 300);

    }


    @Override
    public void onLongClick(String title, View view)
    {
        int position = playlists.indexOf(title);
        buildAlertDialog(position, view);
    }

    @Override
    public void onInfoClicked(String title, View view)
    {
        int position = playlists.indexOf(title);
        buildAlertDialog(position, view);
    }
}
