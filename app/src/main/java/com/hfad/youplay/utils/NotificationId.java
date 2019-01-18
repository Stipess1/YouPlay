package com.hfad.youplay.utils;

import java.util.concurrent.atomic.AtomicInteger;

public class NotificationId {
    private final static AtomicInteger atomic = new AtomicInteger(0);
    public static int getID()
    {
        return atomic.incrementAndGet();
    }
}
