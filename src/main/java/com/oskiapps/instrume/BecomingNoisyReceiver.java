package com.oskiapps.instrume;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.widget.Toast;

public class BecomingNoisyReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
            // Pause the playback
            Toast.makeText(context,"Cable removed, restart recording", Toast.LENGTH_SHORT).show();
            LiveEffectEngine.stopAll();
            ((Activity) context).finish();
        }
    }
}
