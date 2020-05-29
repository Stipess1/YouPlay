package com.stipess.youplay.extractor;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {

    public static String matchGroup(String pattern, String input, int group) throws Exception {
        Pattern pat = Pattern.compile(pattern);
        Matcher mat = pat.matcher(input);
        boolean foundMatch = mat.find();
        if (foundMatch) {
            return mat.group(group);
        } else {
            throw new Exception("failed to find pattern \"" + pattern);
        }
    }

    public static Map<String, String> compatParseMap(final String input) throws UnsupportedEncodingException {
        Map<String, String> map = new HashMap<>();
        for (String arg : input.split("&")) {
            String[] splitArg = arg.split("=");
            if (splitArg.length > 1) {
                map.put(splitArg[0], URLDecoder.decode(splitArg[1], "UTF-8"));
            } else {
                map.put(splitArg[0], "");
            }
        }
        return map;
    }

    public static boolean isMatch(String pattern, String input) {
        Pattern pat = Pattern.compile(pattern);
        Matcher mat = pat.matcher(input);
        return mat.find();
    }
}
