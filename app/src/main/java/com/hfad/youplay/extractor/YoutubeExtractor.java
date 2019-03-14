package com.hfad.youplay.extractor;

import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ScriptableObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class YoutubeExtractor {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/40.0.2214.115 Safari/537.36";
    private  static String DECRYPTION_SIGNATURE_FUNCTION_REGEX =
            "(\\w+)\\s*=\\s*function\\((\\w+)\\)\\{\\s*\\2=\\s*\\2\\.split\\(\"\"\\)\\s*;";
    private  static String DECRYPTION_AKAMAIZED_STRING_REGEX =
            "yt\\.akamaized\\.net/\\)\\s*\\|\\|\\s*.*?\\s*c\\s*&&\\s*d\\.set\\([^,]+\\s*,\\s*(:encodeURIComponent\\s*\\()([a-zA-Z0-9$]+)\\(";
    private static String DECRYPTION_AKAMAIZED_SHORT_STRING_REGEX =
            "\\bc\\s*&&\\s*d\\.set\\([^,]+\\s*,\\s*(:encodeURIComponent\\s*\\()([a-zA-Z0-9$]+)\\(";

    private String decryptionCode = "";

    private String url = "https://www.youtube.com/watch?v=B6L-dViA4Vc";
    private String id = "B6L-dViA4Vc";
    private String decryptUrl = "https://youplayandroid.com/decrypt/decrypt.json";

    private Document document;
    private JsonObject playerArgs;
    private final Map<String, String> videoInfoPage = new HashMap<>();

    public void parse(String url)
    {
        this.url = url;
        id = url.substring(32);
        Connection connect = Jsoup.connect(url);
        try{

            URL url1 = new URL(decryptUrl);
            HttpURLConnection urlConnection = (HttpURLConnection) url1.openConnection();
            urlConnection.setRequestProperty("User-Agent", USER_AGENT);
            urlConnection.connect();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));

            String next;
            while ((next = bufferedReader.readLine()) != null)
            {
                JsonObject object = JsonParser.object().from(next);
                DECRYPTION_SIGNATURE_FUNCTION_REGEX = object.getString("DECRYPTION_SIGNATURE_FUNCTION_REGEX");
                DECRYPTION_AKAMAIZED_SHORT_STRING_REGEX = object.getString("DECRYPTION_AKAMAIZED_SHORT_STRING_REGEX");
                DECRYPTION_AKAMAIZED_STRING_REGEX = object.getString("DECRYPTION_AKAMAIZED_STRING_REGEX");
            }

            String pageContent = connect.get().html();
            document = Jsoup.parse(pageContent);

            String playerUrl;
            if(pageContent.contains("<meta property=\"og:restrictions:age"))
            {
                EmbeddedInfo info = getImbeddedInfo();
                String audioInfo = getAudioInfoUrl(id, info.sts);
                String infoPageResponse = Jsoup.connect(audioInfo).get().html();
                videoInfoPage.putAll(Parser.compatParseMap(infoPageResponse));
                playerUrl = info.url;
                //Age restricted
            }
            else
            {
                JsonObject playerConfig = getPlayerConfig(pageContent);
                playerArgs = getPlayerArgs(playerConfig);
                playerUrl = getPlayerUrl(playerConfig);
            }



            if(decryptionCode.isEmpty())
                decryptionCode = decryptionCodeLoad(playerUrl);

        }
        catch (Exception e)
        {
            System.out.println(e);
        }

    }

    private Map<String, Itag> getItags() {
        Map<String, Itag> urlAndItags = new LinkedHashMap<>();
        String encodedUrlMap = "";
        if (playerArgs != null && playerArgs.isString("adaptive_fmts")) {
            encodedUrlMap = playerArgs.getString("adaptive_fmts", "");
        } else if (videoInfoPage.containsKey("adaptive_fmts")) {
            encodedUrlMap = videoInfoPage.get("adaptive_fmts");
        }
        for (String url_data_str : encodedUrlMap.split(",")) {
            try {
                Map<String, String> tags = Parser.compatParseMap(
                        org.jsoup.parser.Parser.unescapeEntities(url_data_str, true));

                int itag = Integer.parseInt(tags.get("itag"));

                if (Itag.isSupported(itag)) {
                    Itag itagItem = Itag.getItag(itag);
                    if (itagItem.itagType == Itag.ItagType.AUDIO) {
                        String streamUrl = tags.get("url");
                        if (tags.get("s") != null) {
                            streamUrl = streamUrl + "&signature=" + decryptSignature(tags.get("s"), decryptionCode);
                        }
                        urlAndItags.put(streamUrl, itagItem);
                    }
                }
            }
            catch (Exception ignored) {
            }
        }

        return urlAndItags;
    }

    public Audio getAudio() throws IOException {
        Audio audio = null;
        try {
            for (Map.Entry<String, Itag> entry : getItags().entrySet()) {

                url = entry.getKey();
                audio = new Audio(url);

            }
        } catch (Exception ignored) {

        }

        return audio;
    }

    public String getThumbnailUrl()
    {
        try{
            return document.select("link[itemprop=\"thumbnailUrl\"]").first().absUrl("href");
        } catch (Exception ignored)
        {

        }

        if(playerArgs != null && playerArgs.isString("thumbnail_url"))
            return playerArgs.getString("thumbnail_url");

        try
        {
            return videoInfoPage.get("thumbnail_url");
        }catch (Exception ignored)
        {

        }
        return null;
    }

    private String decryptSignature(String encryptedSig, String decryptionCode) {
        Context context = Context.enter();
        context.setOptimizationLevel(-1);
        Object result = null;
        try {
            ScriptableObject scope = context.initStandardObjects();
            context.evaluateString(scope, decryptionCode, "decryptionCode", 1, null);
            Function decryptionFunc = (Function) scope.get("decrypt", scope);
            result = decryptionFunc.call(context, scope, scope, new Object[]{encryptedSig});
        } catch (Exception ignored) {

        } finally {
            Context.exit();
        }
        return result == null ? "" : result.toString();
    }

    private String decryptionCodeLoad(String playerUrl) {
        try{
            if (!playerUrl.contains("https://youtube.com")) {
                playerUrl = "https://youtube.com" + playerUrl;
            }

            String player = download(playerUrl);

            String decryptionFunctionName;

            if(Parser.isMatch(DECRYPTION_AKAMAIZED_SHORT_STRING_REGEX, player))
                decryptionFunctionName = Parser.matchGroup(DECRYPTION_AKAMAIZED_SHORT_STRING_REGEX, player, 1);
            else if(Parser.isMatch(DECRYPTION_AKAMAIZED_STRING_REGEX, player))
                decryptionFunctionName = Parser.matchGroup(DECRYPTION_AKAMAIZED_SHORT_STRING_REGEX, player, 1);
            else
                decryptionFunctionName = Parser.matchGroup(DECRYPTION_SIGNATURE_FUNCTION_REGEX, player, 1);

            final String funPattern = "("
                    + decryptionFunctionName.replace("$", "\\$")
                    + "=function\\([a-zA-Z0-9_]+\\)\\{.+?\\})";
            final String decryptFunction = "var " + Parser.matchGroup(funPattern, player,1) + ";";

            final String objectName =
                    Parser.matchGroup(";([A-Za-z0-9_\\$]{2})\\...\\(", decryptFunction,1);
            final String helperPattern =
                    "(var " + objectName.replace("$", "\\$") + "=\\{.+?\\}\\};)";
            final String helperObject =
                    Parser.matchGroup(helperPattern, player.replace("\n", ""),1);

            final String callerFunction =
                    "function " + "decrypt" + "(a){return " + decryptionFunctionName + "(a);}";

            return helperObject + decryptFunction + callerFunction;
        }catch (Exception ignore)
        {

        }
        return null;
    }

    private String download(String siteUrl) throws IOException
    {
        URL url = new URL(siteUrl);
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
        } catch(Exception e) {
            throw new IOException(e);
        } finally {
            if(in != null) {
                in.close();
            }
        }

        return response.toString();
    }

    private String getPlayerUrl(JsonObject playerConfig) {
        try {
            String playerUrl;

            JsonObject ytAssets = playerConfig.getObject("assets");
            playerUrl = ytAssets.getString("js");

            if (playerUrl.startsWith("//")) {
                playerUrl = "https:" + playerUrl;
            }
            return playerUrl;
        } catch (Exception ignore) {

        }
        return null;
    }

    private JsonObject getPlayerArgs(JsonObject playerConfig) {
        JsonObject playerArgs = null;

        try {
            playerArgs = playerConfig.getObject("args");
        } catch (Exception ignore) {

        }

        return playerArgs;
    }

    private JsonObject getPlayerConfig(String pageContent) throws Exception {
        try
        {
            String playerConfig = Parser.matchGroup("ytplayer.config\\s*=\\s*(\\{.*?\\});", pageContent, 1);
            return JsonParser.object().from(playerConfig);
        }catch (Exception ignored){

        }
        return null;
    }

    private EmbeddedInfo getImbeddedInfo()
    {
        try{
            String embedUrl = "https://www.youtube.com/embed/" + id;
            String pageContent = Jsoup.connect(embedUrl).get().html();

            final String assetsPattern = "\"assets\":.+?\"js\":\\s*(\"[^\"]+\")";
            String playerUrl = Parser.matchGroup(assetsPattern, pageContent, 1).replace("\\", "").replace("\"", "");
            if(playerUrl.startsWith("//"))
            {
                playerUrl = "https:" + playerUrl;
            }

            String stsPattern = "\"sts\"\\s*:\\s*(\\d+)";
            String sts = Parser.matchGroup(stsPattern, pageContent, 1);
            return new EmbeddedInfo(playerUrl, sts);

        }catch (Exception ignored)
        {

        }
        return null;
    }

    private String getAudioInfoUrl(String id, String sts)
    {
        return "https://www.youtube.com/get_video_info?" + "video_id=" + id +
                "&eurl=https://youtube.googleapis.com/v/" + id +
                "&sts=" + sts + "&ps=default&gl=US&hl=en";
    }

    private class EmbeddedInfo {
        final String url;
        final String sts;

        EmbeddedInfo(final String url, final String sts) {
            this.url = url;
            this.sts = sts;
        }
    }
}

