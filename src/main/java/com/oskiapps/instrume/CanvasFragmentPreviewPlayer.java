package com.oskiapps.instrume;


import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

/**
 * Created by Oskar on 30.09.2018.
 */

public class CanvasFragmentPreviewPlayer extends Fragment {

    public DrawClass drawClass;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_canvas_preview_player, container, false);
        RelativeLayout relativeLayout = (RelativeLayout) rootView.findViewById(R.id.rect_preview_player);
        drawClass = new DrawClass(getActivity());
        relativeLayout.addView(drawClass);

        return rootView;
    }

    public static interface OnCompleteListener {
        public abstract void onComplete();
    }

    private OnCompleteListener mListener;

    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            this.mListener = (OnCompleteListener)context;

        }
        catch (final ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnCompleteListener");
        }
    }
    private ScaleGestureDetector mScaleDetector;
    private GestureDetector mScrollDetector;
    private float mScaleFactor = 1.0f;

    private Bitmap bitmap;
    public class DrawClass extends View {
        Canvas drCanvas;
        Paint recPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint mp3Paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint markerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        Paint clearPaint = new Paint();
        Paint currentCursorPaint = new Paint();
        Rect rect = new Rect(0, 0, 0, 0);
        public int initCanvWidth = 0;
        public int initCanvHeight = 0;

        Context context;

        double b0,b1,b2,a0,a1,a2, alpha, cf;
        public DrawClass(Context ctx) {
            super(ctx);
            context = ctx;
            clearPaint.setColor(getResources().getColor(R.color.colorPrimaryLight));
            recPaint.setColor(getResources().getColor(R.color.colorRed));
            markerPaint.setColor(getResources().getColor(R.color.colorRed));
            markerPaint.setTextSize(72f);
            currentCursorPaint.setColor(getResources().getColor(R.color.colorRed));

            mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
            mScrollDetector = new GestureDetector(context, new ScrollListener());
        }
        int initKeyboardHeight = 0;
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            if (bitmap != null) {
                bitmap .recycle();
            }
            drCanvas= new Canvas();
            if(h > 0) {

                bitmap = Bitmap.createBitmap(w/2-10, h, Bitmap.Config.ARGB_8888);

            }
            drCanvas.setBitmap(bitmap);


            if(initCanvWidth == 0) {
                initCanvWidth=w;
                initCanvHeight=h;
                initKeyboardHeight = getRootView().findViewById(R.id.canvasFragmentAutoTuneLive).getLayoutParams().height;
            }
            mp3Paint.setAntiAlias(true);
            recPaint.setAntiAlias(true);

            //drawFromFile(AudioConstants.filePathWav,(int)(drCanvas.getHeight()/2), AudioConstants.markerStart, AudioConstants.markerEnd, 0, mp3Paint);
            //drawFromFile(AudioConstants.filePathRecLong, (int)(drCanvas.getHeight()/2),(int)(drCanvas.getHeight()), AudioConstants.markerStart, AudioConstants.markerEnd, recPaint);

            mContentRect = new Rect(0,0,w,h);


            Paint helpPaint = new Paint();
            helpPaint.setColor(Color.WHITE);
            helpPaint.setTextSize(36f);

            Typeface plain = ResourcesCompat.getFont(context,R.font.roboto_thin);
            Typeface bold = Typeface.create(plain, Typeface.NORMAL);
            helpPaint.setTypeface(bold);

            //drCanvas.drawRect(0,0,drCanvas.getWidth(),drCanvas.getHeight(),clearPaint);
            drCanvas.drawText("#1 Use the keyboard and have fun!", 25,mContentRect.height()-80,helpPaint);
            drCanvas.drawText("#2 ^ Click to use your voice to play the keyboard", 25,75,helpPaint);
            drCanvas.drawText("(You will need headphones or external speakers for that)", 25,105,helpPaint);
            drCanvas.drawText("#3 Adjust the threshold in noisy environments", 25,150,helpPaint);

            AudioPerPixel = (float)((float)(AudioConstants.markerEnd-AudioConstants.markerStart) / (float)mContentRect.width());
            //AudioConstants.prevMarkerStart = AudioConstants.markerStart;
            //AudioConstants.prevMarkerEnd = AudioConstants.markerStart + 10;

            //drawTrackFromFile(AudioConstants.filePathRecLong, AudioConstants.markerStart, AudioConstants.markerEnd, recPaint);


            //Executor executor = Executors.newSingleThreadExecutor();
            //SimpleDrawFiles simpleDrawer = new SimpleDrawFiles(context);
            //executor.execute(simpleDrawer);


            volumeRects = new Rect[25];
            thresholdRect = new Rect();
            thresholdRect.left = 0;
            thresholdRect.right = 10;
            thresholdRect.top = 0;
            thresholdRect.bottom = 130;
            for(int i = 0; i < 25; i++) {
                volumeRects[i] = new Rect(0, (int)(mContentRect.height()-(i*mContentRect.height())),(int)(mContentRect.width()),(int)(i*mContentRect.height() - mContentRect.height()));
            }

            //mListener.onComplete();
        }

        public class SimpleDrawFiles implements Runnable {
            Context theContext;
            public SimpleDrawFiles(Context ctx) {
                if(theContext == null) {
                    theContext = ctx;
                }
            }
            public void run() {
                drawFromFile2(AudioConstants.filePathWav, (int) (drCanvas.getHeight() / 2), 0, mp3Paint);
                drawFromFile2(AudioConstants.filePathRecLong, (int) (drCanvas.getHeight() / 2), (int) (drCanvas.getHeight()), recPaint);
            }
        }
        long audioFullLen = 1;
        public float drawFromFile2(String whichFile, int whichHeight, int offsetY, Paint whichPaint) {
            File file = new File(whichFile);
            FileInputStream fin = null;
            int ch;
            deleteRec();
            long fileLen = file.length();
            if(whichPaint == mp3Paint) {
                audioFullLen = fileLen;

            }
            float audioRatio = (float)(mContentRect.width()/(fileLen/(7104f*16)));
            byte[] bb = new byte[(int) (7104*8)];
            float tmpCnt = 0;

            try {
                fin = new FileInputStream(file);
                int skipCnt = 0;
                while ((ch = fin.read(bb, 0, bb.length)) != -1) {
                    if (skipCnt % 2 == 0) {
                        short[] shortsOrig = new short[bb.length / 2];
                        ByteBuffer.wrap(bb).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortsOrig);
                        int[] minMax = MainHelper.getMinAndMaxFromShorts(shortsOrig, 0);
                        if (whichPaint == mp3Paint) {
                            mp3Paint.setShader(new LinearGradient(0, whichHeight / 2, 0, (int) ((whichHeight + offsetY + (float) shortsOrig[minMax[1]] / Short.MAX_VALUE * whichHeight) / 2), Color.rgb(255, 235, 59), Color.rgb(255, 87, 34), Shader.TileMode.MIRROR));
                        } else if (whichPaint == recPaint) {
                            recPaint.setShader(new LinearGradient(0, (int) (whichHeight * 1.5), 0, (int) ((whichHeight + offsetY + (float) shortsOrig[minMax[1]] / Short.MAX_VALUE * whichHeight) / 2), Color.rgb(210, 253, 246), Color.rgb(3, 7, 55), Shader.TileMode.MIRROR));
                        }

                        //drCanvas.drawRect(tmpCnt,drCanvas.getHeight()/2+shortsOrig[minMax[1]],tmpCnt+1,drCanvas.getHeight()/2+shortsOrig[minMax[0]],new Paint());
                        drCanvas.drawRect(tmpCnt, (int) ((whichHeight + offsetY + (float) shortsOrig[minMax[1]] / Short.MAX_VALUE * whichHeight) / 2), tmpCnt + 1, (int) ((whichHeight + offsetY + (float) shortsOrig[minMax[0]] / Short.MAX_VALUE * whichHeight) / 2), whichPaint);

                        tmpCnt+=audioRatio;
                    }

                    skipCnt++;

                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                // close the stream using close method
                try {

                    if (fin != null) {
                        fin.close();
                    }
                } catch (IOException ioe) {
                    System.out.println("Error while closing stream: " + ioe);
                }
            }
            return tmpCnt;
        }
        /**
         * Sets the text size for a Paint object so a given string of text will be a
         * given width.
         *
         * @param paint
         *            the Paint to set the text size for
         * @param desiredHeight
         *            the desired width
         * @param text
         *            the text that should be that width
         */
        public void setTextSizeForHeight(Paint paint, float desiredHeight, String text) {
            // Pick a reasonably large value for the test. Larger values produce
            // more accurate results, but may cause problems with hardware
            // acceleration. But there are workarounds for that, too; refer to
            // http://stackoverflow.com/questions/6253528/font-size-too-large-to-fit-in-cache
            final float testTextSize = 96f;

            // Get the bounds of the text, using our testTextSize.
            paint.setTextSize(testTextSize);
            Rect bounds = new Rect();
            paint.getTextBounds(text, 0, text.length(), bounds);

            // Calculate the desired size as a proportion of our testTextSize.
            float desiredTextSize = testTextSize * desiredHeight / bounds.height();

            // Set the paint for that size.
            paint.setTextSize(desiredTextSize);
        }

        protected int currentMarkerPos = 0;

        float translX;
        int dx;
        public void deleteRec() {
            drCanvas.drawRect(0,drCanvas.getHeight()/2,drCanvas.getWidth(),drCanvas.getHeight(),clearPaint);
        }

        public void drawTrackFromFile(String whichFile, int start, int end, Paint whichPaint) {
            deleteRec();


            RandomAccessFile accessWaveMerged = null;
            try {
                int tmpCnt = 0;
                int partLen = (int) (44.1 * 4 * (end - start));
                float audioPerPixel = partLen / mContentRect.width();

                //System.out.println("oski " +AudioPos);
                //noinspection CaughtExceptionImmediatelyRethrown

                accessWaveMerged = new RandomAccessFile(whichFile, "rw");
                byte[] insertBytes = new byte[(int) (44.1 * 4 * (end - start))];
                // ChunkSize
                long AudioPos = (long) (4 * start * 44.1000);
                if (AudioPos % 2 != 0) {
                    AudioPos = AudioPos - 1;
                }
                accessWaveMerged.seek((long) (AudioPos));
                accessWaveMerged.read(insertBytes, 0, (int) insertBytes.length);
                for (int i = 0; i < insertBytes.length - audioPerPixel; i += audioPerPixel) {
                    byte[] byteSnippet = new byte[(int)audioPerPixel];
                    System.arraycopy(insertBytes,i,byteSnippet,0,(int)audioPerPixel);
                    short[] shortsOrig = new short[byteSnippet.length / 2];
                    ByteBuffer.wrap(byteSnippet).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortsOrig);
                    int[] minMax = MainHelper.getMinAndMaxFromShorts(shortsOrig, 0);
                    drCanvas.drawRect(tmpCnt, (int)
                                    ((mContentRect.height()/2 +  (float) ((float)shortsOrig[minMax[1]] / Short.MAX_VALUE) * (mContentRect.height()/2d))),
                                    tmpCnt + 1,
                                    (int) ((mContentRect.height()/2 + (float) ((float)shortsOrig[minMax[0]] / Short.MAX_VALUE) * (mContentRect.height()/2d))),
                                    whichPaint);
                    tmpCnt++;
                    System.out.println("oski look " + shortsOrig[minMax[0]] + " "  + shortsOrig[minMax[1]]);
                }

                //accessWaveMerged.write(insertBytes, 0, insertBytes.length);

                //System.out.println("oski acceswavemerged passed "+ finalOrig[1000]);
            } catch (IOException ex) {
                // Rethrow but we still close accessWave in our finally
                try {
                    throw ex;
                } catch (IOException e) {
                    e.printStackTrace();
                }
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
        /*public void drawTrackFromFile(String whichFile, int whichHeight, int start, int end, Paint whichPaint) {
            int fullTrackLengthInBytes = (int) (44.1 * 4 * (end - start));
            long AudioPos = (long) (4 * start * 44.1000);
            if (AudioPos % 2 != 0) {
                AudioPos = AudioPos - 1;
            }
            int stepSize = 4096 * 4;
            RandomAccessFile accessWaveMerged = null;

            int partLen = (int) (44.1 * 4 * (end - start));
            float audioPerPixel = partLen / mContentRect.width();

            File inFile = new File(whichFile);
            int tmpCnt = 0;
            for (int j = 0; j < fullTrackLengthInBytes; j += stepSize) {
                AudioPos += stepSize;
                byte[] insertBytes = new byte[(int) (stepSize)];
                try {
                    accessWaveMerged = new RandomAccessFile(inFile, "rw");
                    // ChunkSize

                    accessWaveMerged.seek((long) (AudioPos));
                    accessWaveMerged.read(insertBytes, 0, (int) insertBytes.length);

                    short[] shortsOrig = new short[insertBytes.length / 2];
                    ByteBuffer.wrap(insertBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortsOrig);

                    ByteBuffer.wrap(insertBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortsOrig);
                    int[] minMax = MainHelper.getMinAndMaxFromShorts(shortsOrig,0);
                    if(whichPaint == mp3Paint) {
                        mp3Paint.setShader(new LinearGradient(0, whichHeight/2, 0, (int) ((whichHeight +(float) shortsOrig[minMax[1]]/Short.MAX_VALUE*whichHeight)/2), Color.rgb(255,235,59), Color.rgb(255,87,34), Shader.TileMode.MIRROR));
                    } else if(whichPaint == recPaint) {
                        recPaint.setShader(new LinearGradient(0, (int)(whichHeight*1.5), 0,(int) ((whichHeight +(float) shortsOrig[minMax[1]]/Short.MAX_VALUE*whichHeight)/2)  ,  Color.rgb(210,253,246),Color.rgb(3,7,55) , Shader.TileMode.MIRROR));
                    }
                    //drCanvas.drawRect(tmpCnt,drCanvas.getHeight()/2+shortsOrig[minMax[1]],tmpCnt+1,drCanvas.getHeight()/2+shortsOrig[minMax[0]],new Paint());
                    drCanvas.drawRect(tmpCnt,(int) ((whichHeight +(float) shortsOrig[minMax[1]]/Short.MAX_VALUE*whichHeight)/2),tmpCnt+1,(int)((whichHeight + (float) shortsOrig[minMax[0]]/Short.MAX_VALUE*whichHeight)/2),whichPaint);

                    tmpCnt++;

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }*/

        public void drawFromFile(String whichFile, int whichHeight, int offsetY, int start, int end, Paint whichPaint) {
            File file = new File(whichFile);
            FileInputStream fin = null;
            int ch;
            deleteRec();
            byte[] bb = new byte[(int)(AudioConstants.drawBuffer)];
            try {
                fin = new FileInputStream(file);
                int tmpCnt = initCanvWidth/2;
                int skipCnt = 0;
                while((ch = fin.read(bb,0,bb.length)) != -1) {
                    if(skipCnt*AudioConstants.drawBuffer > start * AudioConstants.drawPixelRatio && skipCnt*AudioConstants.drawBuffer  < end*AudioConstants.drawPixelRatio) {
                        if(skipCnt %2 == 0) {
                            short[] shortsOrig = new short[bb.length/2];
                            ByteBuffer.wrap(bb).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortsOrig);
                            int[] minMax = MainHelper.getMinAndMaxFromShorts(shortsOrig,0);
                            if(whichPaint == mp3Paint) {
                                mp3Paint.setShader(new LinearGradient(0, whichHeight/2, 0, (int) ((whichHeight +offsetY +(float) shortsOrig[minMax[1]]/Short.MAX_VALUE*whichHeight)/2), Color.rgb(255,235,59), Color.rgb(255,87,34), Shader.TileMode.MIRROR));
                            } else if(whichPaint == recPaint) {
                                recPaint.setShader(new LinearGradient(0, (int)(whichHeight*1.5), 0,(int) ((whichHeight +offsetY+(float) shortsOrig[minMax[1]]/Short.MAX_VALUE*whichHeight)/2)  ,  Color.rgb(210,253,246),Color.rgb(3,7,55) , Shader.TileMode.MIRROR));
                            }
                            //drCanvas.drawRect(tmpCnt,drCanvas.getHeight()/2+shortsOrig[minMax[1]],tmpCnt+1,drCanvas.getHeight()/2+shortsOrig[minMax[0]],new Paint());
                            drCanvas.drawRect(tmpCnt,(int) ((whichHeight +offsetY +(float) shortsOrig[minMax[1]]/Short.MAX_VALUE*whichHeight)/2),tmpCnt+1,(int)((whichHeight + offsetY + (float) shortsOrig[minMax[0]]/Short.MAX_VALUE*whichHeight)/2),whichPaint);

                            tmpCnt++;
                        }
                    }
                    skipCnt++;
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                // close the stream using close method
                try {
                    if (fin != null) {
                        fin.close();
                    }
                }
                catch (IOException ioe) {
                    System.out.println("Error while closing stream: " + ioe);
                }
            }
        }

        float scrollByX = 0; // x amount to scroll by
        float xDown = 0;
        float xUp = 0;
        float offsetX = 0;
        float canvPosX = 0; // x amount to scroll by

        @Override
        public boolean performClick() {
            super.performClick();
            return true;
        }
        float AudioPerPixel = 1;
        float startTouchPosX = 0;
        float startTouchPosY = 0;

        int startMidiNote = 0;
        float startMidiNoteX = 0;
        float endMidiNoteX = 0;
        float midiNoteLength = 0;

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            //mScaleDetector.onTouchEvent(event);
            super.onTouchEvent(event);

            final int action = event.getAction();
            System.out.println("oski debug "+event.getAction());
            switch (action & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN : {
                    if(AudioConstants.midiEditMode == 4) {
                        float newStartX = ((translX+event.getX()-mContentRect.width()/2)/mContentRect.width()) * 10.0f;
                        int newPitch = 50;//(int) ((event.getY())/mContentRect.height()*10f);
                        LiveEffectEngine.addNoteAt(AudioConstants.midiEvents.length,newPitch,  newStartX);
                        System.out.println("Oskiweeeh" + translX+ " " +((event.getX())+ " " +mContentRect.width()) + " " + newPitch + " " + newStartX);
                        AudioConstants.midiEditMode = 1;
                        Button tmpBtn = (Button) getView().getRootView().findViewById(R.id.midiAddBtn);
                        tmpBtn.setTextColor(Color.WHITE);
                    }
                }
                case MotionEvent.ACTION_MOVE :
                {

                    if(AudioConstants.midiEditMode == 2) {
                        if(startTouchPosY == 0) {
                            startTouchPosY = event.getY();
                            startMidiNote = (int) AudioConstants.midiEvents[AudioConstants.markedMidiNote][0];
                        }

                        if(startTouchPosX == 0) {
                            startTouchPosX = event.getX();
                            startMidiNoteX = AudioConstants.midiEvents[AudioConstants.markedMidiNote][1];
                            endMidiNoteX = AudioConstants.midiEvents[AudioConstants.markedMidiNote][2];
                            midiNoteLength = AudioConstants.midiEvents[AudioConstants.markedMidiNote][2]
                                            - AudioConstants.midiEvents[AudioConstants.markedMidiNote][1];
                        }

                        float newStartX = startMidiNoteX + ((event.getX()-startTouchPosX)/mContentRect.width())*10f;
                        System.out.println("oski " + startMidiNoteX + " "+event.getX() + " " + startTouchPosX + " " +(startMidiNoteX - (event.getX()-startTouchPosX)/100f));
                        float newEndX = newStartX+midiNoteLength;
                        //AudioConstants.midiEvents[AudioConstants.markedMidiNote][1] = event.
                        //AudioConstants.midiEvents[AudioConstants.markedMidiNote][2] =
                        int newPitch = (int) (startMidiNote - (event.getY()-startTouchPosY)/mContentRect.height()*10f);
                        AudioConstants.midiEvents[AudioConstants.markedMidiNote][0] = newPitch;
                        //System.out.println("oskigo "+ startMidiNote + " "+ event.getY() +" " +startTouchPosY+ " " +mContentRect.height());

                        for(int i = 0; i < AudioConstants.midiEvents.length; i++) {
                            System.out.println("osekydig" + AudioConstants.midiEvents[i][0]);
                        }
                        LiveEffectEngine.syncMidiEvents(AudioConstants.markedMidiNote, newPitch, newStartX,newEndX);
                        invalidate();
                    } else if(AudioConstants.midiEditMode == 3) {

                        if(startTouchPosX == 0) {
                            startTouchPosX = event.getX();
                            startMidiNoteX = AudioConstants.midiEvents[AudioConstants.markedMidiNote][1];
                            endMidiNoteX = AudioConstants.midiEvents[AudioConstants.markedMidiNote][2];
                            midiNoteLength = AudioConstants.midiEvents[AudioConstants.markedMidiNote][2]
                                    - AudioConstants.midiEvents[AudioConstants.markedMidiNote][1];
                        }

                        float newEndX = endMidiNoteX + ((event.getX()-startTouchPosX)/mContentRect.width())*10f;
                        System.out.println("oski " + startMidiNoteX + " "+event.getX() + " " + startTouchPosX + " " +(startMidiNoteX - (event.getX()-startTouchPosX)/100f));
                        AudioConstants.midiEvents[AudioConstants.markedMidiNote][2] = newEndX;
                        //System.out.println("oskigo "+ startMidiNote + " "+ event.getY() +" " +startTouchPosY+ " " +mContentRect.height());
                        LiveEffectEngine.syncMidiEvents(AudioConstants.markedMidiNote, (int) AudioConstants.midiEvents[AudioConstants.markedMidiNote][0], AudioConstants.midiEvents[AudioConstants.markedMidiNote][1],newEndX);

                    }
                    /*for(int i = 0; i < AudioObjects.allEffects.size(); i++) {
                        if((event.getX()+translX-initCanvWidth/2) > AudioObjects.allEffects.get(i).start/39.2f/2
                                && (event.getX()+translX-initCanvWidth/2) < AudioObjects.allEffects.get(i).end/39.2f/2
                                && event.getY() > initCanvHeight - 100) {
                            AudioObjects.allEffects.remove(i);
                            invalidate();
                        }
                    }*/
                    currentMarkerPos = (int) event.getX();

                    AudioConstants.prevMarkerStart = (int) (AudioConstants.markerStart + event.getX() * AudioPerPixel);
                    AudioConstants.prevMarkerEnd = (int) (AudioConstants.markerStart + event.getX() * AudioPerPixel) + 10;
                    if(AudioConstants.markerEnd < AudioConstants.prevMarkerEnd) {
                        AudioConstants.prevMarkerEnd = AudioConstants.markerEnd- 10;
                    }

                    AudioConstants.livePositionBytes = (long) ((event.getX()/ (float)mContentRect.width())*audioFullLen);
                    System.out.println("oskiwey " + AudioConstants.livePositionBytes+  "  " + AudioPerPixel + " " + AudioConstants.prevMarkerStart+ " " + AudioConstants.prevMarkerEnd);

                }
                break;
                case MotionEvent.ACTION_UP: {
                    //LiveEffectEngine.setPlayerPosition(AudioConstants.livePositionBytes);
                    startTouchPosX = 0;
                    startTouchPosY = 0;
                    startMidiNote=0;
                    startMidiNoteX = 0;
                    endMidiNoteX = 0;
                    midiNoteLength = 0;
                    mListener.onComplete();
                    break;
                }
            }
            if(event.getPointerCount() < 2) {
                //mGestureDetector.onTouchEvent(event);

            }
            mScrollDetector.onTouchEvent(event);
            //mScaleDetector.onTouchEvent(event);

            return true; // done with this event so consume it
        }

        float AXIS_X_MIN = 200;
        float AXIS_Y_MIN= 200;
        float AXIS_X_MAX = 500;
        float AXIS_Y_MAX = 500;

        // The current viewport. This rectangle represents the currently visible
        // chart domain and range.
        // private RectF mCurrentViewport;
        RectF mCurrentViewport = new RectF(AXIS_X_MIN, AXIS_Y_MIN, AXIS_X_MAX, AXIS_Y_MAX);

        // The current destination rectangle (in pixel coordinates) into which the
        // chart data should be drawn.
        private Rect mContentRect;


        private class ScaleListener
                extends ScaleGestureDetector.SimpleOnScaleGestureListener {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                //if(mScaleFactor < )
                mScaleFactor *= detector.getScaleFactor();

                // Don't let the object get too small or too large.
                //mScaleFactor = Math.max(0.01f, Math.min(mScaleFactor, 10.0f));
                invalidate();
                return true;
            }
        }

        public float globalQ = 1.414f;
        public float globalFreq = 100;

        private class ScrollListener
                extends GestureDetector.SimpleOnGestureListener {

            @Override
            public void onLongPress(MotionEvent e) {
                super.onLongPress(e);

            }

            @Override
            public boolean onSingleTapUp(MotionEvent event) {

                // e will give you the location and everything else you want
                // This is where you will be doing whatever you want to.
                if(AudioConstants.midiEditMode == 1) {

                    float eX = event.getX();
                    float eY = event.getY();
                    boolean hitSth = false;
                    for(int i = 0; i < AudioConstants.midiEvents.length;i++) {
                        if(eX >= (AudioConstants.midiEvents[i][1]/10)*mContentRect.width()-translX+(mContentRect.width()/2) && eX <= (AudioConstants.midiEvents[i][2]/10)*mContentRect.width()-translX+(mContentRect.width()/2)) {
                            System.out.println("oskilala " + i);
                            AudioConstants.markedMidiNote = i;
                            hitSth = true;
                        }
                    }
                    if(!hitSth) {
                        AudioConstants.markedMidiNote = -1;
                    } else {
                        LiveEffectEngine.playMidiNote((int) AudioConstants.midiEvents[AudioConstants.markedMidiNote][0]-36,true);
                        Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            public void run() {
                                // turn note off after 1 sec or else infinitytrumpet
                                if(AudioConstants.markedMidiNote >=0) {
                                    LiveEffectEngine.playMidiNote((int) AudioConstants.midiEvents[AudioConstants.markedMidiNote][0]-36,false);
                                }

                            }
                        }, 250);

                    }
                }

                return true;
            }


            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2,
                                    float distanceX, float distanceY) {

                if(AudioConstants.midiEditMode != 2 && AudioConstants.midiEditMode != 3) {
                    System.out.println("oski doing scroll " + distanceX);
                    // offset within the current viewport.
                    translX += distanceX/mScaleFactor;
                    globalQ += distanceY/100;
                    globalFreq += distanceX;

                    TextView textTime = (TextView) ((Activity) context).findViewById(R.id.textTime);
                    String timeString = String.format(Locale.US,"%02d:%02d",
                            TimeUnit.MILLISECONDS.toMinutes(AudioConstants.mPlayerPosition),
                            TimeUnit.MILLISECONDS.toSeconds(AudioConstants.mPlayerPosition) -
                                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(AudioConstants.mPlayerPosition))
                    );
                }

                return true;
            }
        }

        Rect[] volumeRects;
        Rect thresholdRect;

        public Rect[] generateVolumeBars(int screenWidth, int screenHeight, int barVolume){

            float rectWidth = 20;
            float rectHeight = 130;
            int paddingLeft = 25;
            int paddingBottom = 25;
            for(int i = 1; i <= 25 ; i++) {

                volumeRects[i-1].left = (int)((i*rectWidth)+2*i);//paddingLeft;
                volumeRects[i-1].top = 25;
                volumeRects[i-1].right =(int)(i*rectWidth+2*i-rectWidth/3);// (int)(rectWidth);
                volumeRects[i-1].bottom = (int) rectHeight;
                //System.out.println("oskiway " + volumeRects[i-1].left+ " " + volumeRects[i-1].top + " " + volumeRects[i-1].right +" " + volumeRects[i-1].bottom);

            }

            return volumeRects;
        }



        float toDrawDB = 0;

        @Override
        public void onDraw(Canvas canvas) {

            super.onDraw(canvas);


            //recPaint.setColor(Color.RED);
            //mp3Paint.setColor(Color.GREEN);
            canvas.drawLine(mContentRect.width()/2f,0,mContentRect.width()/2f+1,mContentRect.height(),recPaint);
            generateVolumeBars(getWidth(),getHeight(),(int)(AudioConstants.liveVolumeDb) * (mContentRect.height()/4));
            int howManyBars = Math.min((int)((AudioConstants.liveVolumeDb/AudioConstants.deviceBufferSize)),volumeRects.length);
            //System.out.println("oskidra "+howManyBars + " " +AudioConstants.liveVolumeDb +" " + AudioConstants.liveVolumeDb/100f + " " +  AudioConstants.liveVolumeDb);
            for(int i = 0; i < howManyBars;i++) {
                if(AudioConstants.midiRecActive) {
                    if(i < 1) {
                        recPaint.setColor(Color.GRAY);
                    } else if(i<12) {
                        recPaint.setColor(getResources().getColor(R.color.green));
                    } else if(i<18) {
                        recPaint.setColor(getResources().getColor(R.color.yellow));
                    } else {
                        recPaint.setColor(getResources().getColor(R.color.colorRed));
                    }
                } else {
                    recPaint.setColor(Color.GRAY);
                }

                if(AudioConstants.liveVolumeDb/1000f > 0.6f) {

                }
                if(i < volumeRects.length){
                    canvas.drawRect(volumeRects[i], recPaint);
                }

            }
            canvas.drawRect(AudioConstants.midiRecThreshold*(mContentRect.width()/3f),0,AudioConstants.midiRecThreshold*(mContentRect.width()/3f)+5,150,recPaint);

            canvas.save();
            canvas.translate(-((AudioConstants.livePositionBytes/(float)AudioConstants.deviceSampleRate)/10f)*mContentRect.width() + mContentRect.width()/2f,0);
            canvas.scale(mScaleFactor, mScaleFactor, mContentRect.width()/2, 1);
            canvas.drawRect(-translX,0,-translX+5,mContentRect.height(),recPaint);
            canvas.drawBitmap(bitmap,-translX-mContentRect.width()/2,0, recPaint);

            AudioConstants.midiEvents = LiveEffectEngine.getMyMidiEvents();
            if(AudioConstants.midiEvents.length > 0) {
                for(int i = 0; i < AudioConstants.midiEvents.length; i++) {
                    float displayMidiRatio = mContentRect.height()/35f;
                    float tmpTop = mContentRect.height()-(AudioConstants.midiEvents[i][0]-36) *displayMidiRatio ;
                    //float tmpBottom = midiEvents[i][1];
                    //System.out.println("oskijavao "+AudioConstants.midiEvents[i][0]+ " "+AudioConstants.midiEvents[i][1]+" " +AudioConstants.midiEvents[i][2]);
                    if(i == AudioConstants.markedMidiNote) {
                        recPaint.setColor(Color.GREEN);
                    } else {
                        recPaint.setColor(Color.LTGRAY);
                    }
                    canvas.drawRect((AudioConstants.midiEvents[i][1]/10)*mContentRect.width()-translX,tmpTop,(AudioConstants.midiEvents[i][2]/10)*mContentRect.width()-translX,tmpTop-displayMidiRatio,recPaint);
                }
            }

            if(AudioConstants.livePitch > 0) {
                float displayMidiRatio = mContentRect.height()/35f;
                float tmpTop = mContentRect.height()-(AudioConstants.livePitch-36) *displayMidiRatio ;

                canvas.drawRect(0,tmpTop,mContentRect.width(),tmpTop-displayMidiRatio,recPaint);

            }
            //canvas.drawBitmap(bitmap,0,0, recPaint);
            //float midPos =((+translX+ canvas.getWidth()/2) );
            //currentMarkerPos = (int)midPos;
            /*recPaint.setAlpha(100);
            canvas.drawRect(new Rect((int)(currentMarkerPos),0,(int)(currentMarkerPos+(AudioConstants.prevMarkerEnd-AudioConstants.prevMarkerStart)/AudioPerPixel)+2,canvas.getHeight()), recPaint);
            recPaint.setAlpha(255);
            canvas.drawRect(new Rect((int)currentMarkerPos,0,(int)currentMarkerPos+2,canvas.getHeight()), currentCursorPaint);
            canvas.drawRect(new Rect((int)(currentMarkerPos+(AudioConstants.prevMarkerEnd-AudioConstants.prevMarkerStart)/AudioPerPixel),0,(int)(currentMarkerPos+(AudioConstants.prevMarkerEnd-AudioConstants.prevMarkerStart)/AudioPerPixel)+2,canvas.getHeight()), currentCursorPaint);*/

            /*canvas.drawRect(new Rect((int)(((float)AudioConstants.livePositionBytes/audioFullLen) * mContentRect.width()),0,              (int)(((float)AudioConstants.livePositionBytes/audioFullLen) * mContentRect.width())+2,mContentRect.height()), currentCursorPaint);
            if(AudioConstants.liveIsRecording > 0) {
                toDrawDB = (AudioConstants.liveVolumeDb/200f) * (mContentRect.height()/4);
                drCanvas.drawRect(new Rect((int)(((float)AudioConstants.livePositionBytes/audioFullLen) * mContentRect.width()),(int)(mContentRect.height()-(mContentRect.height()/4)+toDrawDB),(int)(((float)AudioConstants.livePositionBytes/audioFullLen) * mContentRect.width())+2, (int)(mContentRect.height()-(mContentRect.height()/4)-toDrawDB)), currentCursorPaint);
            }*/
            //System.out.println("oskiww " + AudioConstants.liveVolumeDb);

            canvas.restore();

            invalidate();
            dx = 0;
        }
    }
}


