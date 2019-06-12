package com.oskiapps.instrume;


import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;

//uncomment when ads are needed
//import com.google.android.gms.ads.AdListener;
//import com.google.android.gms.ads.MobileAds;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.leff.midi.MidiFile;
import com.leff.midi.MidiTrack;
import com.leff.midi.event.NoteOff;
import com.leff.midi.event.NoteOn;
import com.leff.midi.event.ProgramChange;
import com.leff.midi.event.meta.Tempo;
import com.leff.midi.event.meta.TimeSignature;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.sample.audio_device.AudioDeviceListEntry;
import com.google.sample.audio_device.AudioDeviceSpinner;

import static android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION;
import static android.os.Environment.getExternalStorageDirectory;

//import com.google.android.gms.ads.*;
//import com.google.android.gms.ads.AdRequest;
//import com.google.android.gms.ads.InterstitialAd;

/**
 * Created by Oskar on 20.09.2018.
 */


public  class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback, CanvasFragmentPreviewPlayer.OnCompleteListener {
    private static final int AUDIO_EFFECT_REQUEST = 0;
    private static final int OBOE_API_AAUDIO = 0;
    private static final int OBOE_API_OPENSL_ES = 1;
    private int apiSelection = OBOE_API_AAUDIO;
    private boolean aaudioSupported = false;

    private static final int AUDIO_ECHO_REQUEST = 0;

    private AudioDeviceSpinner mPlaybackDeviceSpinner;

    AudioManager mAudioMgr;

    ToggleButton midiRecActiveToggle;

    ToggleButton pianoToggleBtn;
    ToggleButton otherInstrumentToggleBtn;
    ToggleButton guitarToggleBtn;
    ToggleButton saxoToggleBtn;

    Button midiAddBtn;
    Button midiMoveBtn;
    Button midiDeleteBtn;
    Button midiStretchBtn;
    Button midiEditBtn;
    Button playToggleBtn;
    Button recToggleBtn;
    ToggleButton playMidiToggleBtn;
    EditText midiInstrNrEdit;
    SeekBar midiThreshSB;
    SeekBar tuneLineSpinner;
    Button midiExportBtn;
    ToggleButton midiRecToggleBtn;
    Button midiClearBtn;
    Button backBtn;

    private String nativeSampleRate;
    private String nativeSampleBufSize;

    private boolean supportRecording;
    private Boolean isPlaying = false;
    private boolean isRecording = false;

    private float currMusicVol;
    private float currVocalVol;

    private float currDryWet = 0.5f;
    private float currDecay = 0.5f;

    private LinearLayout layoutEQ;
    private LinearLayout layoutVolume;
    private LinearLayout layoutReverb;
    private LinearLayout layoutAutotune;
    LinearLayout linearFxBox;

    SeekBar musicVolumeSB;
    SeekBar vocalVolumeSB;

    private IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
    private BecomingNoisyReceiver myNoisyAudioStreamReceiver = new BecomingNoisyReceiver();


    MainHelper.ToggleClickStates toggleClickStatesObj;
    boolean preMidiRecClicked = false;
    boolean preMidiRecActiveClicked = false;
    boolean prePlayMidiClicked = false;
    ToggleButton[] toggleBtnArray = new ToggleButton[4];
    //private InterstitialAd mInterstitialAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voiceboard_main);

        canvFragAuto = (CanvasFragmentMain) getSupportFragmentManager().findFragmentById(R.id.canvasFragmentAutoTuneLive);
        canvFragPrev = (CanvasFragmentPreviewPlayer) getSupportFragmentManager().findFragmentById(R.id.canvasFragmentPreviewPlayer);

        linearFxBox = findViewById(R.id.linearLayoutFxBox);

        layoutEQ = (LinearLayout) findViewById(R.id.linearLayoutEQ);
        layoutReverb = (LinearLayout) findViewById(R.id.linearLayoutReverb);
        layoutVolume = (LinearLayout) findViewById(R.id.linearLayoutVolumes);
        layoutAutotune = (LinearLayout) findViewById(R.id.LinearLayoutAutotune);

        guitarToggleBtn = findViewById(R.id.guitarToggle);
        saxoToggleBtn = findViewById(R.id.saxoToggle);
        pianoToggleBtn = findViewById(R.id.fenderToggle);
        otherInstrumentToggleBtn = findViewById(R.id.otherInstrumentToggle);

        recToggleBtn = (Button) findViewById(R.id.recToggle);
        playToggleBtn = (Button) findViewById(R.id.playToggle);
        backBtn = (Button) findViewById(R.id.backBtn);
        midiStretchBtn = findViewById(R.id.midiStretchBtn);
        midiMoveBtn = findViewById(R.id.midiMoveBtn);
        midiAddBtn = findViewById(R.id.midiAddBtn);
        midiDeleteBtn = findViewById(R.id.midiDeleteBtn);
        midiRecActiveToggle = findViewById(R.id.midiRecActiveToggle);
        playMidiToggleBtn = findViewById(R.id.playMidiToggle);
        midiEditBtn = findViewById(R.id.midiEditBtn);
        midiClearBtn = findViewById(R.id.midiClearBtn);
        midiRecToggleBtn = findViewById(R.id.midiRecordToggle);
        midiExportBtn = findViewById(R.id.midiExportBtn);
        midiInstrNrEdit = (EditText) findViewById(R.id.editMidiInstr);
        musicVolumeSB = findViewById(R.id.musicvolumeSB);
        vocalVolumeSB = findViewById(R.id.vocalvolumeSB);
        midiThreshSB = findViewById(R.id.midiThreshSB);


        toggleClickStatesObj = new MainHelper.ToggleClickStates();

        toggleBtnArray[0] = pianoToggleBtn;
        toggleBtnArray[1] = guitarToggleBtn;
        toggleBtnArray[3] = saxoToggleBtn;
        toggleBtnArray[2] = otherInstrumentToggleBtn;



        initButtonBehaviors();
        verifyStoragePermissions(MainActivity.this, REQUEST_EXTERNAL_STORAGE);

        //reload commercial
        // Sample AdMob app ID: ca-app-pub-3940256099942544~3347511713
        //MobileAds.initialize(this,
        //        AudioConstants.admobId);

        //mInterstitialAd = new InterstitialAd(this);
        //mInterstitialAd.setAdUnitId(AudioConstants.admobAdId);
        //mInterstitialAd.loadAd(new AdRequest.Builder().build());
        /*mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                // Load the next interstitial.
                mInterstitialAd.loadAd(new AdRequest.Builder().build());
            }

        });*/
    }

    private void initButtonBehaviors() {
        recToggleBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (isPlaying) {
                    boolean toggleState = LiveEffectEngine.setToggleRecording();
                    isRecording = toggleState;
                    if (!toggleState) {
                        recToggleBtn.setBackground(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_recbtntry1));
                        System.out.println("oski onDestroy called");
                        LiveEffectEngine.setEffectOn(false);
                        isPlaying = false;
                        AudioConstants.markerEnd = (int) (AudioConstants.livePositionBytes / (AudioConstants.deviceSampleRate / 1000f) / 4f);
                        AudioConstants.markerState = 2;

                        finish();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Push Play & setup effects first", Toast.LENGTH_SHORT).show();
                }

            }
        });

        playToggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isPlaying = LiveEffectEngine.setTogglePlaying();
                if (isPlaying) {
                    linearFxBox.setVisibility(View.VISIBLE);
                    layoutVolume.setVisibility(View.VISIBLE);
                    layoutReverb.setVisibility(View.GONE);
                    layoutEQ.setVisibility(View.GONE);
                    layoutAutotune.setVisibility(View.GONE);
                    playToggleBtn.setBackground(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_pausebtntry1));

                } else {
                    playToggleBtn.setBackground(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_playbtntry1));
                    layoutVolume.setVisibility(View.VISIBLE);
                }
            }
        });

        playToggleBtn.setBackground(ContextCompat.getDrawable(this, R.drawable.ic_playbtntry1));
        recToggleBtn.setBackground(ContextCompat.getDrawable(this, R.drawable.ic_recbtntry1));

        backBtn.setBackground(ContextCompat.getDrawable(this, R.drawable.ic_backbtntry1));
        backBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                AudioConstants.livePositionBytes = 1000;
                LiveEffectEngine.setPlayerPosition(AudioConstants.livePositionBytes);
            }
        });


        pianoToggleBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                MainHelper.resetOtherInstrumentButtons(toggleBtnArray, toggleClickStatesObj, "fender");
                LiveEffectEngine.setMidiInstrument(0);
            }
        });


        guitarToggleBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                MainHelper.resetOtherInstrumentButtons(toggleBtnArray, toggleClickStatesObj, "guitar");
                LiveEffectEngine.setMidiInstrument(26);

            }
        });

        saxoToggleBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                MainHelper.resetOtherInstrumentButtons(toggleBtnArray, toggleClickStatesObj, "trumpet");
                LiveEffectEngine.setMidiInstrument(57);
            }
        });

        otherInstrumentToggleBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                AlertDialog.Builder builderSingle = new AlertDialog.Builder(MainActivity.this);
                builderSingle.setIcon(R.drawable.ic_instrumelogo_round);
                builderSingle.setTitle("Choose an Instrument");

                final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.select_dialog_singlechoice);
                arrayAdapter.addAll(AudioConstants.instrumentList);

                builderSingle.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

                builderSingle.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        MainHelper.resetOtherInstrumentButtons(toggleBtnArray, toggleClickStatesObj, "other");
                        LiveEffectEngine.setMidiInstrument(which);

                    }
                });
                builderSingle.show();

            }
        });


        midiMoveBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if((AudioConstants.midiEditMode == 1) && AudioConstants.markedMidiNote < 0) {
                    Toast.makeText(MainActivity.this,"Which one? Tap on a note first",Toast.LENGTH_LONG).show();
                }
                else if(AudioConstants.midiEditMode == 1) {
                    AudioConstants.midiEditMode = 2;
                    midiMoveBtn.setTextColor(Color.GREEN);
                } else if(AudioConstants.midiEditMode == 2) {
                    AudioConstants.midiEditMode = 1;
                    midiMoveBtn.setTextColor(Color.WHITE);
                } else if(AudioConstants.midiEditMode == 3) {
                    AudioConstants.midiEditMode = 2;
                    midiMoveBtn.setTextColor(Color.GREEN);
                    midiStretchBtn.setTextColor(Color.WHITE);

                }
            }
        });
        midiStretchBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if((AudioConstants.midiEditMode == 1) && AudioConstants.markedMidiNote < 0) {
                    Toast.makeText(MainActivity.this,"Which one? Tap on a note first",Toast.LENGTH_LONG).show();
                }
                else if(AudioConstants.midiEditMode == 1) {
                    AudioConstants.midiEditMode = 3;
                    midiStretchBtn.setTextColor(Color.GREEN);
                } else if(AudioConstants.midiEditMode == 2) {
                    AudioConstants.midiEditMode = 3;
                    midiMoveBtn.setTextColor(Color.WHITE);
                    midiStretchBtn.setTextColor(Color.GREEN);
                } else if(AudioConstants.midiEditMode == 3) {
                    AudioConstants.midiEditMode = 1;
                    midiStretchBtn.setTextColor(Color.WHITE);
                }
            }
        });

        midiAddBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(AudioConstants.midiEditMode == 1) {
                    AudioConstants.midiEditMode = 4;
                    midiAddBtn.setTextColor(Color.GREEN);
                } else if(AudioConstants.midiEditMode == 2) {
                    AudioConstants.midiEditMode = 4;
                    midiMoveBtn.setTextColor(Color.WHITE);
                    midiStretchBtn.setTextColor(Color.WHITE);
                    midiAddBtn.setTextColor(Color.GREEN);
                } else if(AudioConstants.midiEditMode == 3) {
                    AudioConstants.midiEditMode = 4;
                    midiStretchBtn.setTextColor(Color.WHITE);
                    midiAddBtn.setTextColor(Color.GREEN);
                } else if(AudioConstants.midiEditMode == 4) {
                    AudioConstants.midiEditMode = 1;
                    midiAddBtn.setTextColor(Color.WHITE);
                }
            }
        });

        midiDeleteBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(AudioConstants.markedMidiNote >= 0){
                    LiveEffectEngine.deleteNoteAt(AudioConstants.markedMidiNote);
                } else {
                    Toast.makeText(MainActivity.this, "Which one? Tap on a note first", Toast.LENGTH_LONG).show();
                }
            }
        });

        midiRecActiveToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                preMidiRecActiveClicked = LiveEffectEngine.setToggleRecActive();
                AudioConstants.midiRecActive = preMidiRecActiveClicked;
                if (preMidiRecActiveClicked) {
                    midiRecActiveToggle.setTextColor(getResources().getColor(R.color.green));

                } else {
                    midiRecActiveToggle.setTextColor(Color.WHITE);
                }

            }
        });

        playMidiToggleBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prePlayMidiClicked = LiveEffectEngine.setTogglePlayMidi();
                canvFragPrev.drawClass.translX = 0;

                if (prePlayMidiClicked) {
                    playMidiToggleBtn.setTextColor(getResources().getColor(R.color.green));

                } else {
                    playMidiToggleBtn.setTextColor(Color.WHITE);
                }

            }
        });


        midiClearBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                LiveEffectEngine.clearMidiSong();

                playMidiToggleBtn.setVisibility(View.GONE);
                midiEditBtn.setVisibility(View.GONE);
                midiStretchBtn.setVisibility(View.GONE);
                midiMoveBtn.setVisibility(View.GONE);
                midiAddBtn.setVisibility(View.GONE);
                midiDeleteBtn.setVisibility(View.GONE);

                midiAddBtn.setTextColor(Color.WHITE);
                midiStretchBtn.setTextColor(Color.WHITE);
                midiMoveBtn.setTextColor(Color.WHITE);

                showCommercial();
            }
        });

        midiEditBtn.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                View frag = findViewById(R.id.canvasFragmentAutoTuneLive);
                ViewGroup.LayoutParams params = frag.getLayoutParams();
                if(AudioConstants.midiEditMode == 0) {
                    AudioConstants.midiEditMode = 1;
                    midiEditBtn.setTextColor(Color.GREEN);
                    params.height = 100;
                    frag.setLayoutParams(params);
                    midiStretchBtn.setVisibility(View.VISIBLE);
                    midiMoveBtn.setVisibility(View.VISIBLE);
                    midiStretchBtn.setTextColor(Color.WHITE);
                    midiMoveBtn.setTextColor(Color.WHITE);

                    midiAddBtn.setVisibility(View.VISIBLE);
                    midiDeleteBtn.setVisibility(View.VISIBLE);
                } else {
                    AudioConstants.midiEditMode = 0;
                    midiEditBtn.setTextColor(Color.WHITE);
                    params.height = canvFragPrev.drawClass.initKeyboardHeight;
                    frag.setLayoutParams(params);
                    frag.setLayoutParams(params);
                    midiStretchBtn.setVisibility(View.GONE);
                    midiMoveBtn.setVisibility(View.GONE);
                    midiAddBtn.setVisibility(View.GONE);
                    midiDeleteBtn.setVisibility(View.GONE);

                    midiStretchBtn.setTextColor(Color.WHITE);
                    midiMoveBtn.setTextColor(Color.WHITE);
                    AudioConstants.markedMidiNote = -1;
                }

                showCommercial();

            }

        });

        midiRecToggleBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                preMidiRecClicked = LiveEffectEngine.setToggleMidiRec();
                if (preMidiRecClicked) {
                    midiRecToggleBtn.setTextColor(getResources().getColor(R.color.green));
                } else {
                    if (AudioConstants.midiEvents.length > 0) {
                        playMidiToggleBtn.setVisibility(View.VISIBLE);
                        midiEditBtn.setVisibility(View.VISIBLE);

                        SharedPreferences appSharedPrefs = PreferenceManager
                                .getDefaultSharedPreferences(MainActivity.this);
                        SharedPreferences.Editor prefsEditor = appSharedPrefs.edit();
                        Gson gson = new Gson();
                        String json = gson.toJson(AudioConstants.midiEvents);
                        prefsEditor.putString("midievents", json);
                        System.out.print("Oski curious " + json );
                        prefsEditor.commit();
                    } else {
                        playMidiToggleBtn.setVisibility(View.GONE);
                    }
                    midiRecToggleBtn.setTextColor(Color.WHITE);
                }

            }
        });



        midiExportBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                showCommercial();

                final Dialog dialog = new Dialog(MainActivity.this);
                dialog.setContentView(R.layout.dialog_midi_export_options);
                dialog.setTitle("ALERT!!");
                // set values for custom dialog components - text, image and button
                Button okbtn = (Button) dialog.findViewById(R.id.okbtn);
                Button cancelbtn = (Button) dialog.findViewById(R.id.cancelbtn);

                final Button exportMidiPlayBtn = (Button) dialog.findViewById(R.id.exportMidiPlayBtn);

                final File firstOutputFile = exportMidiFile("exporttest.mid");

                final MediaPlayer exportMp = new MediaPlayer();
                try {
                    exportMp.setDataSource(firstOutputFile.toString());
                    exportMp.prepare();

                } catch (IOException e) {
                    e.printStackTrace();
                }

                exportMp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    public void onCompletion(MediaPlayer mp) {
                        exportMidiPlayBtn.setBackground(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_playbtntry1));
                    }
                });

                dialog.show();
                dialog.getWindow().setSoftInputMode(
                        WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                // if decline button is clicked, close the custom dialog
                cancelbtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });
                okbtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final int permission = ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE);

                        verifyStoragePermissions(MainActivity.this, REQUEST_EXTERNAL_STORAGE);

                        System.out.println("going to import");
                        if (permission == PackageManager.PERMISSION_GRANTED) {
                            final Dialog dialogObj = Dialog.class.cast(dialog);

                            EditText fileName = (EditText) dialogObj.findViewById(R.id.fileNameExportTxt);
                            String fileNameStr = fileName.getText().toString();

                            File output = exportMidiFile(fileNameStr);


                            Uri uri = FileProvider.getUriForFile(MainActivity.this, BuildConfig.APPLICATION_ID, output);
                            grantUriPermission(getApplicationContext().getPackageName(), uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

                            Intent shareIntent = new Intent();
                            shareIntent.setAction(Intent.ACTION_SEND);
                            shareIntent.setFlags(FLAG_GRANT_READ_URI_PERMISSION);

                            // I am opening a PDF file so I give it a valid MIME effectType
                            //shareIntent.setDataAndType(uri, "audio/mp3");

                            shareIntent.setType("audio/*");
                            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);

                            // validate that the device can open your File!
                            PackageManager pm = MainActivity.this.getPackageManager();
                            if (shareIntent.resolveActivity(pm) != null) {
                                MainActivity.this.startActivity(shareIntent);
                            }

                        }
                    }

                    ;
                });
                exportMidiPlayBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (exportMp.isPlaying()) {
                            exportMidiPlayBtn.setBackground(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_playbtntry1));
                            exportMp.pause();
                            exportMp.seekTo(0);
                            //exportMp.release();
                        } else {
                            exportMidiPlayBtn.setBackground(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_pausebtntry1));
                            exportMp.start();
                        }
                    }
                });
            }
        });

        midiInstrNrEdit.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {
                LiveEffectEngine.setMidiInstrument(Integer.parseInt(s.toString()));
                System.out.println("oskiouto " + Integer.parseInt(s.toString()));
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        musicVolumeSB.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currMusicVol = progress / 100f;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                LiveEffectEngine.setVolumeMusic(currMusicVol);
            }
        });

        vocalVolumeSB.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currVocalVol = progress / 300f;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                LiveEffectEngine.setVolumeVocal(currVocalVol);
            }
        });

        /*SeekBar reverbDrySB = findViewById(R.id.reverbDrySB);
        reverbDrySB.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currDryWet = progress / 30f;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                LiveEffectEngine.setReverbDry(currDryWet);
            }
        });*/


        midiThreshSB.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currDecay = progress / 100f;
                AudioConstants.midiRecThreshold = currDecay;
                System.out.println("oskioutt " + AudioConstants.midiRecThreshold);
                LiveEffectEngine.setThresholdLevel(currDecay);

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                AudioConstants.midiRecThreshold = currDecay;
                LiveEffectEngine.setThresholdLevel(currDecay);
            }
        });

        /*tuneLineSpinner = findViewById(R.id.autoTuneLiveSpinner);
        final String[] items = new String[]{"C-Major", "C-Minor", "D-Major", "D-Minor", "E-Major", "E-Minor", "F-Major", "F-Minor", "G-Major", "G-Minor", "A-Major", "A-Minor", "B-Major"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items);
        tuneLineSpinner.setAdapter(adapter);

        tuneLineSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            boolean firstStart = true;

            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                if (firstStart) {
                    AudioConstAutoTune.scale = (String) parent.getItemAtPosition(position);
                    System.out.println("oski nowi " + AudioConstAutoTune.scale);
                    LiveEffectEngine.setAutoTuneScale(AudioConstAutoTune.scale, AudioConstAutoTune.dryWet);
                } else {
                    firstStart = false;
                }
                //Log.v("item", (String) parent.getItemAtPosition(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });*/
    }

    private int getPlaybackDeviceId() {
        return ((AudioDeviceListEntry) mPlaybackDeviceSpinner.getSelectedItem()).getId();
    }

    private void setupPlaybackDeviceSpinner() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mPlaybackDeviceSpinner.setDirectionType(AudioManager.GET_DEVICES_OUTPUTS);
            mPlaybackDeviceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    int theOutDevice = getPlaybackDeviceId();
                    LiveEffectEngine.setPlaybackDeviceId(theOutDevice);
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });
        }
    }

    private static int compareInts(int a, int b) {
        if (a == b) {
            return 0;
        } else if (a > b) {
            return 1;
        } else {
            return -1;
        }
    }

    private int getMaxSampleRate(AudioDeviceInfo info) {
        int[] sampleRates = info.getSampleRates();
        if (sampleRates == null || sampleRates.length == 0) {
            System.out.println("didthis #1");
            return 48000;
        }
        int sampleRate = sampleRates[0];
        for (int i = 1; i < sampleRates.length; i++) {
            if (sampleRates[i] > sampleRate) {
                sampleRate = sampleRates[i];
            }
        }

        return sampleRate;
    }

    public File exportMidiFile(String gotFileName) {

        MidiTrack tempoTrack = new MidiTrack();
        MidiTrack noteTrack = new MidiTrack();

        // 2. Add events to the tracks
        // 2a. Track 0 is typically the tempo map
        TimeSignature ts = new TimeSignature();
        ts.setTimeSignature(4, 4, TimeSignature.DEFAULT_METER, TimeSignature.DEFAULT_DIVISION);

        ProgramChange program = new ProgramChange(0, 0, LiveEffectEngine.getMidiInstrument());
        noteTrack.insertEvent(program);

        Tempo t = new Tempo();
        t.setBpm(120);

        tempoTrack.insertEvent(ts);
        tempoTrack.insertEvent(t);

        /*// 2b. Track 1 will have some notes in it
        for (int i = 0; i < 80; i++) {
            int channel = 0, pitch = 1 + i, velocity = 100;
            NoteOn on = new NoteOn(i * 480, channel, pitch, velocity);
            NoteOff off = new NoteOff(i * 480 + 120, channel, pitch, 0);

            noteTrack.insertEvent(on);
            noteTrack.insertEvent(off);

        }*/

        for (int i = 0; i < AudioConstants.midiEvents.length; i++) {
            int channel = 0, pitch = (int) AudioConstants.midiEvents[i][0], velocity = 100;
            NoteOn on = new NoteOn((long) (AudioConstants.midiEvents[i][1] * 480 * 2), channel, pitch, velocity);
            NoteOff off = new NoteOff((long) (AudioConstants.midiEvents[i][2] * 480 * 2), channel, pitch, 0);

            noteTrack.insertEvent(on);
            noteTrack.insertEvent(off);
        }

        // It's best not to manually insert EndOfTrack events; MidiTrack will
        // call closeTrack() on itself before writing itself to a file

        // 3. Create a MidiFile with the tracks we created
        ArrayList<MidiTrack> tracks = new ArrayList<MidiTrack>();
        tracks.add(tempoTrack);
        tracks.add(noteTrack);

        MidiFile midi = new MidiFile(MidiFile.DEFAULT_RESOLUTION, tracks);

        // 4. Write the MIDI data to a file
        File output = new File(AudioConstants.dirPath + gotFileName + ".mid");
        try {
            midi.writeToFile(output);
        } catch (IOException e) {
            System.err.println(e);
        }

        return output;
    }

    final static String TARGET_BASE_PATH = Environment.getExternalStorageDirectory() + "/RapOnMp3/";

    private void copyFilesToSdCard() {
        copyFileOrDir(""); // copy all files in assets folder in my project
    }

    private void copyFileOrDir(String path) {
        AssetManager assetManager = this.getAssets();
        String assets[] = null;
        try {

            AudioConstants.internalPath = getExternalStorageDirectory().getAbsolutePath();
            AudioConstants.buildStrings();
            System.out.println("mypaths " + AudioConstants.internalPath + " " + AudioConstants.filePathWav);

            boolean success = false;
            File topdir = new File(AudioConstants.dirPath);
            if (!topdir.exists()) {
                success = topdir.mkdir();
            }
            if (!success) {
                System.out.println("oski could not create raponmp3");
            }
            Log.i("tag", "copyFileOrDir() " + path);
            assets = assetManager.list(path);
            if (assets.length == 0) {
                copyFile(path);
            } else {
                String fullPath = AudioConstants.dirPath + path;
                Log.i("tag", "path=" + fullPath);
                File dir = new File(fullPath);
                if (!dir.exists() && !path.startsWith("images") && !path.startsWith("sounds") && !path.startsWith("webkit"))
                    if (!dir.mkdirs())
                        Log.i("tag", "could not create dir " + fullPath);
                for (int i = 0; i < assets.length; ++i) {
                    String p;
                    if (path.equals(""))
                        p = "";
                    else
                        p = path + "/";

                    if (!path.startsWith("images") && !path.startsWith("sounds") && !path.startsWith("webkit"))
                        copyFileOrDir(p + assets[i]);
                }
            }
        } catch (IOException ex) {
            Log.e("tag", "I/O Exception", ex);
        }
    }

    private void copyFile(String filename) {
        AssetManager assetManager = this.getAssets();

        InputStream in = null;
        OutputStream out = null;
        String newFileName = null;
        try {
            Log.i("tag", "copyFile() " + filename);
            in = assetManager.open(filename);
            if (filename.endsWith(".jpg")) // extension was added to avoid compression on APK file
                newFileName = TARGET_BASE_PATH + filename.substring(0, filename.length() - 4);
            else
                newFileName = TARGET_BASE_PATH + filename;
            out = new FileOutputStream(newFileName);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            in = null;
            out.flush();
            out.close();
            out = null;
        } catch (Exception e) {
            Log.e("tag", "Exception in copyFile() of " + newFileName);
            Log.e("tag", "Exception in copyFile() " + e.toString());
        }

    }


    private void EnableAudioApiUI(boolean enable) {
        if (apiSelection == OBOE_API_AAUDIO && !aaudioSupported) {
            apiSelection = OBOE_API_OPENSL_ES;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

    }

    private void requestRecordPermission() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.RECORD_AUDIO},
                AUDIO_EFFECT_REQUEST);
    }

    int chosenOutputDevice = 0;
    boolean externalAudioOutput = false;

    private boolean isHeadphonesPlugged() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AudioDeviceInfo[] audioDevices = mAudioMgr.getDevices(AudioManager.GET_DEVICES_ALL);

            for (AudioDeviceInfo deviceInfo : audioDevices) {
                if (deviceInfo.getType() == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
                        || deviceInfo.getType() == AudioDeviceInfo.TYPE_WIRED_HEADSET
                        || deviceInfo.getType() == AudioDeviceInfo.TYPE_USB_HEADSET
                        || deviceInfo.getType() == AudioDeviceInfo.TYPE_AUX_LINE
                        || deviceInfo.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
                        || deviceInfo.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
                        || deviceInfo.getType() == AudioDeviceInfo.TYPE_LINE_ANALOG
                        || deviceInfo.getType() == AudioDeviceInfo.TYPE_USB_DEVICE
                        || deviceInfo.getType() == AudioDeviceInfo.TYPE_USB_ACCESSORY
                        ) {
                    externalAudioOutput = true;
                    chosenOutputDevice = deviceInfo.getId();
                    return true;
                }
            }
            for (AudioDeviceInfo deviceInfo : audioDevices) {
                if (deviceInfo.getType() == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE) {
                    chosenOutputDevice = deviceInfo.getId();
                    externalAudioOutput = false;

                    return true;
                }
            }


        } else {
            return true;
        }
        return false;
    }

    public void showCommercial() {
        /*if (mInterstitialAd.isLoaded()) {
            if(AudioConstants.skipCommercialCnt > 3) {
                mInterstitialAd.show();
                AudioConstants.skipCommercialCnt = 0;
            } else {
                AudioConstants.skipCommercialCnt++;
            }
        } else {
            Log.d("TAG", "The interstitial wasn't loaded yet.");
        }*/
    }


    private void startEffect() {
        Log.d("oski", "Attempting to start");

        if (!isRecordPermissionGranted()) {
            requestRecordPermission();
            return;
        }
        LiveEffectEngine.setEffectOn(true);
    }

    CanvasFragmentMain canvFragAuto;
    CanvasFragmentPreviewPlayer canvFragPrev;

    private boolean isRecordPermissionGranted() {
        return (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED);
    }

    @Override
    protected void onDestroy() {
        if (supportRecording) {
            if (isPlaying) {
                isPlaying = false;

            }
        }
        //unregisterReceiver(myNoisyAudioStreamReceiver);

        super.onDestroy();
    }


    @Override
    public AssetManager getAssets() {
        return super.getAssets();
    }

    private AssetManager assetManager;

    //File fileRef;
    public void onEchoClick() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) !=
                PackageManager.PERMISSION_GRANTED) {
            //statusView.setText(getString(R.string.request_permission_status_msg));
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    AUDIO_ECHO_REQUEST);
            return;
        }
        queryNativeAudioParameters();

        boolean isHeadPhone = isHeadphonesPlugged();

        LiveEffectEngine.create();
        SharedPreferences appSharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(this.getApplicationContext());
        String json = appSharedPrefs.getString("midievents", "");
        if(json != null) {
            if(!json.equals("")) {
                Object[] result = new Gson().fromJson(json, Object[].class);
                for(int i = 0; i < result.length; i++) {
                    JsonElement je = new JsonParser().parse(result[i].toString());
                    JsonArray list = je.getAsJsonArray(); // to get rid of the value part
                    Type listType1 = new TypeToken<float[]>() {}.getType();
                    Gson g = new Gson();

                    float[] floatArr = g.fromJson(list, listType1);
                    LiveEffectEngine.syncMidiEvents(i,(int)floatArr[0],floatArr[1],floatArr[2]);
                    System.out.println("oskicreate "+(int)floatArr[0]+" "+floatArr[1]+" "+floatArr[2]);
                }
                playMidiToggleBtn.setVisibility(View.VISIBLE);
                midiStretchBtn.setTextColor(Color.WHITE);
                midiMoveBtn.setTextColor(Color.WHITE);
                midiEditBtn.setTextColor(Color.WHITE);
                midiEditBtn.setVisibility(View.VISIBLE);
            }
        }
        //s5mini thinks he has a hdmi output selected if no input device
        if (chosenOutputDevice == 9) {
            externalAudioOutput = false;
        }
        System.out.println("oski device" + " " + chosenOutputDevice);
        if (!externalAudioOutput) {
            LiveEffectEngine.setVolumeVocal(0.0f);
            LiveEffectEngine.setVolumeMusic(0.2f);
            vocalVolumeSB.setProgress(0);
            vocalVolumeSB.setEnabled(false);
            musicVolumeSB.setProgress(15);

        } else {
            LiveEffectEngine.setVolumeVocal(0.70f);
            LiveEffectEngine.setVolumeMusic(0.30f);
            vocalVolumeSB.setProgress(70);
            musicVolumeSB.setProgress(30);
        }

        LiveEffectEngine.setupLowLatencyParams(Integer.parseInt(nativeSampleBufSize) * 2, (int) (AudioConstants.deviceSampleRate));

        LiveEffectEngine.setPlayerPosition(AudioConstants.livePositionBytes);
        System.out.println("ossi " + AudioConstants.livePositionBytes + " " + nativeSampleBufSize + " " + chosenOutputDevice + " " + AudioConstants.deviceSampleRate);

        aaudioSupported = LiveEffectEngine.isAAudioSupported();
        System.out.println("ossi " + AudioConstants.livePositionBytes + " " + aaudioSupported);

        EnableAudioApiUI(true);
        LiveEffectEngine.setAPI(apiSelection);
        assetManager = getAssets();
        LiveEffectEngine.addAssetMgr(assetManager, AudioConstants.filePathWav, AudioConstants.filePathRecLong, AudioConstants.filePathRecLongFx, AudioConstants.filePathMixed, AudioConstants.dirPath + "rawwaves/");

        startEffect();

    }

    @Override
    public void onResume() {
        super.onResume();
        canvFragPrev.drawClass.translX = 0;
        AudioConstants.livePositionBytes = 0;
        registerReceiver(myNoisyAudioStreamReceiver, intentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(myNoisyAudioStreamReceiver);
    }

    public int getValidSampleRates() {
        int lastBuffSize = 0;
        int retSize = 0;
        for (int rate : new int[]{8000, 11025, 16000, 22050, 44100, 48000}) {  // add the rates you wish to check against
            int bufferSize = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            if (bufferSize > 0) {
                if (bufferSize > lastBuffSize) {
                    retSize = rate;
                } else {
                    retSize = rate;
                }
                System.out.println("oskiciao " + bufferSize + " " + lastBuffSize + " " + retSize);
                // buffer size is valid, Sample rate supported

            }
            lastBuffSize = rate;
        }
        return retSize;
    }

    public boolean validSampleRate(int sample_rate) {
        AudioRecord recorder = null;
        try {
            int bufferSize = AudioRecord.getMinBufferSize(sample_rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, sample_rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
        } catch (IllegalArgumentException e) {
            return false;
        } finally {
            if (recorder != null)
                recorder.release();
        }
        return true;
    }

    ArrayList<Integer> TrueMan;
    public class Bigestnumber extends AsyncTask<String, String, String> {
        ProgressDialog pdLoading = new ProgressDialog(MainActivity.this);

        @Override
        protected String doInBackground(String... params) {
            final int validSampleRates[] = new int[]{
                    48000, 44100, 44056, 37800, 32000, 22050, 16000, 11025, 4800, 8000};
            TrueMan = new ArrayList<Integer>();
            for (int smaple : validSampleRates) {
                if (validSampleRate(smaple) == true) {
                    TrueMan.add(smaple);
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            Integer largest = Collections.max(TrueMan);
            System.out.println("Largest   " + String.valueOf(largest));
        }

    }

    public void getLowLatencyParameters(View view) {
        //updateNativeAudioUI();
    }

    private void queryNativeAudioParameters() {

        Bigestnumber sampleRateGetter = new Bigestnumber();
        sampleRateGetter.execute();
        supportRecording = true;
        mAudioMgr = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        //audioSessionId = mAudioMgr.generateAudioSessionId();
        if(mAudioMgr == null) {
            supportRecording = false;
            return;
        }

        nativeSampleRate  =  mAudioMgr.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE);
        AudioDeviceInfo myDeviceinfo ;
        AudioDeviceInfo[] deviceInfos = mAudioMgr.getDevices(AudioManager.GET_DEVICES_INPUTS);
        int theNativeSR = getMaxSampleRate(deviceInfos[0]);
        System.out.println("oskioutbest " + theNativeSR);
        nativeSampleBufSize = mAudioMgr.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER);
        int recBufSize = AudioRecord.getMinBufferSize(
                theNativeSR,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        if (recBufSize == AudioRecord.ERROR ||
                recBufSize == AudioRecord.ERROR_BAD_VALUE) {
            supportRecording = false;
        }
        AudioConstants.deviceSampleRate = theNativeSR;
        // hardcoded channel to mono: both sides -- C++ and Java sides

        System.out.println("oskibest" + recBufSize + " " + nativeSampleRate + " " + nativeSampleBufSize +  " " + AudioConstants.deviceBufferSize);
    }


    public void showHeadphonesPopup() {
        final AlertDialog.Builder adb = new AlertDialog.Builder(MainActivity.this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_output_source, null);
        adb.setView(dialogView);
        mPlaybackDeviceSpinner = dialogView.findViewById(R.id.playbackDevicesSpinner);

        setupPlaybackDeviceSpinner();

        adb.setTitle("Connect Headphones/Speaker");
        adb.setIcon(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_input_cable));
        adb.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                onEchoClick();
                dialog.dismiss();
            }
        });
        adb.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        adb.create();
        adb.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        /*
         * if any permission failed, the sample could not play
         */
        if (AUDIO_ECHO_REQUEST != requestCode) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }
        if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            copyFileOrDir("rawwaves");
            showHeadphonesPopup();
        }

        if (grantResults.length != 1  ||
                grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            /*
             * When user denied permission, throw a Toast to prompt that RECORD_AUDIO
             * is necessary; also display the status on UI
             * Then application goes back to the original state: it behaves as if the button
             * was not clicked. The assumption is that user will re-click the "start" button
             * (to retry), or shutdown the app in normal way.
             */
            //statusView.setText(getString(R.string.permission_error_msg));
            Toast.makeText(getApplicationContext(),
                    getString(R.string.permission_prompt_msg),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        /*
         * When permissions are granted, we prompt the user the status. User would
         * re-try the "start" button to perform the normal operation. This saves us the extra
         * logic in code for async processing of the button listener.
         */
        //statusView.setText(getString(R.string.permission_granted_msg,getString(R.string.cmd_start_echo)));
        // The callback runs on app's thread, so we are safe to resume the action
    }

    @Override
    public void onComplete() {
        Paint thePaint = new Paint();
        thePaint.setColor(Color.GREEN);
    }

    @Override
    public void onBackPressed() {
        System.out.println("oski onDestroy called");
        isPlaying = false;
        if (AudioConstants.midiEvents.length > 0) {

            SharedPreferences appSharedPrefs = PreferenceManager
                    .getDefaultSharedPreferences(MainActivity.this);
            SharedPreferences.Editor prefsEditor = appSharedPrefs.edit();
            Gson gson = new Gson();
            String json = gson.toJson(AudioConstants.midiEvents);
            prefsEditor.putString("midievents", json);
            System.out.print("Oski curious " + json );
            prefsEditor.commit();
        }
        LiveEffectEngine.setEffectOn(false);

        super.onBackPressed();
    }

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    private static final int PERMISSION_RECORD_AUDIO = 0;
    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.MANAGE_DOCUMENTS
    };
    public void verifyStoragePermissions(Activity activity, int rescode) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int permission2 = ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    rescode
            );
        } else {
            copyFileOrDir("rawwaves");
            showHeadphonesPopup();
        }
        if (permission2 != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    rescode
            );
        } else {
            copyFileOrDir("rawwaves");

        }

    }
}