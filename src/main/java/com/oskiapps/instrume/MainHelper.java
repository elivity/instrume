package com.oskiapps.instrume;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.net.Uri;
import android.os.StrictMode;
import android.widget.ToggleButton;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


/**
 * Created by Oskar on 26.03.2018.
 */

public class MainHelper {

    public static int resetOtherInstrumentButtons(ToggleButton[] gotToggleBtn, Object obj, String gotName) {
        int retInt = 0;
        int currI = 0;
        for (Field field : obj.getClass().getDeclaredFields()) {
            //field.setAccessible(true); // if you want to modify private fields
            try {
                if(!field.getName().equals(gotName)) {
                    gotToggleBtn[currI].setTextColor(Color.WHITE);
                    field.setBoolean(obj,false);
                } else {
                    gotToggleBtn[currI].setTextColor(Color.GREEN);
                    field.setBoolean(obj,true);

                    retInt = currI;
                }

                System.out.println(field.getName()
                        + " - " + field.getType()
                        + " - " + field.get(obj));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            currI++;

        }
        return retInt;
    }

    public static class ToggleClickStates
    {
        public boolean fender;
        public boolean guitar;
        public boolean other;
        public boolean trumpet;

    };

    protected static void initShareIntent(Activity theActivity, Context theContext) {
        boolean found = false;
        Intent shareIntent = new Intent(Intent.ACTION_VIEW, null);
        //shareIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        // gets the list of intents that can be loaded.
        List<ResolveInfo> resInfo = theContext.getPackageManager().queryIntentActivities(shareIntent, 0);
        if (!resInfo.isEmpty()){
            for (ResolveInfo info : resInfo) {
                System.out.println("oski checksapps" + info.activityInfo.packageName);
                if(info.activityInfo.packageName.contains("com.loopstation.loopstation")){
                    found = true;
                    StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
                    StrictMode.setVmPolicy(builder.build());
                    //shareIntent.setAction(Intent.ACTION_SEND);
                    shareIntent.setPackage(info.activityInfo.packageName);
// validate that the device can open your File!
                    //theActivity.startActivity(shareIntent);

                    PackageManager pm = theContext.getPackageManager();
                    if (shareIntent.resolveActivity(pm) != null) {
                        System.out.println("oski found app");

                        theActivity.startActivity(shareIntent);
                    }
                } else {

                }
                /*if (info.activityInfo.packageName.toLowerCase().contains(effectType) ||
                        info.activityInfo.name.toLowerCase().contains(effectType) ) {
                    share.putExtra(Intent.EXTRA_SUBJECT,  "subject");
                    share.putExtra(Intent.EXTRA_TEXT,     "your text");
                    share.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(myFile)) ); // Optional, just if you wanna share an image.
                    share.setPackage(info.activityInfo.packageName);
                    found = true;
                    break;
                }*/
            }
            if (!found) {
                final String appPackageName = "com.loopstation.loopstation"; // getPackageName() from Context or Activity object
                try {
                    theContext.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                } catch (android.content.ActivityNotFoundException anfe) {
                    theContext.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                }
            }

            //ctx.startActivity(Intent.createChooser(share, "Select"));
        }
    }
    public static void convertToMp3(Context ctx, String fileName) {

    }

    byte [] ShortToByte(short [] input, int elements) {
        int short_index, byte_index;
        int iterations = elements; //input.length;
        byte [] buffer = new byte[iterations * 2];

        short_index = byte_index = 0;

        for(/*NOP*/; short_index != iterations; /*NOP*/)
        {
            buffer[byte_index]     = (byte) (input[short_index] & 0x00FF);
            buffer[byte_index + 1] = (byte) ((input[short_index] & 0xFF00) >> 8);

            ++short_index; byte_index += 2;
        }

        return buffer;
    }
    public static byte [] ShortsToByte(short [] input, int elements) {
        int short_index, byte_index;
        int iterations = elements; //input.length;
        byte [] buffer = new byte[iterations * 2];

        short_index = byte_index = 0;

        for(/*NOP*/; short_index != iterations; /*NOP*/)
        {
            buffer[byte_index]     = (byte) (input[short_index] & 0x00FF);
            buffer[byte_index + 1] = (byte) ((input[short_index] & 0xFF00) >> 8);

            ++short_index; byte_index += 2;
        }

        return buffer;
    }

    public static int getValidSampleRates() {
        for (int rate : new int[] {48000, 44100, 22050, 16000, 11025, 8000}) {  // add the rates you wish to check against
            int bufferSize = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_CONFIGURATION_DEFAULT, AudioFormat.ENCODING_PCM_16BIT);
            if (bufferSize > 0) {
                // buffer size is valid, Sample rate supported
                return rate;
            }
        }
        return 44100;
    }

    public static void doCompression(int start, int end) {
        //save a copy of recording for having a clean copy
        //10 secs = 44100*4*10
        int bytelen = (int)(44.1*4*(end-start));
        if ( bytelen %2 != 0 ) {
            bytelen+=1;
        }
        byte[] insertBytes = new byte[bytelen];
        /*for(int i = 0; i < emptyBytes.length; i++) {
            emptyBytes[i] = 0;
        }*/
        RandomAccessFile accessWaveRec = null;
        //noinspection CaughtExceptionImmediatelyRethrown
        try {
            accessWaveRec = new RandomAccessFile(new File(AudioConstants.filePathRecLong), "rw");
            // ChunkSize

            long modCheck = 44+((int)(44.1*4*start));
            if(modCheck%2!=0) {
                modCheck+=1;
            }
            accessWaveRec.seek(modCheck);//(long) (start*44.1));

            accessWaveRec.read(insertBytes,0,(int)insertBytes.length);
            System.out.println("oski out"+start*44.1 + " start "+ start + " end"+ end +" "+(int)((end-start)*44.1));

            short[] shorts = new short[insertBytes.length / 2];
            ByteBuffer.wrap(insertBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
            short[] compressedShorts = Compress(shorts,0.08,8);
            int[] minMaxInsert = MainHelper.getMinAndMaxFromShorts(compressedShorts,0);

            byte[] compressedBytes = normalizeBuffer(ShortsToByte(compressedShorts,compressedShorts.length),Short.MAX_VALUE/compressedShorts[minMaxInsert[1]]);

            accessWaveRec.write(compressedBytes, 0, compressedBytes.length);

        } catch (IOException ex) {
            // Rethrow but we still close accessWave in our finally
            try {
                throw ex;
            } catch (IOException e) {
                e.printStackTrace();
            }
        } finally {
            if (accessWaveRec != null) {
                try {
                    accessWaveRec.close();
                } catch (IOException ex) {
                    //
                }
            }
        }

    }
    public static short[] Compress(short[] input, double thresholdDb, double ratio)
    {
        double maxDb = thresholdDb - (thresholdDb / ratio);
        double maxGain = Math.pow(10, -maxDb / 20.0);

        for (int i = 0; i < input.length; i++)
        {
            // convert sample values to ABS gain and store original signs
            int signL = input[i] < 0 ? -1 : 1;
            double valL = (double)input[i] / 32768.0;
            if (valL < 0.0)
            {
                valL = -valL;
            }
            if(i + 1 < input.length)
            {
                int signR = input[i + 1] < 0 ? -1 : 1;
                double valR = (double)input[i + 1] / 32768.0;
                if (valR < 0.0)
                {
                    valR = -valR;
                }

                // calculate mono value and compress
                double val = (valL + valR) * 0.5;
                double posDb = -Math.log10(val) * 20.0;
                if (posDb < thresholdDb)
                {
                    posDb = thresholdDb - ((thresholdDb - posDb) / ratio);
                }

                // measure L and R sample values relative to mono value
                double multL = valL / val;
                double multR = valR / val;

                // convert compressed db value to gain and amplify
                val = Math.pow(10, -posDb / 20.0);
                val = val / maxGain;

                // re-calculate L and R gain values relative to compressed/amplified
                // mono value
                valL = val * multL;
                valR = val * multR;

                double lim = 1.5; // determined by experimentation, with the goal
                // being that the lines below should never (or rarely) be hit
                if (valL > lim)
                {
                    valL = lim;
                }
                if (valR > lim)
                {
                    valR = lim;
                }

                double maxval = 32000.0 / lim;

                // convert gain values back to sample values
                input[i] = (short)(valL * maxval);
                input[i] *= (short)signL;
                input[i + 1] = (short)(valR * maxval);
                input[i + 1] *= (short)signR;
            }

        }
        // shortsA = input;
        return input;
    }


    /*public static void copyContentFile(Context ctx,Uri sourceuri, String toFile)
    {
        String sourceFilename= sourceuri.getPath();
        String destinationFilename = toFile;

        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;

        try {
            bis = new BufferedInputStream((ctx.getContentResolver().openInputStream(sourceuri)));
            bos = new BufferedOutputStream(new FileOutputStream(destinationFilename, false));
            byte[] buf = new byte[1024];
            bis.read(buf);
            do {
                bos.write(buf);
            } while(bis.read(buf) != -1);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bis != null) bis.close();
                if (bos != null) bos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }*/

    public static void copyContentFile(Context ctx, Uri gotUri, String toFile) throws IOException {

        InputStream in =  ctx.getContentResolver().openInputStream(gotUri);
        OutputStream out = new FileOutputStream(new File(toFile));
        byte[] buf = new byte[1024];
        int len;
        while((len=in.read(buf))>0){
            out.write(buf,0,len);
        }
        out.close();
        in.close();
    }

    public static boolean isSafeStartEnd(int firstStart, int firstEnd, int secondStart, int secondEnd) {
        //System.out.println("oski firstStart "+ firstStart + " firstEnd " + firstEnd + " secondStart " + secondStart + " secondEnd " +secondEnd);
        if(firstStart >= secondEnd)
        {
            //System.out.println("oski first #1");
            return true;
        } else if(firstEnd <= secondStart) {
            //System.out.println("oski first #2");

            return true;
        }
        return false;
    }

    public static int getMatchingHeight(int gotElementNumber) {
        int tmpStart = (int) (AudioObjects.partialEffects.get(gotElementNumber).start / AudioConstants.drawMsRatio);
        int tmpEnd = (int) (AudioObjects.partialEffects.get(gotElementNumber).end / AudioConstants.drawMsRatio);
        //determine which row
        short whichRow = 0;
        for (int j = 0; j < AudioObjects.partialEffects.size(); j++) {
            int compareStart = (int) (AudioObjects.partialEffects.get(j).start / AudioConstants.drawMsRatio);
            int compareEnd = (int) (AudioObjects.partialEffects.get(j).end / AudioConstants.drawMsRatio);
            if (gotElementNumber != j && j < gotElementNumber) {
                if (!MainHelper.isSafeStartEnd(tmpStart, tmpEnd, compareStart, compareEnd)) {
                    whichRow++;
                }
            }

        }
        return whichRow;
    }

    public static int[] getMinAndMaxFromShorts(short[] shorts, int offset) {
        short maxVal = 0;
        int maxIndex = 0;
        short minVal = 0;
        int minIndex = 0;
        for(int i = offset; i < shorts.length; i++) {
            if(shorts[i] > maxVal) {
                maxVal = shorts[i];
                maxIndex = i;
            } else if(shorts[i] < minVal) {
                minVal = shorts[i];
                minIndex = i;
            }
        }
        return new int[]{minIndex,maxIndex};
    }

    public static int[] getMinAndMaxFromShorts(short[] shorts, int offset, int length) {
        float maxVal = 0;
        int maxIndex = 0;
        float minVal = 0;
        int minIndex = 0;
        for(int i = offset; i < offset+length; i++) {
            if(shorts[i] > maxVal) {
                maxVal = shorts[i];
                maxIndex = i;
            } else if(shorts[i] < minVal) {
                minVal = shorts[i];
                minIndex = i;

            }
            //System.out.println("oski is long enough " + offset + " " + length);
        }
        return new int[]{minIndex,maxIndex};
    }

    public static void deleteRecAudioSnippet(String whichFile,int start, int end) {
        //save a copy of recording for having a clean copy
        //10 secs = 44100*4*10
        int theSteps = 8192;
        int theStepCnt = (int)(AudioConstants.drawPixelRatio*(end-start)) / theSteps;
        byte[] emptyBytes = new byte[theSteps];
        for(int i = 0; i < emptyBytes.length; i++) {
            emptyBytes[i] = 0;
        }

        long AudioPos = 44+((int)(AudioConstants.drawPixelRatio*start));
        if(AudioPos % 2 != 0) {
            AudioPos = AudioPos-1;
        }
        RandomAccessFile accessWaveRec = null;
        //noinspection CaughtExceptionImmediatelyRethrown
        try {
            accessWaveRec = new RandomAccessFile(new File(whichFile), "rw");
            // ChunkSize

            for(int i = 0 ; i < theStepCnt*theSteps; i+=theSteps) {
                accessWaveRec.seek(AudioPos+i);//(long) (start*44.1));

                accessWaveRec.write(emptyBytes, 0, emptyBytes.length);

            }
            System.out.println("oski out"+start*AudioConstants.drawPixelRatio + " start "+ start + " end"+ end +" "+(int)((end-start)*44.1));


        } catch (IOException ex) {
            // Rethrow but we still close accessWave in our finally
            try {
                throw ex;
            } catch (IOException e) {
                e.printStackTrace();
            }
        } finally {
            if (accessWaveRec != null) {
                try {
                    accessWaveRec.close();
                } catch (IOException ex) {
                    //
                }
            }
        }
    }

    public static void insertAudio(File origWav, File insertWav, File outWav, long audioPos, boolean mix) throws IOException {


        byte[] insertBytes = new byte[(int)insertWav.length()];
        RandomAccessFile accessWaveSnippet = null;
        //noinspection CaughtExceptionImmediatelyRethrown
        try {
            accessWaveSnippet = new RandomAccessFile(insertWav, "rw");
            // ChunkSize

            accessWaveSnippet.read(insertBytes,0,(int)insertBytes.length);

        } catch (IOException ex) {
            // Rethrow but we still close accessWave in our finally
            throw ex;
        } finally {
            if (accessWaveSnippet != null) {
                try {
                    accessWaveSnippet.close();
                } catch (IOException ex) {
                    //
                }
            }
        }


        byte[] origSnippets = new byte[insertBytes.length];
        byte[] mergedSnippets = new byte[origSnippets.length];
        long AudioPos = (long) (4*audioPos*44.1000);
        if(AudioPos % 2 != 0) {
            AudioPos = AudioPos-1;
        }
        RandomAccessFile accessWave = null;
        //noinspection CaughtExceptionImmediatelyRethrown
        try {

            accessWave = new RandomAccessFile(origWav, "rw");
            // ChunkSize

            accessWave.seek((long) ((long) (AudioPos)));
            accessWave.read(origSnippets,0,(int)insertWav.length());

            short[] shortsOrig = new short[origSnippets.length/2];
            ByteBuffer.wrap(origSnippets).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortsOrig);
            short[] shortsInsert = new short[insertBytes.length/2];
            ByteBuffer.wrap(insertBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortsInsert);
            //Original file is already limited to 50%;
            //int[] minMaxOrig = MainHelper.getMinAndMaxFromShorts(shortsOrig,22);
            //double normMaxOrig = ((minMaxOrig[1] / (Short.MAX_VALUE)));
            int[] minMaxInsert = MainHelper.getMinAndMaxFromShorts(shortsInsert,22);
            //double normAbsMaxInsert = 1/((shortsInsert[minMaxInsert[1]] / (Short.MAX_VALUE)));

            short[] mergedShort = new short[origSnippets.length/2];

            System.out.println("oski shortsmax" + Short.MAX_VALUE + " normmax "+ " minmax "+ shortsInsert[minMaxInsert[1]]);
            if(mix) {
                double normMaxInsert = 1/((shortsInsert[minMaxInsert[1]] / (Short.MAX_VALUE*0.4)));

                for(int i = 22; i < mergedShort.length; i++) {
                    mergedShort[i] = (short)(shortsOrig[i]+shortsInsert[i]*normMaxInsert);
                }
            } else {
                for(int i = 22; i < mergedShort.length; i++) {
                    double normMaxInsert = 1/((shortsInsert[minMaxInsert[1]] / (Short.MAX_VALUE*0.9)));
                    mergedShort[i] = (short) (shortsInsert[i]*normMaxInsert);
                }
            }
            ByteBuffer.wrap(mergedSnippets).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(mergedShort);
        } catch (IOException ex) {
            // Rethrow but we still close accessWave in our finally
            throw ex;
        } finally {
            if (accessWave != null) {
                try {
                    accessWave.close();
                } catch (IOException ex) {
                    //
                }
            }
        }

            //save a copy of recording for having a clean copy
            System.out.println("oski " +AudioPos);
            RandomAccessFile accessWaveRec = null;
            //noinspection CaughtExceptionImmediatelyRethrown
            try {
                accessWaveRec = new RandomAccessFile(new File(AudioConstants.filePathRecLong), "rw");
                // ChunkSize

                accessWaveRec.seek((long) (AudioPos));

                accessWaveRec.write(insertBytes, 0, insertBytes.length);

            } catch (IOException ex) {
                // Rethrow but we still close accessWave in our finally
                throw ex;
            } finally {
                if (accessWaveRec != null) {
                    try {
                        accessWaveRec.close();
                    } catch (IOException ex) {
                        //
                    }
                }
            }


        System.out.println("oski " +AudioPos);
        RandomAccessFile accessWaveMerged = null;
        //noinspection CaughtExceptionImmediatelyRethrown
        try {
            accessWaveMerged = new RandomAccessFile(outWav, "rw");
            // ChunkSize

            accessWaveMerged.seek((long) (AudioPos));
            accessWaveMerged.write(mergedSnippets, 0, mergedSnippets.length);
            System.out.println("oski acceswavemerged passed "+ mergedSnippets.length);
        } catch (IOException ex) {
            // Rethrow but we still close accessWave in our finally
            throw ex;
        } finally {
            if (accessWaveMerged != null) {
                try {
                    accessWaveMerged.close();
                } catch (IOException ex) {
                    //
                }
            }
        }


    }

    public static void mixFiles(File origWave, File recWave, File mixed) throws IOException {

        //clean mix #1
        //LiveEffectEngine.copyFileNative(origWave.getAbsolutePath(), AudioConstants.filePathWavNormalized);
        //MainHelper.copyFile(origWave, wavTmp);
        //normalize mix #1
        //new todo
        //normalizeFile(wavTmp, AudioConstants.percentageOrig);
        //File wavTmp = new File(AudioConstants.filePathWav);
        LiveEffectEngine.lastMix(origWave.getAbsolutePath(),recWave.getAbsolutePath(), mixed.getAbsolutePath(),(float)AudioConstants.percentageOrig,(float) AudioConstants.percentageRec);

        System.out.println("oski "+ AudioConstants.percentageOrig + " " +AudioConstants.percentageRec);
        MainHelper.writeWavHeader2(origWave,(short)2,AudioConstants.deviceSampleRate,(short)16);


        //mixing
        /*InputStream origIs = new FileInputStream(wavTmp);
        BufferedInputStream origBis = new BufferedInputStream(origIs);
        DataInputStream origDis = new DataInputStream(origBis);

        InputStream recIs = new FileInputStream(recWave);
        BufferedInputStream recBis = new BufferedInputStream(recIs);
        DataInputStream recDis = new DataInputStream(recBis);



        byte[] bufferOrig = new byte[32786];
        byte[] bufferRec = new byte[32786];

        System.out.println("oskistarted");

        FileOutputStream mixedOut = new FileOutputStream(mixed);
        //writeWavHeader(mixedOut,(short)2,44100,(short)16);
        int skipFirst = 44;
        while(origDis.read(bufferOrig, 0, bufferOrig.length) != -1) {
            if(recDis.read(bufferRec, 0, bufferRec.length) != -1){

            }
            mixedOut.write(mixBuffers(bufferOrig, bufferRec),skipFirst,bufferOrig.length-skipFirst);
            //System.out.println("oski bufflen " + bufferOrig.length + " " + bufferRec.length);
            skipFirst=0;
        }
        if(mixedOut != null) {
            mixedOut.close();

        }
        if(origDis != null) {
            origIs.close();
            origDis.close();


        }
        if(origDis != null) {
            recIs.close();
            recDis.close();
        }*/
        //updateWavHeader(new File(AudioConstants.filePathMixed));
        System.out.println("oskifinished");

    }

    private static byte[] mixBuffers(byte[] bufferA, byte[] bufferB) {
        byte[] array = new byte[bufferA.length];

        for (int i=0; i<bufferA.length; i+=2) {
            short buf1A = bufferA[i+1];
            short buf2A = bufferA[i];
            buf1A = (short) ((buf1A & 0xff) << 8);
            buf2A = (short) (buf2A & 0xff);

            short buf1B = bufferB[i+1];
            short buf2B = bufferB[i];
            buf1B = (short) ((buf1B & 0xff) << 8);
            buf2B = (short) (buf2B & 0xff);

            short buf1C = (short) ((buf1A + buf1B* AudioConstants.percentageRec));
            short buf2C = (short) ((buf2A + buf2B*AudioConstants.percentageRec));

            short res = (short) (buf1C + buf2C);

            array[i] = (byte) res;
            array[i+1] = (byte) (res >> 8);
        }

        return array;
    }

    public static void normalizeRec(File toNormalizeRec, double normalizePercentage) throws IOException {

        InputStream toNormalizeIs = new FileInputStream(AudioConstants.filePathRecLong);
        BufferedInputStream toNormalizeBis = new BufferedInputStream(toNormalizeIs);
        DataInputStream toNormalizeDis = new DataInputStream(toNormalizeBis);


        byte[] toNormalizeBuffer = new byte[1024];


        FileOutputStream sameFileOut = new FileOutputStream(toNormalizeRec);
        //writeWavHeader(mixedOut,(short)2,44100,(short)16);
        int skipFirst = 44;
        while(toNormalizeDis.read(toNormalizeBuffer, 0, toNormalizeBuffer.length) != -1){
            sameFileOut.write(normalizeBuffer(toNormalizeBuffer,normalizePercentage),skipFirst,toNormalizeBuffer.length-skipFirst);
            skipFirst=0;
        }
        toNormalizeIs.close();
        toNormalizeBis.close();
        sameFileOut.close();
        //updateWavHeader(new File(AudioConstants.filePathMixed));

    }
    public static void normalizeFile(File toNormalizeFile, double normalizePercentage) throws IOException {

        InputStream toNormalizeIs = new FileInputStream(AudioConstants.filePathWav);
        BufferedInputStream toNormalizeBis = new BufferedInputStream(toNormalizeIs);
        DataInputStream toNormalizeDis = new DataInputStream(toNormalizeBis);


        byte[] toNormalizeBuffer = new byte[32786];


        FileOutputStream sameFileOut = new FileOutputStream(toNormalizeFile);
        //writeWavHeader(mixedOut,(short)2,44100,(short)16);
        int skipFirst = 44;
        while(toNormalizeDis.read(toNormalizeBuffer, 0, toNormalizeBuffer.length) != -1){
            sameFileOut.write(normalizeBuffer(toNormalizeBuffer,normalizePercentage),skipFirst,toNormalizeBuffer.length-skipFirst);
            skipFirst=0;
        }
        toNormalizeIs.close();
        toNormalizeBis.close();
        sameFileOut.close();
        //updateWavHeader(new File(AudioConstants.filePathMixed));

    }
    private static byte[] normalizeBuffer(byte[] toNormalizeBuffer, double normalizePercentage) {
        byte[] array = new byte[toNormalizeBuffer.length];

        for (int i=0; i<toNormalizeBuffer.length; i+=2) {
            short buf1A = toNormalizeBuffer[i+1];
            short buf2A = toNormalizeBuffer[i];
            buf1A = (short) ((buf1A & 0xff) << 8);
            buf2A = (short) (buf2A & 0xff);

            short res = (short) ((buf1A + buf2A)*normalizePercentage);

            array[i] = (byte) res;
            array[i+1] = (byte) (res >> 8);
        }

        return array;
    }

    public static void copyFile(File sourceFile, File destFile) throws IOException {
        if (!sourceFile.getParentFile().exists()) {
            sourceFile.getParentFile().mkdirs();
            System.out.println("oski dbg "+sourceFile.getParentFile().getAbsolutePath());
        }

        if (!sourceFile.exists()) {
            System.out.println("oski dbg2 "+sourceFile.getParentFile().getAbsolutePath());
            sourceFile.createNewFile();
        }

        if (!destFile.getParentFile().exists()) {
            destFile.getParentFile().mkdirs();
            System.out.println("oski dbg "+destFile.getParentFile().getAbsolutePath());
        }

        if (!destFile.exists()) {
            System.out.println("oski dbg2 "+destFile.getParentFile().getAbsolutePath());
            destFile.createNewFile();
        }

        FileChannel source = null;
        FileChannel destination = null;

        try {
            System.out.println("oski source size starting");

            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
            System.out.println("oski source size"+destFile.getAbsolutePath() + " " +source.size());
        } finally {
            if (source != null) {
                source.close();
            }
            if (destination != null) {
                destination.close();
            }
            System.out.println("oski source size end");

        }
    }



    public static void readPCM(String path1, String path2, DataOutputStream toOutputStream, double gotOffset) {
        double[] result1 = null;
        double[] result2 = null;
        boolean done = false;
        try {
            File file1 = new File(path1);
            InputStream in1 = new FileInputStream(file1);
            int bufferSize1 = (int) (file1.length() / 2);
            File file2 = new File(path2);
            InputStream in2 = new FileInputStream(file2);
            int bufferSize2 = (int) (file1.length() / 2);

            DataInputStream is1 = new DataInputStream(in1);
            DataInputStream is2 = new DataInputStream(in2);

            int size = 2048;
            byte[] bytes = new byte[size];
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file1));

            byte[] bytes2 = new byte[size];
            BufferedInputStream buf2 = new BufferedInputStream(new FileInputStream(file2));
            int skipFirst44 = 22;
            double myOffset = gotOffset * 44.1;
System.out.println("oski"+ myOffset + " " + gotOffset);
            double thePos = 0;

            while (buf.read(bytes, 0, bytes.length) != -1) {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                buf2.read(bytes2, 0, bytes2.length);

                short[] shorts = new short[bytes.length/2];
                // to turn bytes to shorts as either big endian or little endian.
                ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
                short[] shorts2 = new short[bytes.length/2];
                // to turn bytes to shorts as either big endian or little endian.
                ByteBuffer.wrap(bytes2).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts2);
                for (int i = skipFirst44; i < shorts.length; i++) {
                    thePos++;
                    short outWrite;
                    if(thePos >= myOffset) {
                        outWrite = (short) ((shorts[i]/3 + shorts2[i]*2));

                    } else {
                        outWrite = (short) ((shorts[i]/3));
                    }
                    //short tmpshort = (short) bytes[i];
                    //os.write((tmpshort >> 8) & 0xff);
                    //os.write(tmpshort & 0xff);
                    os.write(shortToBytes(outWrite));
                }
                //System.out.println("oski "+thePos);

                toOutputStream.write(os.toByteArray());
                //already skipped
                skipFirst44=0;
            }
            buf.close();


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }



    public static short bytesToShort(byte[] bytes) {
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getShort();
    }

    public static byte[] shortToBytes(short value) {
        return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value).array();
    }


    public static void makeEmptySameLength(String gotFile, String toFile) throws IOException {
        //make an empty file for filePathRecLong
        File fromFile = new File(gotFile);
        File toWriteFile = new File(toFile);
        //clear old File if exists;
        PrintWriter pw = new PrintWriter(toFile);
        pw.close();

        DataOutputStream wavOut = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(toWriteFile)));
        MainHelper.writeWavHeader(wavOut,(short)2,AudioConstants.deviceSampleRate,(short)16);
        RandomAccessFile accessWaveCopy = null;

        //noinspection CaughtExceptionImmediatelyRethrown
        try {
            accessWaveCopy = new RandomAccessFile(toWriteFile, "rw");
            accessWaveCopy.setLength(fromFile.length());

            // ChunkSize


        } catch (IOException ex) {
            // Rethrow but we still close accessWave in our finally

            throw ex;
        } finally {
            if (accessWaveCopy != null) {
                try {
                    accessWaveCopy.close();
                } catch (IOException ex) {
                    //
                }
            }
        }

    }



    public static void writeWavHeader2(File wav, short channels, int sampleRate, short bitDepth) throws IOException {
        // Convert the multi-byte integers to raw bytes in little endian format as required by the spec
        byte[] sizes = ByteBuffer
                .allocate(8)
                .order(ByteOrder.LITTLE_ENDIAN)
                // There are probably a bunch of different/better ways to calculate
                // these two given your circumstances. Cast should be safe since if the WAV is
                // > 4 GB we've already made a terrible mistake.
                .putInt((int) (wav.length() - 8)) // ChunkSize
                .putInt((int) (wav.length() - 44)) // Subchunk2Size
                .array();
        byte[] littleBytes = ByteBuffer
                .allocate(14)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putShort(channels)
                .putInt(sampleRate)
                .putInt(sampleRate * channels * (bitDepth / 8))
                .putShort((short) (channels * (bitDepth / 8)))
                .putShort(bitDepth)
                .array();
        byte[] bigBytes = new byte[]{
                // RIFF header
                'R', 'I', 'F', 'F', // ChunkID
                sizes[0], sizes[1], sizes[2], sizes[3], // ChunkSize (must be updated later)
                'W', 'A', 'V', 'E', // Format
                // fmt subchunk
                'f', 'm', 't', ' ', // Subchunk1ID
                16, 0, 0, 0, // Subchunk1Size
                1, 0, // AudioFormat
                littleBytes[0], littleBytes[1], // NumChannels
                littleBytes[2], littleBytes[3], littleBytes[4], littleBytes[5], // SampleRate
                littleBytes[6], littleBytes[7], littleBytes[8], littleBytes[9], // ByteRate
                littleBytes[10], littleBytes[11], // BlockAlign
                littleBytes[12], littleBytes[13], // BitsPerSample
                // data subchunk
                'd', 'a', 't', 'a', // Subchunk2ID
                sizes[4], sizes[5], sizes[6], sizes[7] // Subchunk2Size (must be updated later)
        };


        RandomAccessFile accessWave = null;
        //noinspection CaughtExceptionImmediatelyRethrown
        try {
            accessWave = new RandomAccessFile(wav, "rw");
            // ChunkSize
            accessWave.seek(0);
            accessWave.write(bigBytes, 0, 44);
        } catch (IOException ex) {
            // Rethrow but we still close accessWave in our finally
            throw ex;
        } finally {
            if (accessWave != null) {
                try {
                    accessWave.close();
                } catch (IOException ex) {
                    //
                }
            }
        }

    }


    public static void writeWavHeader(OutputStream out, short channels, int sampleRate, short bitDepth) throws IOException {
        // Convert the multi-byte integers to raw bytes in little endian format as required by the spec
        byte[] littleBytes = ByteBuffer
                .allocate(14)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putShort(channels)
                .putInt(sampleRate)
                .putInt(sampleRate * channels * (bitDepth / 8))
                .putShort((short) (channels * (bitDepth / 8)))
                .putShort(bitDepth)
                .array();

        // Not necessarily the best, but it's very easy to visualize this way
        out.write(new byte[]{
                // RIFF header
                'R', 'I', 'F', 'F', // ChunkID
                0, 0, 0, 0, // ChunkSize (must be updated later)
                'W', 'A', 'V', 'E', // Format
                // fmt subchunk
                'f', 'm', 't', ' ', // Subchunk1ID
                16, 0, 0, 0, // Subchunk1Size
                1, 0, // AudioFormat
                littleBytes[0], littleBytes[1], // NumChannels
                littleBytes[2], littleBytes[3], littleBytes[4], littleBytes[5], // SampleRate
                littleBytes[6], littleBytes[7], littleBytes[8], littleBytes[9], // ByteRate
                littleBytes[10], littleBytes[11], // BlockAlign
                littleBytes[12], littleBytes[13], // BitsPerSample
                // data subchunk
                'd', 'a', 't', 'a', // Subchunk2ID
                0, 0, 0, 0, // Subchunk2Size (must be updated later)
        });
    }

    /**
     * Updates the given wav file's header to include the final chunk sizes
     *
     * @param wav The wav file to update
     * @throws IOException
     */
    public static void updateWavHeader(File wav) throws IOException {
        byte[] sizes = ByteBuffer
                .allocate(8)
                .order(ByteOrder.LITTLE_ENDIAN)
                // There are probably a bunch of different/better ways to calculate
                // these two given your circumstances. Cast should be safe since if the WAV is
                // > 4 GB we've already made a terrible mistake.
                .putInt((int) (wav.length() - 8)) // ChunkSize
                .putInt((int) (wav.length() - 44)) // Subchunk2Size
                .array();

        RandomAccessFile accessWave = null;
        //noinspection CaughtExceptionImmediatelyRethrown
        try {
            accessWave = new RandomAccessFile(wav, "rw");
            // ChunkSize
            accessWave.seek(4);
            accessWave.write(sizes, 0, 4);

            // Subchunk2Size
            accessWave.seek(40);
            accessWave.write(sizes, 4, 4);
        } catch (IOException ex) {
            // Rethrow but we still close accessWave in our finally
            throw ex;
        } finally {
            if (accessWave != null) {
                try {
                    accessWave.close();
                } catch (IOException ex) {
                    //
                }
            }
        }
    }
}
