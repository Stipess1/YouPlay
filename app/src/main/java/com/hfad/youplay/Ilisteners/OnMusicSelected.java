package com.hfad.youplay.Ilisteners;

import android.view.View;

import com.hfad.youplay.music.Music;

import java.util.List;

/**
 * Created by Stjepan on 8.12.2017..
 */

public interface OnMusicSelected {

    void onClick(Music pjesma, View view);

    void onLongClick(Music pjesma, View v);

    void onInfoClicked(int position, View v);

    void onShuffle();
}
