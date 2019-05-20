package com.oskiapps.instrume;

/**
 * Created by Oskar on 03.04.2018.
 */

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
//import android.support.v4.BuildConfig;
import android.widget.Toast;

//import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
//import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
//import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;

import java.io.File;

import androidx.core.content.FileProvider;

/**
 * Created by Oskar on 02.04.2018.
 */

public class ConvertHelper extends AsyncTask<Void, Void, Object[]> {

    ProgressDialog progDialog;
    Context ctx;
    public ConvertHelper(Context gotCtx) {
        ctx = gotCtx;
    }

    @Override
    protected void onPreExecute() {
        progDialog = new ProgressDialog(ctx);
        progDialog.setTitle("Converting to MP3 - exporting to \\RapOnMp3\\RapOnMp3-export.mp3");

        progDialog.show();
    }
    boolean done = false;
    @Override
    protected Object[] doInBackground(Void... voids) {


        while(!done) {

        }
        return new Object[0];
    }

    @Override
    protected void onPostExecute(Object[] obj) {

    }

    private void publishProgress(int currentPosition) {

    }
}
