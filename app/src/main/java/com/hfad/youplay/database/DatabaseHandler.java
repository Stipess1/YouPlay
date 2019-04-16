package com.hfad.youplay.database;

import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;

import com.hfad.youplay.Ilisteners.OnDataChanged;
import com.hfad.youplay.music.Music;

import java.util.ArrayList;
import java.util.List;

import static com.hfad.youplay.utils.Constants.DOWNLOADED;

public class DatabaseHandler extends AsyncTask<Boolean, Object, Void>
{

    private String tableName;
    private String databaseName;
    private UpdateType type;
    private YouPlayDatabase db;
    private Music pjesma;
    private List<Music> pjesme;
    private OnDataChanged onDataChanged;

    /**
     * svaki put kad zovemo funkciju iz @YouPlayDatabase klase treba proci kroz ovu funkciju
     * tako da se nedesi nikakv lag tokom prisupanje baze.
     * @param booleans /
     * @return void
     */
    @Override
    protected Void doInBackground(Boolean... booleans) {

        SQLiteDatabase database = null;
        if(databaseName.equals(YouPlayDatabase.PLAYLIST_DB))
        {
            if(type == UpdateType.GET)
            {
                pjesme.addAll(db.getDataTable(tableName));
            }
            else
            {
                database = db.getDatabase(YouPlayDatabase.PLAYLIST_DB);
                if(database != null)
                for(Music pjesma : pjesme)
                    db.insertInTable(pjesma, tableName);
            }
        }
        else
        {
            database = db.getDatabase(YouPlayDatabase.YOUPLAY_DB);
            if(type == UpdateType.ADD)
            {
                if(!db.ifItemExists(pjesma.getId()) && database != null)
                    YouPlayDatabase.insertSong(database, pjesma);
                else if(pjesma.getDownloaded() == 1)
                    db.updateSong(DOWNLOADED, pjesma);
            }
            else if(type == UpdateType.REMOVE)
            {
                db.deleteData(pjesma.getId());
            }
            else if(type == UpdateType.REMOVE_LIST)
            {
                for(int i = 0; i < pjesme.size(); i++)
                {
                    Log.d("DatabaseHandler", "Deleting " + pjesme.get(i).getTitle());
                    db.deleteData(pjesme.get(i).getId());
                    publishProgress(i, pjesme.get(i).getTitle());
                }
            }
            else if(type == UpdateType.GET)
            {
                pjesme.addAll(db.getData());
            }
        }
        if(database != null)
            database.close();
        return null;
    }

    @Override
    protected void onProgressUpdate(Object... values)
    {
        if(onDataChanged != null)
            onDataChanged.deleteProgress((int) values[0], (String) values[1]);
    }

    @Override
    protected void onPostExecute(Void aVoid)
    {
        if(isCancelled()) return;

        if(onDataChanged != null && type != UpdateType.GET)
            onDataChanged.dataChanged(type, databaseName, pjesma);
        else if(onDataChanged != null)
            onDataChanged.dataChanged(type, pjesme);
//        else if(onDataChanged != null)
//            onDataChanged.dataChanged(type, pjesme);
    }

    @Override
    protected void onPreExecute() {

    }

    public enum UpdateType
    {
        ADD,
        // Dohvati sve pjesme iz SQL
        GET,

        REMOVE,

        REMOVE_LIST
    }

    public DatabaseHandler setDataChangedListener(OnDataChanged onDataChanged)
    {
        this.onDataChanged = onDataChanged;
        return this;
    }

    public DatabaseHandler(List<Music> pjesme, String tableName, String databaseName, UpdateType type)
    {
        this.tableName = tableName;
        this.databaseName = databaseName;
        this.type = type;
        this.pjesme = pjesme;

        db = YouPlayDatabase.getInstance();

    }

    // Koristiti kada dohvacamo pjesme
    public DatabaseHandler(UpdateType type, OnDataChanged dataChanged, String databaseName, String tableName)
    {
        this.type = type;
        this.databaseName = databaseName;
        this.tableName = tableName;
        this.pjesme = new ArrayList<>();
        this.onDataChanged = dataChanged;
        db = YouPlayDatabase.getInstance();
    }

    public DatabaseHandler(Music pjesma, String tableName, String databaseName, UpdateType type)
    {
        this.tableName = tableName;
        this.databaseName = databaseName;
        this.pjesma = pjesma;
        this.type = type;

        db = YouPlayDatabase.getInstance();
    }
}
