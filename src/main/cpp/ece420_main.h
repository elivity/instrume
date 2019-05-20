//
// Created by daran on 1/12/2017 to be used in ECE420 Sp17 for the first time.
// Modified by dwang49 on 1/1/2018 to adapt to Android 7.0 and Shield Tablet updates.
//

#ifndef ECE420_MAIN_H
#define ECE420_MAIN_H

#include <vector>
#include <cmath>
#include "audio_common.h"
#include "buf_manager.h"
#include "debug_utils.h"
class ece420_main {
public:
    ece420_main();
    ~ece420_main();

    int detectBufferPeriod(std::vector<float> buffer);
    void findEpochLocations(std::vector<int> &epochLocations, std::vector<float> buffer, int periodLen);
    void overlapAddArray(float *dest, float *src, int startIdx, int len);
    //bool lab5PitchShift(std::vector<float> bufferIn);
//void ece420ProcessFrame(sample_buf *dataBuf);
    std::vector<int16_t> ece420ProcessFrame2(std::vector<int16_t> dataBuf);
    float getThePitch();
    float getTheCorrectedPitch();
    void setBestParams(int buffsize, int sampleRate);

    float pitchCorrector(float gotPitch, float *gotScale);
    void setScale(const char* txt);

    float getTheRatio();

    //float detectBufferPeriod2(std::vector<int16_t> buffer);

    float detectBufferPeriod2(std::vector<float> buffer);

    void setScale(std::string theTxt);

    std::mutex fftwlock;

    //void freeCfgs();

    float pitchCorrector(float gotPitch, std::unique_ptr<float[]> gotScale);

    float pitchCorrector(float gotPitch, std::vector<float[]> gotScale);

    static float pitchCorrector(float gotPitch, std::vector<float> gotScale);

    //float detectBufferPeriod2ThreadSafe(std::vector<int16_t> buffer);

    float detectBufferPeriod2(std::vector<int16_t> buffer);

    //float detectBufferPeriod2ThreadSafe(std::vector<int16_t> &buffer);
    float detectBufferPeriod2ThreadSafe(std::vector<float> &buffer);

    float detectBufferPeriod2LongMode(std::vector<int16_t> &buffer);

    float detectPitchYin(std::vector<int16_t> &buffer);

    int countZeroCrossings(std::vector<int16_t> &buffer);

    int countZeroCrossings(int16_t *&buffer);

    int countZeroCrossings(int16_t *&buffer, int16_t cnt);

    int countZeroCrossings(int16_t *&buffer, int32_t cnt);

    std::vector<float> getSpectrum();

    std::vector<float> getSpectrum(std::vector<float> gotBuffer);

    std::vector<float> getSpectrum(std::vector<int16_t> gotBuffer);

    int16_t getTheMidiNote();

    float midiToFreq(int midiNote);

    int16_t getTheRawMidiNote();
};

#endif //ECE420_MAIN_H
