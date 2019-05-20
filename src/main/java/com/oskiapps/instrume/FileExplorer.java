package com.oskiapps.instrume;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by Oskar on 29.03.2018.
 */
public class FileExplorer extends Activity {

    // Stores names of traversed directories
    ArrayList<String> str = new ArrayList<String>();

    // Check if the first level of the directory structure is the one showing
    private Boolean firstLvl = true;

    private static final String TAG = "F_PATH";

    private Item[] fileList;
    private File path = new File(Environment.getExternalStorageDirectory() + "");
    private String chosenFile;
    private static final int DIALOG_LOAD_FILE = 1000;

    ListAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        loadFileList();

        showDialog(DIALOG_LOAD_FILE);
        Log.d(TAG, path.getAbsolutePath());

    }

    private void loadFileList() {
        try {
            path.mkdirs();
        } catch (SecurityException e) {
            Log.e(TAG, "unable to write on the sd card ");
        }

        // Checks whether path exists
        if (path.exists()) {
            FilenameFilter filter = new FilenameFilter() {
                @Override
                public boolean accept(File dir, String filename) {
                    File sel = new File(dir, filename);
                    // Filters based on whether the file is hidden or not
                    boolean extMp3 = false;
                    //System.out.println("oski "+filename.lastIndexOf("."));
                    if(filename.lastIndexOf(".") != -1) {
                        extMp3 = filename.substring(filename.lastIndexOf(".")).equals(".mp3");
                    }
                    return (sel.isFile() || sel.isDirectory())
                            && !sel.isHidden() && (sel.isDirectory() || extMp3);

                }
            };

            String[] fList = path.list(filter);
            Arrays.sort(fList);
            fileList = new Item[fList.length];
            for (int i = 0; i < fList.length; i++) {
                fileList[i] = new Item(fList[i], android.R.drawable.ic_media_play);

                // Convert into file path
                File sel = new File(path, fList[i]);

                // Set drawables
                if (sel.isDirectory()) {
                    fileList[i].icon = android.R.drawable.ic_input_get;
                    Log.d("DIRECTORY", fileList[i].file);
                } else {
                    Log.d("FILE", fileList[i].file);
                }
            }

            if (!firstLvl) {
                Item temp[] = new Item[fileList.length + 1];
                for (int i = 0; i < fileList.length; i++) {
                    temp[i + 1] = fileList[i];
                }
                temp[0] = new Item("Up", android.R.drawable.arrow_up_float);
                fileList = temp;
            }
        } else {
            Log.e(TAG, "path does not exist");
        }
        adapter = new ArrayAdapter<Item>(this,
                android.R.layout.select_dialog_item, android.R.id.text1,
                fileList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                // creates view
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view
                        .findViewById(android.R.id.text1);

                //TODO load own images for file explorer
                // put the image on the text view
                textView.setCompoundDrawablesWithIntrinsicBounds(
                       fileList[position].icon, 0, 0, 0);

                // add margin between image and text (support various screen
                // densities)
                int dp5 = (int) (5 * getResources().getDisplayMetrics().density + 0.5f);
                //textView.setCompoundDrawablePadding(dp5);

                return view;
            }
        };

    }

    private class Item {
        public String file;
        public int icon;

        public Item(String file, Integer icon) {
            this.file = file;
            this.icon = icon;
        }

        @Override
        public String toString() {
            return file;
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                // dialog dismiss without button press
                finish();
            }
        });

        if (fileList == null) {
            Log.e(TAG, "No files loaded");
            dialog = builder.create();
            return dialog;
        }

        switch (id) {
            case DIALOG_LOAD_FILE:
                builder.setTitle("Choose your file");
                builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        chosenFile = fileList[which].file;
                        File sel = new File(path + "/" + chosenFile);
                        if (sel.isDirectory()) {
                            firstLvl = false;

                            // Adds chosen directory to list
                            str.add(chosenFile);
                            fileList = null;
                            path = new File(sel + "");

                            loadFileList();

                            removeDialog(DIALOG_LOAD_FILE);
                            showDialog(DIALOG_LOAD_FILE);
                            Log.d(TAG, path.getAbsolutePath());

                        }

                        // Checks if 'up' was clicked
                        else if (chosenFile.equalsIgnoreCase("up") && !sel.exists()) {

                            // present directory removed from list
                            String s = str.remove(str.size() - 1);

                            // path modified to exclude present directory
                            path = new File(path.toString().substring(0,
                                    path.toString().lastIndexOf(s)));
                            fileList = null;

                            // if there are no more directories in the list, then
                            // its the first level
                            if (str.isEmpty()) {
                                firstLvl = true;
                            }
                            loadFileList();

                            removeDialog(DIALOG_LOAD_FILE);
                            showDialog(DIALOG_LOAD_FILE);
                            Log.d(TAG, path.getAbsolutePath());

                        }
                        // File picked
                        else {
                            // Perform action with file picked
                            AudioConstants.filePathMp3 = sel.getAbsolutePath();
                            System.out.println("going to decode");
                            progDialog = new ProgressDialog(FileExplorer.this);
                            progDialog.setTitle("Importing MP3 "+ AudioConstants.filePathMp3);
                            progDialog.show();
                            Executor executor = Executors.newSingleThreadExecutor();
                            FileExplorer.ImportFilesRunnable importFilesRunnable = new ImportFilesRunnable();
                            executor.execute(importFilesRunnable);


                            //gotCanvasFrag.drawClass.drawFromFile(AudioConstants.filePathWav,(int)(gotCanvasFrag.drawClass.drCanvas.getHeight()/2), 0, gotCanvasFrag.drawClass.mp3Paint);

                        }

                    }
                });
                break;
        }
        //dialog.setCanceledOnTouchOutside(true);
        dialog = builder.show();
        return dialog;
    }
    ProgressDialog progDialog;
    class ImportFilesRunnable implements Runnable {
        public void run() {
            /*try {
                AudioConstants.filePathChosenMp3 = path + "/" + chosenFile;

                //MainHelper.firstImport(getApplicationContext());



                Intent resultIntent = new Intent();
                resultIntent.putExtra("wavok", 0);
                setResult(Activity.RESULT_OK, resultIntent);

                progDialog.dismiss();
                finish();

            } catch (IOException e) {
                e.printStackTrace();
            }*/

            //database insert code here
        }
    }

}