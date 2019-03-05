package com.hfad.youplay.Ilisteners;

import com.hfad.youplay.music.Music;
import com.hfad.youplay.radio.Station;

import java.util.ArrayList;
import java.util.List;

public interface OnItemClicked {

    // zove onClick u history fragmentu.
    void onMusicClick(Music pjesma);

    void onMusicClick(Music pjesma, List<Music> pjesme, String table, boolean shuffled);

    void refresh(Music pjesma);

    void setStation(Station station);

    void setMusic(Music pjesma);

    void refreshSuggestions(List<Music> data, boolean queue);

    void refreshPlaylist();

    void stream(Station station, ArrayList<Station> stations);

    void refreshSearchList(Music pjesma);

    void pauseSong();

    void refreshSpinnerAdapter();
}
