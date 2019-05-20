package com.oskiapps.instrume;


import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import androidx.fragment.app.Fragment;

/**
 * Created by Oskar on 30.09.2018.
 */

public class CanvasFragmentEffectsPlayer extends Fragment {

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
    private GestureDetector mGestureDetector;
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
            currentCursorPaint.setColor(Color.WHITE);

            mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
            mGestureDetector = new GestureDetector(context, new ScrollListener());
        }

        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            if (bitmap != null) {
                bitmap .recycle();
            }
            drCanvas= new Canvas();
            if(h > 0) {
                bitmap = Bitmap.createBitmap(2048, h, Bitmap.Config.ARGB_8888);

            }

            drCanvas.setBitmap(bitmap);
            if(initCanvWidth == 0) {
                initCanvWidth=w;
                initCanvHeight=h;
            }
            mp3Paint.setAntiAlias(true);
            recPaint.setAntiAlias(true);

            mContentRect = new Rect(0,0,w,h);
            drCanvas.drawRect(0,0,drCanvas.getWidth(),drCanvas.getHeight(),clearPaint);


            Executor executor = Executors.newSingleThreadExecutor();
            SimpleDrawFiles simpleDrawer = new SimpleDrawFiles(context);
            executor.execute(simpleDrawer);


            //+ ((AudioConstants.markerEnd-AudioConstants.markerStart)))/mScaleFactor/AudioPerPixel;
            invalidate();
            //mListener.onComplete();
        }



        long audioFullLen = 1;
        public float drawFromFile2(String whichFile, int whichHeight, int offsetY, Paint whichPaint) {
            File file = new File(whichFile);
            FileInputStream fin = null;
            int ch;
            deleteRec();
            long fileLen = file.length();
            audioFullLen = fileLen;
            //float audioRatio = (float)(mContentRect.width()/(fileLen/((float)AudioConstants.drawBuffer*2f)));
            byte[] bb = new byte[(int) (AudioConstants.drawBuffer)];
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

                        tmpCnt++;
                        //tmpCnt+=audioRatio;
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

        protected int currentMarkerPos = 0;

        float translX;
        int dx;
        public void deleteRec() {
            drCanvas.drawRect(0,drCanvas.getHeight()/2,drCanvas.getWidth(),drCanvas.getHeight(),clearPaint);
        }


        public void drawFromFile(String whichFile, int whichHeight, int offsetY, int start, int end, Paint whichPaint) {
            File file = new File(whichFile);
            FileInputStream fin = null;
            int ch;
            deleteRec();
            byte[] bb = new byte[(int)(7104)];
            try {
                fin = new FileInputStream(file);
                int tmpCnt = initCanvWidth/2;
                int skipCnt = 0;
                while((ch = fin.read(bb,0,bb.length)) != -1) {
                    if(skipCnt*7104/2 > start * 44.1*4 && skipCnt*7104/2  < end*44.1*4) {
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
            return true;
        }
        float AudioPerPixel = 1;
        @Override
        public boolean onTouchEvent(MotionEvent event) {
            //mScaleDetector.onTouchEvent(event);
            super.onTouchEvent(event);

            final int action = event.getAction();
            System.out.println("oski debug "+event.getAction());
            switch (action & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN :
                case MotionEvent.ACTION_MOVE: {
                    /*for(int i = 0; i < AudioObjects.allEffects.size(); i++) {
                        if((event.getX()+translX-initCanvWidth/2) > AudioObjects.allEffects.get(i).start/39.2f/2
                                && (event.getX()+translX-initCanvWidth/2) < AudioObjects.allEffects.get(i).end/39.2f/2
                                && event.getY() > initCanvHeight - 100) {
                            AudioObjects.allEffects.remove(i);
                            invalidate();
                        }
                    }*/
                    //currentMarkerPos = (int) event.getX();

                    //AudioConstants.prevMarkerStart = (int) (AudioConstants.markerStart + event.getX() * AudioPerPixel);
                    //AudioConstants.prevMarkerEnd = (int) (AudioConstants.markerStart + event.getX() * AudioPerPixel) + 10;
                    //if(AudioConstants.markerEnd < AudioConstants.prevMarkerEnd) {
                    //    AudioConstants.prevMarkerEnd = AudioConstants.markerEnd- 10;
                    //}

                    //AudioConstants.livePositionBytes = (long) ((event.getX()/ (float)mContentRect.width())*audioFullLen);
                    System.out.println("oskiwey " + AudioConstants.livePositionBytes+  "  " + AudioPerPixel + " " + AudioConstants.prevMarkerStart+ " " + AudioConstants.prevMarkerEnd);

                    if(event.getPointerCount() < 2) {

                        int touchPos = (int) ((translX + ((event.getX() / mContentRect.width()) * mContentRect.width())) / mScaleFactor);
                        if (editingMarkerStart) {
                            if (touchPos * AudioPerPixel < AudioConstants.markerEnd) {
                                AudioConstants.markerStart = (int) (touchPos * AudioPerPixel);
                                AudioConstants.prevMarkerStart = (int) (AudioConstants.markerStart);
                                AudioConstants.prevMarkerEnd = (int) (AudioConstants.markerStart) + 5000;

                            }

                        } else if (editingMarkerEnd) {
                            if (touchPos * AudioPerPixel > AudioConstants.markerStart) {
                                AudioConstants.markerEnd = (int) (touchPos * AudioPerPixel);
                                AudioConstants.prevMarkerEnd = (int) (AudioConstants.markerStart) + 5000;

                            }
                        }
                    }
                }
                break;
                case MotionEvent.ACTION_UP: {
                    if(editingMarkerEnd || editingMarkerStart) {
                       // mListener.onComplete();
                    }
                    editingMarkerStart = false;
                    editingMarkerEnd = false;
                    //LiveEffectEngine.setPlayerPosition(AudioConstants.livePositionBytes);

                    break;
                }
            }
            if(event.getPointerCount() < 2) {
                mGestureDetector.onTouchEvent(event);
            }
            mScaleDetector.onTouchEvent(event);
            return true; // done with this event so consume it
        }

        float AXIS_X_MIN = 200;
        float AXIS_Y_MIN= 200;
        float AXIS_X_MAX = 500;
        float AXIS_Y_MAX = 500;

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

        boolean editingMarkerStart = false;
        boolean editingMarkerEnd = false;

        private class ScrollListener
                extends GestureDetector.SimpleOnGestureListener {

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2,
                                    float distanceX, float distanceY) {
                System.out.println("oski doing scroll " + distanceX);
                // offset within the current viewport.
                translX += distanceX;
                globalQ += distanceY/100;
                globalFreq += distanceX;

                TextView textTime = (TextView) ((Activity) context).findViewById(R.id.textTime);
                String timeString = String.format(Locale.US,"%02d:%02d",
                        TimeUnit.MILLISECONDS.toMinutes(AudioConstants.mPlayerPosition),
                        TimeUnit.MILLISECONDS.toSeconds(AudioConstants.mPlayerPosition) -
                                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(AudioConstants.mPlayerPosition))
                );
                return true;
            }
            public void onLongPress(MotionEvent e) {
                float audioPixelMs = (AudioPerPixel/1000f);
                int touchPos = (int) ((translX+((e.getX()/mContentRect.width())*mContentRect.width()))/mScaleFactor);
                System.out.println("oski ta "+ AudioConstants.markerStart + " " + (AudioConstants.markerStart/AudioPerPixel) + " " + translX + " " + (translX+((e.getX()/mContentRect.width())*mContentRect.width()))/mScaleFactor + " " + mScaleFactor);
                //Log.e("", "Longpress detected");
                if(e.getAction() == MotionEvent.ACTION_DOWN) {
                    if(touchPos > AudioConstants.markerEnd/AudioPerPixel) {
                        System.out.println("oskiknow2 " + audioPixelMs + " " + e.getX()+ " " +AudioPerPixel + " "+ translX +" " +  e.getX()/AudioPerPixel+translX);
                        //AudioConstants.markerEnd = (int) (touchPos*AudioPerPixel);
                        editingMarkerEnd = true;
                    } else if(touchPos < AudioConstants.markerStart/AudioPerPixel) {
                        System.out.println("oskiknow2 " + audioPixelMs + " " + e.getX()+ " " +AudioPerPixel + " "+ translX +" " +  e.getX()/AudioPerPixel+translX);
                        //AudioConstants.markerStart = (int) (touchPos*AudioPerPixel);
                        editingMarkerStart = true;

                    }
                }
            }
        }



        @Override
        public void onDraw(Canvas canvas) {

            super.onDraw(canvas);
            //recPaint.setColor(Color.RED);
            //mp3Paint.setColor(Color.GREEN);
            canvas.save();
            canvas.translate((-translX),0);

            canvas.scale(mScaleFactor, 1);

            canvas.drawBitmap(bitmap,0,0, recPaint);
            //float midPos =((+translX+ canvas.getWidth()/2) );
            //currentMarkerPos = (int)midPos;
            recPaint.setAlpha(25);
            //int touchPos = (int) ((translX+((e.getX()/mContentRect.width())*mContentRect.width()))/mScaleFactor);

            canvas.drawRect(new Rect((int)(AudioConstants.markerStart/AudioPerPixel),0,(int)(AudioConstants.markerEnd/AudioPerPixel)+2,canvas.getHeight()), recPaint);
            recPaint.setAlpha(255);
            canvas.drawRect(new Rect((int)(AudioConstants.markerStart/AudioPerPixel),0,(int)(AudioConstants.markerStart/AudioPerPixel)+2,canvas.getHeight()), recPaint);
            canvas.drawRect(new Rect((int)(AudioConstants.markerEnd/AudioPerPixel),0,(int)(AudioConstants.markerEnd/AudioPerPixel)+2,canvas.getHeight()), recPaint);
            //canvas.drawRect(new Rect((int)(((float)AudioConstants.livePositionBytes/audioFullLen) * mContentRect.width()),0,(int)(((float)AudioConstants.livePositionBytes/audioFullLen) * mContentRect.width())+2,canvas.getHeight()), currentCursorPaint);

            canvas.restore();
            invalidate();

            dx = 0;
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
                int pixelcnt = (int) drawFromFile2(AudioConstants.filePathRecLong, (int) (drCanvas.getHeight() / 2), (int) (drCanvas.getHeight()), recPaint);

                float audioInMs = ((audioFullLen/(AudioConstants.deviceSampleRate/1000f)/2/2));
                mScaleFactor = (float)audioInMs/((AudioConstants.markerEnd-AudioConstants.markerStart )*3);
                AudioPerPixel = (float)((float)audioInMs / (float)pixelcnt);

                //translX = +mContentRect.width()/mScaleFactor;
                translX += (AudioConstants.markerStart/AudioPerPixel*mScaleFactor) - (AudioConstants.markerEnd-AudioConstants.markerStart)/AudioPerPixel*mScaleFactor;
            }
        }
    }
}


