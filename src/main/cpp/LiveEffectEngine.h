/*
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

#ifndef OBOE_LIVEEFFECTENGINE_H
#define OBOE_LIVEEFFECTENGINE_H

#include <jni.h>
#include <oboe/Oboe.h>
#include <android/asset_manager_jni.h>

#include <string>
#include <thread>
#include <fstream>
#include <BiQuad.h>

constexpr int32_t kBufferSizeAutomatic = 0;
class LiveEffectEngine : public oboe::AudioStreamCallback {
   public:
    LiveEffectEngine();
    ~LiveEffectEngine();
    void setRecordingDeviceId(int32_t deviceId);
    void setPlaybackDeviceId(int32_t deviceId);
    void setEffectOn(bool isOn);
    double mCurrentOutputLatencyMillis = 0;

    /*
     * oboe::AudioStreamCallback interface implementation
     */
    oboe::DataCallbackResult onAudioReady(oboe::AudioStream *oboeStream,
                                          void *audioData, int32_t numFrames);
    void onErrorBeforeClose(oboe::AudioStream *oboeStream, oboe::Result error);
    void onErrorAfterClose(oboe::AudioStream *oboeStream, oboe::Result error);

    bool setAudioApi(oboe::AudioApi);
    bool isAAudioSupported(void);

    void addAssetManager(AAssetManager *assetMgr, const char *filename, const char *outfilename,
                             const char *outfilenamefx, jstring pJstring);
    const char *theFileSrc;
    const char *theFileSrcOut;
    const char *theFileSrcOutFx;
    const char *theFileSrcMixed;
    const char *theRawFolderSrc;

    std::vector<float> soundBuffer;
    std::vector<int16_t> soundFreqBuffer;

    std::vector<std::vector<float>> myMidiEvents;

    void closeAllStreams();
    void restartStreams();

    //external player variables
    int64_t playerTime;
    int64_t playerLoudness;
    int16_t playerAutotuneLevel;
    int16_t rawPitch = 0;


    int16_t playMidiNote = 0;
    bool playMidiOnMessage = false;



    uint64_t playModeStart = 0;
    bool isPlayMode = false;
    bool isMidiRecording = false;
    uint64_t midiRecTime;


    //int8_t chosenInstrument = 1;

    bool isPlaying = false;
    bool isRecording = false;
    bool isRhodey = true;
    bool isSaxofony = false;
    bool isGuitar = false;
    bool isWurley = false;
    bool isBlow;

    bool isSetInstrument = false;
    float volumeVocal = 1.0f;
    float volumeMusic = 1.0f;
    jfloat thresholdLevel = 0.17f;
    jfloat reverbDry = 0.5;
    jint bestBufferSize = 96*2;
    jint bestSampleRate = 48000;
    jint bufferMulti = 8;
    void setTheReverb();
    stk::BiQuad * reconfigureBiQuad(const char *whichbq, int type, int cf, double gotGainDB, double theQ);

    //int passedSessid = 0;

    jfloat reedStiffVal = 0.5;
    jfloat reedAperatureVal = 0;
    jfloat noiseGainVal = 0;
    jfloat blowPowVal = 0;
    jfloat vibrFreqVal = 0;
    jfloat vibrGainVal = 0;
    jfloat breathPressureVal = 0;

    std::string autoTuneScale = "C-Major";
    float autoTuneDryWet = 1;

    void addAssetManager(AAssetManager *assetMgr, const char *filename, const char *filenameOut,
                         const char *filenameOutFx, const char *filenameMixed);

    void addAssetManager(AAssetManager *assetMgr, const char *filename, const char *filenameOut,
                         const char *filenameOutFx, const char *filenameMixed,
                         const char *nativerawWavesFolder);
    std::vector<float> vcircularBuffer;
    bool isDrums = false;
    jint passedMidiInstrument = 90;

    uint64_t mProcessedFrameCount = 0;

    jboolean isRecActive = false;

    void changeMidiInstrument(int number);

    jint chosenInstrument = 0;


private:
    std::unique_ptr<oboe::LatencyTuner> mLatencyTuner;
    bool mIsLatencyDetectionSupported = false;
    oboe::Result calculateCurrentOutputLatencyMillis(oboe::AudioStream *stream, double *latencyMillis);
    bool isLatencyDetectionSupported();
    int32_t mBufferSizeSelection = kBufferSizeAutomatic;
    int32_t mFramesPerBurst;

    oboe::AudioStream *mPlayStream = nullptr;
    oboe::AudioStream *mRecordingStream = nullptr;
    bool mIsEffectOn = false;
    uint64_t mSystemStartupFrames = 0;
    int32_t mRecordingDeviceId = oboe::kUnspecified;
    int32_t mPlaybackDeviceId = oboe::kUnspecified;
    oboe::AudioFormat mFormat = oboe::AudioFormat::Float;
    int32_t mSampleRate = oboe::kUnspecified;
    int32_t mInputChannelCount = oboe::ChannelCount::Stereo;
    int32_t mOutputChannelCount = oboe::ChannelCount::Stereo;
    std::mutex mRestartingLock;
    oboe::AudioApi mAudioApi = oboe::AudioApi::AAudio;


    void openRecordingStream();
    void openPlaybackStream();

    void startStream(oboe::AudioStream *stream);
    void stopStream(oboe::AudioStream *stream);
    void closeStream(oboe::AudioStream *stream);

    void openAllStreams();

    oboe::AudioStreamBuilder *setupCommonStreamParameters(
        oboe::AudioStreamBuilder *builder);
    oboe::AudioStreamBuilder *setupRecordingStreamParameters(
        oboe::AudioStreamBuilder *builder);
    oboe::AudioStreamBuilder *setupPlaybackStreamParameters(
        oboe::AudioStreamBuilder *builder);
    void warnIfNotLowLatency(oboe::AudioStream *stream);

    std::vector<int16_t> loadFromAssets(long theoffset, int howmany);
    const int16_t *loadFromAssets(AAsset *asset, long theoffset);

    std::ifstream theInStream;
    std::fstream theOutStream;
    std::fstream theOutStreamFx;
    std::fstream theOutStreamMixed;


};

#endif  // OBOE_LIVEEFFECTENGINE_H
