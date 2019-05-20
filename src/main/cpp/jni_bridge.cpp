/**
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include<cstdlib>
#include<cstring>
#include<string>

#include <jni.h>
#include <logging_macros.h>
#include <android/asset_manager_jni.h>
#include "LiveEffectEngine.h"
#include "MyNativeEffects.h"
#include <android/bitmap.h>

static const int kOboeApiAAudio = 0;
static const int kOboeApiOpenSLES = 1;

template<typename T, typename... Args>
std::unique_ptr<T> make_unique(Args&&... args)
{
    return std::unique_ptr<T>(new T(std::forward<Args>(args)...));
}


LiveEffectEngine *engine = nullptr;

extern "C" {


JNIEXPORT jint JNICALL
Java_com_oskiapps_instrume_LiveEffectEngine_getMidiInstrument(JNIEnv *env, jclass type) {

    jint retInt = 0;
    if(engine != nullptr) {
        retInt = engine->chosenInstrument;
    }
    return retInt;

}

JNIEXPORT jboolean JNICALL
Java_com_oskiapps_instrume_LiveEffectEngine_setToggleMidiRec(JNIEnv *env, jclass type) {

    if(engine != nullptr) {
        if(!engine->isMidiRecording) {
            engine->isMidiRecording = true;
            engine->midiRecTime = engine->mProcessedFrameCount;


        } else {
            engine->isMidiRecording = false;
            engine->playerTime = 0;
        }
        return static_cast<jboolean>(engine->isMidiRecording);

    }
    return static_cast<jboolean>(false);

}

JNIEXPORT jboolean JNICALL
Java_com_oskiapps_instrume_LiveEffectEngine_clearMidiSong(JNIEnv *env, jclass type) {

    if(engine != nullptr) {
        engine->myMidiEvents.clear();
        engine->myMidiEvents.resize(0);
        engine->midiRecTime = 0;

        return true;
    }
    return false;
    // TODO

}

JNIEXPORT jboolean JNICALL
Java_com_oskiapps_instrume_LiveEffectEngine_setTogglePlayMidi(JNIEnv *env, jclass type) {
    if(engine != nullptr) {
        if(engine->isPlayMode) {
            engine->isPlayMode = false;
            engine->playModeStart = 0;
            engine->playerTime = 0;
        } else {
            engine->isPlayMode = true;
            engine->playModeStart = engine->mProcessedFrameCount;

        }
        LOGV("ozzey %i", engine->isPlayMode );
        return static_cast<jboolean>(engine->isPlayMode);
    }
    return static_cast<jboolean>(false);
    // TODO

}

JNIEXPORT void JNICALL
Java_com_oskiapps_instrume_LiveEffectEngine_playMidiNote(JNIEnv *env, jclass type,
                                                         jint integer, jboolean onOff) {
    if(engine != nullptr) {
        engine->playMidiNote = static_cast<int16_t>(integer);
        engine->playMidiOnMessage = onOff;
    }
    // TODO

}

JNIEXPORT jboolean JNICALL
Java_com_oskiapps_instrume_LiveEffectEngine_setMidiInstrument(JNIEnv *env, jclass type,
                                                              jint theInstrument) {

    if(engine != nullptr) {
        engine->passedMidiInstrument = theInstrument;
        engine->changeMidiInstrument(theInstrument);
    }
    return true;

    // TODO

}

JNIEXPORT jboolean JNICALL
Java_com_oskiapps_instrume_LiveEffectEngine_setSaxofonyParams(JNIEnv *env, jclass type,
                                                              jfloat reedStif, jfloat reedAperature,
                                                              jfloat noiseGain, jfloat blowPos,
                                                              jfloat vibrFreq, jfloat vibrGain,
                                                              jfloat breathPressure) {

    if(engine != nullptr) {
        engine->reedStiffVal = reedStif;
        engine->reedAperatureVal = reedAperature;
        engine->noiseGainVal = noiseGain;
        engine->blowPowVal = blowPos;
        engine->vibrFreqVal = vibrFreq;
        engine->vibrGainVal = vibrGain;
        engine->breathPressureVal = breathPressure;
        LOGV("oskivals %f %f %f %f %f %f %f ", reedStif,reedAperature ,noiseGain,blowPos,vibrFreq,vibrGain,breathPressure);
    }

    return static_cast<jboolean>(true);
}

JNIEXPORT void JNICALL
Java_com_oskiapps_instrume_LiveEffectEngine_lastMix(JNIEnv *env, jclass type, jstring theFile_, jstring theFileRec_,jstring theFileOut_,jfloat percentageOrig,
                                                    jfloat percentageRec) {

    jboolean isCopy;
    const char* nativeStringOrig = env->GetStringUTFChars(theFile_ , &isCopy);
    const char* nativeStringRec = env->GetStringUTFChars(theFileRec_ , &isCopy);
    const char* nativeStringOut = env->GetStringUTFChars(theFileOut_ , &isCopy);
    // TODO

    int buffsize = 8192;
    //theFileOut = fopen(theFileSrcOut,"w+");
    //std::ifstream theInFxStream;
    std::ifstream theOrigFxStream;
    std::ifstream theRecFxStream;
    std::ofstream theOutFxStream;
    theOrigFxStream = std::ifstream();
    theOrigFxStream.open(nativeStringOrig,std::ios_base::binary);
    theRecFxStream = std::ifstream();
    theRecFxStream.open(nativeStringRec,std::ios_base::binary);
    theOutFxStream = std::ofstream();
    theOutFxStream.open(nativeStringOut,std::ios_base::out | std::ios_base::in | std::ios_base::binary);

    //theInStream.ignore( std::numeric_limits<std::streamsize>::max() );
    //std::streamsize length = theInStream.gcount();
    //theInStream.clear();   //  Since ignore will have set eof.
    theOrigFxStream.ignore( std::numeric_limits<std::streamsize>::max() );
    std::streamsize length = theOrigFxStream.gcount();
    theOrigFxStream.clear();   //  Since ignore will have set eof.
    theOrigFxStream.seekg( 0, std::ios_base::beg );

    long startInBytes = 0;
    long endInBytes = length;
    //LOGE("oskido gainval  %f", gotGain);

    char*inBuffOrig = new char[buffsize];
    char*inBuffRec = new char[buffsize];
    // int16_t* outShorts = new int16_t[buffsize/2];
    int16_t *outFloats = new int16_t[buffsize/2];

    int stepCnt = 0;
    for(long i = startInBytes; i < endInBytes; i+=buffsize, stepCnt++) {
        theOrigFxStream.seekg(i);
        theOrigFxStream.read(inBuffOrig, buffsize);
        theRecFxStream.seekg(i);
        theRecFxStream.read(inBuffRec, buffsize);

        //std::vector<int16_t> tmpAudioVec(static_cast<unsigned long>(buffsize / 2));
        int16_t *audioBufferOrig = reinterpret_cast<int16_t*>(inBuffOrig);
        int16_t *audioBufferRec = reinterpret_cast<int16_t*>(inBuffRec);

        for(int j = 0; j < buffsize/2; j++) {
            outFloats[j] = (int16_t)(audioBufferOrig[j]*percentageOrig + audioBufferRec[j] * percentageRec);// (int16_t)(audioBuffer[i]*gotGain);
        }

        theOutFxStream.seekp(i);
        theOutFxStream.write(reinterpret_cast<const char *>(outFloats),buffsize);

    }

    if(theOrigFxStream.is_open()) {
        theOrigFxStream.close();
    }
    if(theRecFxStream.is_open()) {
        theRecFxStream.close();
    }
    if(theOutFxStream.is_open()) {
        theOutFxStream.close();
    }

    if(inBuffOrig) {
        delete[] inBuffOrig;
    }
    if(inBuffRec) {
        delete[] inBuffRec;
    }

    if(outFloats) {
        delete[] outFloats;
    }

    if (isCopy == JNI_TRUE) {
        env->ReleaseStringUTFChars(theFile_, nativeStringOrig);
        env->ReleaseStringUTFChars(theFileOut_, nativeStringRec);
        env->ReleaseStringUTFChars(theFileOut_, nativeStringOut);
    }
}

JNIEXPORT void JNICALL
Java_com_oskiapps_instrume_LiveEffectEngine_copyFileNative(JNIEnv *env, jclass type,
                                                        jstring theFile_,jstring theFileOut_) {

    //if (engine != nullptr)
    {
        jboolean isCopy;
        const char* nativeString = env->GetStringUTFChars(theFile_ , &isCopy);
        const char* nativeStringOut = env->GetStringUTFChars(theFileOut_ , &isCopy);
        std::ifstream  src(nativeString, std::ios::binary);
        std::ofstream  dst(nativeStringOut,   std::ios::binary);

        dst << src.rdbuf();
        src.close();
        dst.close();

        if (isCopy == JNI_TRUE) {
            env->ReleaseStringUTFChars(theFile_, nativeString);
            env->ReleaseStringUTFChars(theFileOut_, nativeStringOut);
        }
    }
    // Init - One time to initialize the method id, (use an init() function)
    //engine->midStr = env->GetMethodID(effectType, "javaDrawVal", engine->sigStr);
    //game = std::make_unique<Game>(assetManager);
    //game->start();
}

JNIEXPORT void JNICALL
Java_com_oskiapps_instrume_LiveEffectEngine_doGain(JNIEnv *env, jclass type, jstring inFile_,
                                                   jstring outFile_, jint gotStart, jint gotEnd,
                                                   jfloat theGain) {
    const char *inFile = env->GetStringUTFChars(inFile_, 0);
    const char *outFile = env->GetStringUTFChars(outFile_, 0);

    // TODO
    std::unique_ptr<MyNativeEffects> myNativeEffects = make_unique<MyNativeEffects>();

    if(engine != nullptr) {
        myNativeEffects->setBestParams(engine->bestSampleRate);
    }
    myNativeEffects->doGain(inFile,outFile,gotStart,gotEnd,theGain);

    env->ReleaseStringUTFChars(inFile_, inFile);
    env->ReleaseStringUTFChars(outFile_, outFile);
}

JNIEXPORT void JNICALL
Java_com_oskiapps_instrume_LiveEffectEngine_setAutoTuneScale(JNIEnv *env, jclass type,
                                                             jstring scale_, jfloat dryWet) {
    const char *scale = env->GetStringUTFChars(scale_, 0);

    if(engine != nullptr) {
        std::string tmpStr(scale,7);

        if(!strcmp(scale,"")) {
            engine->autoTuneScale = tmpStr;
        } else {
            engine->autoTuneDryWet = dryWet;
        }

        //LOGV("oski scale jni %s, %s", scale,engine->autoTuneScale);

    }

    env->ReleaseStringUTFChars(scale_, scale);
}

JNIEXPORT void JNICALL
Java_com_oskiapps_instrume_LiveEffectEngine_setupLowLatencyParams(JNIEnv *env, jclass type,
                                                                  jint bestBufferSize,
                                                                  jint bestSampleRate) {
    if(engine != nullptr) {
        engine->bestBufferSize = bestBufferSize;
        engine->bestSampleRate = bestSampleRate;

        float bufferOptimalRatio = (96*(float)8*8) / (bestBufferSize);

        if(bufferOptimalRatio > 1) {
            engine->bufferMulti = (int) (bufferOptimalRatio);
        } else {
            engine->bufferMulti = 1;
        }

        LOGV("oskibestbufferSize %i, %i", engine->bestBufferSize,engine->bufferMulti);
    }
    // TODO

}

JNIEXPORT void JNICALL
Java_com_oskiapps_instrume_LiveEffectEngine_doEQ(JNIEnv *env, jclass type, jstring inFile_,
                                                 jstring outFile_, jlong start, jlong end,jint freq1, jfloat gain1, jint freq2, jfloat gain2, jint freq3, jfloat gain3) {
    const char *inFile = env->GetStringUTFChars(inFile_, 0);
    const char *outFile = env->GetStringUTFChars(outFile_, 0);
    std::unique_ptr<MyNativeEffects> myNativeEffects = make_unique<MyNativeEffects>();

    if(engine != nullptr) {
        myNativeEffects->setBestParams(engine->bestSampleRate);
    }
    //LOGV("oski sherlock #4 going native doeq %li %li",start, end);
    myNativeEffects->doEQ(inFile,outFile,start,end,freq1,gain1,freq2,gain2,freq3,gain3);
    //delete myNativeEffects;
    // TODO

    env->ReleaseStringUTFChars(inFile_, inFile);
    env->ReleaseStringUTFChars(outFile_, outFile);
}

JNIEXPORT void JNICALL
Java_com_oskiapps_instrume_LiveEffectEngine_doReverb(JNIEnv *env, jclass type, jstring inFile_,
                                                     jstring outFile_, jint start, jint end, jfloat gotDryWet, jfloat gotDecay) {
    const char *inFile = env->GetStringUTFChars(inFile_, 0);
    const char *outFile = env->GetStringUTFChars(outFile_, 0);
    std::unique_ptr<MyNativeEffects> myNativeEffects = make_unique<MyNativeEffects>();

    if(engine != nullptr) {
        myNativeEffects->setBestParams(engine->bestSampleRate);
    }
    myNativeEffects->doReverb(inFile,outFile,start,end,gotDryWet,gotDecay);
    // TODO
    env->ReleaseStringUTFChars(inFile_, inFile);
    env->ReleaseStringUTFChars(outFile_, outFile);
}

JNIEXPORT void JNICALL
Java_com_oskiapps_instrume_LiveEffectEngine_doAutoTune(JNIEnv *env, jclass jclass1, jstring inFile_,
                                                       jstring outFile_, jint start, jint end,
                                                       jstring scale_, jfloat dryWet) {
    const char *inFile = env->GetStringUTFChars(inFile_, 0);
    const char *outFile = env->GetStringUTFChars(outFile_, 0);
    const char *scale = env->GetStringUTFChars(scale_, 0);

    //jintArray result;
    std::unique_ptr<MyNativeEffects> myNativeEffects = make_unique<MyNativeEffects>();

    if(engine != nullptr) {
        myNativeEffects->setBestParams(engine->bestSampleRate);
    }
    std::string tmpStr(scale,7);
    LOGV("oski sherlock #4 going native doAutotune %i %i %s",start, end, scale);

    //jint *retPitches =
            myNativeEffects->doAutoTune(inFile, outFile, start, end, tmpStr, dryWet);
    // TODO

    env->ReleaseStringUTFChars(inFile_, inFile);
    env->ReleaseStringUTFChars(outFile_, outFile);
    //result = env->NewIntArray(myNativeEffects->howManySteps-1);
    //if (result == NULL) {
        //return NULL; /* out of memory error thrown */
    //}
    //int i;
    // fill a temp structure to use to populate the java int arrays
// move from the temp structure to the java structure
    /*env->SetIntArrayRegion(result, 0, myNativeEffects->howManySteps-1, retPitches);
    //delete(myNativeEffects);

    env->ReleaseStringUTFChars(inFile_, inFile);
    env->ReleaseStringUTFChars(outFile_, outFile);
    env->ReleaseStringUTFChars(scale_, scale);

    delete myNativeEffects;
    delete[] retPitches;*/

//    return result;

}

JNIEXPORT jfloatArray JNICALL
Java_com_oskiapps_instrume_LiveEffectEngine_doAutoTuneRetInt(JNIEnv *env, jclass jclass1, jstring inFile_,
                                                       jstring outFile_, jint start, jint end,
                                                       jstring scale_, jfloat dryWet) {
    const char *inFile = env->GetStringUTFChars(inFile_, 0);
    const char *outFile = env->GetStringUTFChars(outFile_, 0);
    const char *scale = env->GetStringUTFChars(scale_, 0);
    //jint *retarr = env->GetIntArrayElements(retarr_, NULL);

    jfloatArray result;
    std::unique_ptr<MyNativeEffects> myNativeEffects = make_unique<MyNativeEffects>();

    if(engine != nullptr) {
        myNativeEffects->setBestParams(engine->bestSampleRate);
    }
    std::string tmpStr(scale,7);
    LOGV("oski sherlock #4 going native doAutotune %i %i %s",start, end, scale);
    //jint* retPitches =
    //retarr =
    myNativeEffects->visualizerArray = new jfloat[static_cast<int>((end * (long)myNativeEffects->drawPixelRatio - start * (long)myNativeEffects->drawPixelRatio) / (2048*4))];

    myNativeEffects->doAutoTuneRetInt(inFile, outFile, start, end, tmpStr, dryWet);
    LOGV("oski probably out of mem %i %f" ,myNativeEffects->howManySteps, myNativeEffects->visualizerArray[10]);
    //delete(myNativeEffects);
    //env->ReleaseIntArrayElements(retarr_, retarr, 0);

    //delete myNativeEffects;
    //delete[] retPitches;
    //delete retPitches;
    //retPitches = nullptr;
    int16_t steps = 0;
    //if(myNativeEffects.howManySteps > 100) {
    //    steps = 100;
    //} else {
        steps = static_cast<int16_t>(myNativeEffects->howManySteps);
    //}
    result = env->NewFloatArray(steps+4);
    if (result == NULL) {
        return NULL; /* out of memory error thrown */
    }
    //int i;
    // fill a temp structure to use to populate the java int arrays
    jfloat fill[steps+4];

    fill[0] = 0; // put whatever logic you want to populate the values here.
    fill[1] = 0;
    fill[2] = 0;
    fill[3] = 0;
    for(int i = 4; i < steps+4;i++) {
        fill[i] = myNativeEffects->visualizerArray[i-4];
    }
    if(myNativeEffects->visualizerArray) {
        delete[] myNativeEffects->visualizerArray;
        myNativeEffects->visualizerArray = nullptr;
    }
    // move from the temp structure to the java structure
    env->SetFloatArrayRegion(result, 0, steps+4, fill);

    env->ReleaseStringUTFChars(inFile_, inFile);
    env->ReleaseStringUTFChars(outFile_, outFile);
    env->ReleaseStringUTFChars(scale_, scale);
    return result;

}

void Java_com_oskiapps_instrume_LiveEffectEngine_setLowEQ( JNIEnv*  env, jclass type, jint cf, jfloat lowEQVal) {
    if(engine != nullptr) {
        engine->reconfigureBiQuad("low",5,cf,lowEQVal,1);
    }
}
void Java_com_oskiapps_instrume_LiveEffectEngine_setMidEQ( JNIEnv*  env, jclass type, jint cf, jfloat midEQVal) {
    if(engine != nullptr) {
       engine->reconfigureBiQuad("mid",3,cf,midEQVal,1);
    }
}
void Java_com_oskiapps_instrume_LiveEffectEngine_setHighEQ( JNIEnv*  env, jclass type, jint cf, jfloat highEQVal) {
    if(engine != nullptr) {
        engine->reconfigureBiQuad("hig",6,cf,highEQVal,1);
    }
}
jboolean Java_com_oskiapps_instrume_LiveEffectEngine_setThresholdLevel( JNIEnv*  env, jclass type, jfloat volVoc) {
    jboolean retVal = false;
    if(engine != nullptr) {
        engine->thresholdLevel = volVoc;
        //engine->setTheReverb();
        retVal = true;
    }
    return retVal;
}
jboolean Java_com_oskiapps_instrume_LiveEffectEngine_setReverbDry( JNIEnv*  env, jclass type, jfloat volVoc) {
    jboolean retVal = false;
    if(engine != nullptr) {
        engine->reverbDry = volVoc;
        engine->setTheReverb();
        retVal = true;
    }
    return retVal;
}

/*JNIEXPORT void JNICALL
Java_com_raponmp3_raponmp3_LiveEffectEngine_passAudioSessionId(JNIEnv *env, jclass effectType, jint gotsessid) {
    engine->passedSessid = gotsessid;
    //LOGE("engine->soundBuffer[i] %i ",sessid);

    //return sessid;
    // TODO

}

JNIEXPORT jint JNICALL
Java_com_raponmp3_raponmp3_LiveEffectEngine_getSessionId(JNIEnv *env, jclass effectType) {
    int sessid = 5;
    sessid = engine->mPlayStream->getSessionId();
    LOGE("engine->soundBuffer[i] %i ",sessid);

    return sessid;
    // TODO

}*/

jboolean Java_com_oskiapps_instrume_LiveEffectEngine_setVolumeVocal( JNIEnv*  env, jclass type, jfloat volVoc) {
    jboolean retVal = false;
    if(engine != nullptr) {
        engine->volumeVocal = volVoc;
        retVal = true;
    }
    return retVal;
}
jboolean Java_com_oskiapps_instrume_LiveEffectEngine_setVolumeMusic( JNIEnv*  env, jclass type, jfloat volMusic) {
    jboolean retVal = false;
    if(engine != nullptr) {
        engine->volumeMusic = volMusic;
        retVal = true;
    }
    return retVal;
}

jboolean Java_com_oskiapps_instrume_LiveEffectEngine_setToggleRecActive( JNIEnv*  env, jclass type) {
    jboolean retVal = false;
    if(engine != nullptr) {
        if(engine->isRecActive == true){
            engine->isRecActive = false;
        } else {
            engine->isRecActive = true;
        }
        retVal = static_cast<jboolean>(engine->isRecActive);
    }
    return retVal;
}


jboolean Java_com_oskiapps_instrume_LiveEffectEngine_setToggleBlow( JNIEnv*  env, jclass type) {
    jboolean retVal = false;
    if(engine != nullptr) {
        if(engine->isBlow == true){
            engine->isBlow = false;
        } else {
            engine->isBlow  = true;
        }
        retVal = static_cast<jboolean>(engine->isBlow);
    }
    return retVal;
}


jboolean Java_com_oskiapps_instrume_LiveEffectEngine_setToggleWurley( JNIEnv*  env, jclass type) {
    jboolean retVal = false;
    if(engine != nullptr) {
        if(engine->isWurley == true){
            engine->isWurley = false;
        } else {
            engine->isWurley = true;
        }
        retVal = static_cast<jboolean>(engine->isWurley);
    }
    return retVal;
}


jboolean Java_com_oskiapps_instrume_LiveEffectEngine_setToggleGuitar( JNIEnv*  env, jclass type) {
    jboolean retVal = false;
    if(engine != nullptr) {
        if(engine->isGuitar == true){
            engine->isGuitar = false;
        } else {
            engine->isGuitar = true;
            engine->changeMidiInstrument(24);

        }
        retVal = static_cast<jboolean>(engine->isGuitar);
    }
    return retVal;
}

jboolean Java_com_oskiapps_instrume_LiveEffectEngine_setToggleDrums( JNIEnv*  env, jclass type) {
    jboolean retVal = false;
    if(engine != nullptr) {
        if(engine->isDrums == true){
            engine->isDrums = false;
        } else {
            engine->isDrums = true;
        }
        retVal = static_cast<jboolean>(engine->isDrums);
    }
    return retVal;
}

jboolean Java_com_oskiapps_instrume_LiveEffectEngine_setToggleFender( JNIEnv*  env, jclass type) {
    jboolean retVal = false;
    if(engine != nullptr) {

        if(engine->isRhodey == true){
            engine->isRhodey = false;
        } else {
            engine->isRhodey = true;
            engine->changeMidiInstrument(0);

        }
        retVal = static_cast<jboolean>(engine->isRhodey);
    }
    return retVal;
}
jboolean Java_com_oskiapps_instrume_LiveEffectEngine_setToggleSaxofony( JNIEnv*  env, jclass type) {
    jboolean retVal = false;
    if(engine != nullptr) {
        if(engine->isSaxofony == true){
            engine->isSaxofony = false;
        } else {
            engine->isSaxofony = true;
            engine->changeMidiInstrument(56);

        }
        retVal = static_cast<jboolean>(engine->isSaxofony);
    }
    return retVal;
}
jboolean Java_com_oskiapps_instrume_LiveEffectEngine_setTogglePlaying( JNIEnv*  env, jclass type) {
    jboolean retVal = static_cast<jboolean>(false);
    if(engine != nullptr) {
        if(engine->isPlaying == true){
            engine->isPlaying = false;
        } else {
            engine->isPlaying = true;
        }
        retVal = static_cast<jboolean>(engine->isPlaying);
    }
    return retVal;
}
jboolean Java_com_oskiapps_instrume_LiveEffectEngine_setToggleRecording( JNIEnv*  env, jclass type) {
    jboolean retVal = static_cast<jboolean>(false);
    if(engine != nullptr) {
        if(engine->isRecording == true){
            engine->isRecording = false;
        } else {
            engine->isRecording = true;
        }
        retVal = static_cast<jboolean>(engine->isRecording);
    }
    return retVal;
}
jboolean Java_com_oskiapps_instrume_LiveEffectEngine_setPlayerPosition( JNIEnv*  env, jclass type, jlong playerTime) {
    if(engine != nullptr) {
        if(playerTime%2 != 0) {
            playerTime+=1;
        }

        engine->playerTime = playerTime;
        //engine->mProcessedFrameCount = static_cast<uint64_t>(playerTime);
    }
    return static_cast<jboolean>(true);
}

jlongArray Java_com_oskiapps_instrume_LiveEffectEngine_drawSth( JNIEnv*  env, jclass type,
                                          jint width, jint height) {

    jlongArray result;
    jlong retValTime = 0;
    jlong retValLoudness = 0;
    jlong retValAutotuneLevel = 0;
    jlong retValRawPitch = 0;
    jlong retValRecording = 0;

        //int len = width*height*4;
    //LOGV("oski out %i",  (unsigned char)&engine->soundBuffer[0]);

    if(engine != nullptr ) {
        //unsigned char* rgb = 0;
        //AndroidBitmap_lockPixels(env,theBitmap,(void**)&rgb);


        /*        int moduloHeightCnt = 0;
       unsigned char theArr[width][height*4];
      for(int i = 0; i < width*height*4; i++) {
           //theArr[moduloHeightCnt][i-(moduloHeightCnt*height*4)] = 100;

           if(moduloHeightCnt == height/2) {
               //theArr[i-(moduloWidthCnt*width*4)][moduloWidthCnt] = 100;
           }
           if(i%(height*4) == 0) {
               moduloHeightCnt++;
           }

       }*/



        //if(engine->isPlaying)
        {
            retValTime = engine->playerTime;
            retValLoudness = engine->playerLoudness;
            retValAutotuneLevel = engine->playerAutotuneLevel;
            retValRawPitch = engine->rawPitch;
            retValRecording = engine->isRecording;

        }

    //AndroidBitmap_unlockPixels(env,theBitmap);
    //LOGE("engine->soundBuffer[i] %i",int16_t(abs((float)engine->soundBuffer[100])));


        /*for(int i = 0; i < width*4; i++) {
            rgb[i] = cntColor;
            //rgb[i] =  (engine->soundBuffer[0]);
            cntColor++;
            if(cntColor >=255) {
                cntColor = 0;
            }
        }*/

    result = env->NewLongArray(engine->bestBufferSize+5);
    if (result == NULL) {
        return NULL; /* out of memory error thrown */
    }
    //int i;
    // fill a temp structure to use to populate the java int arrays
    jlong fill[engine->bestBufferSize+5];

        fill[0] = retValTime; // put whatever logic you want to populate the values here.
        fill[1] = retValLoudness;
        fill[2] = retValAutotuneLevel;
        fill[3] = retValRecording;
        fill[4] = retValRawPitch;


        for(int i = 5; i < engine->bestBufferSize+5;i++) {
            fill[i] = (long)engine->soundBuffer[i-5];
        }

        // move from the temp structure to the java structure
        env->SetLongArrayRegion(result, 0, engine->bestBufferSize+5, fill);
        //delete engine->soundBuffer;
        //engine->soundBuffer=nullptr;

        return result;
    }

    result = env->NewLongArray(5);
    if (result == NULL) {
        return NULL; /* out of memory error thrown */
    }
    jlong fill[5];

    fill[0] = retValTime; // put whatever logic you want to populate the values here.
    fill[1] = retValLoudness;
    fill[2] = retValAutotuneLevel;
    fill[3] = retValRecording;
    fill[4] = retValRawPitch;

    env->SetLongArrayRegion(result, 0, 5, fill);

    return result;

}

JNIEXPORT void JNICALL
Java_com_oskiapps_instrume_LiveEffectEngine_stopAll(JNIEnv *env, jclass type) {
    //fclose(engine->theFileOut);
    //fclose(engine->theFile);
    engine->closeAllStreams();
    //engine->soundBuffer = nullptr;
    // TODO

}


jlongArray Java_com_oskiapps_instrume_LiveEffectEngine_getFreqs( JNIEnv*  env, jclass type,
                                                                jint width, jint height) {

    jlongArray result;

    //int len = width*height*4;
    //LOGV("oski out %i",  (unsigned char)&engine->soundBuffer[0]);

    if(engine != nullptr && (engine->soundFreqBuffer[0] != 0)) {

        result = env->NewLongArray(engine->bestBufferSize);
        if (result == NULL) {
            return NULL; /* out of memory error thrown */
        }
        //int i;
        // fill a temp structure to use to populate the java int arrays
        jlong fill[engine->bestBufferSize];

        for(int i = 0; i < engine->bestBufferSize;i++) {
            fill[i] = engine->soundFreqBuffer[i];
        }

        // move from the temp structure to the java structure
        env->SetLongArrayRegion(result, 0, engine->bestBufferSize, fill);
        //delete engine->soundBuffer;
        //engine->soundBuffer=nullptr;

        return result;
    }

    result = env->NewLongArray(4);
    if (result == NULL) {
        return NULL; /* out of memory error thrown */
    }
    jlong fill[1];
    fill[0] = 0;

    env->SetLongArrayRegion(result, 0, 1, fill);

    return result;

}

void Java_com_oskiapps_instrume_LiveEffectEngine_syncMidiEvents( JNIEnv*  env, jclass type,
                                                           jint whichElement, jint whichPitch, jfloat whichStart, jfloat whichEnd) {
    if(engine != nullptr) {
        if(whichElement >= engine->myMidiEvents.size()) {
            engine->myMidiEvents.resize(static_cast<unsigned long>(whichElement + 1));
            engine->myMidiEvents[whichElement].resize(3);

        }

        engine->myMidiEvents[whichElement][0] = whichPitch;
        engine->myMidiEvents[whichElement][1] = whichStart;
        engine->myMidiEvents[whichElement][2] = whichEnd;
        LOGV("oskidbg#1 %i %i", (int)engine->myMidiEvents.size(), whichElement);

        LOGV("oskidbgl %f %f %f", engine->myMidiEvents[whichElement][0],engine->myMidiEvents[whichElement][1],engine->myMidiEvents[whichElement][2]);

    }
}

void Java_com_oskiapps_instrume_LiveEffectEngine_addNoteAt( JNIEnv*  env, jclass type,
                                                            jint whichPos, jint whichNote, float whichX) {
    if(engine != nullptr) {
        std::vector<float> singleMidiEvent;
        singleMidiEvent.resize(3);
        singleMidiEvent[0] = whichNote;
        singleMidiEvent[1] = whichX;
        singleMidiEvent[2] = whichX+0.3f;

        LOGV("oskiwees %f %f %f", singleMidiEvent[0] ,singleMidiEvent[1] ,singleMidiEvent[2]);

        engine->myMidiEvents.insert(engine->myMidiEvents.begin()+whichPos, singleMidiEvent);
    }
}
void Java_com_oskiapps_instrume_LiveEffectEngine_deleteNoteAt( JNIEnv*  env, jclass type,
                                                               jint whichPos) {
    if(engine != nullptr) {
        engine->myMidiEvents.erase(engine->myMidiEvents.begin() + whichPos);
    }
}



jobjectArray Java_com_oskiapps_instrume_LiveEffectEngine_getMyMidiEvents(JNIEnv *env, jclass type) {

    if(engine != nullptr) {
        jclass floatClass = env->FindClass("[F"); //
        jsize height = engine->myMidiEvents.size();

// Create the returnable 2D array
        jobjectArray jObjarray = env->NewObjectArray(height, floatClass, NULL);

// Go through the first dimension and add the second dimension arrays
        for (unsigned int i = 0; i < height; i++) {
            jfloatArray floatArray = env->NewFloatArray(engine->myMidiEvents[i].size());
            env->SetFloatArrayRegion(floatArray, (jsize) 0, (jsize) engine->myMidiEvents[i].size(), (jfloat*) engine->myMidiEvents[i].data());
            env->SetObjectArrayElement(jObjarray, (jsize) i, floatArray);
            env->DeleteLocalRef(floatArray);
        }

        return jObjarray;

    }
    jclass floatClass = env->FindClass("[F"); //
    jsize height = 0;

// Create the returnable 2D array
    jobjectArray jObjarray = env->NewObjectArray(height, floatClass, NULL);
    return jObjarray;

}

JNIEXPORT void JNICALL
Java_com_oskiapps_instrume_LiveEffectEngine_addAssetMgr(JNIEnv *env, jclass type,
                                                                     jobject jAssetManager,jstring theFile_,jstring theFileOut_,jstring theFileOutFx_, jstring filePathMixed_, jstring rawWavesFolder) {

    AAssetManager *assetManager = AAssetManager_fromJava(env, jAssetManager);
    if (engine != nullptr) {
        jboolean isCopy;
        const char* nativeString = env->GetStringUTFChars(theFile_ , &isCopy);
        const char* nativeStringOut = env->GetStringUTFChars(theFileOut_ , &isCopy);
        const char* nativeStringOutFx = env->GetStringUTFChars(theFileOutFx_ , &isCopy);
        const char *filePathMixed = env->GetStringUTFChars(filePathMixed_, 0);
        const char *nativerawWavesFolder= env->GetStringUTFChars(rawWavesFolder, 0);



        engine->addAssetManager(assetManager, nativeString, nativeStringOut, nativeStringOutFx,
                                filePathMixed, nativerawWavesFolder);
        if (isCopy == JNI_TRUE) {
            env->ReleaseStringUTFChars(theFile_, nativeString);
            env->ReleaseStringUTFChars(theFileOut_, nativeStringOut);
            env->ReleaseStringUTFChars(theFileOutFx_, nativeStringOutFx);
            env->ReleaseStringUTFChars(filePathMixed_, filePathMixed);
            //env->ReleaseStringUTFChars(rawWavesFolder, nativerawWavesFolder);


        }
    }
    // Init - One time to initialize the method id, (use an init() function)
    //engine->midStr = env->GetMethodID(effectType, "javaDrawVal", engine->sigStr);
    //game = std::make_unique<Game>(assetManager);
    //game->start();
}

JNIEXPORT jboolean JNICALL
Java_com_oskiapps_instrume_LiveEffectEngine_create(JNIEnv *env,
                                                               jclass) {
    //if (engine == nullptr) {
        LOGE(
                "oski was nullptr");
        engine = new LiveEffectEngine();// make_unique<LiveEffectEngine>();
        //std::unique_ptr<MyNativeEffects> myNativeEffects = make_unique<MyNativeEffects>();

    /*} else {
        LOGE(
                "oski was not nullptr");
        //engine->restartStreams();
    }*/

    return (engine != nullptr);
}

JNIEXPORT void JNICALL
Java_com_oskiapps_instrume_LiveEffectEngine_delete(JNIEnv *env,
                                                               jclass) {

    //delete engine;
    //engine = nullptr;
}

JNIEXPORT void JNICALL
Java_com_oskiapps_instrume_LiveEffectEngine_setEffectOn(
    JNIEnv *env, jclass, jboolean isEffectOn) {
    if (engine == nullptr) {
        LOGE(
            "Engine is null, you must call createEngine before calling this "
            "method");
        return;
    }
    engine->setEffectOn(isEffectOn);
    if(isEffectOn == false) {
        //engine->closeAllStreams();
        //engine.release();
        delete engine;
        engine = nullptr;

    }

}

JNIEXPORT void JNICALL
Java_com_oskiapps_instrume_LiveEffectEngine_setRecordingDeviceId(
    JNIEnv *env, jclass, jint deviceId) {
    if (engine == nullptr) {
        LOGE(
            "Engine is null, you must call createEngine before calling this "
            "method");
        return;
    }

    engine->setRecordingDeviceId(deviceId);
}

JNIEXPORT void JNICALL
Java_com_oskiapps_instrume_LiveEffectEngine_setPlaybackDeviceId(
    JNIEnv *env, jclass, jint deviceId) {
    if (engine == nullptr) {
        LOGE(
            "Engine is null, you must call createEngine before calling this "
            "method");
        return;
    }

    engine->setPlaybackDeviceId(deviceId);
}

JNIEXPORT jboolean JNICALL
Java_com_oskiapps_instrume_LiveEffectEngine_setAPI(JNIEnv *env,
                                                               jclass type,
                                                               jint apiType) {
    if (engine == nullptr) {
        LOGE(
            "Engine is null, you must call createEngine "
            "before calling this method");
        return JNI_FALSE;
    }

    oboe::AudioApi audioApi;
    switch (apiType) {
        case kOboeApiAAudio:
            audioApi = oboe::AudioApi::AAudio;
            break;
        case kOboeApiOpenSLES:
            audioApi = oboe::AudioApi::OpenSLES;
            break;
        default:
            LOGE("Unknown API selection to setAPI() %d", apiType);
            return JNI_FALSE;
    }

    return static_cast<jboolean>(engine->setAudioApi(audioApi) ? JNI_TRUE
                                                               : JNI_FALSE);
}

JNIEXPORT jboolean JNICALL
Java_com_oskiapps_instrume_LiveEffectEngine_isAAudioSupported(
    JNIEnv *env, jclass type) {
    if (engine == nullptr) {
        LOGE(
            "Engine is null, you must call createEngine "
            "before calling this method");
        return JNI_FALSE;
    }
    return static_cast<jboolean>(engine->isAAudioSupported() ? JNI_TRUE
                                                             : JNI_FALSE);
}
}
