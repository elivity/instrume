package com.oskiapps.instrume;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;

import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;


/**
 * Created by Oskar on 31.03.2018.
 */

public class CanvasFragmentMain extends Fragment {

    public DrawClass drawClass;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_canvas_autotunelive, container, false);

        RelativeLayout relativeLayout = (RelativeLayout) rootView.findViewById(R.id.rect_autotunelive);
        drawClass = new DrawClass(getActivity());
        relativeLayout.addView(drawClass);

        return rootView;
    }

    public static interface OnCompleteListener {
        public abstract void onComplete(CanvasFragmentMain gotCanv);
    }

    //private CanvasFragmentMain.OnCompleteListener mListener;

    /*public Context upperContext;
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            upperContext = context;
            this.mListener = (CanvasFragmentMain.OnCompleteListener)context;
        }
        catch (final ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnCompleteListener");
        }
    }

    */
    private ScaleGestureDetector mScaleDetector;
    private GestureDetector mGestureDetector;
    private float mScaleFactor = 1.0f;

    private Bitmap bitmap;
    public class DrawClass extends View {
        Canvas drCanvas;
        Paint recPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint mp3Paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint soundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint markerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);


        Paint clearPaint = new Paint();
        Paint currentCursor = new Paint();
        Rect rect = new Rect(0, 0, 0, 0);
        int initCanvWidth = 0;
        int initCanvHeight = 0;
        long tmpSoundInfo[] = new long[5];
        long tmpFreqs[] = new long[1];
        Context context;

        public DrawClass(Context ctx) {
            super(ctx);
            context = ctx;
            clearPaint.setColor(getResources().getColor(R.color.colorPrimaryLight));
            recPaint.setColor(getResources().getColor(R.color.colorRed));
            recPaint.setStrokeWidth(1);

            markerPaint.setColor(getResources().getColor(R.color.colorRed));
            markerPaint.setTextSize(72f);
            currentCursor.setColor(Color.WHITE);

            mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
            mGestureDetector = new GestureDetector(context, new ScrollListener());

            soundPaint.setColor(Color.GREEN);
            soundPaint.setStrokeWidth(3);
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

            //Shader shader = new LinearGradient(0, (float)mContentRect.height(), new int[] {Color.GRAY, Color.GREEN,Color.YELLOW,Color.RED}, new float[]{(float)0,(float)0.1f,(float)0.5f,(float)0.75f});
            //recPaint.setShader(shader);

           /* AXIS_X_MIN = 0;
            AXIS_Y_MIN = 0;
            AXIS_X_MAX = drCanvas.getWidth();
            AXIS_Y_MAX = drCanvas.getHeight();*/

            initializeKeyboard(drCanvas);
            //mListener.onComplete(CanvasFragmentMain.this);

        }


        public void deleteRec() {
            drCanvas.drawRect(0, drCanvas.getHeight() / 2, drCanvas.getWidth(), drCanvas.getHeight(), clearPaint);
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

        float hitMidiKey = 0;

        boolean downpress = false;

        float lastMidiKey = 0;
        @Override
        public boolean onTouchEvent(MotionEvent event) {
            //mScaleDetector.onTouchEvent(event);
            super.onTouchEvent(event);

            final int action = event.getAction();
            System.out.println("oski debug " + event.getAction());
            switch (action & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_UP: {
                    if(downpress) {
                        LiveEffectEngine.playMidiNote((int) lastMidiKey,false);
                        System.out.println("oskiwy up " + event.getAction() + " " + lastMidiKey);
                        lastMidiKey = 0;
                        downpress=false;
                    }
                    break;
                }
                case MotionEvent.ACTION_MOVE:
                case MotionEvent.ACTION_DOWN: {
                    System.out.println("oskiouto" + (lastMidiKey != (int)((event.getX()/mContentRect.width())*20)));
                    if(downpress==false || lastMidiKey != (int)((event.getX()/mContentRect.width())*20) || lastMidiKey != ((int)((event.getX()/mContentRect.width())*20) - 0.5f)) {
                        System.out.println("oskiz down " + event.getAction() + " " +halfesMidiMatching.get(hitMidiKey).intValue() +
                        " " + lastMidiKey + " " +((int)((event.getX()/mContentRect.width())*20) - 0.5f) );

                        LiveEffectEngine.playMidiNote((int)lastMidiKey,false);

                        if(lastMidiKey == ((int)((event.getX()/mContentRect.width())*20))) {
                            //LiveEffectEngine.playMidiNote((int)lastMidiKey,false);

                        }
                        if(event.getY() > mContentRect.height()/2) {
                            hitMidiKey = (int)((event.getX()/mContentRect.width()/mScaleFactor)*20);
                            LiveEffectEngine.playMidiNote(halfesMidiMatching.get(hitMidiKey).intValue()+1, true);

                        } else {
                            if(((int)((event.getX()/mContentRect.width()/mScaleFactor)*20) - 0.5f) != 2.5 && ((int)((event.getX()/mContentRect.width()/mScaleFactor)*20) - 0.5f) != 6.5
                                    &&((int)((event.getX()/mContentRect.width()/mScaleFactor)*20) - 0.5f) != 9.5 && ((int)((event.getX()/mContentRect.width()/mScaleFactor)*20) - 0.5f) != 13.5
                                    &&((int)((event.getX()/mContentRect.width()/mScaleFactor)*20) - 0.5f) != 16.5) {
                                if(((int)((event.getX()/mContentRect.width()/mScaleFactor)*20) - 0.5f)>0) {
                                    hitMidiKey = ((int)((event.getX()/mContentRect.width()/mScaleFactor)*20) - 0.5f);

                                }
                                LiveEffectEngine.playMidiNote(halfesMidiMatching.get(hitMidiKey).intValue()+1,true);

                            }
                        }
                        lastMidiKey = halfesMidiMatching.get(hitMidiKey).intValue()+1;

                        downpress = true;
                    }

                    System.out.println("oskiwy down "+ halfesMidiMatching.get(hitMidiKey).intValue() + " " + hitMidiKey + " " + event.getX()/mContentRect.width());

                break;
                }
            }

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

        public void drawSingleBuffer(int position, int value) {
            drCanvas.drawRect(translX - 1, drCanvas.getHeight() / 2, translX, value, recPaint);
        }

        public void drawFromFile(String whichFile, int initCanvHeight, int theIi, int start, int end, Paint thePaint) {
            //clearAllCanvas();

            //deleteRec();

            RandomAccessFile accessWaveMerged = null;
            try {
                int tmpCnt = 0;
                int partLen = (int) (44.1 * 4 * (end - start));

                float audioPerPixel = partLen / mContentRect.width();

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
                    //drCanvasSoundLayer.drawRect(tmpCnt, (int) ((mContentRect.height() +  (float) ((float)shortsOrig[minMax[1]] / Short.MAX_VALUE) * mContentRect.height())/2d), tmpCnt + 1, (int) ((mContentRect.height() + (float) ((float)shortsOrig[minMax[0]] / Short.MAX_VALUE) * mContentRect.height())/2d), whichPaint);
                    tmpCnt++;
                }
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


        private class ScaleListener
                extends ScaleGestureDetector.SimpleOnScaleGestureListener {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                //if(mScaleFactor < )
                mScaleFactor *= detector.getScaleFactor();

                // Don't let the object get too small or too large.
                mScaleFactor = Math.max(1.0f, Math.min(mScaleFactor, 8.0f));
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


        public Drawable loadDrawableFromAssets(Context context, String path)
        {
            InputStream stream = null;
            try
            {
                stream = context.getAssets().open(path);
                System.out.println("osworked");
                return Drawable.createFromStream(stream, null);
            }
            catch (Exception ignored) {} finally
            {
                try
                {
                    if(stream != null)
                    {
                        stream.close();
                    }
                } catch (Exception ignored) {}
            }
            return null;
        }

        Drawable hitKey0Drw;
        Drawable hitKey1Drw;
        HashMap<Float,Integer> halfesMidiMatching = new HashMap<>();
        public void initializeKeyboard(Canvas theCanvas) {
            Drawable d1 = loadDrawableFromAssets(context,"key0.png");
            Drawable d2 = loadDrawableFromAssets(context,"keyboardbtn2.png");

            hitKey0Drw = loadDrawableFromAssets(context,"key0clicked.png");
            hitKey1Drw = loadDrawableFromAssets(context,"key1clicked.png");
            //d1.setBounds(left, top, right, bottom);
            //d1.draw(theCanvas);

            /*for(int i = 1; i <= 35; i++) {
                //canvas.drawLine(0,i*((float)mContentRect.height()/35),mContentRect.width(),i*((float)mContentRect.height()/35), markerPaint );
                //canvas.drawLine((int)(i)*(float)mContentRect.width()/35,0,(int)(i)*(float)mContentRect.width()/35,mContentRect.height(), markerPaint );
                if(i%3==0){

                } else if(i %5 ==0) {

                }else if(i %7 ==0) {

                } else {
                    d1.setBounds((int)((i)*(float)mContentRect.width()/20), 0, (int)((i)*(float)mContentRect.width()/20+((float)mContentRect.width()/20)), mContentRect.height());
                    d1.draw(theCanvas);
                }
            }*/

            for(int i = 0; i < 20; i++) {
                //canvas.drawLine(0,i*((float)mContentRect.height()/35),mContentRect.width(),i*((float)mContentRect.height()/35), markerPaint );
                //canvas.drawLine((int)(i)*(float)mContentRect.width()/35,0,(int)(i)*(float)mContentRect.width()/35,mContentRect.height(), markerPaint );

                d1.setBounds((int)((i)*(float)mContentRect.width()/20), 0, (int)((i)*(float)mContentRect.width()/20+((float)mContentRect.width()/20)), mContentRect.height());
                d1.draw(theCanvas);

                if(i%7==1){
                    d2.setBounds((int)((i)*(float)mContentRect.width()/20 - (float)mContentRect.width()/40), 0, (int)((i)*(float)mContentRect.width()/20+((float)mContentRect.width()/40)), (int)(mContentRect.height()/1.5f));
                    d2.draw(theCanvas);
                } else if(i %7 ==2) {
                    d2.setBounds((int)((i)*(float)mContentRect.width()/20 - (float)mContentRect.width()/40), 0, (int)((i)*(float)mContentRect.width()/20+((float)mContentRect.width()/40)), (int)(mContentRect.height()/1.5f));
                    d2.draw(theCanvas);
                }else if(i %7 ==4) {
                    d2.setBounds((int)((i)*(float)mContentRect.width()/20 - (float)mContentRect.width()/40), 0, (int)((i)*(float)mContentRect.width()/20+((float)mContentRect.width()/40)), (int)(mContentRect.height()/1.5f));
                    d2.draw(theCanvas);
                }else if(i %7 ==5) {
                    d2.setBounds((int)((i)*(float)mContentRect.width()/20 - (float)mContentRect.width()/40), 0, (int)((i)*(float)mContentRect.width()/20+((float)mContentRect.width()/40)), (int)(mContentRect.height()/1.5f));
                    d2.draw(theCanvas);
                }else if(i %7 ==6) {
                    d2.setBounds((int)((i)*(float)mContentRect.width()/20 - (float)mContentRect.width()/40), 0, (int)((i)*(float)mContentRect.width()/20+((float)mContentRect.width()/40)), (int)(mContentRect.height()/1.5f));
                    d2.draw(theCanvas);
                } else {

                }
            }

            halfesMidiMatching.put(0f,0);
            halfesMidiMatching.put(0.5f,1);
            halfesMidiMatching.put(1.0f,2);
            halfesMidiMatching.put(1.5f,3);
            halfesMidiMatching.put(2.0f,4);
            halfesMidiMatching.put(3.0f,5);
            halfesMidiMatching.put(3.5f,6);
            halfesMidiMatching.put(4.0f,7);
            halfesMidiMatching.put(4.5f,8);
            halfesMidiMatching.put(5.0f,9);
            halfesMidiMatching.put(5.5f,10);
            halfesMidiMatching.put(6.0f,11);
            halfesMidiMatching.put(7.0f,12);
            halfesMidiMatching.put(7.5f,13);
            halfesMidiMatching.put(8.0f,14);
            halfesMidiMatching.put(8.5f,15);
            halfesMidiMatching.put(9.0f,16);
            halfesMidiMatching.put(10.0f,17);
            halfesMidiMatching.put(10.5f,18);
            halfesMidiMatching.put(11.0f,19);
            halfesMidiMatching.put(11.5f,20);
            halfesMidiMatching.put(12.0f,21);
            halfesMidiMatching.put(12.5f,22);
            halfesMidiMatching.put(13.0f,23);
            halfesMidiMatching.put(14.0f,24);
            halfesMidiMatching.put(14.5f,25);
            halfesMidiMatching.put(15.0f,26);
            halfesMidiMatching.put(15.5f,27);
            halfesMidiMatching.put(16.0f,28);
            halfesMidiMatching.put(17.0f,29);
            halfesMidiMatching.put(17.5f,30);
            halfesMidiMatching.put(18.0f,31);
            halfesMidiMatching.put(18.5f,32);
            halfesMidiMatching.put(19.0f,33);
            halfesMidiMatching.put(19.5f,34);
            halfesMidiMatching.put(20.0f,35);
            halfesMidiMatching.put(21.0f,36);
        }


        float[] midiHalfes = new float[]{0,1,1.5f,2,
                3,3.5f,4,4.5f,5,5.5f,6,
                7,7.5f,8,8.5f,9,
                10,10.5f,11,11.5f,12,12.5f,13,
                14,14.5f,15,15.5f,16,
                17,17.5f,18,18.5f,19,19.5f,
                20.5f,21};


        int drawCnt = 0;
        float pitchRaw = 0;
        float correctedPitch = 0;
        float theMidikey = 0;

        int drawStep = 0;
        @Override
        public void onDraw(Canvas canvas) {

            super.onDraw(canvas);
            //recPaint.setColor(Color.RED);
            //mp3Paint.setColor(Color.GREEN);
            canvas.scale(mScaleFactor, 1, 0, 1);

            canvas.save();
            canvas.translate((translX),0);


            //clearCanvas();
            //canvas.translate((+initCanvWidth/2),0);
               //AudioConstants.prevMarkerStart =
            if(drawCnt > 5) {

                drawCnt = 0;
                tmpSoundInfo = LiveEffectEngine.drawSth(0,0);
                tmpFreqs = LiveEffectEngine.getFreqs(0,0);
                //System.out.println("tmpposval " + tmpSoundInfo[0]);
                if(tmpSoundInfo[0] > 0) {
                    AudioConstants.livePositionBytes = tmpSoundInfo[0];
                }
                if(tmpSoundInfo[1] > 0) {
                    AudioConstants.liveVolumeDb = tmpSoundInfo[1];
                    AudioConstants.liveIsRecording = tmpSoundInfo[3];
                }

                if(tmpSoundInfo[4] > 36) {
                    pitchRaw = (tmpSoundInfo[4]/100f) -37;
                }

                if(tmpSoundInfo[2] > 0) {
                    correctedPitch = tmpSoundInfo[2];
                    AudioConstants.livePitch = (int)correctedPitch;
                } else {
                    AudioConstants.livePitch = 0;

                }

            }
            drawCnt++;



            for(int i = 5; i < tmpFreqs.length; i++)
            {
                //recPaint.setStrokeWidth(5);

                //recPaint.setStrokeWidth(AudioConstants.liveVolumeDb);
                //canvas.drawRect((i-4)/96f*mContentRect.width(),getHeight()/2- tmpFreqs[i-1]/100f,(i-5)/96f*mContentRect.width(),0,recPaint);
                canvas.drawRect(((float)i/tmpFreqs.length)*mContentRect.width(),getHeight()/2- tmpFreqs[i-1]/100f,((float)i/tmpFreqs.length)*mContentRect.width()+2,0,recPaint);

            }
            clearCanvas();
//System.out.println("oskioutiii " + tmpSoundInfo.length);
            for(int i = 5; i < tmpSoundInfo.length-1; i++)
            {

                //recPaint.setStrokeWidth(AudioConstants.liveVolumeDb);
                //canvas.drawLine((i-5)/96f*mContentRect.width(),getHeight()/2-tmpSoundInfo[i-1]/100f,(i-4)/96f*mContentRect.width(),getHeight()/2-tmpSoundInfo[i]/100f,recPaint);
                canvas.drawLine(((float)i/tmpFreqs.length)*mContentRect.width(),getHeight()/2-tmpSoundInfo[i-1]/100f,((float)i/tmpFreqs.length)*mContentRect.width()+2,getHeight()/2-tmpSoundInfo[i]/100f,soundPaint);

                //canvas.drawLine(i,tmpSoundInfo[i]/Short.MAX_VALUE,);
                drCanvas.drawLine(i, ((float)tmpSoundInfo[i]*100)*mContentRect.height()+mContentRect.height()/2,i+1,((float)tmpSoundInfo[i+1]*100)*mContentRect.height()+mContentRect.height()/2,markerPaint);
                //drawStep++;
            }

            //System.out.println("oski "+ AudioConstants.prevMarkerStart +" " + tmpSoundInfo + " " + AudioConstants.livePositionBytes);
            //track start-line
            //canvas.drawRect(new Rect((int) -translX,getHeight(), (int) (150-translX),getHeight()-(int)tmpSoundInfo[1]+1), recPaint );
            markerPaint.setColor(Color.GREEN);

            //spin and draw behaviour for the autotune line
            /*translX++;
            if(translX > mContentRect.width()/2) {
                translX-=mContentRect.width();
                clearCanvas();
            } else {
            }*/
            //start of canvas
            //canvas.drawRect(new Rect((int)(getCurrentBarPos()),0,(int)(getCurrentBarPos()+2),canvas.getHeight()), recPaint );
            //drCanvas.drawRect(rect, recPaint );

            canvas.drawBitmap(bitmap,0,0, recPaint);

            //EffectEQOld oskieq = (EffectEQOld) AudioObjects.allEffects.get(i);

            canvas.restore();
            /*for(int i = 1; i <= 35; i++) {
                //canvas.drawLine(0,i*((float)mContentRect.height()/35),mContentRect.width(),i*((float)mContentRect.height()/35), markerPaint );
                canvas.drawLine((int)(i)*(float)mContentRect.width()/35,0,(int)(i)*(float)mContentRect.width()/35,mContentRect.height(), markerPaint );

            }*/
            //canvas.drawBitmap(bitmap,0,0, markerPaint);

            /*float theMidikey = tmpSoundInfo[4];//(int) (12*Math.log(tmpSoundInfo[2]/440.0f)+69)-50;
            //System.out.println("oski "+ theMidikey + " "+ (tmpSoundInfo[2]-50));
            float lowerFreq = (float) ((Math.pow(2,(theMidikey-69)/12))*440f);
            float upperFreq = (float) ((Math.pow(2,(theMidikey+1-69)/12))*440f);

            float scaleRatio = upperFreq/lowerFreq;
            float hitRatio = tmpSoundInfo[2]/lowerFreq;

            float toDraw = theMidikey+(hitRatio/scaleRatio)*(upperFreq-lowerFreq);
            float toDrawUpper = theMidikey+1+(hitRatio/scaleRatio)*(upperFreq-lowerFreq);*/

            markerPaint.setStrokeWidth(7);
            //System.out.println("ozzi " + theMidikey+ " " + tmpSoundInfo[2]);

            if(correctedPitch > 36 && correctedPitch < 72) {
                correctedPitch -=37;
                theMidikey = midiHalfes[(int) correctedPitch];
                hitMidiKey = theMidikey;

            }
            float tmpLeft1 = (hitMidiKey) * mContentRect.width()/20 ;
            float tmpRight1 = (hitMidiKey) * mContentRect.width()/20 + mContentRect.width()/20;

            float tmpLeftRaw1 = (theMidikey + pitchRaw-correctedPitch) * mContentRect.width()/20;
            float tmpRightRaw1 = (theMidikey + pitchRaw-correctedPitch) * mContentRect.width()/20;
            //System.out.println("pitchraw0 "+ theMidikey+ " " + correctedPitch + " " + pitchRaw+ " " + tmpLeftRaw1);

            //System.out.println("pitchraw "+ pitchRaw + " " + correctedPitch + " " + hitMidiKey);
            if(hitMidiKey-(int)hitMidiKey == 0) {
                hitKey0Drw.setBounds((int)tmpLeft1, 0, (int)tmpRight1, (int)(mContentRect.height()));
                hitKey0Drw.draw(canvas);
                canvas.drawLine(tmpLeftRaw1,0,tmpRightRaw1,mContentRect.height(), markerPaint );

                //canvas.drawRect(tmpLeft1,0,tmpRight1,  (int)(mContentRect.height()), markerPaint );
            } else {
                hitKey1Drw.setBounds((int)tmpLeft1, 0, (int)tmpRight1, (int)(mContentRect.height()/1.5f));
                hitKey1Drw.draw(canvas);
                canvas.drawLine(tmpLeftRaw1,0,tmpRightRaw1,mContentRect.height()/1.5f, markerPaint );

                //canvas.drawRect(tmpLeft1,0,tmpRight1,  (int)(mContentRect.height()/2), markerPaint );

            }



            /*if( theMidikey % 12 == 2|| theMidikey % 12 == 4 || theMidikey % 12 == 7|| theMidikey % 12 == 9|| theMidikey % 12 == 11) {
                int tmpKey = (int)(theMidikey);

                int keypos = (int)  ((tmpKey)/2.0f);
                //keypos += (int)Math.floor(tmpKey/12.0f) *12;
                int howManyOctaves = (int)((theMidikey/12.0f)-4)*12;

                System.out.println("oskiprrh "+ howManyOctaves+" "+ tmpKey + " " + tmpKey%12 + " " + " " + ((tmpKey%12)/2.0f) + " " + Math.round((tmpKey%12)/2.0f));

                float tmpLeft = (keypos+1) * mContentRect.width()/20 - mContentRect.width()/40;
                float tmpRight = (keypos+1) * mContentRect.width()/20 + mContentRect.width()/40;
                canvas.drawRect(tmpLeft,0,tmpRight,  (int)(mContentRect.height()/1.5f), markerPaint );

            } else {
                int tmpKey = (int)(theMidikey);

                int keypos = (int) (Math.round(tmpKey/2.0f));
                //keypos += (int)Math.floor(theMidikey/12.0f) *12;
                int howManyOctaves = (int)((theMidikey/12.0f)-4)*12;

                System.out.println("oskiprrf "+howManyOctaves+" "+ tmpKey + " " + tmpKey%12 + " " + " " + ((tmpKey%12)/2.0f) + " " + Math.round((tmpKey%12)/2.0f));
                //keypos += (int)Math.floor(tmpKey/12.0f) *12;
                //int steps = tmpKey/12;
                if(tmpKey%12 ==5 ) {
                    //keypos+=1;
                }
                if(tmpKey%12 >= 4 ) {
                    //keypos-=1;
                }

                float tmpLeft = (keypos) * (mContentRect.width()/20);
                float tmpRight = (keypos) * mContentRect.width()/20 + mContentRect.width()/20;
                canvas.drawRect(tmpLeft,0,tmpRight,  (int)(mContentRect.height()), markerPaint );
            }*/

            /*if(theMidikey % 12 == 2 || theMidikey %12==4 || theMidikey %12 == 7|| theMidikey %12==9||theMidikey%12 == 11) {
                canvas.drawRect((int)((theMidikey)*(float)mContentRect.width()/36+ (float)mContentRect.width()/72),0,(int)((theMidikey)*(float)mContentRect.width()/36-((float)mContentRect.width()/72)),  (int)(mContentRect.height()/1.5f), markerPaint );

            } else {
                canvas.drawRect((int)(theMidikey)*(float)mContentRect.width()/36-(float)mContentRect.width()/72,0,(int)(theMidikey)*(float)mContentRect.width()/36+(float)mContentRect.width()/36,mContentRect.height(), markerPaint );
            }*/

            //canvas.drawLine(0,(int)(toDraw)*(float)mContentRect.height()/35,mContentRect.width(),(int)(toDraw+1)*(float)mContentRect.height()/35, markerPaint );
            //canvas.drawRect(0,(int)(toDraw)*(float)mContentRect.height()/35,mContentRect.width(),(int)(toDraw+1)*(float)mContentRect.height()/35, markerPaint );
            //canvas.drawLine(0,(toDraw)*((float)mContentRect.height()/35),mContentRect.width(),(toDraw)*((float)mContentRect.height()/35), mp3Paint );

            //System.out.println("oskiyy" + tmpSoundInfo[2] + " " +theMidikey +" " +lowerFreq+ " "+upperFreq+ hitRatio+" "+scaleRatio + " "+ toDraw);
            invalidate();
            dx = 0;
        }
    }
}