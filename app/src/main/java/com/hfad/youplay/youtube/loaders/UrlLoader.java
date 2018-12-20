package com.hfad.youplay.youtube.loaders;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

import org.schabi.newpipe.extractor.Downloader;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.utils.Localization;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class UrlLoader extends AsyncTaskLoader<List<String>>
{
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/40.0.2214.115 Safari/537.36";
    private static final String TAG = UrlLoader.class.getSimpleName();
    private String getYoutubeLink;

    public UrlLoader(Context context, String getYoutubeLink)
    {
        super(context);
        this.getYoutubeLink = getYoutubeLink;
    }

    @Nullable
    @Override
    public List<String> loadInBackground()
    {
        NewPipe.init(new Downloader() {


            @Override
            public String download(String siteUrl, Localization localization) throws IOException, ReCaptchaException {
                Map<String, String> requestProperties = new HashMap<>();
                requestProperties.put("Accept-Language", localization.getLanguage());
                return download(siteUrl, requestProperties);
            }

            @Override
            public String download(String siteUrl, Map<String, String> customProperties) throws IOException, ReCaptchaException {
                URL url = new URL(siteUrl);
                Log.d(TAG, "URL: " + siteUrl);
                HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
                BufferedReader in = null;
                StringBuilder response = new StringBuilder();
                try {
                    con.setRequestMethod("GET");
                    con.setRequestProperty("User-Agent", USER_AGENT);

                    in = new BufferedReader(new InputStreamReader(con.getInputStream()));

                    String inputLine;
                    while((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                } catch(UnknownHostException uhe) {//thrown when there's no internet connection
                    throw new IOException("unknown host or no network", uhe);
                    //Toast.makeText(getActivity(), uhe.getMessage(), Toast.LENGTH_LONG).show();
                } catch(Exception e) {
                    /*
                     * HTTP 429 == Too Many Request
                     * Receive from Youtube.com = ReCaptcha challenge request
                     * See : https://github.com/rg3/youtube-dl/issues/5138
                     */
                    if (con.getResponseCode() == 429) {
                        throw new ReCaptchaException("reCaptcha Challenge requested");
                    }
                    throw new IOException(e);
                } finally {
                    if(in != null) {
                        in.close();
                    }
                }

                return response.toString();
            }

            @Override
            public String download(String siteUrl) throws IOException, ReCaptchaException {
                Map<String, String> requestProperties = new HashMap<>();
                return download(siteUrl, requestProperties);
            }
        }, new Localization("GB", "en"));
        List<String> data = new ArrayList<>();
        try
        {
            StreamInfo streamInfo = StreamInfo.getInfo(getYoutubeLink);

            data.add(streamInfo.getThumbnailUrl());
            Log.d(TAG, "Size: " + streamInfo.getNextVideo().getName());
            Crashlytics.setString("last_action", "downloading: " + getYoutubeLink);
            Log.d(TAG, "url" + streamInfo.getAudioStreams().size());
            for(AudioStream url : streamInfo.getAudioStreams())
            {
                if(url.getAverageBitrate() == 128)
                {
                    data.add(url.getUrl());
                    return data;
                }
            }
            data.add(streamInfo.getAudioStreams().get(0).getUrl());
            return data;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        Crashlytics.setString("last_action", "downloading failed: " + getYoutubeLink);
        Crashlytics.log("An error has occurred: " + getYoutubeLink);
        return null;
    }
}
