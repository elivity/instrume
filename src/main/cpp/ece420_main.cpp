//
// Created by daran on 1/12/2017 to be used in ECE420 Sp17 for the first time.
// Modified by dwang49 on 1/1/2018 to adapt to Android 7.0 and Shield Tablet updates.
//

#include <jni.h>
#include <linux/time.h>
#include <sys/time.h>
#include <string>
#include <logging_macros.h>
//#include <Yin.h>
#include "ece420_main.h"
#include "ece420_lib.h"
#include "kiss_fft/kiss_fft.h"
#include "Util.h"

// JNI Function
extern "C" {
JNIEXPORT void JNICALL
Java_com_oskiapps_instrume_DialogAutoTuneLiveHelper_writeNewFreq(JNIEnv *env, jclass, jint);

}

// Student Variables
#define EPOCH_PEAK_REGION_WIGGLE 30
#define VOICED_THRESHOLD 200000

float betterPitch = 0;
int F_S = 44100;
int BESTBUFF = 96;
int bufferMulti = 8;
int FRAME_SIZE =96*bufferMulti * 2;
int BUFFER_SIZE =(FRAME_SIZE);
std::vector<float> bufferIn(BUFFER_SIZE);
std::vector<float> bufferOut(BUFFER_SIZE);
std::vector<int16_t> outputArr(FRAME_SIZE);

float FREQ_NEW = 300;

std::vector<float> autoc;
kiss_fft_cfg cfg;
kiss_fft_cfg cfg_ifft;

std::vector<float> theScale;

float lastPitch = 0;

std::vector<float> setUpScale(std::string  whichScale) {
    std::vector<float> tmpArr(42);
    std::vector<float> theChosenScale(7);

    float tmpCMajArr[] =  {65.41f/4.0f, 73.42f/4.0f, 82.41f/4.0f, 87.31f/4.0f,98.00f/4.0f,110.00f/4.0f,123.47f/4.0f};
    float tmpCMinArr[] = {65.41f/4.0f, 73.42f/4.0f, 77.78f/4.0f, 87.31f/4.0f,98.00f/4.0f,103.83f/4.0f,116.54f/4.0f};
    float tmpDMajArr[] =  {73.42f/4.0f, 82.41f/4.0f, 92.50f/4.0f, 98.00f/4.0f,110.0f/4.0f,123.47f/4.0f, 138.59f/4.0f};
    float tmpDMinArr[] =  {73.42f/4.0f, 82.41f/4.0f, 87.31f/4.0f, 98.00f/4.0f,110.0f/4.0f,116.54f/4.0f, 130.81f/4.0f};
    float tmpEMajArr[] = {69.30f/4.0f, 77.78f/4.0f, 82.41f/4.0f, 92.50f/4.0f,103.83f/4.0f,110.00f/4.0f,123.47f/4.0f};
    float tmpEMinArr[] = {65.41f/4.0f, 73.42f/4.0f, 82.41f/4.0f, 92.50f/4.0f,98.00f/4.0f,110.00f/4.0f,123.47f/4.0f};
    float tmpFMajArr[] =  {65.41f/4.0f, 73.42f/4.0f, 82.41f/4.0f, 87.31f/4.0f,98.00f/4.0f,110.00f/4.0f, 116.54f/4.0f};
    float tmpFMinArr[] = {65.41f/4.0f, 69.30f/4.0f, 77.78f/4.0f, 87.31f/4.0f,98.00f/4.0f,103.83f/4.0f, 116.54f/4.0f};
    float tmpGMajArr[] =  {65.41f/4.0f, 73.42f/4.0f, 82.41f/4.0f, 92.50f/4.0f,98.00f/4.0f,110.00f/4.0f,123.47f/4.0f};
    float tmpGMinArr[] =  {65.41f/4.0f, 73.42f/4.0f, 77.78f/4.0f, 87.31f/4.0f,98.00f/4.0f,110.00f/4.0f, 116.54f/4.0f};
    float tmpAMajArr[] =  {69.30f/4.0f, 73.42f/4.0f, 82.41f/4.0f, 92.50f/4.0f,103.83f/4.0f,110.00f/4.0f, 123.47f/4.0f};
    float tmpAMinArr[] = {65.41f/4.0f, 73.42f/4.0f, 82.41f/4.0f, 87.31f/4.0f,98.00f/4.0f,110.00f/4.0f, 123.47f/4.0f};
    float tmpBMajArr[] = {69.30f/4.0f, 77.78f/4.0f, 82.41f/4.0f, 92.50f/4.0f,103.83f/4.0f,116.54f/4.0f, 123.47f/4.0f};
    if (whichScale == "C-Major") {
        //theChosenScale = tmpCMajArr;
        for (int i = 0; i < 7; i++) {
            theChosenScale[i] = tmpCMajArr[i];
        }
    } else if(whichScale == "C-Minor") {
        for (int i = 0; i < 7; i++) {
            theChosenScale[i] = tmpCMinArr[i];
        }
    }
    else if(whichScale == "D-Major") {
        for (int i = 0; i < 7; i++) {
            theChosenScale[i] = tmpDMajArr[i];
        }
    } else if(whichScale == "D-Minor") {
        for (int i = 0; i < 7; i++) {
            theChosenScale[i] = tmpDMinArr[i];
        }
    } else if(whichScale == "E-Major") {
        for (int i = 0; i < 7; i++) {
            theChosenScale[i] = tmpEMajArr[i];
        }
    }else if(whichScale == "E-Minor") {
        for (int i = 0; i < 7; i++) {
            theChosenScale[i] = tmpEMinArr[i];
        }
    }else if(whichScale == "F-Major") {
        for (int i = 0; i < 7; i++) {
            theChosenScale[i] = tmpFMajArr[i];
        }
    }else if(whichScale == "F-Minor") {
        for (int i = 0; i < 7; i++) {
            theChosenScale[i] = tmpFMinArr[i];
        }
    }else if(whichScale == "G-Major") {
        for (int i = 0; i < 7; i++) {
            theChosenScale[i] = tmpGMajArr[i];
        }
    }else if(whichScale == "G-Minor") {
        for (int i = 0; i < 7; i++) {
            theChosenScale[i] = tmpGMinArr[i];
        }
    }else if(whichScale == "A-Major") {
        for (int i = 0; i < 7; i++) {
            theChosenScale[i] = tmpAMajArr[i];
        }
    }else if(whichScale == "A-Minor") {
        for (int i = 0; i < 7; i++) {
            theChosenScale[i] = tmpAMinArr[i];
        }
    }else if(whichScale == "B-Major") {
        for (int i = 0; i < 7; i++) {
            theChosenScale[i] = tmpBMajArr[i];
        }
    }

    int cntRun = 1;
    int eachStep =0;
    for(unsigned int i = 0; i < 42-7; i+= 7) {

        for(unsigned int j = 0; j < 7; j++) {
            //LOGD("ITERATOR: %i %i %f \r\n", i,cntRun, theChosenScale[j]);

            //System.out.println("oskidbg " + cntRun + " " +cntRun*j + " " + theChosenScale[j]*cntRun);
            tmpArr[eachStep] = theChosenScale[j] * cntRun;
            eachStep++;
        }
        cntRun*=2;
    }
    return tmpArr;
}


ece420_main::ece420_main() {
theScale = setUpScale("C-Major");

}

ece420_main::~ece420_main() {
    LOGV(" ece DESTRYED");
    //free(cfg);
    //free(cfg_ifft);
    //delete[] theScale;
}
  float ece420_main::pitchCorrector(float gotPitch, std::vector<float> gotScale) {
        float tmpPitch = gotPitch;

        int midiLast = (int)round(12 * log2(lastPitch / 440.0f)+69);
        int midiThis =(int)round(12 * log2(gotPitch / 440.0f)+69);

        float lastTheMidikey = (int) (12*log2(midiLast/440.0f)+69);
        float lastLowerFreq = (float) ((pow(2,(lastTheMidikey-69)/12))*440.0f);
        float lastUpperFreq = (float) ((pow(2,(lastTheMidikey+1-69)/12))*440.0f);

        float lastMiddleFreq = abs((lastLowerFreq+lastUpperFreq)/2.0f);
        float deltaLastMiddleToThis = abs(lastMiddleFreq-gotPitch);

        float deltaLast = abs(lastUpperFreq-lastLowerFreq);
        float deltaRatio = deltaLast / deltaLastMiddleToThis;

        LOGV("oskilog %f %i %i",deltaRatio, midiLast, midiThis);

        if(deltaRatio > 0.0012f) {
            tmpPitch = static_cast<float>((int)round(12 * log2(gotPitch / 440.0f)+69+1));
            tmpPitch = (float) ((pow(2,(tmpPitch-69)/12))*440.0f);
        } else {
            tmpPitch = lastPitch;
        }
        LOGV("oskitell %f" ,tmpPitch);

        return tmpPitch;
    }

void ece420_main::setScale(std::string theTxt) {
    theScale = setUpScale(theTxt);
}
 float thePitch = 1;
 float ece420_main::getThePitch() {
     return thePitch;
 }
int16_t ece420_main::getTheRawMidiNote() {
    int16_t midiThis =(int16_t)((12.0f * log2(thePitch / 440.0f)+69)*100.0f);
    LOGV("oskithemidi %i %f %f" ,midiThis,(12.0f * log2(thePitch / 440.0f)+69)*100.0f ,thePitch);

    return midiThis;
}

 int16_t ece420_main::getTheMidiNote() {
     int16_t midiThis =(int16_t)(12 * log2(thePitch / 440.0f)+69);
     LOGV("oskithemidi %i %f %f" ,midiThis,(12 * log2(thePitch / 440.0f)+69) ,thePitch);

     return midiThis;
 }


float ece420_main::midiToFreq(int midiNote) {

    float midiThis = static_cast<float>((pow(2, (midiNote-69) / 12.0f)) * 440.0f);
    LOGV("oskithemidi %f %f %f" ,midiThis,(12 * log2(thePitch / 440.0f)+69) ,thePitch);

    return midiThis;
}

 void ece420_main::setBestParams(int buffsize, int sampleRate) {
     F_S = sampleRate;
     float bufferOptimalRatio = 1;

     if(buffsize > 96*8*4) {
         BESTBUFF = buffsize;
         bufferOptimalRatio = static_cast<float>((96.0 * 8*8) / (BESTBUFF) );
     } else {
         BESTBUFF = buffsize;
         bufferOptimalRatio = static_cast<float>((96.0 * 8*8) / (BESTBUFF ));
     }

     if(bufferOptimalRatio > 1) {
         bufferMulti =   (bufferOptimalRatio);
     } else {
         bufferMulti = 1;
     }

    FRAME_SIZE = BESTBUFF*bufferMulti;
    BUFFER_SIZE = ( FRAME_SIZE);

    BUFFER_SIZE = static_cast<int>(BUFFER_SIZE/4);

    bufferIn.resize(BUFFER_SIZE);
    bufferOut.resize(BUFFER_SIZE);
    outputArr.resize(FRAME_SIZE);

    autoc.resize(BUFFER_SIZE);

    cfg = kiss_fft_alloc(BUFFER_SIZE, false, 0, 0);
    cfg_ifft = kiss_fft_alloc(BUFFER_SIZE, true, 0, 0);

    LOGV("oskipassed %i %i %i %i",BESTBUFF,bufferMulti,BUFFER_SIZE, F_S);

    //newEpochIdx = FRAME_SIZE;

 }

std::vector<int16_t> ece420_main::ece420ProcessFrame2(std::vector<int16_t> gotBufferIn) {
    // Keep in mind, we only have 20ms to process each buffer!
    struct timeval start;
    struct timeval end;
    gettimeofday(&start, NULL);

    // Shift our old data back to make room for the new data
    for (int i = 0; i < 2 * FRAME_SIZE; i++) {
        bufferIn[i] = bufferIn[i + FRAME_SIZE - 1];
    }

    // Finally, put in our new data.
    for (int i = 0; i < FRAME_SIZE; i++) {
        bufferIn[i + 2 * FRAME_SIZE - 1] = ((float) gotBufferIn[i]);
    }
    //LOGD("preVoiced true: %f %f us",  bufferIn[0], bufferIn[2 * FRAME_SIZE]);

    // The whole kit and kaboodle -- pitch shift
    bool isVoiced = true;// lab5PitchShift(bufferIn);
    LOGD("Voiced true: %i us",  isVoiced);

    //bool isVoiced = true;

        for (int i = 0; i < FRAME_SIZE; i++) {
            //int16_t newVal = (int16_t) bufferOut[i];
            if (isVoiced) {
                outputArr[i] = bufferOut[i];
            } else {
                outputArr[i] = bufferIn[i+2*FRAME_SIZE];
            }
        }

    // Very last thing, update your output circular buffer!
    for (int i = 0; i < 2 * FRAME_SIZE; i++) {
        bufferOut[i] = bufferOut[i + FRAME_SIZE - 1];
    }

    for (int i = 0; i < FRAME_SIZE; i++) {
        //outputArr[i] = (int16_t)bufferOut[i];

        bufferOut[i + 2 * FRAME_SIZE - 1] = 0;

    }

    gettimeofday(&end, NULL);
    LOGD("Time delay:oski %i %ld us %i", outputArr[111], ((end.tv_sec * 1000000 + end.tv_usec) - (start.tv_sec * 1000000 + start.tv_usec)), outputArr[100]);
    return outputArr;
}


std::vector<float> passbuffer;
bool highVariance = false;
float ece420_main::detectBufferPeriod2ThreadSafe(std::vector<float> &buffer) {
    passbuffer.clear();
    passbuffer.resize(buffer.size()/2);

        for(int i = 0; i < passbuffer.size(); i++) {
            //passbuffer[i] = getHanningCoef((int)buffer.size(),buffer[i]);
           passbuffer[i] = buffer[i*2];
        }

        LOGV("oskios %f %f  %f %i %i %i",passbuffer[10], (float)buffer[10], buffer[10], (int) buffer.size(), BUFFER_SIZE,(int)passbuffer.size());

            kiss_fft_cpx buffer_in[BUFFER_SIZE];
            kiss_fft_cpx buffer_fft[BUFFER_SIZE];

            //LOGV("oskiii multi %f", multi);
            for (int i = 0; i < BUFFER_SIZE; i++) {
                buffer_in[i].r = passbuffer[i];
                buffer_in[i].i = 0;
            }

            //LOGV("oski smaller than threshold1.85 %f", buffer_fft[10].r);

            kiss_fft(cfg, buffer_in, buffer_fft);
            //free(cfg);


            // Autocorrelation is given by:
            // autoc = ifft(fft(x) * conj(fft(x))
            //
            // Also, (a + jb) (a - jb) = a^2 + b^2

            kiss_fft_cpx multiplied_fft[BUFFER_SIZE];
            kiss_fft_cpx autoc_kiss[BUFFER_SIZE];
            for (int i = 0; i < BUFFER_SIZE; i++) {

                multiplied_fft[i].r = (buffer_fft[i].r * buffer_fft[i].r)
                                      + (buffer_fft[i].i * buffer_fft[i].i);
                multiplied_fft[i].i = 0;
            }

            kiss_fft(cfg_ifft, multiplied_fft, autoc_kiss);
            //free(cfg_ifft);

            for (int i = 0; i < BUFFER_SIZE; i++) {
                autoc[i] = (autoc_kiss[i].r);
            }
            //LOGV("oskiii autoc %f" ,(float)autoc[10]);

            vector<int> out;

            findPeaks(autoc, out);

            for(int i=0; i<10; ++i) {
                //LOGV("oskiouts %i",out[i]);
                if(abs(out[i]) > 5000) {
                    highVariance = true;
                }
            }
        // Move to a normal float arrays rather than a struct arrays of r/i components


        // We're only interested in pitches below 1000Hz.
        // Why does this line guarantee we only identify pitches below 1000Hz?

        int minIdx = F_S / 1000;
        int maxIdx = BUFFER_SIZE / 2;

        int periodLen = findMaxArrayIdx(autoc, minIdx, maxIdx);
        float freq = ((float) F_S) / periodLen;
        if(highVariance) {
            //freq = 0;
            highVariance = false;
        }
        thePitch = freq;
        // TODO: tune
        if (freq < 10) {
            periodLen = -1;
        }
        betterPitch = pitchCorrector(freq, theScale);
        if(betterPitch < 10 || betterPitch> 600) {
            betterPitch = 0;
        }
        lastPitch = betterPitch;

        return freq;;
}


std::vector<float> ece420_main::getSpectrum(std::vector<int16_t> gotBuffer) {
    kiss_fft_cpx buffer_in[BUFFER_SIZE];
    kiss_fft_cpx buffer_fft[BUFFER_SIZE];

    //LOGV("oskiii multi %f", multi);
    for (int i = 0; i < BUFFER_SIZE; i++) {
        buffer_in[i].r = (float)(gotBuffer[i]);;
        buffer_in[i].i = 0;
    }

    //LOGV("oski smaller than threshold1.85 %f", buffer_fft[10].r);

    kiss_fft(cfg, buffer_in, buffer_fft);
    //free(cfg);

    std::vector<float> retFreqs(static_cast<unsigned long>(BUFFER_SIZE));
    for(int i = 0; i < BUFFER_SIZE; i++) {
        retFreqs[i] = buffer_fft[i].r;
    }
    return retFreqs;

}


JNIEXPORT void JNICALL
Java_com_oskiapps_instrume_DialogAutoTuneLiveHelper_writeNewFreq(JNIEnv *env, jclass, jint newFreq) {
    //FREQ_NEW_ANDROID = (int) newFreq;
    return;
}
