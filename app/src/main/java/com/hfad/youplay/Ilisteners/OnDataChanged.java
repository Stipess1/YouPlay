package com.hfad.youplay.Ilisteners;

import com.hfad.youplay.database.DatabaseHandler;
import com.hfad.youplay.music.Music;

public interface OnDataChanged
{
    void dataChanged(DatabaseHandler.UpdateType type, String databaseName, Music pjesma);

    void deleteProgress(int length, String title);
}
