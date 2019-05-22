package com.hfad.youplay.listeners;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.media.session.MediaButtonReceiver;
import android.view.KeyEvent;

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
                context.startService(newIntent);
                break;
            case NEXT:
                Intent nextIntent = new Intent(context, AudioService.class);
                nextIntent.putExtra(AudioService.ACTION, 2);
                context.startService(nextIntent);
                break;
            case PREVIOUS:
                Intent prevIntent = new Intent(context, AudioService.class);
                prevIntent.putExtra(AudioService.ACTION, 3);
                context.startService(prevIntent);
                break;
            case EXIT:
                Intent cancelIntent = new Intent(context, AudioService.class);
                cancelIntent.putExtra(AudioService.ACTION, 4);
                context.startService(cancelIntent);
                break;
        }

    }
}
