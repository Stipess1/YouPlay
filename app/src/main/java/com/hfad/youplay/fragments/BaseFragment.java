package com.hfad.youplay.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.View;

import com.hfad.youplay.MainActivity;

/**
 * A simple {@link Fragment} subclass.
 */
public abstract class BaseFragment extends Fragment{


    private static final String TAG = BaseFragment.class.getSimpleName();

    private Handler handler;
    private Runnable runnable;

    public BaseFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        handler = new Handler();
        setRetainInstance(true);
    }

    public void setPlayScreen()
    {
        runnable = () -> {
            Activity activity = getActivity();
            if(activity instanceof MainActivity)
                ((MainActivity)getActivity()).pager.setCurrentItem(0, true);
        };
        handler.postDelayed(runnable, 150);

    }

    public abstract void buildAlertDialog(int position, View view);

    public abstract void setupActionBar();

    public abstract void initAudioService();

}
