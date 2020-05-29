package com.stipess.youplay.Ilisteners;

import com.stipess.youplay.music.Music;
import com.stipess.youplay.radio.Station;

import java.util.ArrayList;

public interface OnItemClicked {

    // zove onClick u history fragmentu.
    void onMusicClick(Music pjesma);

    void onMusicClick(Music pjesma, ArrayList<Music> pjesme, String table, boolean shuffled);

    void refresh(Music pjesma);

    void setStation(Station station);

    void setMusic(Music pjesma);

    void refreshSuggestions(ArrayList<Music> data, boolean queue);

    void refreshPlaylist();

    void stream(Station station, ArrayList<Station> stations);

    void refreshSearchList(Music pjesma);

    void refreshSpinnerAdapter();
}
