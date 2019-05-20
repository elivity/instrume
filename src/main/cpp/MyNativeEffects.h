#ifndef SAMPLES_MYNATIVEEFFECTS_H
#define SAMPLES_MYNATIVEEFFECTS_H

#include <jni.h>
#include <oboe/Oboe.h>
#include <android/asset_manager_jni.h>

#include <string>
#include <thread>
#include <fstream>
#include <BiQuad.h>

class MyNativeEffects {
public:

    MyNativeEffects();
    ~MyNativeEffects();

    stk::BiQuad * reconfigureBiQuad(int type, int cf, double gotGainDB, double theQ);


    bool doEQ(const char *inFile, const char *outFile, jlong gotStart, jlong gotEnd, jint freq1,
              jfloat gain1, jint freq2, jfloat gain2, jint freq3, jfloat gain3);

    bool doReverb(const char *inFile, const char *outFile, int gotStart, int gotEnd, float gotDryWet,
                  float gotDecay);
    float *visualizerArray;
    int howManySteps;
    int deviceSampleRate = 48000;
    double drawPixelRatio = (deviceSampleRate / 1000.0)*4;

    void doAutoTune(const char *inFile, const char *outFile, jint gotStart, jint gotEnd,
                    std::string gotString, float gotDryWet);


    void
    doAutoTuneRetInt(const char *inFile, const char *outFile, jint gotStart, jint gotEnd,
                     std::string gotString, float gotDryWet);

    void doGain(const char *string, const char *string1, jint i, jint i1, jfloat d);


    void setBestParams(int theSampleRate);

};
#endif  // OBOE_LIVEEFFECTENGINE_H
