#include "ece420_main.h"
#include "MyNativeEffects.h"
//#include <assert.h>
#include <logging_macros.h>
#include <climits>
#include <oboe/Oboe.h>
#include "LiveEffectEngine.h"

#include <cstdlib>
#include <inttypes.h>
#include <cmath>
#include <fstream>
#include <FreeVerb.h>
#include <Effect.h>
#include <Stk.h>
#include <BiQuad.h>
#include <NRev.h>
#include <PitShift.h>

//stk::FreeVerb myfreeverb;
//stk::BiQuad *biQuad;


template<typename T, typename... Args>
std::unique_ptr<T> make_unique(Args&&... args)
{
    return std::unique_ptr<T>(new T(std::forward<Args>(args)...));
}



MyNativeEffects::MyNativeEffects() {

}

MyNativeEffects::~MyNativeEffects() {
}

bool MyNativeEffects::doEQ(const char *inFile, const char *outFile, jlong gotStart, jlong gotEnd, jint freq1,jfloat gain1,jint freq2,jfloat gain2,jint freq3, jfloat gain3) {
    stk::BiQuad *biQuad1 = reconfigureBiQuad(5,freq1,gain1,1);
    stk::BiQuad *biQuad3 = reconfigureBiQuad(6,freq3,gain3,1);
    stk::BiQuad *biQuad2 = reconfigureBiQuad(3,freq2,gain2,1);;

    int buffsize = 96*8*2*2;

    stk::StkFrames processFloats(96*8,2);
    const char* theFileSrc = inFile;
    //theFile = fopen(theFileSrc,"r");
    const char* theFileSrcOut = outFile;
    //theFileOut = fopen(theFileSrcOut,"w+");

    std::ifstream theInFxStream;
    std::fstream theOutFxStream;
    theInFxStream = std::ifstream();
    theInFxStream.open(theFileSrc,std::ios_base::binary);
    theOutFxStream = std::fstream();
    theOutFxStream.open(theFileSrcOut,std::ios_base::out | std::ios_base::in | std::ios_base::binary);


    //theInStream.ignore( std::numeric_limits<std::streamsize>::max() );
    //std::streamsize length = theInStream.gcount();
    //theInStream.clear();   //  Since ignore will have set eof.

    long startInBytes = gotStart * (long)drawPixelRatio;
    long endInBytes = gotEnd * (long)drawPixelRatio;

    char*inBuff = new char[buffsize];
    int16_t* outShorts = new int16_t[buffsize/2];
    //LOGE("oski eqtest %li %li",startInBytes, gotStart);

    for(long i = startInBytes; i < endInBytes; i+=buffsize) {
        LOGV("oski sherlock #5 writing!! deepest doeq %f %f %li",gain2, gain2,i);

        theInFxStream.seekg(i);
        theInFxStream.read(inBuff, buffsize);
        const int16_t *audioBuffer = reinterpret_cast<const int16_t*>(inBuff);
        /*for(int j = 0; j < buffsize/2; j++) {
            processFloats[j] = (stk::StkFloat)audioBuffer[j]/(stk::StkFloat)SHRT_MAX;
        }*/

        for(int j = 0; j < buffsize/2; j++) {
            processFloats[j] = (stk::StkFloat)audioBuffer[j]/(stk::StkFloat)SHRT_MAX;

            processFloats[j] = biQuad1->tick(processFloats[j]);
            processFloats[j] += biQuad2->tick(processFloats[j]);
            processFloats[j] += biQuad3->tick(processFloats[j]);
            processFloats[j] /= 3;
            outShorts[j] = processFloats[j]*SHRT_MAX;
        }
        theOutFxStream.seekp(i);
        theOutFxStream.write(reinterpret_cast<const char *>(outShorts),buffsize);
        LOGV("oski sherlock #5 deepest outputcheck %i %f",audioBuffer[100], gain2);

        /**/
        if (audioBuffer == nullptr){
            LOGE("Could not get buffer for track");
            return false;
        }
    }
    theInFxStream.close();
    theOutFxStream.close();

    delete biQuad1;
    delete biQuad2;
    delete biQuad3;

    if(inBuff) {
        delete[] inBuff;
    }
    if(outShorts){
        delete[] outShorts;
    }

    return true;
}

bool MyNativeEffects::doReverb(const char *inFile,const char *outFile, int gotStart, int gotEnd, float gotDryWet, float gotDecay) {
    int buffsize = 96*8*2*2;

    static stk::NRev myNRev;
    myNRev.setT60(gotDecay);
    //myfreeverb.setDamping(0.2);
    //myfreeverb.setSampleRate(48000);
    myNRev.setEffectMix(gotDryWet);
    stk::StkFrames processFloats(96*8,2);
    const char* theFileSrc = inFile;
    //theFile = fopen(theFileSrc,"r");
    const char* theFileSrcOut = outFile;
    //theFileOut = fopen(theFileSrcOut,"w+");
    std::ifstream theInFxStream;
    std::fstream theOutFxStream;
    theInFxStream = std::ifstream();
    theInFxStream.open(theFileSrc,std::ios_base::binary);
    theOutFxStream = std::fstream();
    theOutFxStream.open(theFileSrcOut,std::ios_base::out | std::ios_base::in | std::ios_base::binary);


    //theInStream.ignore( std::numeric_limits<std::streamsize>::max() );
    //std::streamsize length = theInStream.gcount();
    //theInStream.clear();   //  Since ignore will have set eof.

    long startInBytes = gotStart * (long)drawPixelRatio;
    long endInBytes = gotEnd * (long)drawPixelRatio;

    char*inBuff = new char[buffsize];
    int16_t* outShorts = new int16_t[buffsize/2];
    //LOGE("oski eqtest %li %i",startInBytes, gotStart);

    for(long i = startInBytes; i < endInBytes; i+=buffsize) {
        theInFxStream.seekg(i);
        theInFxStream.read(inBuff, buffsize);
        const int16_t *audioBuffer = reinterpret_cast<const int16_t*>(inBuff);
        for(int j = 0; j < buffsize/2; j++) {
            processFloats[j] = (stk::StkFloat)audioBuffer[j]/(stk::StkFloat)SHRT_MAX;
        }
        processFloats = myNRev.tick(processFloats);
        for(int j = 0; j < buffsize/2; j++) {
            outShorts[j] = processFloats[j]*SHRT_MAX;
        }
        theOutFxStream.seekp(i);
        theOutFxStream.write(reinterpret_cast<const char *>(outShorts),buffsize);
        /**/
        if (audioBuffer == nullptr){
            LOGE("Could not get buffer for track");
            return false;
        }
    }

    theInFxStream.close();
    theOutFxStream.close();
    return true;
}

//jint* retDrawPitchesArray;

void MyNativeEffects::setBestParams(int theSampleRate) {
    deviceSampleRate = theSampleRate;
    drawPixelRatio = (theSampleRate/1000.0)*4;
}


void MyNativeEffects::doAutoTuneRetInt(const char *inFile, const char *outFile, jint gotStart, jint gotEnd, std::string gotString, float gotDryWet) {
    //stk::PitShift * myEffectsPitShift = new stk::PitShift;

    std::unique_ptr<ece420_main> eceAutoTune(new ece420_main()); // = std::make_shared<ece420_main>();
    std::unique_ptr<stk::PitShift> myEffectsPitShift(new stk::PitShift()); // = std::make_shared<ece420_main>();

    //int16_t *toBufferIn = new int16_t[96*6*2];
    int buffsize = 2048*4;
    //std::vector<int16_t> processOutput(buffsize/2);
    //stk::PitShift myPitShift;
    myEffectsPitShift->setSampleRate(deviceSampleRate);

    //const char* theFileSrc = inFile;
    //theFile = fopen(theFileSrc,"r");
    const char* theFileSrcOut = outFile;
    //theFileOut = fopen(theFileSrcOut,"w+");
    //std::ifstream theInFxStream;
    std::fstream theOutFxStream;
    //theInFxStream = std::ifstream();
    //theInFxStream.open(theFileSrc,std::ios_base::binary);
    theOutFxStream = std::fstream();
    theOutFxStream.open(theFileSrcOut,std::ios_base::out | std::ios_base::in | std::ios_base::binary);

    //theInStream.ignore( std::numeric_limits<std::streamsize>::max() );
    //std::streamsize length = theInStream.gcount();
    //theInStream.clear();   //  Since ignore will have set eof.

    long startInBytes = gotStart * (long)drawPixelRatio;
    long endInBytes = gotEnd * (long)drawPixelRatio;

    char*inBuff = new char[buffsize];

    LOGV("oski outshortslast auto");
    int16_t* outShorts = new int16_t[buffsize/2];
    //eceAuto = new ece420_main();
    eceAutoTune->setBestParams(2048*2,deviceSampleRate);
    eceAutoTune->setScale(gotString);

    //howManySteps = static_cast<int>((endInBytes - startInBytes) / buffsize);
    //retDrawPitchesArray = new jint[howManySteps];
    int16_t stepCnt = 0;
    float theRatio = 1;
    myEffectsPitShift->setEffectMix(gotDryWet);
    int16_t *audioBuffer = 0;

    //Todo make it a shared or unique ptr
    std::vector<float> tmpAudioVec(static_cast<unsigned long>(buffsize / 2));

    for(long i = startInBytes; i < endInBytes; i+=buffsize, stepCnt++) {

        if(i+buffsize < endInBytes) {
            stk::StkFrames outputFrames(2048*2,1);

            theOutFxStream.seekg(i);
            theOutFxStream.read(inBuff, buffsize);
            audioBuffer = reinterpret_cast<int16_t*>(inBuff);
            for(int j = 0; j < buffsize/2; j++) {
                tmpAudioVec[j] = audioBuffer[j];
                outputFrames[j] =(stk::StkFloat)tmpAudioVec[j]/(stk::StkFloat)SHRT_MAX;
            }
            theRatio = eceAutoTune->detectBufferPeriod2ThreadSafe(tmpAudioVec);

            float procPitch = eceAutoTune->getThePitch();

            //if(theRatio > 0.8 && theRatio < 1.2)

            if(procPitch < 350){
                myEffectsPitShift->setShift(theRatio);
            }
            //processOutput =  eceAuto->ece420ProcessFrame2(tmpAudioVec);

            LOGE("oskido autotunetest retint %li %li %li  %i %i %f %f",i,startInBytes,endInBytes, gotStart,audioBuffer[40], theRatio, procPitch);
            //if(stepCnt<1000)

            if(procPitch < 350 && procPitch > 10)
            {
                visualizerArray[stepCnt] = procPitch/2;

            } else {
                visualizerArray[stepCnt] = 0;
            }
            for(int j = 0; j < buffsize/2; j++) {
                outputFrames[j] = myEffectsPitShift->tick(outputFrames[j]);
                outShorts[j] = static_cast<int16_t>(outputFrames[j] * SHRT_MAX);// processOutput[j];
            }
            theOutFxStream.seekp(i);
            theOutFxStream.write(reinterpret_cast<const char *>(outShorts),buffsize);
            /**/
            if (audioBuffer == nullptr){
                LOGE("Could not get buffer for track");
                //return false;
            }
        }

    }
    howManySteps = stepCnt;

    //eceAuto.freeCfgs();
    //theInFxStream.close();
    //theOutFxStream.close();
    //delete myPitShift;

    //delete myEffectsPitShift;
    if(theOutFxStream.is_open()) {
        theOutFxStream.close();
    }
    if(outShorts) {
        delete[] outShorts;
    }

    if(audioBuffer != 0) {
        delete[] audioBuffer;
    }
        //delete(myPitShift);
    //delete eceAuto;
    //return retDrawPitchesArray;

}


void MyNativeEffects::doAutoTune(const char *inFile, const char *outFile, jint gotStart, jint gotEnd, std::string gotString, float gotDryWet) {
    std::unique_ptr<ece420_main> eceAutoTune( new ece420_main());
    std::unique_ptr<stk::PitShift> myEffectsPitShift( new stk::PitShift());

    //int16_t *toBufferIn = new int16_t[96*6*2];
    int buffsize = 8192;
    //std::vector<int16_t> processOutput(buffsize/2);

    myEffectsPitShift->setSampleRate(deviceSampleRate);

    //const char* theFileSrc = inFile;
    //theFile = fopen(theFileSrc,"r");
    const char* theFileSrcOut = outFile;
    //theFileOut = fopen(theFileSrcOut,"w+");
    //std::ifstream theInFxStream;
    std::fstream theOutFxStream;
    //theInFxStream = std::ifstream();
    //theInFxStream.open(theFileSrc,std::ios_base::binary);
    theOutFxStream = std::fstream();
    theOutFxStream.open(theFileSrcOut,std::ios_base::out | std::ios_base::in | std::ios_base::binary);

    //theInStream.ignore( std::numeric_limits<std::streamsize>::max() );
    //std::streamsize length = theInStream.gcount();
    //theInStream.clear();   //  Since ignore will have set eof.

    long startInBytes = gotStart * (long)drawPixelRatio;
    long endInBytes = gotEnd * (long)drawPixelRatio;

    char*inBuff = new char[buffsize];
    int16_t* outShorts = new int16_t[buffsize/2];
    //eceAuto = new ece420_main();
    eceAutoTune->setBestParams(8192/4,(int)deviceSampleRate);
    eceAutoTune->setScale(gotString);

    //howManySteps = static_cast<int>((endInBytes - startInBytes) / buffsize);
    //jint * retDrawPitchesArray = new jint[howManySteps];
    int16_t stepCnt = 0;
    float theRatio = 1;
    myEffectsPitShift->setEffectMix(gotDryWet);
    int16_t *audioBuffer;
    std::vector<int16_t> tmpAudioVec(static_cast<unsigned long>(buffsize / 2));

    for(long i = startInBytes; i < endInBytes; i+=buffsize, stepCnt++) {

        if(i+buffsize < endInBytes) {
            stk::StkFrames outputFrames(8192/4,2);

            theOutFxStream.seekg(i);
            theOutFxStream.read(inBuff, buffsize);
            audioBuffer = reinterpret_cast<int16_t*>(inBuff);
            for(int j = 0; j < buffsize/2; j++) {
                tmpAudioVec[j] = audioBuffer[j];
                outputFrames[j] =(stk::StkFloat)tmpAudioVec[j]/(stk::StkFloat)SHRT_MAX;
            }
            //theRatio = eceAutoTune->detectBufferPeriod2LongMode(tmpAudioVec);


            if(eceAutoTune->getThePitch() < 350){
                myEffectsPitShift->setShift(theRatio);
            }
            //processOutput =  eceAuto->ece420ProcessFrame2(tmpAudioVec);
            LOGE("oskido autotunetest  %li %li %li  %i ",i,startInBytes,endInBytes, gotStart);
            //int procPitch = eceAuto.getThePitch();
            //if(stepCnt<1000)
            /*if(procPitch < 600 && procPitch > 100)
            {
                retDrawPitchesArray[stepCnt] = procPitch;

            }*/
            for(int j = 0; j < buffsize/2; j++) {
                outputFrames[j] = myEffectsPitShift->tick(outputFrames[j]);
                outShorts[j] = static_cast<int16_t>(outputFrames[j] * SHRT_MAX);// processOutput[j];
            }
            theOutFxStream.seekp(i);
            theOutFxStream.write(reinterpret_cast<const char *>(outShorts),buffsize);
            /**/
            if (audioBuffer == nullptr){
                LOGE("Could not get buffer for track");
                //return false;
            }
        }

    }

    //eceAuto.freeCfgs();

    //theInFxStream.close();
    //theOutFxStream.close();
    //delete myPitShift;

    //delete(myPitShift);
    //delete eceAuto;
    //return retDrawPitchesArray;
    //delete[] inBuff;

    //delete myEffectsPitShift;

    if(theOutFxStream.is_open()) {
        theOutFxStream.close();
    }

    delete[] outShorts;
    delete[] inBuff;

}


void MyNativeEffects::doGain(const char *inFile, const char *outFile, jint gotStart, jint gotEnd, jfloat gotGain) {
    int buffsize = 8192;
    //std::vector<int16_t> processOutput(buffsize/2);

    //const char* theFileSrc = inFile;
    //theFile = fopen(theFileSrc,"r");
    const char* theFileSrcOut = outFile;
    //theFileOut = fopen(theFileSrcOut,"w+");
    //std::ifstream theInFxStream;
    std::fstream theOutFxStream;
    //theInFxStream = std::ifstream();
    //theInFxStream.open(theFileSrc,std::ios_base::binary);
    theOutFxStream = std::fstream();
    theOutFxStream.open(theFileSrcOut,std::ios_base::out | std::ios_base::in | std::ios_base::binary);

    //theInStream.ignore( std::numeric_limits<std::streamsize>::max() );
    //std::streamsize length = theInStream.gcount();
    //theInStream.clear();   //  Since ignore will have set eof.

    long startInBytes = gotStart * (long)drawPixelRatio;
    long endInBytes = gotEnd * (long)drawPixelRatio;
    LOGE("oskido gainval  %f", gotGain);

   // int16_t* outShorts = new int16_t[buffsize/2];

    int stepCnt = 0;
    for(long i = startInBytes; i < endInBytes; i+=buffsize, stepCnt++) {
        char*inBuff = new char[buffsize];

        //int16_t *outFloats = new int16_t[buffsize/2];

        theOutFxStream.seekg(i);
        theOutFxStream.read(inBuff, buffsize);
        //std::vector<int16_t> tmpAudioVec(static_cast<unsigned long>(buffsize / 2));
        int16_t *audioBuffer = reinterpret_cast<int16_t*>(inBuff);

        for(int j = 0; j < buffsize/2; j++) {
            audioBuffer[j] = (int16_t)(audioBuffer[j]*gotGain);// (int16_t)(audioBuffer[i]*gotGain);
        }

        theOutFxStream.seekp(i);
        theOutFxStream.write(reinterpret_cast<const char *>(audioBuffer),buffsize);
        /*if(audioBuffer) {
            delete[] audioBuffer;
        }*/

        /*if(outFloats) {
            delete[] outFloats;
        }*/
        if(inBuff) {
            delete[] inBuff;
        }
    }



    if(theOutFxStream.is_open()) {
        theOutFxStream.close();
    }
    //delete[] outShorts;
    ///delete[] outFloats;
    //theOutFxStream.close();
    //theInFxStream.close();
}



stk::BiQuad * MyNativeEffects::reconfigureBiQuad(int type, int cf, double gotGainDB, double theQ) {
    const static int LOWPASS = 0;
    const static int HIGHPASS = 1;
    const static int BANDPASS = 2;
    const static int PEAK = 3;
    const static int NOTCH = 4;
    const static int LOWSHELF = 5;
    const static int HIGHSHELF = 6;
    static int center_freq =1000;
    static double gain_abs = 0.5;
    static double gainDB = 1;
    static int sample_rate = deviceSampleRate;
    static double globalQ = 0;
    double a0 =0, a1=0, a2=0, b0=0, b1=0, b2=0;

    stk::BiQuad *localBiQuad = new stk::BiQuad();
    globalQ = theQ;
    gainDB = gotGainDB;

    center_freq = cf;
    // only used for peaking and shelving filters
    gain_abs = pow(10, gainDB / 20);
    double omega = 2 * M_PI * cf / sample_rate;
    double sn = sin(omega);
    double cs = cos(omega);
    double alpha = sn / (2 * theQ);
    double beta = sqrt(gain_abs + gain_abs);
    //System.out.println("oski print " + Q +" "+ gain_abs + " " + gainDB);
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
            gain_abs = pow(10, gainDB / 40);
            alpha =  sin(omega)/2 * sqrt( (gain_abs + 1/gain_abs)*(1/theQ - 1) + 2 );
            beta = sqrt(gain_abs + gain_abs);
            b0 = gain_abs * ((gain_abs + 1) - (gain_abs - 1) * cs + beta * sn);
            b1 = 2 * gain_abs * ((gain_abs - 1) - (gain_abs + 1) * cs);
            b2 = gain_abs * ((gain_abs + 1) - (gain_abs - 1) * cs - beta * sn);
            a0 = (gain_abs + 1) + (gain_abs - 1) * cs + beta * sn;
            a1 = -2 * ((gain_abs - 1) + (gain_abs + 1) * cs);
            a2 = (gain_abs + 1) + (gain_abs - 1) * cs - beta * sn;
            break;
        case HIGHSHELF:
            gain_abs = pow(10, gainDB / 40);
            alpha =  sin(omega)/2 * sqrt( (gain_abs + 1/gain_abs)*(1/theQ - 1) + 2 );
            beta = sqrt(gain_abs + gain_abs);
            b0 = gain_abs * ((gain_abs + 1) + (gain_abs - 1) * cs + beta * sn);
            b1 = -2 * gain_abs * ((gain_abs - 1) + (gain_abs + 1) * cs);
            b2 = gain_abs * ((gain_abs + 1) + (gain_abs - 1) * cs - beta * sn);
            a0 = (gain_abs + 1) - (gain_abs - 1) * cs + beta * sn;
            a1 = 2 * ((gain_abs - 1) - (gain_abs + 1) * cs);
            a2 = (gain_abs + 1) - (gain_abs - 1) * cs - beta * sn;
            break;
        default:break;
    }


    // prescale flter constants
    b0 /= a0;
    b1 /= a0;
    b2 /= a0;
    a1 /= a0;
    a2 /= a0;

    localBiQuad->setCoefficients(b0,b1,b2,a1,a2);
    return localBiQuad;
}
