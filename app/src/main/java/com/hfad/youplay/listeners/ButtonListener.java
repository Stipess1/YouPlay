package com.hfad.youplay.listeners;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.hfad.youplay.AudioService;
import static com.hfad.youplay.utils.Constants.*;


/**
 * Created by Stjepan on 24.12.2017..
 */

public class ButtonListener extends BroadcastReceiver {

    public static final String BUTTON = "button";

    public ButtonListener()
    {

    }

    @Override
    public void onReceive(Context context, Intent intent) {

        switch (intent.getStringExtra(BUTTON))
        {
            case PLAY_PAUSE:
                Intent newIntent = new Intent(context, AudioService.class);
                newIntent.putExtra(AudioService.ACTION, 1);
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    context.startForegroundService(newIntent);
                else
                    context.startService(newIntent);
                break;
            case NEXT:
                Intent nextIntent = new Intent(context, AudioService.class);
                nextIntent.putExtra(AudioService.ACTION, 2);
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    context.startForegroundService(nextIntent);
                else
                    context.startService(nextIntent);
                break;
            case PREVIOUS:
                Intent prevIntent = new Intent(context, AudioService.class);
                prevIntent.putExtra(AudioService.ACTION, 3);
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    context.startForegroundService(prevIntent);
                else
                    context.startService(prevIntent);
                break;
            case EXIT:
                Intent cancelIntent = new Intent(context, AudioService.class);
                cancelIntent.putExtra(AudioService.ACTION, 4);
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    context.startForegroundService(cancelIntent);
                else
                    context.startService(cancelIntent);
                break;
        }

    }
}
