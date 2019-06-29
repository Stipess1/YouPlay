package com.hfad.youplay.fragments;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.appbar.AppBarLayout;
import com.hfad.youplay.R;

/**
 * A simple {@link androidx.fragment.app.Fragment} subclass.
 */
public class DefaultPlaylistFragment extends BaseFragment {

    private AppBarLayout barLayout;
    public PlaylistFragment playlistFragment;
    private ActionBar actionBar;

    public DefaultPlaylistFragment() {
        // Required empty public constructor
    }

    @Override
    public void buildAlertDialog(int position, View view) {

    }

    @Override
    public void initAudioService() {

    }

    @Override
    public void setupActionBar() {

        if(actionBar != null)
        {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayShowHomeEnabled(false);
            actionBar.setDisplayShowCustomEnabled(true);

            View customToolbar = LayoutInflater.from(getContext()).inflate(R.layout.toolbar_layout, null);
            actionBar.setCustomView(customToolbar);
        }
    }

    public void refreshFragment()
    {
        if(getActivity() != null)
            getActivity().getSupportFragmentManager().beginTransaction().detach(this).attach(this).commitAllowingStateLoss();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_default_playlist, container, false);
        barLayout = view.findViewById(R.id.bar_layout);
        playlistFragment = new PlaylistFragment();

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity)getActivity()).setSupportActionBar(toolbar);
        actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        setupActionBar();
        FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fragment_container, playlistFragment);
        ft.commitAllowingStateLoss();
    }
}
