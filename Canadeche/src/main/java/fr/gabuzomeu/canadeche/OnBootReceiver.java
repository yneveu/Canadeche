package fr.gabuzomeu.canadeche;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

/**
 * Created by yann on 31/07/13.
 */
public class OnBootReceiver extends BroadcastReceiver {

    public static final String TAG = "CanadecheServiceBoot";

    @Override
    public void onReceive(Context context, Intent intent) {
        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
            Log.d("TestApp", "Got the Boot Event>>>");

        }


        Log.d(TAG, "Starting MySimpleService >>> ");
                context.startService(new Intent().setComponent(new ComponentName(
                        context.getPackageName(), CanadecheService.class.getName())));
    }
}


