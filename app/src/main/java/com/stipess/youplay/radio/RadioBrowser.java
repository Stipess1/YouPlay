package com.stipess.youplay.radio;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class RadioBrowser extends Browser
{
    private final String TAG = RadioBrowser.class.getSimpleName();

    private static final String HOST = "http://www.radio-browser.info/webservice";
    private static final String COUNTRIES = "/json/countries";

    private ArrayList<Country> countries = new ArrayList<>();
    private Listener listener;
    private ListType type;

    public RadioBrowser(ListType type)
    {
        this.type = type;
    }

    @Override
    protected void onPreExecute()
    {
        if(listener != null)
            listener.preExecute();
    }

    @Override
    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    protected String doInBackground(String... strings)
    {
        if(type == ListType.COUNTRIES)
        {
            BufferedReader reader = null;
            try
            {
                URL url = new URL(HOST+COUNTRIES);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("User-Agent", "YouPlay");
                connection.connect();

                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                String next;
                while ((next = reader.readLine()) != null)
                {
                    JSONArray array = new JSONArray(next);
                    for(int i = 0; i < array.length(); i++)
                    {
                        JSONObject object = array.getJSONObject(i);

                        String name = object.getString("name");
                        if(!name.equals("Tibet")) {
                            int stationCount = object.getInt("stationcount");
                            Country country = new Country();

                            country.setName(name);
                            country.setStationCount(stationCount);
                            countries.add(country);
                        }
                    }
                }
                reader.close();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            finally
            {
                try
                {
                    reader.close();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(String s)
    {
        if(type == ListType.COUNTRIES && listener != null)
            listener.postExecute(countries);
    }

}
