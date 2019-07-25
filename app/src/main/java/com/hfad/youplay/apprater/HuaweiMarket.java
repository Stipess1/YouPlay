package com.hfad.youplay.apprater;

import android.content.Context;
import android.net.Uri;

public class HuaweiMarket extends Market {
    private static String marketLink = "market://details?id=";

    @Override
    protected Uri getMarketURI(Context context) {
        return Uri.parse(marketLink + Market.getPackageName(context));
    }
}
