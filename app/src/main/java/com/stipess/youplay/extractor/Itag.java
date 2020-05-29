package com.stipess.youplay.extractor;

public class Itag {

    private int id;
    public ItagType itagType;

    public Itag(int id, ItagType type) {
        this.id = id;
        this.itagType = type;
    }

    public enum ItagType {
        AUDIO
    }

    private static final Itag[] ITAG_LIST = {
            new Itag(140, ItagType.AUDIO),
    };

    public static boolean isSupported(int itag) {
        for (Itag item : ITAG_LIST) {
            if (itag == item.id) {
                return true;
            }
        }
        return false;
    }

    public static Itag getItag(int itagId) {
        for (Itag item : ITAG_LIST) {
            if (itagId == item.id) {
                return item;
            }
        }
        return null;
    }
}
