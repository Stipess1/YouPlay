package com.stipess.youplay.youtube.loaders;

import android.os.AsyncTask;

import androidx.annotation.Nullable;

import com.crashlytics.android.Crashlytics;
import com.stipess.youplay.AudioService;
import com.stipess.youplay.extractor.YoutubeExtractor;
import com.stipess.youplay.music.Music;


import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.downloader.Request;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException;
import org.schabi.newpipe.extractor.localization.Localization;
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeStreamExtractor;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;


public class UrlLoader extends AsyncTask<Void,Void,List<String>>
{
    private static final String TAG = UrlLoader.class.getSimpleName();
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:43.0) Gecko/20100101 Firefox/43.0";
    private static final String DEFAULT_HTTP_ACCEPT_LANGUAGE = "en";

    private String getYoutubeLink;
    private Listener listener;
    private List<Music> musicList = new ArrayList<>();
    private List<Music> checkList = new ArrayList<>();
    private boolean relatedVideos;

    public UrlLoader(String getYoutubeLink, boolean relatedVideos)
    {
        this.getYoutubeLink = getYoutubeLink;
        this.relatedVideos = relatedVideos;
    }

    public interface Listener{
        void postExecute(List<String> list);
    }

    public List<Music> getMusicList()
    {
        return musicList;
    }

    @Override
    protected List<String> doInBackground(Void... voids) {
        List<String> data = new ArrayList<>();
        try
        {

            NewPipe.init(new Downloader() {
                @Override
                public Response execute(@Nullable Request request) throws IOException, ReCaptchaException {
                    final String httpMethod = request.httpMethod();
                    final String url = request.url();
                    final Map<String, List<String>> headers = request.headers();
                    @Nullable final byte[] dataToSend = request.dataToSend();
                    @Nullable final Localization localization = request.localization();

                    final HttpsURLConnection connection = (HttpsURLConnection) new URL(url).openConnection();

                    connection.setConnectTimeout(30 * 1000); // 30s
                    connection.setReadTimeout(30 * 1000); // 30s
                    connection.setRequestMethod(httpMethod);

                    connection.setRequestProperty("User-Agent", USER_AGENT);
                    connection.setRequestProperty("Accept-Language", DEFAULT_HTTP_ACCEPT_LANGUAGE);

                    for (Map.Entry<String, List<String>> pair : headers.entrySet()) {
                        final String headerName = pair.getKey();
                        final List<String> headerValueList = pair.getValue();

                        if (headerValueList.size() > 1) {
                            connection.setRequestProperty(headerName, null);
                            for (String headerValue : headerValueList) {
                                connection.addRequestProperty(headerName, headerValue);
                            }
                        } else if (headerValueList.size() == 1) {
                            connection.setRequestProperty(headerName, headerValueList.get(0));
                        }
                    }

                    @Nullable OutputStream outputStream = null;
                    @Nullable InputStreamReader input = null;
                    try {
                        if (dataToSend != null && dataToSend.length > 0) {
                            connection.setDoOutput(true);
                            connection.setRequestProperty("Content-Length", dataToSend.length + "");
                            outputStream = connection.getOutputStream();
                            outputStream.write(dataToSend);
                        }

                        final InputStream inputStream = connection.getInputStream();
                        final StringBuilder response = new StringBuilder();

                        // Not passing any charset for decoding here... something to keep in mind.
                        input = new InputStreamReader(inputStream);

                        int readCount;
                        char[] buffer = new char[32 * 1024];
                        while ((readCount = input.read(buffer)) != -1) {
                            response.append(buffer, 0, readCount);
                        }

                        final int responseCode = connection.getResponseCode();
                        final String responseMessage = connection.getResponseMessage();
                        final Map<String, List<String>> responseHeaders = connection.getHeaderFields();
                        final String latestUrl = connection.getURL().toString();

                        return new Response(responseCode, responseMessage, responseHeaders, response.toString(), latestUrl);
                    } catch (Exception e) {
                        /*
                         * HTTP 429 == Too Many Request
                         * Receive from Youtube.com = ReCaptcha challenge request
                         * See : https://github.com/rg3/youtube-dl/issues/5138
                         */
                        if (connection.getResponseCode() == 429) {
                            throw new ReCaptchaException("reCaptcha Challenge requested", url);
                        }

                        throw new IOException(connection.getResponseCode() + " " + connection.getResponseMessage(), e);
                    } finally {
                        if (outputStream != null) outputStream.close();
                        if (input != null) input.close();
                    }
                }
            }, new Localization("en", "GB"));
            StreamingService service = NewPipe.getService("YouTube");
//            StreamExtractor extractor1 = service.getStreamExtractor(getYoutubeLink);
            YoutubeStreamExtractor extractor1 = (YoutubeStreamExtractor) service.getStreamExtractor(getYoutubeLink);
//
            extractor1.fetchPage();
//            StreamInfo info = StreamInfo.getInfo(getYoutubeLink);
//            RelatedStreamInfo streamInfo = RelatedStreamInfo.getInfo(info);
            YoutubeExtractor extractor = new YoutubeExtractor();
            extractor.parse(getYoutubeLink);

            data.add(extractor1.getThumbnailUrl());
            data.add(extractor1.getAudioStreams().get(0).getUrl());

//            for(StreamInfoItem item : extractor1.getRelatedStreams().getStreamInfoItemList()) {
//                String title = item.getName();
//                String id = item.getUrl();
//                String duration = Utils.convertDuration(item.getDuration());
//                String author = item.getUploaderName();
//                long views = item.getViewCount();
//                String urlImg = item.getThumbnailUrl();
//                Log.d(TAG, "ID: " + id);
//                Music music = new Music();
//                music.setTitle(title);
//                music.setId(id);
//                music.setDuration(duration);
//                music.setAuthor(author);
//
//                music.setViews(Utils.convertViewsToString(views));
//                music.setUrlImage(urlImg);
//
//                for(Music pjesma : checkList)
//                {
//                    if(pjesma.getId().equals(music.getId()))
//                    {
//                        if(pjesma.getDownloaded() == 1)
//                        {
//                            music.setPath(FileManager.getMediaPath(music.getId()));
//                            music.setDownloaded(1);
//                        }
//                    }
//                }
//                if(relatedVideos)
//                    musicList.add(music);
//            }

//            Log.d(TAG, "thumbnail url: " + extractor1.getThumbnailUrl() + " URL: " + extractor1.getAudioStreams().get(0).getUrl());
            if(relatedVideos) {
                musicList.addAll(extractor.getMusicList());
            }

            Crashlytics.setString("last_action", "downloading: " + getYoutubeLink);

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

    @Override
    protected void onPostExecute(List<String> strings) {
        AudioService audioService = AudioService.getInstance();
        if(listener != null && audioService != null && !audioService.isDestroyed())
            listener.postExecute(strings);
    }

    public void setListener(Listener listener){
        this.listener = listener;
    }
}
