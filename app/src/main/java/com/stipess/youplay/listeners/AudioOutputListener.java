package com.stipess.youplay.listeners;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.widget.Toast;

import com.stipess.youplay.AudioService;
import com.stipess.youplay.R;

import static com.stipess.youplay.utils.Constants.PLAY_PAUSE_SONG;

/**
 * Created by Stjepan on 1.1.2018..
 */

public class AudioOutputListener extends BroadcastReceiver{

    @Override
    public void onReceive(Context context, Intent intent) {

        if(intent != null)
        {
            if(intent.getAction().equals(AudioManager.ACTION_AUDIO_BECOMING_NOISY) && AudioService.getInstance().getAudioPlayer().getPlayWhenReady())
            {
                Toast.makeText(context, R.string.headphones, Toast.LENGTH_SHORT).show();
                Intent newIntent = new Intent(context, AudioService.class);
                newIntent.putExtra(AudioService.ACTION, PLAY_PAUSE_SONG);
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    context.startForegroundService(newIntent);
                else
                    context.startService(newIntent);
            }
        }

    }
}
