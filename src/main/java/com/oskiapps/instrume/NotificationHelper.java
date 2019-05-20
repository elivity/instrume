package com.oskiapps.instrume;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import static android.content.Context.NOTIFICATION_SERVICE;

/**
 * Created by Oskar on 22.04.2018.
 */

public class NotificationHelper {
    Context ctx;
    NotificationManager notificationManager;
    NotificationManagerCompat notificationManagerComp;

    NotificationCompat.Builder mBuilder;
    public NotificationHelper(Context gotCtx) {
        ctx = gotCtx;
        setupNotification();
    }

    private void setupNotification() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel
            CharSequence name = ctx.getString(R.string.channel_name);
            String description = ctx.getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel mChannel = new NotificationChannel("conv_wav", name, importance);
            mChannel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            notificationManager = (NotificationManager) ctx.getSystemService(
                   NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(mChannel);

            mBuilder = new NotificationCompat.Builder(ctx, "conv_wav")
                    .setOngoing(true)
                    .setProgress(100,0,false)
                    .setOnlyAlertOnce(true)
                    .setSmallIcon(R.drawable.ic_sharebtntry1)
                    .setContentTitle("Converting")
                    .setContentText("Converting your rap to MP3")
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText("Saving to \\RapOnMP3\\RapOnMP3-Exported"))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);


            notificationManager.notify(1337, mBuilder.build());
        } else {
            notificationManager =  (NotificationManager) ctx.getSystemService(
                    NOTIFICATION_SERVICE);
            mBuilder = new NotificationCompat.Builder(ctx, "conv_wav")
                    .setOngoing(true)
                    .setProgress(100,0,false)
                    .setOnlyAlertOnce(true)
                    .setSmallIcon(R.drawable.bgrapontrack)
                    .setContentTitle("Converting")
                    .setContentText("Converting your rap to MP3")
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText("Saving to \\RapOnMP3\\RapOnMP3-Exported"))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);


            notificationManager.notify(1337, mBuilder.build());
        }


    }

    public void updateProgress(int notiId, int progressInt) {
        //Update notification information:
        mBuilder.setProgress(100, progressInt, false);

//Send the notification:
        //notification = mBuilder.build();
        notificationManager.notify(notiId, mBuilder.build());
    }
    public void cancelNotification (int notiId) {
        notificationManager.cancel(notiId);

// notificationId is a unique int for each notification that you must define


    }
}
