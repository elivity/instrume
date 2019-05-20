package com.oskiapps.instrume;
/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.res.AssetManager;

public enum LiveEffectEngine {

    INSTANCE;

    // Load native library
    static {
        //System.loadLibrary("fftw3");

        System.loadLibrary("midi");
        System.loadLibrary("autotuner");

    }

    // Native methods
    static native void addAssetMgr(AssetManager assetManager, String theFile, String theFileOut, String theFileOutFx, String filePathMixed, String s);
    static native boolean create();
    static native boolean isAAudioSupported();
    static native boolean setAPI(int apiType);
    static native void setEffectOn(boolean isEffectOn);
    static native void setRecordingDeviceId(int deviceId);
    static native void setPlaybackDeviceId(int deviceId);
    static native void delete();
    static native void stopAll();
    static native long[] drawSth(int width, int height);
    static native boolean setTogglePlaying();
    static native boolean setToggleRecording();
    static native boolean setToggleFender();
    static native boolean setToggleReverb();
    static native boolean setPlayerPosition( long pos);
    static native boolean setVolumeMusic(float volMusic);
    static native boolean setVolumeVocal(float volVoc);

    static native boolean setSaxofonyParams(float reedStif,float reedAperature,float noiseGain,float blowPos,float vibrFreq,float vibrGain,float breathPressure);

    //static native int getSessionId();
    //static native void passAudioSessionId(int sessid);

    static native boolean setReverbDry(float currVocalVol);
    static native boolean setThresholdLevel(float currVocalVol);

    static native boolean setToggleGuitar();

    public static native int getMidiInstrument();

    public static native void setLowEQ(int currLowEQ, float gain1);
    public static native void setMidEQ(int f2, float currMidEQ) ;
    public static native void setHighEQ(int f3, float currMidEQ) ;

    public static native void doEQ(String inFile, String outFile, long start, long end,int freq1,float gain1, int freq2, float gain2, int freq3, float gain3);
    public static native void doReverb(String inFile, String outFile, int start, int end, float dryWet, float decay);
    public static native void doAutoTune(String inFile, String outFile, int start, int end, String scale, float dryWet);
    public static native float[] doAutoTuneRetInt(String inFile, String outFile, int start, int end, String scale, float dryWet);

    public static native void setupLowLatencyParams(int bestBufferSize, int bestSampleRate);

    public static native void setAutoTuneScale(String scale, float dryWet);

    public static native void doGain(String inFile, String outFile, int gotStart, int gotEnd, float theGain);
    public static native void copyFileNative(String inFile, String outFile);

    public static native void lastMix(String absoluteFile, String absoluteFile1, String file, float percentageOrig, float percentageRec);

    public static native boolean setToggleDrums();
    public static native boolean setToggleSaxofony();
    public static native boolean setToggleWurley();
    public static native boolean setToggleBlow();

    public static native boolean setMidiInstrument(int theInstrument);
    public static native float[][] getMyMidiEvents();
    public static native void syncMidiEvents(int whichElement, int whichPitch, float whichStart, float whichEnd);
    public static native void addNoteAt(int whichElement, int whichPitch, float whichX);
    public static native void deleteNoteAt(int whichElement);

    public static native long[] getFreqs(int width, int height);
    public static native void playMidiNote(int integer, boolean onOff);

    public static native boolean setTogglePlayMidi();

    public static native boolean clearMidiSong();

    public static native boolean setToggleMidiRec();

    public static native boolean setToggleRecActive();
}
