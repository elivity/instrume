package com.oskiapps.instrume;

/**
 * Created by Oskar on 04.09.2018.
 */

public class EffectClass  {

    //start and stop in samples ex. 44132
    public int start = 0;
    public int end  = 0;
    public boolean bypass = false;

    public String effectType = "";

    public void process(String inFile, String outFile, int start, int end) {



    }


    public void process(String inFile, String outFile) {



    }

    public short[] process(short[] inArray) {

        return new short[0];
    }


}
