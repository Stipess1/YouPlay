package com.stipess.youplay.web;

import android.os.AsyncTask;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
/**
 * Created by Stjepan Stjepanovic
 * <p>
 * Copyright (C) Stjepan Stjepanovic 2017 <stipess@youplayandroid.com>
 * YouPlayWeb.java is part of YouPlay.
 * <p>
 * YouPlay is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * YouPlay is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with YouPlay.  If not, see <http://www.gnu.org/licenses/>.
 */
public class YouPlayWeb extends AsyncTask<Void, Void, String> {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/40.0.2214.115 Safari/537.36";
    private static String LINK = "https://youplayandroid.com/version/version.json";
    private Listener listener;
    private boolean error = false;
    private Exception e;

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
            this.e = e;
            error = true;
            return null;
        }
        return null;
    }

    @Override
    protected void onPostExecute(String version) {
        if(error)
        {
            listener.onError(e);
            error = false;
        }
        listener.onConnected(version);
    }

    public interface Listener{
        void onConnected(String version);

        void onError(Exception e);
    }
}
