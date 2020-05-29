package com.stipess.youplay.radio;

import android.os.AsyncTask;

import java.util.ArrayList;

public abstract class Browser extends AsyncTask<String, String, String> {

    public enum ListType
    {
        COUNTRIES,

        STATIONS
    }

    public interface Listener {
        void postExecute(ArrayList<Country> countries);

        void preExecute();

        void getPostExecute(ArrayList<Station> stations);
    }

    public abstract void setListener(Listener listener);

}
