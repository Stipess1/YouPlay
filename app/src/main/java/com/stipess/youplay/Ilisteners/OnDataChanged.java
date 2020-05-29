package com.stipess.youplay.Ilisteners;

import com.stipess.youplay.database.DatabaseHandler;
import com.stipess.youplay.music.Music;

import java.util.ArrayList;


public interface OnDataChanged
{
    void dataChanged(DatabaseHandler.UpdateType type, String databaseName, Music pjesma);

    void deleteProgress(int length, String title);

    void dataChanged(DatabaseHandler.UpdateType type, ArrayList<Music> pjesme);
}
