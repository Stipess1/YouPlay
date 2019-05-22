package com.hfad.youplay.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.hfad.youplay.music.Music;
import com.hfad.youplay.utils.Constants;
import com.hfad.youplay.utils.FileManager;
import com.hfad.youplay.utils.Order;
import com.hfad.youplay.radio.Station;
import com.hfad.youplay.utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Stjepan on 21.11.2017..
 */

public class YouPlayDatabase extends SQLiteOpenHelper
{

    public static final String TAG = YouPlayDatabase.class.getSimpleName();

    private static final String DB_NAME = "youplay";
    private static final int DB_VERSION = 1;
    private static final String TABLE_NAME = "SONGS";

    public static final String YOUPLAY_DB = "youplay.db";
    public static final String PLAYLIST_DB = "playlist_data_table.db";
    private static final String SETTINGS_DB = "settings.db";

    private static YouPlayDatabase instance;

    public YouPlayDatabase(Context context)
    {
        super(context, DB_NAME, null, DB_VERSION);
        instance = this;
    }

    public static synchronized YouPlayDatabase getInstance(Context context)
    {

        if(instance == null)
        {
            instance = new YouPlayDatabase(context.getApplicationContext());
        }
        return instance;
    }

    public static synchronized YouPlayDatabase getInstance()
    {
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
//        db.execSQL("CREATE TABLE SONGS ("
//                + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
//                + "ID TEXT, "
//                + "TITLE TEXT, "
//                + "AUTHOR TEXT, "
//                + "DURATION TEXT, "
//                + "DOWNLOADED INTEGER, "
//                + "VIEWS INTEGER);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion)
    {

    }
    /*
    Radi file playlist_data_tabe.db u database folderu, gdje stoje sve playliste koje su napravljene.
     */
    public void createPlaylistDatabase() throws SQLiteException
    {

        SQLiteDatabase db = getDatabase(PLAYLIST_DB);

        db.execSQL("CREATE TABLE IF NOT EXISTS playlistTables ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "TITLE TEXT, "
                + "ID TEXT);");

        SQLiteDatabase dbSettings = getDatabase(SETTINGS_DB);

        dbSettings.execSQL("CREATE TABLE IF NOT EXISTS settings ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "ORDER_BY TEXT, "
                + "SONG_ID TEXT); ");

        SQLiteDatabase custom = getDatabase(YOUPLAY_DB);

        custom.execSQL("CREATE TABLE IF NOT EXISTS RADIO ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "ID TEXT, "
                + "NAME TEXT, "
                + "URL TEXT, "
                + "ICON TEXT, "
                + "COUNTRY TEXT, "
                + "LANGUAGE TEXT, "
                + "BITRATE INTEGER);");

        custom.execSQL("CREATE TABLE IF NOT EXISTS SONGS ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "ID TEXT, "
                + "TITLE TEXT, "
                + "AUTHOR TEXT, "
                + "DURATION TEXT, "
                + "DOWNLOADED INTEGER, "
                + "VIEWS INTEGER);");

        SQLiteDatabase factory = this.getReadableDatabase();

        String checkQuery = "select DISTINCT tbl_name from sqlite_master where tbl_name = '"
                + TABLE_NAME + "'";
        Cursor c = factory.rawQuery(checkQuery, null);
        if(c.getCount() > 0)
        {
            String query = "SELECT * FROM SONGS";

            Cursor cursor = factory.rawQuery(query, null);
            if(cursor != null && cursor.getCount() > 0 && cursor.moveToFirst())
            {
                do {
                    String id = cursor.getString(cursor.getColumnIndex("ID"));
                    String title = cursor.getString(cursor.getColumnIndex("TITLE"));
                    String author = cursor.getString(cursor.getColumnIndex("AUTHOR"));
                    String duration = cursor.getString(cursor.getColumnIndex("DURATION"));
                    int downloaded = cursor.getInt(cursor.getColumnIndex("DOWNLOADED"));
                    String views = cursor.getString(cursor.getColumnIndex("VIEWS"));

                    Music music = new Music();
                    music.setId(id);
                    music.setTitle(title);
                    music.setAuthor(author);
                    music.setDuration(duration);
                    music.setDownloaded(downloaded);
                    music.setViews(views);
                    insertSong(custom, music);
                }while(cursor.moveToNext());

                factory.execSQL("DROP TABLE SONGS");
            }
            cursor.close();
        }

        ContentValues values = new ContentValues();
        values.put("ORDER_BY", Order.ORDER_LATEST);

        dbSettings.insert("settings", null, values);

        db.close();
        dbSettings.close();
        custom.close();
//        c.close();
        factory.close();
    }

    /**
     * Zna se desit da korisnik obriše database dok aplikacija jos radi tako da svakim put cemo
     * provjeriti dali database postoji ako ne zovemo createDatabaseIfNotExists(); da napravi
     * @param database ime baze
     * @return vrati mjesto baze
     */
    private String getPath(String database)
    {
        Log.d(TAG, "Napravi bazu folder");
        if(FileManager.getDatabaseFolder().exists())
            FileManager.getDatabaseFolder().mkdirs();

        File base = new File(Environment.getExternalStorageDirectory() + File.separator +
                Constants.APP_NAME + File.separator + Constants.DATABASE, database);


        return base.getAbsolutePath();
    }
    /*
    dodavamo u playlistTables ime playliste
     */
    private void addToPlaylistDatabase(String title)
    {
        title = title.replaceAll(" ", "_");
        SQLiteDatabase db = getDatabase(PLAYLIST_DB);
        ContentValues values = new ContentValues();
        values.put("TITLE", title);
        db.insert("playlistTables", null, values);
        db.close();
    }
    /*
    Radi playlistu sa unesenim imenom
     */
    public void createPlaylist(String tableName)
    {

        SQLiteDatabase db = getDatabase(PLAYLIST_DB);

        db.execSQL("CREATE TABLE IF NOT EXISTS [" + tableName +"] ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "ID TEXT, "
                + "TITLE TEXT, "
                + "AUTHOR TEXT, "
                + "DURATION TEXT, "
                + "DOWNLOADED INTEGER, "
                + "VIEWS INTEGER);");

        db.close();

        addToPlaylistDatabase(tableName);
    }

    public void insertRadio(Station station) throws SQLiteException
    {
        if(!radioExists(station))
        {
            ContentValues values = new ContentValues();
            values.put("ID", station.getId());
            values.put("NAME", station.getName());
            values.put("URL", station.getUrl());
            values.put("ICON", station.getIcon());
            values.put("COUNTRY", station.getCountry());
            values.put("LANGUAGE", station.getLanguage());
            values.put("BITRATE", station.getBitRate());

            SQLiteDatabase db = getDatabase(YOUPLAY_DB);

            db.insert("RADIO", null, values);
            db.close();
        }
    }

    public boolean radioExists(Station station)
    {
        SQLiteDatabase db = getDatabase(YOUPLAY_DB);
        Cursor cursor = db.query("RADIO",
                new String[] {Constants.ID},
                Constants.ID + " = ?",
                new String[] {station.getId()},
                null,null,null);

        if(cursor != null && cursor.moveToFirst())
        {
            do {
                if(cursor.getString(cursor.getColumnIndex("ID")).equals(station.getId()))
                {
                    db.close();
                    cursor.close();
                    return true;
                }

            } while(cursor.moveToNext());
            db.close();
            cursor.close();
            return false;
        }
        else
        {
            db.close();
            if(cursor != null)
                cursor.close();
            return false;
        }
    }

    public ArrayList<Station> getRadios() throws SQLiteException
    {
        SQLiteDatabase db = getDatabase(YOUPLAY_DB);
        ArrayList<Station> stations = new ArrayList<>();
        String query = "SELECT * FROM " + "RADIO" + " ORDER BY _id " + getOrder();
        Cursor data = null;
        if(db != null)
            data = db.rawQuery(query, null);

        if(data != null && data.moveToFirst())
        {
            do{
                Station station = new Station();
                station.setId(data.getString(data.getColumnIndex("ID")));
                station.setName(data.getString(data.getColumnIndex("NAME")));
                station.setIcon(data.getString(data.getColumnIndex("ICON")));
                station.setLanguage(data.getString(data.getColumnIndex("LANGUAGE")));
                station.setCountry(data.getString(data.getColumnIndex("COUNTRY")));
                station.setUrl(data.getString(data.getColumnIndex("URL")));
                station.setBitRate(data.getInt(data.getColumnIndex("BITRATE")));
                stations.add(station);

            }
            while(data.moveToNext());

        }
        data.close();
        db.close();
        return stations;
    }

    public void deleteRadio(Station station) throws SQLiteException
    {
        SQLiteDatabase db = getDatabase(YOUPLAY_DB);
        db.delete("RADIO","ID = ?", new String[]{station.getId()});
        db.close();
    }

    public void renamePlaylist(String currentTableName,String newTableName)
    {
        SQLiteDatabase db = getDatabase(PLAYLIST_DB);

        db.execSQL("ALTER TABLE " + "["+currentTableName+"]" + " RENAME TO ["+newTableName+"]");

        ContentValues values = new ContentValues();

        newTableName = newTableName.replaceAll(" ", "_");
        currentTableName = currentTableName.replaceAll(" ", "_");
        values.put("TITLE", newTableName);
        db.update("playlistTables", values, "TITLE = ?", new String[]{currentTableName});
        db.close();
    }
    /*
    Vraca bazu sa svim imenima playlistova
     */
    public SQLiteDatabase getDatabase(String database) throws SQLiteException
    {
        Crashlytics.setBool("File_exists", FileManager.getRootPath().exists());
        Crashlytics.setString("More_info", "freeSpace: " + Utils.freeSpace(true) + " Path: "
                + FileManager.getRootPath().getAbsolutePath());
        try{
            return SQLiteDatabase.openOrCreateDatabase(getPath(database), null);
        }catch (Exception e)
        {
            return null;
        }

    }

    /*
    dobavlja table sa dobivenim imenom
     */
    public ArrayList<Music> getDataTable(String table) throws SQLiteException
    {
        SQLiteDatabase db = getDatabase(PLAYLIST_DB);

        ArrayList<Music> music = new ArrayList<>();
        String query = "SELECT * FROM " + "[" + table + "]";

        Cursor data = db.rawQuery(query, null);

        if(data != null && data.moveToFirst())
        {
            do{
                Music pjesma = new Music();
                pjesma.setTitle(data.getString(data.getColumnIndex("TITLE")));
                pjesma.setId(data.getString(data.getColumnIndex("ID")));
                pjesma.setAuthor(data.getString(data.getColumnIndex("AUTHOR")));
                pjesma.setDuration(data.getString(data.getColumnIndex("DURATION")));
                pjesma.setViews(data.getString(data.getColumnIndex("VIEWS")));
                pjesma.setDownloaded(data.getInt(data.getColumnIndex("DOWNLOADED")));
                pjesma.setPath(FileManager.getMediaPath(pjesma.getId()));
                music.add(pjesma);
            }
            while(data.moveToNext());
            data.close();
            db.close();
        }

        if(data != null)
            data.close();
        db.close();
        return music;
    }

    // Postaviti order u histroy fragmentu
    public void settingsOrderBy(String orderby) throws SQLiteException
    {
        ContentValues values = new ContentValues();
        values.put("ORDER_BY", orderby);
        SQLiteDatabase db = getDatabase(SETTINGS_DB);

        db.update("settings", values, "ORDER_BY = ?", new String[]{getOrder()});
        db.close();
    }

    public String getOrder() throws SQLiteException
    {
        SQLiteDatabase db = getDatabase(SETTINGS_DB);
        String query = "SELECT ORDER_BY FROM settings";

        if(db == null) return Order.ORDER_LATEST;
        Cursor cursor = db.rawQuery(query, null);
        cursor.moveToFirst();

        if(cursor.getCount() == 0)
            settingsOrderBy(Order.ORDER_LATEST);

        String order = cursor.getString(0);

        db.close();
        cursor.close();
        return order;
    }

    /*
    Postavlja sliku na playlistu table
     */
    public String getPicTable(String table) throws SQLiteException
    {
        SQLiteDatabase db = getDatabase(PLAYLIST_DB);
        String query = "SELECT ID FROM " + "[" + table + "]";

        Cursor data = db.rawQuery(query, null);
        if(data != null && data.moveToFirst())
        {
            String pic = data.getString(0);
            db.close();
            data.close();
            return pic;
        }
        db.close();
        if(data != null)
            data.close();
        return null;
    }

    // unosi pjesme u playlist_data_table
    public void insertInTable(Music music, String table) throws SQLiteException
    {
        ContentValues songValues = new ContentValues();

        songValues.put(Constants.ID, music.getId());
        songValues.put(Constants.TITLE, music.getTitle());
        songValues.put(Constants.AUTHOR, music.getAuthor());
        songValues.put(Constants.DURATION, music.getDuration());
        songValues.put(Constants.DOWNLOADED, music.getDownloaded());
        songValues.put(Constants.VIEWS, music.getViews());

        SQLiteDatabase db = getDatabase(PLAYLIST_DB);

        db.insert("["+table+"]", null, songValues);
        db.close();
    }

    public void deleteTableMusic(String table, int position) throws SQLiteException
    {

        SQLiteDatabase db = getDatabase(PLAYLIST_DB);

        String query = "SELECT _id FROM " + "[" +table +"]";
        Cursor cursor = db.rawQuery(query, null);
        cursor.moveToPosition(position);

        db.delete("[" +table +"]","_id = ?", new String[] {cursor.getString(0)});

        db.close();
        cursor.close();
    }


    public List<String> getAllPlaylists() throws SQLiteException
    {
        List<String> list = new ArrayList<>();
        Cursor data = null;
        SQLiteDatabase db = getDatabase(PLAYLIST_DB);
        String query = "SELECT TITLE FROM playlistTables";
        if(db != null)
            data = db.rawQuery(query, null);

        if(data != null && data.moveToFirst())
        {
            do {
                String replace = data.getString(data.getColumnIndex("TITLE"));
                list.add(replace.replaceAll("_", " "));
            }
            while(data.moveToNext());
        }
        data.close();
        db.close();
        return list;
    }

    public void deletePlaylistTable(String title, int position) throws SQLiteException
    {
        SQLiteDatabase base = getDatabase(PLAYLIST_DB);
        Log.d(TAG, "TABLE: " + title);
        base.execSQL("DROP TABLE " + "[" +title +"]");

        Cursor cursor = base.rawQuery("SELECT TITLE FROM playlistTables", null);
        cursor.moveToPosition(position);

        base.delete("playlistTables", "TITLE = ?", new String[] {cursor.getString(0)});

        cursor.close();
        base.close();
    }


    public boolean tableExists(String title) throws SQLiteException
    {
        SQLiteDatabase base = getDatabase(PLAYLIST_DB);
        String query = "SELECT TITLE FROM playlistTables";

        Cursor cursor = base.rawQuery(query, null);
        if(cursor != null && cursor.moveToFirst())
        {
            do
            {
                if(cursor.getString(0).toLowerCase().equals(title.replaceAll(" ", "_").toLowerCase()))
                {
                    cursor.close();
                    base.close();
                    return true;
                }
            }
            while (cursor.moveToNext());
        }
        if(cursor != null)
            cursor.close();
        base.close();

        return false;
    }

    public static void insertSong(SQLiteDatabase db, Music music) throws SQLiteException
    {
        ContentValues songValues = new ContentValues();

        songValues.put(Constants.ID, music.getId());
        songValues.put(Constants.TITLE, music.getTitle());
        songValues.put(Constants.AUTHOR, music.getAuthor());
        songValues.put(Constants.DURATION, music.getDuration());
        songValues.put(Constants.DOWNLOADED, music.getDownloaded());
        songValues.put(Constants.VIEWS, music.getViews());

        db.insert(TABLE_NAME, null, songValues);

    }

    public void updateSong(String key, Music pjesma)
    {
        SQLiteDatabase db = getDatabase(YOUPLAY_DB);
        ContentValues songValue = new ContentValues();

        songValue.put(key, 1);
        db.update(TABLE_NAME, songValue, "ID = ?", new String[]{pjesma.getId()});
        db.close();
    }

    public ArrayList<Music> getData() throws SQLiteException
    {
        SQLiteDatabase db = getDatabase(YOUPLAY_DB);
        ArrayList<Music> music = new ArrayList<>();
        String query = "SELECT * FROM " + TABLE_NAME + " ORDER BY _id " + getOrder();
        Cursor data = null;
        if(db != null)
            data = db.rawQuery(query, null);

           if(data != null && data.moveToFirst())
           {
               do{
                   Music pjesma = new Music();
                   pjesma.setTitle(data.getString(data.getColumnIndex("TITLE")));
                   pjesma.setAuthor(data.getString(data.getColumnIndex("AUTHOR")));
                   pjesma.setId(data.getString(data.getColumnIndex("ID")));
                   pjesma.setViews(data.getString(data.getColumnIndex("VIEWS")));
                   pjesma.setDownloaded(data.getInt(data.getColumnIndex("DOWNLOADED")));
                   pjesma.setPath(FileManager.getMediaPath(pjesma.getId()));

                   /*
                   Posto sam bio prepametan i spremljo duzinu pjesme u string jer youtube tako prikazuje
                   onda cemo ovako oduzet jednu sekund i stavit u listu.
                    */
                   String split = data.getString(data.getColumnIndex("DURATION"));
                   String [] formatted = split.split(":");
                   int convert = Integer.parseInt(formatted[1]);
                   int first = Integer.parseInt(formatted[0]);
                   convert = convert - 1;
                   if(convert < 0)
                   {
                       convert = 59;
                       first = first - 1;
                   }
                   String duration;
                   if(convert < 10)
                       duration = Integer.toString(first) +":0"+ Integer.toString(convert);
                   else
                       duration = Integer.toString(first) +":"+ Integer.toString(convert);

                   pjesma.setDuration(duration);
                   music.add(pjesma);
               }
               while(data.moveToNext());

           }
           if(data != null)
                data.close();

        db.close();
        return music;
    }

    public boolean isDownloaded(String id) throws SQLiteException
    {
        File databaseFile = new File(Environment.getExternalStorageDirectory() +File.separator +
                Constants.APP_NAME + File.separator + Constants.DATABASE, "youplay.db");

        SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(databaseFile.getPath(), null);

        Cursor cursor = db.query(Constants.TABLE_NAME,
                new String[] {Constants.DOWNLOADED},
                Constants.ID + " = ?",
                new String[] {id},
                null, null, null);
        if(cursor != null && cursor.moveToFirst())
        {
            do
            {
                if(cursor.getInt(0) == 1)
                {
                    db.close();
                    cursor.close();
                    return true;
                }
            }
            while (cursor.moveToNext());
            db.close();
            cursor.close();
            return false;
        }
        db.close();
        cursor.close();
        return false;
    }

    public int getIdOrder(String id, String table) throws SQLiteException
    {
        SQLiteDatabase db;
        if(table.equals(Constants.TABLE_NAME))
        {
            File databaseFile = new File(Environment.getExternalStorageDirectory() +File.separator +
                    Constants.APP_NAME + File.separator + Constants.DATABASE, "youplay.db");

            db = SQLiteDatabase.openOrCreateDatabase(databaseFile.getPath(), null);
        }
        else
            db = getDatabase(PLAYLIST_DB);

        String query = "SELECT _id, ID FROM "+table;
        Cursor cursor = db.rawQuery(query, null);

        if(cursor != null && cursor.moveToFirst())
        {
            do {
                if(cursor.getString(1).equals(id))
                {
                    String pos = cursor.getString(0);
                    cursor.close();

                    return Integer.parseInt(pos);
                }

            }while(cursor.moveToNext());
        }
        if(cursor != null)
            cursor.close();

        return -1;
    }

    public boolean ifItemExists(String id) throws SQLiteException
    {
        SQLiteDatabase db = getDatabase(YOUPLAY_DB);
        Cursor cursor = db.query(TABLE_NAME,
                new String[] {Constants.ID},
                Constants.ID + " = ?",
                new String[] {id},
                null,null,null);

        if(cursor != null && cursor.moveToFirst())
        {
            do {
                if(cursor.getString(0).equals(id))
                {
                    db.close();
                    cursor.close();
                    return true;
                }

            } while(cursor.moveToNext());
            db.close();
            cursor.close();
            return false;
        }
        else
        {
            db.close();
            cursor.close();
            return false;
        }
    }

    public void deleteData(String id) throws SQLiteException
    {
        SQLiteDatabase db = getDatabase(YOUPLAY_DB);

        File file = new File(Environment.getExternalStorageDirectory() + File.separator + Constants.APP_NAME, id + Constants.FORMAT);
        if(file.exists())
        {
            file.delete();
            File newfile = new File(Environment.getExternalStorageDirectory() + File.separator + Constants.APP_NAME + File.separator +
                    Constants.NO_MEDIA, id+".jpg");
            newfile.delete();
        }

        db.delete(TABLE_NAME,"ID = ?", new String[] {id});
        db.close();
    }

}
