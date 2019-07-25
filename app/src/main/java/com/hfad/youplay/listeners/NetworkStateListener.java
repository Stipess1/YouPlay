package com.hfad.youplay.listeners;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Build;

import com.hfad.youplay.AudioService;
import com.hfad.youplay.utils.Constants;

/**
 * Created by Stjepan on 1.2.2018..
 */

public class NetworkStateListener extends BroadcastReceiver
{

    @Override
    public void onReceive(Context context, Intent intent) {

        if(intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION))
        {
            Intent newIntent = new Intent(context, AudioService.class);
            newIntent.putExtra(AudioService.ACTION, Constants.ADS);
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                context.startForegroundService(newIntent);
            else
                context.startService(newIntent);
        }
    }
}
