package com.stipess.youplay.youtube.loaders;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.loader.content.AsyncTaskLoader;

import org.apache.commons.lang3.StringEscapeUtils;
import org.json.JSONArray;

import static com.stipess.youplay.utils.Constants.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Stjepan Stjepanovic on 25.12.2017..
 * <p>
 * Copyright (C) Stjepan Stjepanovic 2017 <stipess@youplayandroid.com>
 * YoutubeMusicLoader.java is part of YouPlay.
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
public class SuggestionLoader extends AsyncTaskLoader<List<String>>
{

    private static final String TAG = SuggestionLoader.class.getSimpleName();
    private static final String USER_AGENT = "Mozilla/5.0";
    private String query;

    public SuggestionLoader(Context context, String query)
    {
        super(context);
        this.query = query;
    }

    @Nullable
    @Override
    public List<String> loadInBackground() {

        ArrayList<String> musics = new ArrayList<>();

        String formatted = "";
        try
        {
            formatted = URLEncoder.encode(query, "UTF-8");
        }
        catch (Exception e){ e.printStackTrace(); }

        try{
            URL url = new URL(SUGESS_URL_FIRST + formatted + SUGESS_URL_SECON);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod(RQST_METHOD);
            urlConnection.setRequestProperty("User-Agent", USER_AGENT);
            urlConnection.connect();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));

            String next;
            while ((next = bufferedReader.readLine()) != null) {
                JSONArray jsonArray = new JSONArray(next);
                for (int i = 0; i < jsonArray.length(); i++)
                {
                    if (jsonArray.get(i) instanceof JSONArray)
                    {
                        JSONArray array = jsonArray.getJSONArray(i);
                        for (int j = 0; j < array.length(); j++)
                        {
                            if (array.get(j) instanceof JSONArray)
                            {
                                String suggestion = ((JSONArray) array.get(j)).getString(0);
                                String encodeString = StringEscapeUtils.unescapeHtml4(suggestion);
                                musics.add(encodeString);
                                Log.d("SearchFragment", "Encoded string: " + encodeString);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return musics;
    }
}
