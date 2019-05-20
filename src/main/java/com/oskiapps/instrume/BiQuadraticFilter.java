package com.oskiapps.instrume;

/**
 * Created by Oskar on 11.09.2018.
 */

/**
 *
 * @author lutusp
 */
// http://en.wikipedia.org/wiki/Digital_biquad_filter

final public class BiQuadraticFilter {

    final static int LOWPASS = 0;
    final static int HIGHPASS = 1;
    final static int BANDPASS = 2;
    final static int PEAK = 3;
    final static int NOTCH = 4;
    final static int LOWSHELF = 5;
    final static int HIGHSHELF = 6;
    public double a0, a1, a2, b0, b1, b2;
    double x1, x2, y, y1, y2;
    double gain_abs;
    int type;
    double center_freq, sample_rate, Q, gainDB;

    public BiQuadraticFilter() {
    }

    public BiQuadraticFilter(int type, double center_freq, double sample_rate, double Q, double gainDB) {
        configure(type, center_freq, sample_rate, Q, gainDB);
    }

    // constructor without gain setting
    public BiQuadraticFilter(int type, double center_freq, double sample_rate, double Q) {
        configure(type, center_freq, sample_rate, Q, 0);
    }

    public void reset() {
        x1 = x2 = y1 = y2 = 0;
    }

    public double frequency() {
        return center_freq;
    }

    public void configure(int type, double center_freq, double sample_rate, double Q, double gainDB) {
        reset();
        Q = (Q == 0) ? 1e-9 : Q;
        this.type = type;
        this.sample_rate = sample_rate;
        this.Q = Q;
        this.gainDB = gainDB;
        reconfigure(center_freq);
    }

    public void configure(int type, double center_freq, double sample_rate, double Q) {
        configure(type, center_freq, sample_rate, Q, 0);
    }

    // allow parameter change while running
    public void reconfigure(double cf) {
        center_freq = cf;
        // only used for peaking and shelving filters
        gain_abs = Math.pow(10, gainDB / 20);
        double omega = 2 * Math.PI * cf / sample_rate;
        double sn = Math.sin(omega);
        double cs = Math.cos(omega);
        double alpha = sn / (2 * Q);
        double beta = Math.sqrt(gain_abs + gain_abs);
        System.out.println("oski print " + Q +" "+ gain_abs + " " + gainDB);
        switch (type) {
            case LOWPASS:


                b0 = (1 - cs) / 2;
                b1 = 1 - cs;
                b2 = (1 - cs) / 2;
                a0 = (1 + alpha);
                a1 = -2 * cs;
                a2 = 1 - alpha;
                break;
            case HIGHPASS:
                b0 = (1 + cs) / 2;
                b1 = -(1 + cs);
                b2 = (1 + cs) / 2;
                a0 = 1 + alpha;
                a1 = -2 * cs;
                a2 = 1 - alpha;
                break;
            case BANDPASS:
                b0 = alpha;
                b1 = 0;
                b2 = -alpha;
                a0 = 1 + alpha;
                a1 = -2 * cs;
                a2 = 1 - alpha;
                break;
            case NOTCH:
                b0 = 1;
                b1 = -2 * cs;
                b2 = 1;
                a0 = 1 + alpha;
                a1 = -2 * cs;
                a2 = 1 - alpha;
                break;
            case PEAK:
                b0 = 1 + (alpha * gain_abs);
                b1 = -2 * cs;
                b2 = 1 - (alpha * gain_abs);
                a0 = 1 + (alpha / gain_abs);
                a1 = -2 * cs;
                a2 = 1 - (alpha / gain_abs);
                break;
            case LOWSHELF:
                gain_abs = Math.pow(10, gainDB / 40);
                alpha =  Math.sin(omega)/2 * Math.sqrt( (gain_abs + 1/gain_abs)*(1/Q - 1) + 2 );
                beta = Math.sqrt(gain_abs + gain_abs);
                b0 = gain_abs * ((gain_abs + 1) - (gain_abs - 1) * cs + beta * sn);
                b1 = 2 * gain_abs * ((gain_abs - 1) - (gain_abs + 1) * cs);
                b2 = gain_abs * ((gain_abs + 1) - (gain_abs - 1) * cs - beta * sn);
                a0 = (gain_abs + 1) + (gain_abs - 1) * cs + beta * sn;
                a1 = -2 * ((gain_abs - 1) + (gain_abs + 1) * cs);
                a2 = (gain_abs + 1) + (gain_abs - 1) * cs - beta * sn;
                break;
            case HIGHSHELF:
                gain_abs = Math.pow(10, gainDB / 40);
                alpha =  Math.sin(omega)/2 * Math.sqrt( (gain_abs + 1/gain_abs)*(1/Q - 1) + 2 );
                beta = Math.sqrt(gain_abs + gain_abs);
                b0 = gain_abs * ((gain_abs + 1) + (gain_abs - 1) * cs + beta * sn);
                b1 = -2 * gain_abs * ((gain_abs - 1) + (gain_abs + 1) * cs);
                b2 = gain_abs * ((gain_abs + 1) + (gain_abs - 1) * cs - beta * sn);
                a0 = (gain_abs + 1) - (gain_abs - 1) * cs + beta * sn;
                a1 = 2 * ((gain_abs - 1) - (gain_abs + 1) * cs);
                a2 = (gain_abs + 1) - (gain_abs - 1) * cs - beta * sn;
                break;
        }


        // prescale flter constants
        b0 /= a0;
        b1 /= a0;
        b2 /= a0;
        a1 /= a0;
        a2 /= a0;
    }

    // provide a static amplitude result for testing
    public double result(double f) {
        double phi = Math.pow((Math.sin(2.0 * Math.PI * f / (2.0 * sample_rate))), 2.0);
        double r = (Math.pow(b0 + b1 + b2, 2.0) - 4.0 * (b0 * b1 + 4.0 * b0 * b2 + b1 * b2) * phi + 16.0 * b0 * b2 * phi * phi) / (Math.pow(1.0 + a1 + a2, 2.0) - 4.0 * (a1 + 4.0 * a2 + a1 * a2) * phi + 16.0 * a2 * phi * phi);
        //System.out.println("oski phi "+ phi);
        if(r < 0) {
            r = 0;
        }
        return Math.sqrt(r);
    }

    // provide a static decibel result for testing
    public double log_result(double f) {
        double r;
        try {
            r = 20 * Math.log10(result(f));
        } catch (Exception e) {
            r = -100;
        }
        if(Double.isInfinite(r) || Double.isNaN(r)) {
            r = -100;
        }
        return r;
    }

    // return the constant set for this filter
    public double[] constants() {
        return new double[]{a1, a2, b0, b1, b2};
    }

    // perform one filtering step
    public double filter(double x) {
        y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2;
        x2 = x1;
        x1 = x;
        y2 = y1;
        y1 = y;
        return (y);
    }
}
