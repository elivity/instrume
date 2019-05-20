#
# Copyright 2018 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#


cmake_minimum_required(VERSION 3.4.1)
project(autotuner LANGUAGES C CXX)

get_filename_component(SAMPLE_ROOT_DIR
        ../../.. ABSOLUTE)

### INCLUDE OBOE LIBRARY ###
set (OBOE_DIR ${SAMPLE_ROOT_DIR}/..)
#add_subdirectory(${SAMPLE_ROOT_DIR}/app/src/main/mylibs/ffts)

add_subdirectory(${OBOE_DIR} ./oboe-bin)

#add_executable(autotuner ../libs)


cmake_minimum_required (VERSION 3.0)



#file(GLOB ENDF6_SRC_TOP1
#        ${SAMPLE_ROOT_DIR}/app/src/main/mylibs/fftw3/api/*.c
#        )
#file(GLOB ENDF6_SRC_TOP2
#        ${SAMPLE_ROOT_DIR}/app/src/main/mylibs/fftw3/dft/*.c
#        )
#file(GLOB_RECURSE ENDF6_SRC_NESTED1
#        ${SAMPLE_ROOT_DIR}/app/src/main/mylibs/fftw3/dft/scalar/*.c)
#file(GLOB_RECURSE ENDF6_SRC_NESTED2
#        ${SAMPLE_ROOT_DIR}/app/src/main/mylibs/fftw3/dft/scalar/codelets/*.c)
#file(GLOB ENDF6_SRC_TOP3
#        ${SAMPLE_ROOT_DIR}/app/src/main/mylibs/fftw3/kernel/*.c
#        )
#file(GLOB ENDF6_SRC_TOP4
#        ${SAMPLE_ROOT_DIR}/app/src/main/mylibs/fftw3/rdft/*.c
#        )
#file(GLOB_RECURSE ENDF6_SRC_NESTED3
#        ${SAMPLE_ROOT_DIR}/app/src/main/mylibs/fftw3/rdft/scalar/*.c)
#file(GLOB_RECURSE ENDF6_SRC_NESTED4
#        ${SAMPLE_ROOT_DIR}/app/src/main/mylibs/fftw3/dft/scalar/r2cb/*.c)
#file(GLOB_RECURSE ENDF6_SRC_NESTED5
#        ${SAMPLE_ROOT_DIR}/app/src/main/mylibs/fftw3/dft/scalar/r2cf/*.c)
#file(GLOB_RECURSE ENDF6_SRC_NESTED6
#        ${SAMPLE_ROOT_DIR}/app/src/main/mylibs/fftw3/dft/scalar/r2r/*.c)
#file(GLOB ENDF6_SRC_TOP5
#        ${SAMPLE_ROOT_DIR}/app/src/main/mylibs/fftw3/reodft/*.c
#        )
#
#set(ENDF6_SRC ${ENDF6_SRC_TOP1} ${ENDF6_SRC_TOP2} ${ENDF6_SRC_NESTED1} ${ENDF6_SRC_NESTED2} ${ENDF6_SRC_TOP3} ${ENDF6_SRC_TOP4} ${ENDF6_SRC_NESTED3} ${ENDF6_SRC_NESTED4} ${ENDF6_SRC_NESTED5} ${ENDF6_SRC_NESTED6}  ${ENDF6_SRC_TOP5} )



#add_library(fftw3
#        SHARED

#        ${ENDF6_SRC}
#        ${SAMPLE_ROOT_DIR}/app/src/main/mylibs/fftw3/include/

#        )

#target_include_directories(fftw3
#        PRIVATE
#        ${SAMPLE_ROOT_DIR}/app/src/main/mylibs/fftw3/include/)

#SET_TARGET_PROPERTIES(fftw3 PROPERTIES LINKER_LANGUAGE C)


#add_library(yinpitch
#        SHARED
#        ${SAMPLE_ROOT_DIR}/app/src/main/mylibs/pitch-detection/src/autocorrelation.cpp
#        ${SAMPLE_ROOT_DIR}/app/src/main/mylibs/pitch-detection/src/mpm.cpp
#        ${SAMPLE_ROOT_DIR}/app/src/main/mylibs/pitch-detection/src/parabolic_interpolation.cpp
#        ${SAMPLE_ROOT_DIR}/app/src/main/mylibs/pitch-detection/src/yin.cpp
#        )


#target_include_directories(yinpitch
#        PRIVATE

#        ${SAMPLE_ROOT_DIR}/app/src/main/mylibs/ffts/include
#        ${SAMPLE_ROOT_DIR}/app/src/main/mylibs/pitch-detection/include
#        )

#target_link_libraries(yinpitch
#        PRIVATE
#        ffts)

add_library(autotuner
        SHARED
        cpp/kiss_fft/kiss_fft.c
        #cpp/Yin.c
        cpp/ece420_lib.cpp
        cpp/ece420_main.cpp
        cpp/audio_common.cpp
        cpp/debug_utils.cpp
        cpp/LiveEffectEngine.cpp
        cpp/Util.cpp
        cpp/jni_bridge.cpp
        cpp/MyNativeEffects.cpp
        ${SAMPLE_ROOT_DIR}/debug-utils/trace.cpp
        )

add_library(midi SHARED IMPORTED)
set_property(TARGET midi PROPERTY IMPORTED_LOCATION "${SAMPLE_ROOT_DIR}/instrume/src/main/libs/${ANDROID_ABI}/libmidi.so")

target_include_directories(autotuner
        PRIVATE

        ${SAMPLE_ROOT_DIR}/debug-utils
        ${OBOE_DIR}/include

        #${SAMPLE_ROOT_DIR}/app/src/main/mylibs/fftw338/api
        #${SAMPLE_ROOT_DIR}/ffts/include/

        ${SAMPLE_ROOT_DIR}/instrume/src/main/stkeffect/include
        ${SAMPLE_ROOT_DIR}/instrume/src/main/cpp/MyNativeEffects.h
        ${SAMPLE_ROOT_DIR}/instrume/src/main/cpp/midi.h
)
add_library(stkeffect
        SHARED
        ${SAMPLE_ROOT_DIR}/instrume/src/main/stkeffect/src/Stk.cpp
        ${SAMPLE_ROOT_DIR}/instrume/src/main/stkeffect/src/FreeVerb.cpp
        ${SAMPLE_ROOT_DIR}/instrume/src/main/stkeffect/src/Delay.cpp
        ${SAMPLE_ROOT_DIR}/instrume/src/main/stkeffect/src/OnePole.cpp
        ${SAMPLE_ROOT_DIR}/instrume/src/main/stkeffect/src/BiQuad.cpp
        ${SAMPLE_ROOT_DIR}/instrume/src/main/stkeffect/src/NRev.cpp
        ${SAMPLE_ROOT_DIR}/instrume/src/main/stkeffect/src/PitShift.cpp
        ${SAMPLE_ROOT_DIR}/instrume/src/main/stkeffect/src/DelayL.cpp
        ${SAMPLE_ROOT_DIR}/instrume/src/main/stkeffect/src/LentPitShift.cpp
        ${SAMPLE_ROOT_DIR}/instrume/src/main/stkeffect/src/Saxofony.cpp
        ${SAMPLE_ROOT_DIR}/instrume/src/main/stkeffect/src/OneZero.cpp
        ${SAMPLE_ROOT_DIR}/instrume/src/main/stkeffect/src/Envelope.cpp
        ${SAMPLE_ROOT_DIR}/instrume/src/main/stkeffect/src/SineWave.cpp
        ${SAMPLE_ROOT_DIR}/instrume/src/main/stkeffect/src/Noise.cpp
        ${SAMPLE_ROOT_DIR}/instrume/src/main/stkeffect/src/Rhodey.cpp
        ${SAMPLE_ROOT_DIR}/instrume/src/main/stkeffect/src/Wurley.cpp
        ${SAMPLE_ROOT_DIR}/instrume/src/main/stkeffect/src/BlowHole.cpp
        ${SAMPLE_ROOT_DIR}/instrume/src/main/stkeffect/src/FM.cpp
        ${SAMPLE_ROOT_DIR}/instrume/src/main/stkeffect/src/FileRead.cpp
        ${SAMPLE_ROOT_DIR}/instrume/src/main/stkeffect/src/FileLoop.cpp
        ${SAMPLE_ROOT_DIR}/instrume/src/main/stkeffect/src/ADSR.cpp
        ${SAMPLE_ROOT_DIR}/instrume/src/main/stkeffect/src/TwoZero.cpp
        ${SAMPLE_ROOT_DIR}/instrume/src/main/stkeffect/src/FileWvIn.cpp
        ${SAMPLE_ROOT_DIR}/instrume/src/main/stkeffect/rawwaves/sine.c
        ${SAMPLE_ROOT_DIR}/instrume/src/main/stkeffect/src/Guitar.cpp
        ${SAMPLE_ROOT_DIR}/instrume/src/main/stkeffect/src/Twang.cpp
        ${SAMPLE_ROOT_DIR}/instrume/src/main/stkeffect/src/DelayA.cpp
        ${SAMPLE_ROOT_DIR}/instrume/src/main/stkeffect/src/Fir.cpp
        ${SAMPLE_ROOT_DIR}/instrume/src/main/stkeffect/src/Phonemes.cpp
        ${SAMPLE_ROOT_DIR}/instrume/src/main/stkeffect/src/Drummer.cpp
        ${SAMPLE_ROOT_DIR}/instrume/src/main/stkeffect/src/Brass.cpp
        ${SAMPLE_ROOT_DIR}/instrume/src/main/stkeffect/src/PoleZero.cpp
        ${SAMPLE_ROOT_DIR}/instrume/src/main/stkeffect/src/RtMidi.cpp

        ${SAMPLE_ROOT_DIR}/instrume/src/main/stkeffect/src/include/)

target_include_directories(stkeffect
        PRIVATE
        ${SAMPLE_ROOT_DIR}/instrume/src/main/stkeffect/include/)


target_link_libraries(autotuner
        PRIVATE
        #gljni
        midi
        stkeffect
        oboe
        #ffts
        #yinpitch
        #fftw3f
        #fftw3f_threads
        android
        atomic
        log
        jnigraphics
        #GLESv2

        )

# Enable optimization flags: if having problems with source level debugging,
# disable -Ofast ( and debug ), re-enable it after done debugging.
target_compile_options(autotuner PRIVATE -Wall -Werror "$<$<CONFIG:RELEASE>:-Ofast>")

