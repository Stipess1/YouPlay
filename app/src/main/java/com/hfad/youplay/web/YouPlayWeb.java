package com.hfad.youplay.web;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class YouPlayWeb extends AsyncTask<Void, Void, String> {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/40.0.2214.115 Safari/537.36";
    private static String LINK = "https://youplayandroid.com/version/version.json";
    private Listener listener;

    public void setListener(Listener listener)
    {
        this.listener = listener;
    }

    @Override
    protected String doInBackground(Void... voids) {
        try
        {
            URL url = new URL(LINK);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("User-Agent", USER_AGENT);
            urlConnection.connect();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));

            String next;
            while ((next = bufferedReader.readLine()) != null)
            {
                JSONObject object = new JSONObject(next);
                return object.getString("version");
            }
        }
        catch (Exception e)
        {
            listener.onError(e);
            Log.d("YouPlayWeb", e.getMessage());
            return null;
        }
        return null;
    }

    @Override
    protected void onPostExecute(String version) {
        listener.onConnected(version);
    }

    public interface Listener{
        void onConnected(String version);

        void onError(Exception e);
    }
}
