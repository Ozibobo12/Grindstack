package com.example.myapp;

import android.content.Context;
import com.google.android.gms.ads.MobileAds;

public class AdManager {
    public static void init(Context context) {
        MobileAds.initialize(context, initStatus -> {});
    }
}