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

#include "LiveEffectEngine.h"
#include "ece420_main.h"
#include "MyNativeEffects.h"
#include <assert.h>
#include <logging_macros.h>
#include <climits>
#include <oboe/Oboe.h>

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
#include <LentPitShift.h>
#include <Saxofony.h>
#include "Instrmnt.h"
#include <vector>
#include <Rhodey.h>
#include <sys/param.h>
#include <Guitar.h>
#include <Phonemes.h>
#include <Drummer.h>
#include <Brass.h>
#include <Wurley.h>
#include <BlowHole.h>
#include <RtMidi.h>
#include "midi.h"
#include "ece420_lib.h"


stk::Brass myBrass;
stk::BlowHole myBlow(50);
stk::Drummer myDrummer;

stk::Guitar myGuitar;
stk::Saxofony mySaxo(32);
stk::PitShift myPitShift;

stk::NRev myfreeverb;
stk::BiQuad *lowBiQuad;
stk::BiQuad *midBiQuad;
stk::BiQuad *highBiQuad;

std::vector<std::int16_t> vbufferSoundFile;

std::vector<std::int16_t> vprocessOutput;
std::vector<std::int16_t> vsamplesMix;
std::vector<std::int16_t> vsamplesFx;
std::vector<std::int16_t> vsamplesout;

int16_t lastPlayedMidiNote = 0;

std::vector<float> theFreqs;
int32_t sampleCount;
int buffsize = 96;

/**
 * Duplex is not very stable right after starting up:
 *   the callbacks may not be happening at the right times
 * The time to get it stable varies on different systems. Half second
 * is used for this sample, during the time this sample plays silence.
 */
const float kSystemWarmupTime = 1.5f;

template<typename T, typename... Args>
std::unique_ptr<T> make_unique(Args&&... args)
{
    return std::unique_ptr<T>(new T(std::forward<Args>(args)...));
}
std::unique_ptr<MyNativeEffects> mne(new MyNativeEffects());

ece420_main eceAutoTune;

LiveEffectEngine::LiveEffectEngine() {
    soundBuffer.resize(4096);
    soundFreqBuffer.resize(4096);
}
stk::Rhodey * myRhodey;

LiveEffectEngine::~LiveEffectEngine() {
    LOGV("DESTRUCTOR CALLED");
}

void LiveEffectEngine::setRecordingDeviceId(int32_t deviceId) {
    mRecordingDeviceId = deviceId;
}

void LiveEffectEngine::setPlaybackDeviceId(int32_t deviceId) {
    mPlaybackDeviceId = deviceId;
}

bool LiveEffectEngine::isAAudioSupported() {
    oboe::AudioStreamBuilder builder;
    return builder.isAAudioSupported();
}
bool LiveEffectEngine::setAudioApi(oboe::AudioApi api) {
    if (mIsEffectOn) return false;

    mAudioApi = api;
    return true;
}
void LiveEffectEngine::setEffectOn(bool isOn) {
    LOGV("oski trace %i",isOn);
    if (isOn != mIsEffectOn) {
        mIsEffectOn = isOn;
    }
    if(isOn == true){
        //if(isSaxofony)
        {

            //myfreeverb.setT60(0.2);
            //myfreeverb.setDamping(0.2);
            //myfreeverb.setSampleRate(48000);
            //myfreeverb.setSampleRate(mSampleRate);
            //myfreeverb.setEffectMix(0.2);
        }
        LOGV("oski trace #2 %i",isOn);

        //lowBiQuad = mne->reconfigureBiQuad(0,300,8, 1);
        //midBiQuad = mne->reconfigureBiQuad(2,2000,8, 1);
        highBiQuad = mne->reconfigureBiQuad(0,3000,4, 0.5);

        //myPitShift.setEffectMix(1);
    }
    if (isOn) {
        openAllStreams();
    } else {
        closeAllStreams();
    }
}



void LiveEffectEngine::setTheReverb() {
    myfreeverb.setT60(thresholdLevel);
    myfreeverb.setEffectMix(reverbDry);
}


std::vector<unsigned char> message;
void LiveEffectEngine::openAllStreams() {
    // Note: The order of stream creation is important. We create the playback
    // stream first, then use properties from the playback stream
    // (e.g. sample rate) to create the recording stream. By matching the
    // properties we should get the lowest latency path

    buffsize = bestBufferSize*2;



    LOGV("oski before bestparams #3 %i %i %i",bestBufferSize, bestSampleRate ,mRecordingDeviceId);

    eceAutoTune.setBestParams(bestBufferSize,bestSampleRate);
    mSampleRate = bestSampleRate;

    vcircularBuffer.resize(static_cast<unsigned long>(bestBufferSize * bufferMulti/2));
    theFreqs.resize(static_cast<unsigned long>(bufferMulti));

    vbufferSoundFile.resize(static_cast<unsigned long>(bestBufferSize*bufferMulti));
    vprocessOutput.resize(static_cast<unsigned long>(bestBufferSize*bufferMulti));

    vsamplesMix.resize(static_cast<unsigned long>(bestBufferSize*2));
    vsamplesFx.resize(static_cast<unsigned long>(bestBufferSize*2));
    vsamplesout.resize(static_cast<unsigned long>(bestBufferSize*2));

    midi_init();

    //for visuals
    soundBuffer.resize(4096);
    soundFreqBuffer.resize(4096);

    //myPitShift.setEffectMix(1);
    //myPitShift.setSampleRate(bestSampleRate);
    LOGV("oskyoy %s ",theRawFolderSrc);

    stk::Stk::setRawwavePath(theRawFolderSrc);

    /*myRhodey = new stk::Rhodey();
    myRhodey->setSampleRate( bestSampleRate );
    myRhodey->controlChange(2,60);
    myRhodey->controlChange(1,120);
    myRhodey->controlChange(11,60);
    myRhodey->controlChange(128,10);
    myRhodey->controlChange(4,10);*/

    // Depending on how you compile STK, you may need to explicitly set
    // the path to the rawwave directory.
    //myRhodey.setRawwavePath( "../rawwaves/" );

    openPlaybackStream();
    openRecordingStream();
    // Now start the recording stream first so that we can read from it during
    // the playback stream's dataCallback
    if (mRecordingStream && mPlayStream) {

        startStream(mRecordingStream);
        startStream(mPlayStream);
    } else {
        LOGE("Failed to create recording (%p) and/or playback (%p) stream",
             mRecordingStream, mPlayStream);
        closeAllStreams();
    }
}


/**
 * Stops and closes the playback and recording streams.
 */
void LiveEffectEngine::closeAllStreams() {
    /**
     * Note: The order of events is important here.
     * The playback stream must be closed before the recording stream. If the
     * recording stream were to be closed first the playback stream's
     * callback may attempt to read from the recording stream
     * which would cause the app to crash since the recording stream would be
     * null.
     */

    //LOGE("Failed to create recording %i", theInStream.is_open());
    if (mPlayStream != nullptr) {
        closeStream(mPlayStream);  // Calling close will also stop the stream
        mPlayStream = nullptr;
    }

    if (mRecordingStream != nullptr) {
        closeStream(mRecordingStream);
        mRecordingStream = nullptr;
    }

    if(isRhodey) {
        isRhodey = false;
    }

    /*if(theOutStream.is_open()) {
        theOutStream.close();
    }
    if(theInStream.is_open()) {
        theInStream.close();
    }
    if(theOutStreamMixed.is_open()) {
        theOutStreamMixed.close();
    }
    if(theOutStreamFx.is_open()) {
        theOutStreamFx.close();
    }

    if(lowBiQuad) {
        delete lowBiQuad;
    }
    if(midBiQuad) {
        delete midBiQuad;
    }
    if(highBiQuad) {
        delete highBiQuad;
    }*/
}

/**
 * Creates an audio stream for recording. The audio device used will depend on
 * mRecordingDeviceId.
 * If the value is set to oboe::Unspecified then the default recording device
 * will be used.
 */
void LiveEffectEngine::openRecordingStream() {
    // To create a stream we use a stream builder. This allows us to specify all
    // the parameters for the stream prior to opening it
    oboe::AudioStreamBuilder builder;

    setupRecordingStreamParameters(&builder);

    // Now that the parameters are set up we can open the stream
    oboe::Result result = builder.openStream(&mRecordingStream);

    if (result == oboe::Result::OK && mRecordingStream) {
        //assert(mRecordingStream->getChannelCount() == mInputChannelCount);
        assert(mRecordingStream->getSampleRate() == mSampleRate);
        assert(mRecordingStream->getFormat() == oboe::AudioFormat::Float);
        warnIfNotLowLatency(mRecordingStream);
        LOGV("Failed to create recording stream. Error: %i %i %i",(int)mRecordingStream->getChannelCount(),(int)mRecordingStream->getSampleRate(), (int)mRecordingStream->getFramesPerBurst());
    } else {
        LOGE("Failed to create recording stream. Error: %s",
             oboe::convertToText(result));
    }
}

/**
 * Creates an audio stream for playback. The audio device used will depend on
 * mPlaybackDeviceId.
 * If the value is set to oboe::Unspecified then the default playback device
 * will be used.
 */
void LiveEffectEngine::openPlaybackStream() {
    oboe::AudioStreamBuilder builder;

    setupPlaybackStreamParameters(&builder);
    oboe::Result result = builder.openStream(&mPlayStream);
    if (result == oboe::Result::OK && mPlayStream) {

        mSampleRate = mPlayStream->getSampleRate();
        LOGE("the channelcnt: %i %i", mPlayStream->getChannelCount(), mPlayStream->getSampleRate());
        assert(mPlayStream->getFormat() == oboe::AudioFormat::Float);
        //assert(mOutputChannelCount == mPlayStream->getChannelCount());
        mSystemStartupFrames =
                static_cast<uint64_t>(mSampleRate * kSystemWarmupTime);
        mProcessedFrameCount = 0;

        warnIfNotLowLatency(mPlayStream);

    } else {
        LOGE("Failed to create playback stream. Error: %s",
             oboe::convertToText(result));
    }
}

/**
 * Sets the stream parameters which are specific to recording,
 * including the sample rate which is determined from the
 * playback stream.
 *
 * @param builder The recording stream builder
 */
oboe::AudioStreamBuilder *LiveEffectEngine::setupRecordingStreamParameters(

        oboe::AudioStreamBuilder *builder) {
    // This sample uses blocking read() by setting callback to null
    builder->setCallback(nullptr)
            ->setDeviceId(mRecordingDeviceId)
            ->setDirection(oboe::Direction::Input)
            ->setSampleRate(mSampleRate)
            ->setFormat(oboe::AudioFormat::Float)
                    //->setBufferCapacityInFrames(mRecordingStream->getFramesPerBurst() * 2)
            ->setChannelCount(2);
    return setupCommonStreamParameters(builder);
}

/**
 * Sets the stream parameters which are specific to playback, including device
 * id and the dataCallback function, which must be set for low latency
 * playback.
 * @param builder The playback stream builder
 */
oboe::AudioStreamBuilder *LiveEffectEngine::setupPlaybackStreamParameters(
        oboe::AudioStreamBuilder *builder) {
    builder->setCallback(this)
            ->setDeviceId(mPlaybackDeviceId)
            ->setDirection(oboe::Direction::Output)
            //->setSampleRate(mSampleRate)
            ->setFormat(oboe::AudioFormat::Float)
            ->setChannelCount(2);
    //->setSampleRate(mSampleRate);
    return setupCommonStreamParameters(builder);
}

/**
 * Set the stream parameters which are common to both recording and playback
 * streams.
 * @param builder The playback or recording stream builder
 */
oboe::AudioStreamBuilder *LiveEffectEngine::setupCommonStreamParameters(
        oboe::AudioStreamBuilder *builder) {
    // We request EXCLUSIVE mode since this will give us the lowest possible
    // latency.
    // If EXCLUSIVE mode isn't available the builder will fall back to SHARED
    // mode.
    if(mAudioApi == oboe::AudioApi::AAudio) {
        builder->setAudioApi(mAudioApi)
                ->setFormat(mFormat)
                ->setSharingMode(oboe::SharingMode::Exclusive)
                ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
                ->setFramesPerCallback(bestBufferSize);
    } else {
        builder->setAudioApi(mAudioApi)
                ->setFormat(mFormat)
                ->setSharingMode(oboe::SharingMode::Exclusive)
                ->setPerformanceMode(oboe::PerformanceMode::LowLatency);
                //->setFramesPerCallback(bestBufferSize);

    }
    //->setFramesPerCallback(bestBufferSize);
    //->setBufferCapacityInFrames(bestBufferSize);
    return builder;
}

void LiveEffectEngine::startStream(oboe::AudioStream *stream) {

    assert(stream);
    if (stream) {
        mLatencyTuner = make_unique<oboe::LatencyTuner>(*mPlayStream);

        oboe::Result result = stream->requestStart();
        if (result != oboe::Result::OK) {
            LOGE("Error starting stream. %s", oboe::convertToText(result));
        }
    }
}

void LiveEffectEngine::stopStream(oboe::AudioStream *stream) {
    if (stream) {
        oboe::Result result = stream->stop(0L);
        if (result != oboe::Result::OK) {
            LOGE("Error stopping stream. %s", oboe::convertToText(result));
        }
    }

}

/**
 * Close the stream. AudioStream::close() is a blocking call so
 * the application does not need to add synchronization between
 * onAudioReady() function and the thread calling close().
 * [the closing thread is the UI thread in this sample].
 * @param stream the stream to close
 */
void LiveEffectEngine::closeStream(oboe::AudioStream *stream) {
    if (stream) {
        oboe::Result result = stream->close();
        if (result != oboe::Result::OK) {
            LOGE("Error closing stream. %s", oboe::convertToText(result));
        }
        /*oboe::Result result = stream->close();
        if (result != oboe::Result::OK) {
            LOGE("Error closing stream. %s", oboe::convertToText(result));
        }*/
        //myPitShift->release();
    }
    //fclose(theFile);
    //fclose(theFileOut);

    //fflush(theFileOut);
    //clearerr(theFileOut);
    //fclose(theFileOut);

}

/**
 * Restart the streams. During the restart operation subsequent calls to this
 * method will output a warning.
 */
void LiveEffectEngine::restartStreams() {
    LOGI("Restarting streams");

    if (mRestartingLock.try_lock()) {
        //fclose(theFile);
        //fclose(theFileOut);
        closeAllStreams();
        openAllStreams();
        mRestartingLock.unlock();
    } else {
        LOGW(
                "Restart stream operation already in progress - ignoring this "
                "request");
        // We were unable to obtain the restarting lock which means the restart
        // operation is currently
        // active. This is probably because we received successive "stream
        // disconnected" events.
        // Internal issue b/63087953
    }
}

/**
 * Warn in logcat if non-low latency stream is created
 * @param stream: newly created stream
 *
 */
void LiveEffectEngine::warnIfNotLowLatency(oboe::AudioStream *stream) {
    if (stream->getPerformanceMode() != oboe::PerformanceMode::LowLatency) {
        LOGW(
                "Stream is NOT low latency."
                "Check your requested format, sample rate and channel count %i %i", stream->getSampleRate(),stream->getBufferSizeInFrames());
        //stream->setBufferSizeInFrames(stream->getBufferSizeInFrames()*2);
    }
}

void LiveEffectEngine::changeMidiInstrument(int number) {
    EAS_U8 midiInstr[2];
    midiInstr[0] = 0xC0;
    midiInstr[1] = static_cast<EAS_U8>(number);
    midi_write(midiInstr, 2);
    midi_setVolume(99);

    chosenInstrument = number;
}

// moving median up here so forward declaration isn't needed
double median(std::vector<float> &n) { //don't need size. Vector knows how big it is
    std::sort(n.begin(), n.end()); // use built-in sort function

    double result = 0;
    auto size = n.size();
    if ((size % 2) == 0) {
        result = (n[size/2] + n[(size/2) - 1])/2.0;
    }
    else {
        result = n[size/2];
    };

    // since this function calculates and returns the result, it shouldn't
    // also print. A function should only do one thing. It make them easier
    // to debug and more re-usable
    return result;
}
oboe::Result
LiveEffectEngine::calculateCurrentOutputLatencyMillis(oboe::AudioStream *stream,
                                                      double *latencyMillis) {

    // Get the time that a known audio frame was presented for playing
    auto result = stream->getTimestamp(CLOCK_MONOTONIC);

    if (result == oboe::Result::OK) {

        oboe::FrameTimestamp playedFrame = result.value();

        // Get the write index for the next audio frame
        int64_t writeIndex = stream->getFramesWritten();

        // Calculate the number of frames between our known frame and the write index
        int64_t frameIndexDelta = writeIndex - playedFrame.position;

        // Calculate the time which the next frame will be presented
        int64_t frameTimeDelta = (frameIndexDelta * oboe::kNanosPerSecond) / mSampleRate;
        int64_t nextFramePresentationTime = playedFrame.timestamp + frameTimeDelta;

        // Assume that the next frame will be written at the current time
        using namespace std::chrono;
        int64_t nextFrameWriteTime =
                duration_cast<nanoseconds>(steady_clock::now().time_since_epoch()).count();

        // Calculate the latency
        *latencyMillis = static_cast<double>(nextFramePresentationTime - nextFrameWriteTime)
                         / oboe::kNanosPerMillisecond;
    } else {
        LOGE("Error calculating latency: %s", oboe::convertToText(result.error()));
    }

    return result;
}

/**
 * Handles playback stream's audio request. In this sample, we simply block-read
 * from the record stream for the required samples.
 *
 * @param oboeStream: the playback stream that requesting additional samples
 * @param audioData:  the buffer to load audio samples for playback stream
 * @param numFrames:  number of frames to load to audioData buffer
 * @return: DataCallbackResult::Continue.
 */

int sameFreqCnt = 0;
float theLastPitch = 0;
bool shortSilenceActivated = false;
//int32_t modVol = 0;
int runcnt = 0;

float silentSoundLevel = 0;
int64_t volActivatedCnt = 0;
bool isBlowing = false;
bool isKeyOn = false;
bool isVolActivated = false;
bool lastShortSilence = false;
bool pauseActivated = false;
bool isDeactivatedAgain = false;
float theRatio = 1;
float newFreq = 0;
int32_t lastNumframes = 0;

int16_t currentMidiEvent = -1;

bool isFirstLoud = true;

oboe::DataCallbackResult LiveEffectEngine::onAudioReady(
        oboe::AudioStream *oboeStream, void *audioData, int32_t numFrames) {
    //LOGV("oski went KNOW");
    assert((oboeStream == mPlayStream));
    float retRms = 0;
    int32_t bufferSize = mPlayStream->getBufferSizeInFrames();

    int32_t prevFrameRead = 0, framesRead = 0;
    if (mProcessedFrameCount < mSystemStartupFrames) {
        do {
            // Drain the audio for the starting up period, half second for
            // this sample.
            prevFrameRead = framesRead;

            oboe::ResultWithValue<int32_t> status =
                    mRecordingStream->read(audioData, numFrames, 0);
            framesRead = (!status) ? 0 : status.value();
            if (framesRead == 0) break;



            if (mBufferSizeSelection == kBufferSizeAutomatic) {
                mLatencyTuner->tune();
            }else if (bufferSize != (mBufferSizeSelection * mFramesPerBurst)) {
                auto setBufferResult = mPlayStream->setBufferSizeInFrames(mBufferSizeSelection * mFramesPerBurst);
                if (setBufferResult == oboe::Result::OK) bufferSize = setBufferResult.value();
            }

            if (mIsLatencyDetectionSupported) {
                calculateCurrentOutputLatencyMillis(mPlayStream, &mCurrentOutputLatencyMillis);

                LOGV("oskilatency %f ", mCurrentOutputLatencyMillis);
            }

        } while (framesRead);

        framesRead = prevFrameRead;
    } else {
        oboe::ResultWithValue<int32_t> status =
                mRecordingStream->read(audioData, numFrames, 0);
        if (!status) {
            LOGE("input stream read error: %s",
                 oboe::convertToText(status.error()));
            return oboe::DataCallbackResult ::Stop;
        }
        framesRead = status.value();
    }


    if (framesRead < numFrames) {
        int32_t bytesPerFrame = mRecordingStream->getChannelCount() *
                                oboeStream->getBytesPerSample();
        uint8_t *padPos =
                static_cast<uint8_t *>(audioData) + framesRead * bytesPerFrame;
        memset(padPos, 0, static_cast<size_t>((numFrames - framesRead) * bytesPerFrame));
    }

    //LOGV("oski went KNOW#3");

    // process every sample
    float* samples = static_cast<float*>(audioData);
    sampleCount =numFrames*mInputChannelCount;//*mRecordingStream->getChannelCount();
    //int16_t* samplesout = new int16_t[sampleCount];
    //int16_t* samplesFx = new int16_t[sampleCount];
    stk::StkFrames stkOutput(static_cast<unsigned int>(sampleCount),1);
    //stk::StkFrames stkOutput1(static_cast<unsigned int>(sampleCount),1);
    //stk::StkFrames stkOutput2(static_cast<unsigned int>(sampleCount),1);
    //stk::StkFrames stkOutput3(static_cast<unsigned int>(sampleCount),1);

    silentSoundLevel = thresholdLevel*0.01f;//(thresholdLevel/numFrames/50.0f);

    float rms = 0;
    if(vcircularBuffer.size() < sampleCount)
    {
        vcircularBuffer.resize(sampleCount);
    }
    //LOGV("OSKIERR %i %li",sampleCount, vcircularBuffer.size());
    for(int j = 0; j < sampleCount; j++) {
        samples[j] = static_cast<float>(highBiQuad->tick(samples[j]));

        float doubleauto = samples[j];
        //because of stereo
        if(j %2 == 0) {
            rms+=(doubleauto*doubleauto);
        }

        vcircularBuffer[vcircularBuffer.size()-sampleCount+j] =  (samples[j]);
        //stkOutput[j] = ((stk::StkFloat) samples[j] / (stk::StkFloat) SHRT_MAX);;
    }
    for (int j = 0; j < vcircularBuffer.size() - sampleCount; j++) {
        vcircularBuffer[j] = vcircularBuffer[j + sampleCount];
        //stkOutput[j] = ((stk::StkFloat) vcircularBuffer[j + sampleCount] / (stk::StkFloat) SHRT_MAX);;

    }
    retRms = (sqrt(rms))/numFrames*10;
    //LOGV("oskinumframes %i %i", numFrames, bufferMulti);
    //LOGV("OSKITH %f %f %f",retRms,thresholdLevel, silentSoundLevel);
    playerLoudness = static_cast<int64_t>(retRms * 100000);

    //LOGV("oskicurio %i %i %i %f %i %f",bufferMulti,sampleCount, (int)retRms, sqrt(rms), (int)silentSoundLevel, thresholdLevel );
    //if(volTriggeredCnt + 2000 <= mProcessedFrameCount)
    {
        if(retRms > silentSoundLevel) {
            //volTriggeredCnt = mProcessedFrameCount;
            shortSilenceActivated = false;
        } else {
            shortSilenceActivated = true;
            lastShortSilence = true;
            pauseActivated = true;
            //isBlowing = false;
        }

    }

    lastShortSilence = shortSilenceActivated;

    runcnt++;

    ///LOGV("oskitest %li %li %i ", retRms, silentSoundLevel,retRms > silentSoundLevel);

    if(isPlayMode) {
        for(int i = 0; i < myMidiEvents.size(); i++) {
            if(mProcessedFrameCount - playModeStart >= myMidiEvents[i][1]*bestSampleRate-300 && mProcessedFrameCount - playModeStart <= myMidiEvents[i][1]*bestSampleRate+300) {
                //if(!isKeyOn)
                {
                    //if(isRhodey)
                    {
                        EAS_U8 midi[3];
                        midi[0] = 0x90;
                        midi[1] = static_cast<EAS_U8>(myMidiEvents[i][0]);
                        midi[2] = 63;
                        midi_write(midi, 3);
                        midi_setVolume(99);


                        //message.resize(3);
                        //message[0] = 144;message[1] = 64;message[2] = 90;// Send the message immediately.midiout.sendMessage( &message );
                        //rtMidi.sendMessage(&message);
                        //RtAudio audio;
                        //audio.startStream();
                        //audio.openStream()
                    }
                    //isKeyOn = true;
                }

            } else if(mProcessedFrameCount - playModeStart >= myMidiEvents[i][2]*bestSampleRate-300 && mProcessedFrameCount - playModeStart <= myMidiEvents[i][2]*bestSampleRate+300) {
                //if(isKeyOn)
                {
                    EAS_U8 midi[3];
                    midi[0] = 0x80;
                    midi[1] = static_cast<EAS_U8>(myMidiEvents[i][0]);
                    midi[2] = 63;
                    midi_write(midi, 3);
                    midi_setVolume(99);
                    if(isRhodey) {
                        //myRhodey->noteOff(0.5);
                    } else if(isGuitar) {
                        //myGuitar.noteOff(0.5);
                    }
                    //isKeyOn = false;
                }

            }
            LOGV("oskye %i %i %f", (int)(mProcessedFrameCount - playModeStart),(int)playModeStart, myMidiEvents[i][1]*bestSampleRate);

        }
        playerTime+=numFrames;
    }

    if(isMidiRecording) {
        playerTime+=numFrames;
    }


    if(playMidiOnMessage) {
        //bool specialCase = false;
        LOGV("oskigettt  %i" , playMidiNote);


        if(!isKeyOn) {

            isKeyOn = true;

            //if(isRhodey)
            playerAutotuneLevel = (playMidiNote);

            LOGV("oskigetity  %i" , playMidiNote);
            EAS_U8 midi[3];
            midi[0] = 0x90;
            midi[1] = static_cast<EAS_U8>(playMidiNote + 36);
            midi[2] = 63;
            midi_write(midi, 3);
            midi_setVolume(99);
            if(isMidiRecording) {
                myMidiEvents.resize(static_cast<unsigned long>(myMidiEvents.size() + 1));
                myMidiEvents[myMidiEvents.size()-1].resize(3);
                //float midiFreq = eceAutoTune.getTheMidiNote();
                myMidiEvents[myMidiEvents.size()-1][0] =  (playMidiNote+36);
                myMidiEvents[myMidiEvents.size()-1][1] = ((mProcessedFrameCount-midiRecTime)/(float)bestSampleRate);
            }
        }


        if(playMidiNote != lastPlayedMidiNote && lastPlayedMidiNote > 0) {
            //if(specialCase)
            isKeyOn = false;
            EAS_U8 midi[3];
            midi[0] = 0x80;
            midi[1] = static_cast<EAS_U8>(lastPlayedMidiNote + 36);
            midi[2] = 0;
            midi_write(midi, 3);
            midi_setVolume(99);


            playerAutotuneLevel = (playMidiNote);

            LOGV("oskigetity  %i" , playMidiNote);
            EAS_U8 midiOn[3];
            midiOn[0] = 0x90;
            midiOn[1] = static_cast<EAS_U8>(playMidiNote + 36);
            midiOn[2] = 63;
            midi_write(midiOn, 3);
            midi_setVolume(99);
            {
                if(isMidiRecording) {
                    myMidiEvents[myMidiEvents.size()-1][2] = ((mProcessedFrameCount-midiRecTime)/(float)bestSampleRate);;
                }
            }
            playerAutotuneLevel = playMidiNote;
        } else {
            isKeyOn = true;
        }
        currentMidiEvent++;
        //std::vector<float> event(static_cast<unsigned long>(newFreq));

        lastPlayedMidiNote = playMidiNote;
        LOGV("oskigetitn %i %i" , lastPlayedMidiNote, playMidiNote);
    } else {
        if(isKeyOn) {
            LOGV("oskigetitn  %i" , playMidiNote);

            if(isMidiRecording) {
                myMidiEvents[myMidiEvents.size()-1][2] = ((mProcessedFrameCount-midiRecTime)/(float)bestSampleRate);;
            }
            //myMidiEvents[currentMidiEvent].push_back(event);

            isKeyOn = false;
            //myWurley.noteOff(0.2);
            EAS_U8 midi[3];
            midi[0] = 0x80;
            midi[1] = static_cast<EAS_U8>(playMidiNote + 36);
            midi[2] = 0;
            midi_write(midi, 3);
            midi_setVolume(99);
            //playerAutotuneLevel = 0;
            //myRhodey->noteOff((stk::StkFloat) 1);
            //playMidiNote = 0;
            //lastPlayedMidiNote = 0;
        }
    }



    {
        if(!isVolActivated)
        {

            if(retRms >= silentSoundLevel) {
                isVolActivated = true;
                //tmpRms = 0;
                //isDeactivatedAgain = false;

            }
            //tmpRms = 0;
        }
        else if(isVolActivated)
        {
            if(retRms < silentSoundLevel) {
                isDeactivatedAgain = true;
            }
        }
        //tmpRms = 0;

    }



    //if(!isPlayMode)
    {
        //LOGV("oskioou %i %i", (int)retRms, isRecActive);
        if (isRecActive) {
            //LOGV("oskiret %f %f ", retRms, silentSoundLevel);
            if (retRms > silentSoundLevel ) {
                //if (isRhodey || isSaxofony || isGuitar)
                {
                    volActivatedCnt++;
                }

                if (volActivatedCnt >= bufferMulti || numFrames >= 960)
                {

                    theRatio = eceAutoTune.detectBufferPeriod2ThreadSafe(vcircularBuffer);

                    //if(tmpRms > silentSoundLevel)

                    if (eceAutoTune.getTheMidiNote() == theLastPitch) {
                        sameFreqCnt++;
                    } else {
                        sameFreqCnt = 0;
                    }
                    //LOGV("oski hitm1 %f %i", newFreq, sameFreqCnt);
                    //if(sameFreqCnt > 1 && retRms > silentSoundLevel)
                    if (sameFreqCnt > 2) {
                        newFreq = eceAutoTune.getThePitch();
                        //LOGV("oski hitm2 %f", newFreq);
                        pauseActivated = false;
                        rawPitch = eceAutoTune.getTheRawMidiNote();
                        LOGV("oskitesto %i %i %i %i %i %i",rawPitch, playerAutotuneLevel, bestBufferSize, bestSampleRate, numFrames,bufferMulti);

                    }

                    theLastPitch = eceAutoTune.getTheMidiNote();
                    if (sameFreqCnt > 2) {
                        soundBuffer = vcircularBuffer;
                        if (!isBlowing)
                        {
                            playerAutotuneLevel = eceAutoTune.getTheMidiNote();

                            if (isMidiRecording) {
                                myMidiEvents.resize(
                                        static_cast<unsigned long>(myMidiEvents.size() +
                                                                   1));
                                myMidiEvents[myMidiEvents.size() - 1].resize(3);
                                float midiFreq = eceAutoTune.getTheMidiNote();
                                myMidiEvents[myMidiEvents.size() - 1][0] = (midiFreq);
                                myMidiEvents[myMidiEvents.size() - 1][1] = (
                                        (mProcessedFrameCount - midiRecTime) /
                                        (float) bestSampleRate);
                            }


                            EAS_U8 midi[3];
                            midi[0] = 0x90;
                            midi[1] = static_cast<EAS_U8>(playerAutotuneLevel);
                            midi[2] = 63;
                            midi_write(midi, 3);
                            midi_setVolume(99);

                            isBlowing = true;

                            currentMidiEvent++;
                        }
                        rawPitch = eceAutoTune.getTheRawMidiNote();

                    }

                }
            //SILENCE AGAIN
            } else if (retRms < silentSoundLevel / 2) {
                isFirstLoud = true;
                volActivatedCnt = 0;
                if (isBlowing) {
                    EAS_U8 midi[3];
                    midi[0] = 0x80;
                    midi[1] = static_cast<EAS_U8>(playerAutotuneLevel);
                    midi[2] = 0;
                    midi_write(midi, 3);
                    midi_setVolume(99);

                    isBlowing = false;

                    if (isMidiRecording) {
                        myMidiEvents[myMidiEvents.size() - 1][2] = (
                                (mProcessedFrameCount - midiRecTime) / (float) bestSampleRate);;
                    }
                }

                playerAutotuneLevel = 0;
                rawPitch = 0;

                pauseActivated = true;
            }
            if(retRms > silentSoundLevel) {
                isFirstLoud = false;

            }
        }
    }

    for(int i = 0; i < sampleCount;i++) {
        samples[i] = 0;
        //LOGV("oskiosk");
    }

    //if(isRhodey || isSaxofony || isWurley || isBlow || isGuitar )
    /*{
        for(int k = 0; k < sampleCount; k++) {
            //stkOutput[k] = ((stk::StkFloat) vcircularBuffer[k] / (stk::StkFloat) SHRT_MAX);;
            //stkOutput[k] = ((stk::StkFloat) samples[k] / (stk::StkFloat) SHRT_MAX);;

            if(isRhodey) {
                //stkOutput[k] = myWurley.tick();

                stkOutput[k] = myRhodey->tick();
                samples[k] = static_cast<int16_t>(stkOutput[k] * SHRT_MAX /2);
            } else if(isSaxofony) {

                stkOutput[k] = mySaxo.tick();
                samples[k] = static_cast<int16_t>(stkOutput[k] * SHRT_MAX /2);

            } else if(isGuitar) {
                stkOutput[k] = myGuitar.tick();
                samples[k] = static_cast<int16_t>(stkOutput[k] * SHRT_MAX /2);

            }else if(isBlow) {
                stkOutput[k] = myBlow.tick();
                samples[k] = static_cast<int16_t>(stkOutput[k] * SHRT_MAX /2);

            }
            if(isRecording) {
                vsamplesMix[k] = static_cast<short>(stkOutput[k] * SHRT_MAX);
                vsamplesFx[k] = static_cast<short>(stkOutput[k] * SHRT_MAX);
                vsamplesout[k] = static_cast<short>(stkOutput[k] * SHRT_MAX);
            }
        }
    }*/ /*else if(isDrums) {
        for(int i = 0; i < sampleCount;i++) {
                stkOutput[i] = myDrummer.tick();
                samples[i] = static_cast<int16_t>(stkOutput[i] * SHRT_MAX / 2);
                //LOGV("oskiosk");
        }
    } else {

    }*/

    if(isPlaying) {
        playerTime += sampleCount*4;
    }
    // add your audio processing here
    lastNumframes = numFrames;

    //LOGV("oski went all %i ", lastNumframes);
    mProcessedFrameCount += numFrames;
    return oboe::DataCallbackResult::Continue;

}


/**
 * Oboe notifies the application for "about to close the stream".
 *
 * @param oboeStream: the stream to close
 * @param error: oboe's reason for closing the stream
 */
void LiveEffectEngine::onErrorBeforeClose(oboe::AudioStream *oboeStream,
                                          oboe::Result error) {
    LOGE("%s stream Error before close: %s",
         oboe::convertToText(oboeStream->getDirection()),
         oboe::convertToText(error));


}

/**
 * Oboe notifies application that "the stream is closed"
 *
 * @param oboeStream
 * @param error
 */
void LiveEffectEngine::onErrorAfterClose(oboe::AudioStream *oboeStream,
                                         oboe::Result error) {
    LOGE("%s stream Error after close: %s",
         oboe::convertToText(oboeStream->getDirection()),
         oboe::convertToText(error));


}


void LiveEffectEngine::addAssetManager(AAssetManager *assetMgr, const char *filename, const char *filenameOut,
                                       const char *filenameOutFx, const char * filenameMixed, const char* nativerawWavesFolder) {
    theFileSrc = filename;
    //theFile = fopen(theFileSrc,"r");
    theFileSrcOut = filenameOut;
    theFileSrcOutFx = filenameOutFx;
    theFileSrcMixed = filenameMixed;
    theRawFolderSrc = nativerawWavesFolder;
    LOGV("OSKIWHA %s",theRawFolderSrc);
    //theFileOut = fopen(theFileSrcOut,"w+");

    //theInStream = std::ifstream();
    //theInStream.open(theFileSrc,std::ios_base::binary);
    theOutStream = std::fstream();
    theOutStream.open(theFileSrcOut,std::ios_base::out | std::ios_base::in | std::ios_base::binary);
    //theOutStreamFx = std::fstream();
    //theOutStreamFx.open(theFileSrcOutFx,std::ios_base::out | std::ios_base::in | std::ios_base::binary);

    //theOutStreamMixed = std::fstream();
    //theOutStreamMixed.open(theFileSrcMixed,std::ios_base::out | std::ios_base::in | std::ios_base::binary);

    //AAsset* asset = AAssetManager_open(assetMgr, filename, AASSET_MODE_BUFFER);
    //asset
    LOGE("went in oski %s", filename);

}

std::vector<int16_t> LiveEffectEngine::loadFromAssets(long theoffset, int howmany) {

    char*outBuff = new char[howmany];

    theInStream.seekg(theoffset);
    theInStream.read(outBuff, howmany);

    LOGD("Went in load:  %i %i %i", howmany,buffsize,(int)theoffset);

    int16_t * audioBuffer = reinterpret_cast<int16_t*>(outBuff);
    LOGD("oskiknowl %i %i %i %i %i",howmany, buffsize, (sampleCount*8),bestBufferSize,(int)vbufferSoundFile.size());

    if(vbufferSoundFile.size() < howmany/4) {
        vbufferSoundFile.resize(static_cast<unsigned long>((howmany / 4)));
    }
    if(audioBuffer != nullptr) {
        for(int i = 0; i < howmany/4; i++) {
            vbufferSoundFile[i] = audioBuffer[i*2];
        }

    }

    if(outBuff) {
        delete[] outBuff;
    }


    // There are 4 bytes per frame because
    // each sample is 2 bytes and
    // it's a stereo recording which has 2 samples per frame.
    //int32_t numFrames = static_cast<int32_t>(trackLength / 4);
    //LOGD("Opened backing track, bytes: %ld frames: %d", trackLength, numFrames);

    return vbufferSoundFile;
}


stk::BiQuad* LiveEffectEngine::reconfigureBiQuad(const char *whichbq, int type, int cf, double gotGainDB, double theQ) {
    if(strncmp(whichbq,"low",3)) {
        //lowBiQuad = new stk::BiQuad();
        lowBiQuad = mne->reconfigureBiQuad(type,  cf,  gotGainDB,  theQ);
    } else if(strncmp(whichbq,"mid",3))  {
        //midBiQuad = new stk::BiQuad();
        midBiQuad = mne->reconfigureBiQuad(type,  cf,  gotGainDB,  theQ);

    } else if(strncmp(whichbq,"hig",3)) {
        //highBiQuad = new stk::BiQuad();
        highBiQuad = mne->reconfigureBiQuad(type,  cf,  gotGainDB,  theQ);
    }
    return lowBiQuad;
}
