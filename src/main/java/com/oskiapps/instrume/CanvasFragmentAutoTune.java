package com.oskiapps.instrume;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import java.util.ArrayList;

import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;

/**
 * Created by Oskar on 31.03.2018.
 */

public class CanvasFragmentAutoTune extends Fragment {

    public DrawClass drawClass;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_canvas_autotune, container, false);

        RelativeLayout relativeLayout = (RelativeLayout) rootView.findViewById(R.id.rect_autotune);
        drawClass = new DrawClass(getActivity());
        relativeLayout.addView(drawClass);

        return rootView;
    }

    public static interface OnCompleteListener {
        public abstract void onComplete(CanvasFragmentAutoTune gotCanv);
    }

    private CanvasFragmentAutoTune.OnCompleteListener mListener;

    public Context upperContext;
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            upperContext = context;
            this.mListener = (CanvasFragmentAutoTune.OnCompleteListener)context;
        }
        catch (final ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnCompleteListener");
        }
    }

    private ScaleGestureDetector mScaleDetector;
    private GestureDetector mGestureDetector;
    private float mScaleFactor = 1.0f;

    private Bitmap bitmap;

    public void doDrawings(float[] thePitches) {
        //drawClass.drawFromFile(thePitches);
        drawClass.drawAutotune(thePitches);

    }

    public class DrawClass extends View {
        Canvas drCanvas;
        Paint recPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint mp3Paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint markerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);


        Paint clearPaint = new Paint();
        Paint currentCursor = new Paint();
        Rect rect = new Rect(0, 0, 0, 0);
        int initCanvWidth = 0;
        int initCanvHeight = 0;

        Context context;

        public DrawClass(Context ctx) {
            super(ctx);
            context = ctx;
            clearPaint.setColor(getResources().getColor(R.color.colorPrimaryLight));
            recPaint.setColor(getResources().getColor(R.color.colorRed));
            markerPaint.setColor(getResources().getColor(R.color.colorRed));
            markerPaint.setTextSize(72f);
            currentCursor.setColor(Color.WHITE);

            mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
            mGestureDetector = new GestureDetector(context, new ScrollListener());

        }

        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            if (bitmap != null) {
                bitmap.recycle();
            }
            drCanvas = new Canvas();
            if(h > 0) {
                bitmap = Bitmap.createBitmap(2048, h, Bitmap.Config.ARGB_8888);

            }
            drCanvas.setBitmap(bitmap);

            if (initCanvWidth == 0) {
                initCanvWidth = w;
                initCanvHeight = h;
            }
            mp3Paint.setAntiAlias(true);
            recPaint.setAntiAlias(true);

            mContentRect = new Rect(0, 0, w, h);
           /* AXIS_X_MIN = 0;
            AXIS_Y_MIN = 0;
            AXIS_X_MAX = drCanvas.getWidth();
            AXIS_Y_MAX = drCanvas.getHeight();*/
            mListener.onComplete(CanvasFragmentAutoTune.this);

        }


        public void drawFromFile(ArrayList<double[]> gotPitches, int whichWidth, int gotBufSize, int trackLenBytes, Paint whichPaint) {
            int maxFrames = trackLenBytes/gotBufSize;
            int theBuffSize = gotBufSize;

            trackLenBytes = 0;
            for(int i = 0; i < gotPitches.size(); i++) {
                trackLenBytes+=gotPitches.get(i)[2];
            }
            float trackPixelRatio = (float) trackLenBytes / mContentRect.width();


            for(int i = 0; i < gotPitches.size(); i++) {
                float frameInPixels = ((float)(i*theBuffSize)/(float)trackLenBytes)*(float)mContentRect.width()-(float)(theBuffSize/(float)trackLenBytes);

                float origPitch = (float)gotPitches.get(i)[0];
                float newPitch = (float)gotPitches.get(i)[1];
                System.out.println("oskiuu "+ frameInPixels + " " + mContentRect.width() + " "+ trackLenBytes + " " + mContentRect.width() + " " + i + " " + theBuffSize);
                whichPaint.setColor(Color.GREEN);
                whichPaint.setStrokeWidth(3);

                drCanvas.drawLine(frameInPixels, mContentRect.height() - origPitch*2,frameInPixels+theBuffSize/trackPixelRatio,mContentRect.height() - origPitch*2,whichPaint);
                whichPaint.setColor(Color.YELLOW);
                whichPaint.setStrokeWidth(2);
                drCanvas.drawLine(frameInPixels,mContentRect.height() -  newPitch*2,frameInPixels+theBuffSize/trackPixelRatio,mContentRect.height() - newPitch*2,whichPaint);

                whichPaint.setStrokeWidth(1);
                whichPaint.setColor(Color.GREEN);

            }

           /* float audioPerPixel = trackLenBytes / mContentRect.width();
            int pixeldrawCnt = (int)((whichFrame*gotBufSize)/audioPerPixel);
            for(int i = 0; i < inArray.length-audioPerPixel; i+=audioPerPixel) {

                int[] minMax = MainHelper.getMinAndMaxFromShorts(inArray,i,(int)audioPerPixel);
                System.out.println("oski do live " + minMax[0] + " " + minMax[1] + " " +inArray[minMax[0]] + " " + inArray[minMax[1]]  + " " + inArray.length + " "+ audioPerPixel+" " + pixeldrawCnt + " " + trackLenBytes);
                drCanvasSoundLayerCompr.drawRect(pixeldrawCnt,(int) ((mContentRect.height()  +(float) inArray[minMax[1]]/Short.MAX_VALUE*mContentRect.height())/2),
                        pixeldrawCnt+1,(int)((mContentRect.height()  + (float) inArray[minMax[0]]/Short.MAX_VALUE*mContentRect.height())/2),
                        whichPaint);
                pixeldrawCnt++;
            }
            System.out.println("oski doin stuff");*/
        }


        /**
         * Sets the text size for a Paint object so a given string of text will be a
         * given width.
         *
         * @param paint         the Paint to set the text size for
         * @param desiredHeight the desired width
         * @param text          the text that should be that width
         */
        public void setTextSizeForHeight(Paint paint, float desiredHeight,
                                         String text) {

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

        public int getCurrentMarkerPos() {
            return (int) (AudioConstants.mPlayerPosition);
        }

        int thePlayBar = 0;
        public int getCurrentBarPos() {
            return (int) (thePlayBar);
        }
        public void setCurrentBarPos(int thePos) {
            thePlayBar = thePos;
        }

        float translX;
        int dx;


        public void setBar(int gotMin, int gotMax) {
            translX += 1;
            dx = 1;
            //(int)((whichHeight + offsetY + (float) shortsOrig[minMax[0]]/Short.MAX_VALUE*whichHeight)/2)
            drCanvas.drawRect(translX - 1, drCanvas.getHeight() / 2, translX, drCanvas.getHeight(), clearPaint);
            drCanvas.drawRect(+translX - 1, (int) ((float) gotMax / Short.MAX_VALUE / 2 * drCanvas.getHeight() + drCanvas.getHeight() / 1.25), translX, (int) ((float) gotMin / Short.MAX_VALUE / 2 * drCanvas.getHeight() + drCanvas.getHeight() / 1.25), recPaint);
            //rect.set(initCanvWidth/2+translX-1,(int)((float)gotMax/32768*drCanvas.getHeight()/4 + drCanvas.getHeight()/1.25),initCanvWidth/2+translX,(int)((float)gotMin/32768*drCanvas.getHeight()/4 + drCanvas.getHeight()/1.25));
        }

        public void clearCanvas() {
            drCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        }
        float zoomLevel = 1;

        public void setCanvasPosition(int gotPos) {
            scrollByX = 0;
            translX = (int) ((float) (gotPos / AudioConstants.drawMsRatio));
            AudioConstants.mPlayerPosition = (int) (gotPos);
            //System.out.println("oski" + translX + " " + gotPos);

            dx = 1;
        }

        public void drawScale(float[] gotScale ){
            for(int i = 0; i < gotScale.length;i++) {
                drCanvas.drawRect(0, (float) ((44100d/gotScale[i])/720d*mContentRect.height()-1), mContentRect.width(), (float) ((44100d/gotScale[i])/720d*mContentRect.height()), markerPaint);
            }
        }

        public void clearCanvasAuto() {
            drCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        }
        float scrollByX = 0; // x amount to scroll by
        float xDown = 0;
        float xUp = 0;
        float offsetX = 0;
        float canvPosX = 0; // x amount to scroll by
        boolean zoomFirst = true;
        float zoomFirstVal = 1;


        @Override
        public boolean performClick() {

            return true;
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            //mScaleDetector.onTouchEvent(event);


            final int action = event.getAction();
            System.out.println("oski debug " + event.getAction());
            switch (action & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_UP: {

                }
            }

            super.onTouchEvent(event);
            if (event.getPointerCount() < 2) {
                mGestureDetector.onTouchEvent(event);

            }
            mScaleDetector.onTouchEvent(event);

            System.out.println("oski touchvals " + offsetX + " " + scrollByX + " " + canvPosX + " " + canvPosX + " " + translX + " " + event.getX());

            return true; // done with this event so consume it
        }

        float AXIS_X_MIN = 200;
        float AXIS_Y_MIN = 200;
        float AXIS_X_MAX = 500;
        float AXIS_Y_MAX = 500;

        // The current viewport. This rectangle represents the currently visible
        // chart domain and range.
        // private RectF mCurrentViewport;
        RectF mCurrentViewport = new RectF(AXIS_X_MIN, AXIS_Y_MIN, AXIS_X_MAX, AXIS_Y_MAX);

        // The current destination rectangle (in pixel coordinates) into which the
        // chart data should be drawn.
        public Rect mContentRect;

        public void drawFromFile(int[] ints) {
            clearCanvasAuto();
            //float singleNoteLen = (float)mContentRect.width()/(float)gotDualPitchPeriods.size();
            Paint whichPaint = new Paint();
            whichPaint.setStrokeWidth(5);
            whichPaint.setColor(Color.GREEN);


            Paint outerPaint = new Paint();
            outerPaint.setStrokeWidth(50);
            outerPaint.setColor(Color.RED);
            outerPaint.setAlpha(125);

            Paint scalePaint = new Paint();
            //scalePaint.setStrokeWidth(50);
            scalePaint.setColor(Color.RED);
            scalePaint.setAlpha(125);
            currentScale = setUpScale(AudioConstAutoTune.scale);
            int[] rainbow = context.getResources().getIntArray(R.array.rainbow);

            float heightScaleRatio = mContentRect.height() / (float)currentScale[currentScale.length-1]*4;
            int colorCnt = 1;
            int alphaCnt = 0;
            /*Paint bgPaint = new Paint();
            bgPaint.setColor(Color.LTGRAY);
            drCanvas.drawRect(0,0,mContentRect.width(),mContentRect.height(), bgPaint);*/
            for(int i = 1; i < currentScale.length-1; i++) {
                float topPos = (currentScale[i]+currentScale[i+1]) / 2;
                float bottomPos = (currentScale[i-1] + currentScale[i]) / 2;
                scalePaint.setColor(rainbow[colorCnt]+alphaCnt);
                scalePaint.setAlpha(175);

                if(i%6 == 0) {
                    colorCnt=0;
                    alphaCnt-=100;

                } else {
                    colorCnt++;
                }
                //scalePaint.setAlpha(alphaCnt);

                drCanvas.drawRect(0, mContentRect.height()-bottomPos*heightScaleRatio, mContentRect.width(), mContentRect.height()-topPos*heightScaleRatio,  scalePaint);
                System.out.println("oskidbg #01 "+ currentScale[i]);

            }
            scalePaint.setAlpha(255);

            float arrayDisplayRatio = mContentRect.width()/ints.length * 1.2f ;
            System.out.println("oskidbg #1 "+ currentScale[currentScale.length-1]+" " + arrayDisplayRatio + " " + mContentRect.width()+ " "+ ints.length);
            for(int i = 0 ; i < ints.length-1; i++)
            {
                System.out.println("oskidebugger " + ints[i]);
                if(ints[i] > 10 && ints[i] < 600 &&
                        ints[i+1] > 10 && ints[i+1] < 600) {
                    double[] theCorrectedPitch = pitchCorrector(ints[i]);
                    scalePaint.setColor(rainbow[(int)theCorrectedPitch[3]%6]);
                    drCanvas.drawRect(i*arrayDisplayRatio, mContentRect.height()-(float)theCorrectedPitch[2]*heightScaleRatio, (i+1)*arrayDisplayRatio, mContentRect.height()-(float)theCorrectedPitch[1]*heightScaleRatio,  scalePaint);

                    drCanvas.drawLine(i*arrayDisplayRatio, mContentRect.height()-ints[i]*heightScaleRatio, (i+1)*arrayDisplayRatio, mContentRect.height()-ints[i+1]*heightScaleRatio,  whichPaint);
                    //drCanvas.drawLine(i*arrayDisplayRatio,mContentRect.height()- (float)theCorrectedPitch[0]/60f*mContentRect.height()/2, (i+1)*arrayDisplayRatio,mContentRect.height()- (float)theCorrectedPitch[0]/60f*mContentRect.height()/2,  outerPaint);

                }
            }


        }


        public void drawAutotune(float[] floats) {
            clearCanvasAuto();
            //float singleNoteLen = (float)mContentRect.width()/(float)gotDualPitchPeriods.size();
            Paint whichPaint = new Paint();
            whichPaint.setStrokeWidth(5);
            whichPaint.setColor(Color.GREEN);


            Paint outerPaint = new Paint();
            outerPaint.setStrokeWidth(50);
            outerPaint.setColor(Color.RED);
            outerPaint.setAlpha(125);

            Paint scalePaint = new Paint();
            //scalePaint.setStrokeWidth(50);
            scalePaint.setColor(Color.RED);
            scalePaint.setAlpha(125);
            currentScale = setUpScale(AudioConstAutoTune.scale);
            int[] rainbow = context.getResources().getIntArray(R.array.rainbow);

            //float heightScaleRatio = mContentRect.height() / (float)currentScale[currentScale.length-1]*4;
            int colorCnt = 1;
            int alphaCnt = 0;
            /*Paint bgPaint = new Paint();
            bgPaint.setColor(Color.LTGRAY);
            drCanvas.drawRect(0,0,mContentRect.width(),mContentRect.height(), bgPaint);*/
            /*for(int i = 1; i < currentScale.length-1; i++) {
                float topPos = (currentScale[i]+currentScale[i+1]) / 2;
                float bottomPos = (currentScale[i-1] + currentScale[i]) / 2;
                scalePaint.setColor(rainbow[colorCnt]+alphaCnt);
                scalePaint.setAlpha(175);

                if(i%6 == 0) {
                    colorCnt=0;
                    alphaCnt-=100;

                } else {
                    colorCnt++;
                }
                //scalePaint.setAlpha(alphaCnt);

                drCanvas.drawRect(0, mContentRect.height()-bottomPos*heightScaleRatio, mContentRect.width(), mContentRect.height()-topPos*heightScaleRatio,  scalePaint);
                System.out.println("oskidbg #01 "+ currentScale[i]);

            }
            scalePaint.setAlpha(255);*/

            float heightRatio = mContentRect.height()/21f;
            float arrayDisplayRatio = mContentRect.width()/floats.length;
            System.out.println("oskidbg #1 "+ currentScale[currentScale.length-1]+" " + arrayDisplayRatio + " " + mContentRect.width()+ " "+ floats.length);

            float lastPitch = 0;
            float memStart = 0;
            whichPaint.setColor(Color.WHITE);
            whichPaint.setStrokeWidth(1);

            Paint scaleCornerPaint = new Paint(Color.WHITE);
            scaleCornerPaint.setColor(Color.WHITE);
            scaleCornerPaint.setStrokeWidth(1);
            scaleCornerPaint.setStyle(Paint.Style.STROKE);

            for(int i = 0 ; i < floats.length-1; i++)
            {
                System.out.println("oskidebugger " + floats[i]);
                if(floats[i] > 10 && floats[i] < 600 &&
                        floats[i+1] > 10 && floats[i+1] < 600) {
                    double[] theCorrectedPitch = pitchCorrector(floats[i]);
                    float octaveHeightRatio =(float) (heightRatio / (theCorrectedPitch[2]-theCorrectedPitch[1]));
                    if(octaveHeightRatio>1) {
                        //octaveHeightRatio =(float) ((theCorrectedPitch[2]-theCorrectedPitch[1])/heightRatio);
                    }

                    System.out.println("oskipitcher "+floats[i]+" "+theCorrectedPitch[1] +" "+theCorrectedPitch[2] +" "+ theCorrectedPitch[3]+ " " + heightRatio + " " + octaveHeightRatio + " " +floats[i]);
                    //scalePaint.setColor(rainbow[(int)theCorrectedPitch[3]%6]);
                    float currentBottom = (float) theCorrectedPitch[3]*heightRatio;
                    float currentTop = (float) (theCorrectedPitch[3]*heightRatio+heightRatio);
                    if(lastPitch==theCorrectedPitch[0] && lastPitch != 0) {
                        if(memStart == 0) {
                            memStart = (i-2)*arrayDisplayRatio;
                        }
                    } else if(lastPitch!=theCorrectedPitch[0] && lastPitch != 0) {
                        if(memStart != 0){
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                drCanvas.drawRoundRect(memStart, mContentRect.height()-currentTop, (i-1)*arrayDisplayRatio, mContentRect.height()-currentBottom, 5f,5f, scalePaint);
                                drCanvas.drawRoundRect(memStart, mContentRect.height()-currentTop, (i-1)*arrayDisplayRatio, mContentRect.height()-currentBottom, 5f,5f, scaleCornerPaint);
                            } else {
                                drCanvas.drawRect(memStart, mContentRect.height()-currentTop, (i-1)*arrayDisplayRatio, mContentRect.height()-currentBottom, scalePaint);
                                drCanvas.drawRect(memStart, mContentRect.height()-currentTop, (i-1)*arrayDisplayRatio, mContentRect.height()-currentBottom, scaleCornerPaint);

                            }

                        }
                        memStart = 0;

                    }

                    float theBarDiff = (float) (theCorrectedPitch[2]-theCorrectedPitch[1]);
                    float lineStartY = (float) (heightRatio+currentBottom + ((floats[i]-theCorrectedPitch[1])/theBarDiff)*heightRatio);
                    float lineEndY = (float) (heightRatio+currentBottom + ((floats[i+1]-theCorrectedPitch[1])/theBarDiff)*heightRatio);
                    System.out.println("oski every "+ heightRatio + " "+ currentBottom + " "+  floats[i]+ " " +theBarDiff + " " +((floats[i+1]-currentBottom)/theBarDiff)+ " " + (currentBottom + ((floats[i]-currentBottom)/theBarDiff)));
                    drCanvas.drawLine(i*arrayDisplayRatio, mContentRect.height()-lineStartY, (i+1)*arrayDisplayRatio, mContentRect.height()-lineEndY,  whichPaint);
                    //drCanvas.drawLine(i*arrayDisplayRatio,mContentRect.height()- (float)theCorrectedPitch[0]/60f*mContentRect.height()/2, (i+1)*arrayDisplayRatio,mContentRect.height()- (float)theCorrectedPitch[0]/60f*mContentRect.height()/2,  outerPaint);
                    lastPitch = (float)theCorrectedPitch[0];
                }
            }


        }

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

        private class ScrollListener
                extends GestureDetector.SimpleOnGestureListener {

            @Override
            public void onLongPress(MotionEvent e) {

            }


            @Override
            public boolean onSingleTapUp(MotionEvent event) {
//for global effects



                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2,
                                    float distanceX, float distanceY) {
                translX += distanceX/mScaleFactor;

                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {

                return true;
            }
        }

        /**
         * Sets the current viewport (defined by mCurrentViewport) to the given
         * X and Y positions. Note that the Y value represents the topmost pixel position,
         * and thus the bottom of the mCurrentViewport rectangle.
         */
        private void setViewportBottomLeft(float x, float y) {
            /*
             * Constrains within the scroll range. The scroll range is simply the viewport
             * extremes (AXIS_X_MAX, etc.) minus the viewport size. For example, if the
             * extremes were 0 and 10, and the viewport size was 2, the scroll range would
             * be 0 to 8.
             */

            float curWidth = mCurrentViewport.width();
            float curHeight = mCurrentViewport.height();
            x = Math.max(AXIS_X_MIN, Math.min(x, AXIS_X_MAX - curWidth));
            y = Math.max(AXIS_Y_MIN + curHeight, Math.min(y, AXIS_Y_MAX));
            System.out.println("oski dbg " + x + " " + y);
            mCurrentViewport.set(x, y - curHeight, x + curWidth, y);
            // Invalidates the View to update the display.
            ViewCompat.postInvalidateOnAnimation(this);
        }


        @Override
        public void onDraw(Canvas canvas) {

            super.onDraw(canvas);
            //recPaint.setColor(Color.RED);
            //mp3Paint.setColor(Color.GREEN);
            canvas.scale(mScaleFactor, 1, canvas.getWidth()/2, 1);

            canvas.save();
            canvas.translate((-translX),0);

            //canvas.translate((+initCanvWidth/2),0);

            //track start-line
            canvas.drawRect(new Rect((int)(getCurrentBarPos()),0,(int)(getCurrentBarPos()+2),canvas.getHeight()), recPaint );
            //drCanvas.drawRect(rect, recPaint );

            canvas.drawBitmap(bitmap,0,0, recPaint);

            //EffectEQOld oskieq = (EffectEQOld) AudioObjects.allEffects.get(i);

            canvas.restore();

            invalidate();
            dx = 0;
        }

    }

    static float[] currentScale = new float[42];
    public static float[] setUpScale(String whichScale) {
        float[] tmpArr = new float[42];
        float[] theChosenScale = new float[7];
        float[] tmpCMajArr = new float[] {65.41f/4f, 73.42f/4f, 82.41f/4f, 87.31f/4f,98.00f/4f,110.00f/4f,123.47f/4f};
        float[] tmpCMinArr = new float[] {65.41f/4f, 73.42f/4f, 77.78f/4f, 87.31f/4f,98.00f/4f,103.83f/4f,116.54f/4f};
        float[] tmpDMajArr = new float[] {73.42f/4f, 82.41f/4f, 92.50f/4f, 98.00f/4f,110f/4f,123.47f/4f, 138.59f/4f};
        float[] tmpDMinArr = new float[] {73.42f/4f, 82.41f/4f, 87.31f/4f, 98.00f/4f,110f/4f,116.54f/4f, 130.81f/4f};
        float[] tmpEMajArr = new float[] {69.30f/4f, 77.78f/4f, 82.41f/4f, 92.50f/4f,103.83f/4f,110.00f/4f,123.47f/4f};
        float[] tmpEMinArr = new float[] {65.41f/4f, 73.42f/4f, 82.41f/4f, 92.50f/4f,98.00f/4f,110.00f/4f,123.47f/4f};
        float[] tmpFMajArr = new float[] {65.41f/4f, 73.42f/4f, 82.41f/4f, 87.31f/4f,98.00f/4f,110.00f/4f, 116.54f/4f};
        float[] tmpFMinArr = new float[] {65.41f/4f, 69.30f/4f, 77.78f/4f, 87.31f/4f,98.00f/4f,103.83f/4f, 116.54f/4f};
        float[] tmpGMajArr = new float[] {65.41f/4f, 73.42f/4f, 82.41f/4f, 92.50f/4f,98.00f/4f,110.00f/4f,123.47f/4f};
        float[] tmpGMinArr = new float[] {65.41f/4f, 73.42f/4f, 77.78f/4f, 87.31f/4f,98.00f/4f,110.00f/4f, 116.54f/4f};
        float[] tmpAMajArr = new float[] {69.30f/4f, 73.42f/4f, 82.41f/4f, 92.50f/4f,103.83f/4f,110.00f/4f, 123.47f/4f};
        float[] tmpAMinArr = new float[] {65.41f/4f, 73.42f/4f, 82.41f/4f, 87.31f/4f,98.00f/4f,110.00f/4f, 123.47f/4f};
        float[] tmpBMajArr = new float[] {69.30f/4f, 77.78f/4f, 82.41f/4f, 92.50f/4f,103.83f/4f,116.54f/4f, 123.47f/4f};

        switch (whichScale) {
            case "C-Major":
                theChosenScale = tmpCMajArr;
                break;
            case "C-Minor":
                theChosenScale = tmpCMinArr;
                break;
            case "D-Major":
                theChosenScale = tmpDMajArr;
                break;
            case "D-Minor":
                theChosenScale = tmpDMinArr;
                break;
            case "E-Major":
                theChosenScale = tmpEMajArr;
                break;
            case "E-Minor":
                theChosenScale = tmpEMinArr;
                break;
            case "F-Major":
                theChosenScale = tmpFMajArr;
                break;
            case "F-Minor":
                theChosenScale = tmpFMinArr;
                break;
            case "G-Major":
                theChosenScale = tmpGMajArr;
                break;
            case "G-Minor":
                theChosenScale = tmpGMinArr;
                break;
            case "A-Major":
                theChosenScale = tmpAMajArr;
                break;
            case "A-Minor":
                theChosenScale = tmpAMinArr;
                break;
            case "B-Major":
                theChosenScale = tmpBMajArr;
                break;

        }
        int cntRun = 1;
        int eachStep =0;
        for(int i = 0; i < tmpArr.length; i+= theChosenScale.length) {
            for(int j = 0; j < theChosenScale.length && eachStep < tmpArr.length; j++) {
                //System.out.println("oskidbg +2 "+ theChosenScale[j] + " " + cntRun + " " +cntRun*j + " " + theChosenScale[j]*cntRun);
                tmpArr[eachStep] = theChosenScale[j]*cntRun;
                eachStep++;

            }
            cntRun*=2;
        }
        return tmpArr;
    }

    public static double[] pitchCorrector(double gotPitch) {
        double tmpPitch = gotPitch;

        /*if(tmpPitch == -1 || tmpPitch > 1000  || tmpPitch < 0) {
            tmpPitch = 130.82d;
        }*/
        float topPos = 0;
        float bottomPos = 0;
        float retVal = 0;
        double[] retDouble = new double[4];
        for(int j = 0; j < currentScale.length-1; j++) {
            if(tmpPitch > currentScale[j] && tmpPitch < currentScale[j+1]) {
                System.out.println("oskipitcher2 " + tmpPitch +" "+ currentScale[j] + " " + currentScale[j+1]);
                if (Math.abs(tmpPitch - currentScale[j]) < Math.abs(currentScale[j + 1] - tmpPitch)) {
                    //System.out.println("oski took ceil " + tmpPitch + " " + myarrLess[j]);
                    tmpPitch = currentScale[j];
                     topPos = (currentScale[j]+currentScale[j+1]) / 2;
                     if(j-1 >0) {
                         bottomPos = (currentScale[j-1] + currentScale[j]) / 2;
                     }
                    retVal= j;
                    System.out.println("oskipitcher3 " + retVal);
                    retDouble[0] = tmpPitch;
                    retDouble[1] = bottomPos;
                    retDouble[2] = topPos;
                    retDouble[3] = retVal;
                    return retDouble;

                } else {

                    tmpPitch = currentScale[j + 1];

                    if(currentScale.length > j+2) {
                         topPos = (currentScale[j + 1]+currentScale[j + 2]) / 2;
                         bottomPos = (currentScale[j] + currentScale[j + 1]) / 2;

                    }
                    retVal = j+1;
                    //System.out.println("oski took roof " + tmpPitch + " " + myarrLess[j+1]);
                    System.out.println("oskipitcher3 " + retVal);
                    retDouble[0] = tmpPitch;
                    retDouble[1] = bottomPos;
                    retDouble[2] = topPos;
                    retDouble[3] = retVal;
                    return retDouble;
                }

            }
        }
        /*if(tmpPitch == 0) {
            tmpPitch = 130.82d;
        }*/

        return new double[] {tmpPitch, bottomPos,topPos,retVal};
    }
}