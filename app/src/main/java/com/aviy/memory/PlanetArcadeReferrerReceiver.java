package com.aviy.memory;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by FRAMGIA\luu.hoang.truc on 19/10/2016.
 */

public class PlanetArcadeReferrerReceiver extends BroadcastReceiver {
    private static final String TAG = "PlanetArcade";

    @Override
    public void onReceive(Context context, Intent intent) {
        String referrer = intent.getStringExtra("referrer");
        Log.d(TAG, "onReceive: is: " + referrer);
        if (referrer != null && referrer.equals("planetarcade")) {

        } else {

        }
    }
}
