package com.oskiapps.instrume;

/**
 * Created by Oskar on 27.03.2018.
 */

public  class AudioConstants {
    //todo replace with real ids
    public static String admobId = "";
    public static String admobAdId = "";

    public static int recordedOffset = 0;
    public static int mPlayerPosition = 0;

    public static String internalPath = "";
    public static String dirPath = internalPath+"/RapOnMp3/";
    public static String filePathMp3 = "";
    public static String filePathWav = internalPath+"/RapOnMp3/kaytranada.wav";
    public static String filePathWavNormalized = internalPath+"/RapOnMp3/kaytranada_normalized.wav";

    public static String filePathRec = internalPath+"/RapOnMp3/recording_1.wav";;
    public static String filePathRecLong = internalPath+"/RapOnMp3/kaytranada_rec_long.wav";
    public static String filePathRecLongFx = internalPath+"/RapOnMp3/rec_long_fx.wav";


    public static String filePathMixed = internalPath+"/RapOnMp3/kaytranada_mixed.wav";

    public static double percentageOrig = 0.5;
    public static double percentageRec = 1.0;

    public static String filePathChosenMp3 = "";
    public static String filePathExported = "";

    public static int markerStart = 0;
    public static int markerEnd = 0;
    public static byte markerState = 0;

    public static int prevMarkerStart = 0;
    public static int prevMarkerEnd = 0;

    public static long livePositionBytes = 0;
    public static long liveVolumeDb = 0;
    public static long liveIsRecording = 0;
    public static long livePitch = 0;


    public static int deviceSampleRate = 48000;
    public static int deviceBufferSize = 240;

    public static float drawMsRatio = (24000f/deviceSampleRate)*1000/2;
    public static float drawPixelRatio = (deviceSampleRate/1000f)*4;
    public static int bitmapMaxWidth = 0;
    public static int drawBuffer = (int) (deviceSampleRate/4f);
    public static float midiRecThreshold = 0.17f;

    public static float[][] midiEvents;
    public static boolean midiRecActive = false;
    public static int markedMidiNote = -1;
    public static int midiEditMode = 0;

    public static int skipCommercialCnt = 0;
    public static String[] instrumentList = {"Acoustic grand piano",
            "Acoustic piano",
            "Eectric grand piano",
            "Honky tonk piano",
            "Electric piano 1",
            "Electric piano 2",
            "Harpsicord",
            "Clavinet Chromatic percussion",
            "Celesta",
            "Music box",
            "Vibraphone",
            "Marimba",
            "Xylophone",
            "Tubular bell",
            "Dulcimer Organ",
            "Hammond / drawbar organ",
            "Percussive organ",
            "Rock organ",
            "Church organ",
            "Reed organ",
            "Accordion",
            "Harmonica",
            "Tango accordion	Guitar",
            "Nylon string acoustic guitar",
            "Steel string acoustic guitar",
            "Jazz electric guitar",
            "Clean electric guitar",
            "Muted electric guitar",
            "Overdriven guitar",
            "Distortion guitar",
            "Guitar harmonics Bass",
            "Acoustic bass",
            "Fingered electric bass",
            "Picked electric bass",
            "Fretless bass",
            "Slap bass 1",
            "Slap bass 2",
            "Synth bass 1",
            "Synth bass 2 Strings",
            "Violin",
            "Viola",
            "Cello",
            "Contrabass",
            "Tremolo strings",
            "Pizzicato strings",
            "Orchestral strings / harp",
            "Timpani	Ensemble",
            "String ensemble 1",
            "String ensemble 2 / slow strings",
            "Synth strings 1",
            "Synth strings 2",
            "Choir aahs",
            "Voice oohs",
            "Synth choir / voice",
            "Orchestra hit Brass",
            "Trumpet",
            "Trombone",
            "Tuba",
            "Muted trumpet",
            "French horn",
            "Brass ensemble",
            "Synth brass 1",
            "Synth brass 2	Reed",
            "Soprano sax",
            "Alto sax",
            "Tenor sax",
            "Baritone sax",
            "Oboe",
            "English horn",
            "Bassoon",
            "Clarinet Pipe",
            "Piccolo",
            "Flute",
            "Recorder",
            "Pan flute",
            "Bottle blow / blown bottle",
            "Shakuhachi",
            "Whistle",
            "Ocarina	Synth lead",
            "Synth square wave",
            "Synth saw wave",
            "Synth calliope",
            "Synth chiff",
            "Synth charang",
            "Synth voice",
            "Synth fifths saw",
            "Synth brass and lead Synth pad",
            "Fantasia / new age",
            "Warm pad",
            "Polysynth",
            "Space vox / choir",
            "Bowed glass",
            "Metal pad",
            "Halo pad",
            "Sweep pad	Synth effects",
            "Ice rain",
            "Soundtrack",
            "Crystal",
            "Atmosphere",
            "Brightness",
            "Goblins",
            "Echo drops / echoes",
            "Sci fi Ethnic",
            "Sitar",
            "Banjo",
            "Shamisen",
            "Koto",
            "Kalimba",
            "Bag pipe",
            "Fiddle",
            "Shanai Percussive",
            "Tinkle bell",
            "Agogo",
            "Steel drums",
            "Woodblock",
            "Taiko drum",
            "Melodic tom",
            "Synth drum",
            "Reverse cymbal Sound effects",
            "Guitar fret noise",
            "Breath noise",
            "Seashore",
            "Bird tweet",
            "Telephone ring",
            "Helicopter",
            "Applause",
            "Gunshot"};

    static void buildStrings() {
        dirPath = internalPath+"/RapOnMp3/";
        filePathMp3 = "";
        filePathWav = internalPath+"/RapOnMp3/kaytranada.wav";
        filePathWavNormalized = internalPath+"/RapOnMp3/kaytranada_tmp.wav";

        filePathRec = internalPath+"/RapOnMp3/recording_1.wav";;
        filePathMixed = internalPath+"/RapOnMp3/kaytranada_mixed.wav";
        filePathRecLong = internalPath+"/RapOnMp3/kaytranada_rec_long.wav";
        filePathRecLongFx = internalPath+"/RapOnMp3/rec_long_fx.wav";
        filePathExported = internalPath+"/RapOnMp3/RapOnMp3-export.mp3";

    }
}
