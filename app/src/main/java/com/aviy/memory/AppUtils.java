package com.aviy.memory;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import static android.content.ContentValues.TAG;

/**
 * Created by FRAMGIA\luu.hoang.truc on 21/10/2016.
 */

public class AppUtils {

    public static void openApp(Context context, String packageName, Bundle params) {
        PackageManager pm = context.getPackageManager();
        Intent launcherIntent = pm.getLaunchIntentForPackage(packageName);
        if (launcherIntent != null) {
            if (params != null) {
                launcherIntent.putExtras(params);
            }
            context.startActivity(launcherIntent);
        } else {
            Log.d(TAG, "openApp: Package not found");
        }
    }

    public boolean isIntalledApp (String uri, Context context) {
        PackageManager pm = context.getPackageManager();
        boolean isInstalled;
        try {
            pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
            isInstalled = true;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            isInstalled = false;
        }
        return isInstalled;
    }
}
