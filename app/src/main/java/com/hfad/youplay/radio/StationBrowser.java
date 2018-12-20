package com.hfad.youplay.radio;


import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class StationBrowser extends Browser
{

    private static final String HOST = "http://www.radio-browser.info/webservice";
    private static final String STATIONS = "/json/stations/bycountry/";

    private ListType type;
    private String country;
    private ArrayList<Station> stations = new ArrayList<>();
    private Listener listener;

    public StationBrowser(ListType type, String country)
    {
        this.type = type;
        this.country = country;
    }

    @Override
    protected void onPreExecute() {
        if(listener != null)
            listener.preExecute();
    }

    public String getCountry() {
        return country;
    }

    @Override
    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    protected String doInBackground(String... strings) {
        if(type == ListType.STATIONS)
        {
            try
            {
                URL url = new URL(HOST+STATIONS+country);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("User-Agent", "YouPlay");
                connection.connect();

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                String next;
                while ((next = reader.readLine()) != null)
                {
                    JSONArray array = new JSONArray(next);
                    for(int i = 0; i < array.length(); i++)
                    {
                        JSONObject object = array.getJSONObject(i);
                        if(object.getInt("lastcheckok") == 1)
                        {
                            String id = object.getString("id");
                            String name = object.getString("name");
                            String stream = object.getString("url");
                            String countryName = object.getString("country");
                            String language = object.getString("language");
                            String icon = object.getString("favicon");
                            int bitRate = object.getInt("bitrate");

                            Station station = new Station();
                            station.setId(id);
                            station.setName(name);
                            station.setUrl(stream);
                            station.setCountry(countryName);
                            station.setLanguage(language);
                            station.setBitRate(bitRate);
                            station.setIcon(icon);

                            stations.add(station);
                        }
                    }
                }
                reader.close();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(String s) {
        if(listener != null)
            listener.getPostExecute(stations);
    }

}
