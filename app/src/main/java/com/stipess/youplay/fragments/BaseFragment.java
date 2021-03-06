package com.stipess.youplay.fragments;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.stipess.youplay.MainActivity;

/**
 * A simple {@link Fragment} subclass.
 */
public abstract class BaseFragment extends Fragment {


    private static final String TAG = BaseFragment.class.getSimpleName();

    private Handler handler;

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

    void setPlayScreen()
    {
        Runnable runnable = () -> {
            Activity activity = BaseFragment.this.getActivity();
            if (activity instanceof MainActivity)
                ((MainActivity) BaseFragment.this.getActivity()).pager.setCurrentItem(0, true);
        };
        handler.postDelayed(runnable, 200);
    }

    public boolean internetConnection() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
    }

    public abstract void buildAlertDialog(int position, View view);

    public abstract void setupActionBar();

    public abstract void initAudioService();

}
