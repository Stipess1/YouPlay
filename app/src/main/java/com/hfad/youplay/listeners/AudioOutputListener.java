package com.hfad.youplay.listeners;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.widget.Toast;

import com.hfad.youplay.AudioService;
import com.hfad.youplay.R;

import static com.hfad.youplay.utils.Constants.PLAY_PAUSE_SONG;

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
                context.startService(newIntent);
            }
        }

    }
}
