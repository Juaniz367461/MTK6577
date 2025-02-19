/*
 * Copyright (C) 2007 The Android Open Source Project
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

#define LOG_TAG "AudioSRC"

#include <stdint.h>
#include <string.h>
#include <sys/types.h>
#include <cutils/log.h>

#include "AudioResampler.h"
#include "AudioResamplerCubic.h"

#define LOG_TAG "AudioSRC"

namespace android {
// ----------------------------------------------------------------------------

void AudioResamplerCubic::init() {
    memset(&left, 0, sizeof(state));
    memset(&right, 0, sizeof(state));
}

void AudioResamplerCubic::resample(int32_t* out, size_t outFrameCount,
        AudioBufferProvider* provider) {

    // should never happen, but we overflow if it does
    // LOG_ASSERT(outFrameCount < 32767);

    // select the appropriate resampler
    switch (mChannelCount) {
    case 1:
        resampleMono16(out, outFrameCount, provider);
        break;
    case 2:
        resampleStereo16(out, outFrameCount, provider);
        break;
    }
}

void AudioResamplerCubic::resampleStereo16(int32_t* out, size_t outFrameCount,
        AudioBufferProvider* provider) {

    int32_t vl = mVolume[0];
    int32_t vr = mVolume[1];

    size_t inputIndex = mInputIndex;
    uint32_t phaseFraction = mPhaseFraction;
    uint32_t phaseIncrement = mPhaseIncrement;
    size_t outputIndex = 0;
    size_t outputSampleCount = outFrameCount * 2;
    size_t inFrameCount = (outFrameCount*mInSampleRate)/mSampleRate;

    // fetch first buffer
    if (mBuffer.frameCount == 0) {
        mBuffer.frameCount = inFrameCount;
        provider->getNextBuffer(&mBuffer);
        if (mBuffer.raw == NULL)
            return;
        // LOGW("New buffer: offset=%p, frames=%dn", mBuffer.raw, mBuffer.frameCount);
    }
    int16_t *in = mBuffer.i16;

    while (outputIndex < outputSampleCount) {
        int32_t sample;
        int32_t x;

        // calculate output sample
        x = phaseFraction >> kPreInterpShift;
        out[outputIndex++] += vl * interp(&left, x);
        out[outputIndex++] += vr * interp(&right, x);
        // out[outputIndex++] += vr * in[inputIndex*2];

        // increment phase
        phaseFraction += phaseIncrement;
        uint32_t indexIncrement = (phaseFraction >> kNumPhaseBits);
        phaseFraction &= kPhaseMask;

        // time to fetch another sample
        while (indexIncrement--) {

            inputIndex++;
            if (inputIndex == mBuffer.frameCount) {
                inputIndex = 0;
                provider->releaseBuffer(&mBuffer);
                mBuffer.frameCount = inFrameCount;
                provider->getNextBuffer(&mBuffer);
                if (mBuffer.raw == NULL)
                    goto save_state;  // ugly, but efficient
                in = mBuffer.i16;
                // LOGW("New buffer: offset=%p, frames=%d\n", mBuffer.raw, mBuffer.frameCount);
            }

            // advance sample state
            advance(&left, in[inputIndex*2]);
            advance(&right, in[inputIndex*2+1]);
        }
    }

save_state:
    // LOGW("Done: index=%d, fraction=%u", inputIndex, phaseFraction);
    mInputIndex = inputIndex;
    mPhaseFraction = phaseFraction;
}

void AudioResamplerCubic::resampleMono16(int32_t* out, size_t outFrameCount,
        AudioBufferProvider* provider) {

    int32_t vl = mVolume[0];
    int32_t vr = mVolume[1];

    size_t inputIndex = mInputIndex;
    uint32_t phaseFraction = mPhaseFraction;
    uint32_t phaseIncrement = mPhaseIncrement;
    size_t outputIndex = 0;
    size_t outputSampleCount = outFrameCount * 2;
    size_t inFrameCount = (outFrameCount*mInSampleRate)/mSampleRate;

    // fetch first buffer
    if (mBuffer.frameCount == 0) {
        mBuffer.frameCount = inFrameCount;
        provider->getNextBuffer(&mBuffer);
        if (mBuffer.raw == NULL)
            return;
        // LOGW("New buffer: offset=%p, frames=%d\n", mBuffer.raw, mBuffer.frameCount);
    }
    int16_t *in = mBuffer.i16;

    while (outputIndex < outputSampleCount) {
        int32_t sample;
        int32_t x;

        // calculate output sample
        x = phaseFraction >> kPreInterpShift;
        sample = interp(&left, x);
        out[outputIndex++] += vl * sample;
        out[outputIndex++] += vr * sample;

        // increment phase
        phaseFraction += phaseIncrement;
        uint32_t indexIncrement = (phaseFraction >> kNumPhaseBits);
        phaseFraction &= kPhaseMask;

        // time to fetch another sample
        while (indexIncrement--) {

            inputIndex++;
            if (inputIndex == mBuffer.frameCount) {
                inputIndex = 0;
                provider->releaseBuffer(&mBuffer);
                mBuffer.frameCount = inFrameCount;
                provider->getNextBuffer(&mBuffer);
                if (mBuffer.raw == NULL)
                    goto save_state;  // ugly, but efficient
                // LOGW("New buffer: offset=%p, frames=%dn", mBuffer.raw, mBuffer.frameCount);
                in = mBuffer.i16;
            }

            // advance sample state
            advance(&left, in[inputIndex]);
        }
    }

save_state:
    // LOGW("Done: index=%d, fraction=%u", inputIndex, phaseFraction);
    mInputIndex = inputIndex;
    mPhaseFraction = phaseFraction;
}

// ----------------------------------------------------------------------------
}
; // namespace android

