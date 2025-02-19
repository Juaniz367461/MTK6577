/*
 * Copyright (C) 2008 The Android Open Source Project
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

#include <math.h>

//#define LOG_NDEBUG 0
#define LOG_TAG "A2dpAudioInterface"
#include <utils/Log.h>
#include <utils/String8.h>

#include "A2dpAudioInterface.h"
#include "audio/liba2dp.h"
#include <hardware_legacy/power.h>
#include "AudioYusuDef.h"

#ifdef __BTMTK__
extern "C"
{
#include "bt_a2dp_android.h"
}
#endif

#define MUTECOUNTER (15)

#ifdef ENABLE_LOG_A2DPINTERFACE
#define LOG_A2DPINTERFACE LOGD
#else
#define LOG_A2DPINTERFACE LOGV
#endif

namespace android_audio_legacy {

static const char *sA2dpWakeLock = "A2dpOutputStream";
#define MAX_WRITE_RETRIES  5

// ----------------------------------------------------------------------------

AudioHardwareInterface* A2dpAudioInterface::createA2dpInterface()
{
    LOGD("createA2dpInterface");
    AudioHardwareInterface* hw = 0;
    hw = AudioHardwareInterface::A2DPcreate();
    LOGD("new A2dpAudioInterface(hw: %p)", hw);
    hw = new A2dpAudioInterface(hw);
    return hw;
}

A2dpAudioInterface::A2dpAudioInterface(AudioHardwareInterface* hw) :
    mOutput(0), mHardwareInterface(hw), mBluetoothEnabled(true), mSuspended(false)
{
    LOGD("A2dpAudioInterface:: Constructor()");
}

A2dpAudioInterface::~A2dpAudioInterface()
{
    closeOutputStream((AudioStreamOut *)mOutput);
    delete mHardwareInterface;
}

status_t A2dpAudioInterface::initCheck()
{
    LOGD("A2dpAudioInterface::initCheck()");
    if (mHardwareInterface == 0)
    {
        LOGD("initCheck mHardwareInterface ==0");
        return NO_INIT;
    }
    return NO_ERROR;
}

AudioStreamOut* A2dpAudioInterface::openOutputStream(
    uint32_t devices, int *format, uint32_t *channels, uint32_t *sampleRate, status_t *status)
{
    if (!AudioSystem::isA2dpDevice((AudioSystem::audio_devices)devices))
    {
        LOG_A2DPINTERFACE("A2dpAudioInterface::openOutputStream() open HW device: %x", devices);
        return mHardwareInterface->openOutputStream(devices, format, channels, sampleRate, status);
    }
    LOGD("A2dpAudioInterface::openOutputStream() open HW device: %x", devices);


    status_t err = 0;

    // only one output stream allowed
    if (mOutput)
    {
        LOGE("A2dpAudioInterface::openOutputStream() return NULL");
        if (status)
            *status = -1;
        return NULL;
    }

    // create new output stream
    A2dpAudioStreamOut* out = new A2dpAudioStreamOut();
    if ((err = out->set(devices, format, channels, sampleRate)) == NO_ERROR) {
        mOutput = out;
        mOutput->setBluetoothEnabled(mBluetoothEnabled);
        mOutput->setSuspended(mSuspended);
    } else {
        delete out;
    }

    if (status)
        *status = err;
    return mOutput;
}

void A2dpAudioInterface::closeOutputStream(AudioStreamOut* out) {
    if (mOutput == 0 || mOutput != out) {
        mHardwareInterface->closeOutputStream(out);
    }
    else {
        delete mOutput;
        mOutput = 0;
    }
}


AudioStreamIn* A2dpAudioInterface::openInputStream(
        uint32_t devices, int *format, uint32_t *channels, uint32_t *sampleRate, status_t *status,
        AudioSystem::audio_in_acoustics acoustics)
{
    return NULL;
}

void A2dpAudioInterface::closeInputStream(AudioStreamIn* in)
{
    return ;
}

void A2dpAudioInterface::A2dpAudiosetMode(int mode)
{
    // from ringtone to nirmal
    if(mMode == AudioSystem::MODE_RINGTONE && mode == AudioSystem::MODE_NORMAL)
    {
        if(mOutput)
        {
            mOutput->setMuteModeNormal(MUTECOUNTER);
        }
    }
    mMode = mode;
}

status_t A2dpAudioInterface::setMode(int mode)
{
    LOGD("A2dpAudioInterface setMode");
    A2dpAudiosetMode(mode);
    return 0;
}

status_t A2dpAudioInterface::setMicMute(bool state)
{
    LOGD("A2dpAudioInterface setMicMute");
    return -ENOSYS;
}

status_t A2dpAudioInterface::getMicMute(bool* state)
{
    LOGD("A2dpAudioInterface getMicMute");
    return -ENOSYS;
}

status_t A2dpAudioInterface::setParameters(const String8& keyValuePairs)
{
    AudioParameter param = AudioParameter(keyValuePairs);
    String8 value;
    String8 key;
    status_t status = NO_ERROR;

    LOGV("setParameters() %s", keyValuePairs.string());

    key = "bluetooth_enabled";
    if (param.get(key, value) == NO_ERROR) {
        mBluetoothEnabled = (value == "true");
        if (mOutput) {
            mOutput->setBluetoothEnabled(mBluetoothEnabled);
        }
        param.remove(key);
    }
    key = String8("A2dpSuspended");
    if (param.get(key, value) == NO_ERROR) {
        mSuspended = (value == "true");
        if (mOutput) {
            mOutput->setSuspended(mSuspended);
        }
        param.remove(key);
    }

    key = String8("A2DPIncallSuspened");
    if (param.get(key, value) == NO_ERROR)
    {
        mIncallSuspened = (value == "true");
        LOGD("mIncallSuspened = %d",mIncallSuspened);;
        if (mOutput)
        {
            //mOutput->setSuspended(mIncallSuspened);
        }
        param.remove(key);
    }

    return status;
}

String8 A2dpAudioInterface::getParameters(const String8& keys)
{
    AudioParameter param = AudioParameter(keys);
    AudioParameter a2dpParam = AudioParameter();
    String8 value;
    String8 key;

    key = "bluetooth_enabled";
    if (param.get(key, value) == NO_ERROR) {
        value = mBluetoothEnabled ? "true" : "false";
        a2dpParam.add(key, value);
        param.remove(key);
    }
    key = "A2dpSuspended";
    if (param.get(key, value) == NO_ERROR) {
        value = mSuspended ? "true" : "false";
        a2dpParam.add(key, value);
        param.remove(key);
    }

    String8 keyValuePairs  = a2dpParam.toString();

    LOG_A2DPINTERFACE("getParameters() %s", keyValuePairs.string());
    return keyValuePairs;
}

size_t A2dpAudioInterface::getInputBufferSize(uint32_t sampleRate, int format, int channelCount)
{
    return -ENOSYS;
}

status_t A2dpAudioInterface::setVoiceVolume(float v)
{
    return -ENOSYS;
}

status_t A2dpAudioInterface::setMasterVolume(float v)
{
    return -ENOSYS;
}

status_t A2dpAudioInterface::dump(int fd, const Vector<String16>& args)
{
    return -ENOSYS;
}

// ----------------------------------------------------------------------------

A2dpAudioInterface::A2dpAudioStreamOut::A2dpAudioStreamOut() :
    mFd(-1), mStandby(true), mStartCount(0), mRetryCount(0), mData(NULL),
    // assume BT enabled to start, this is safe because its only the
    // enabled->disabled transition we are worried about
    mBluetoothEnabled(true), mDevice(0), mClosing(false), mSuspended(false)
{
    LOGD("A2dpAudioInterface::A2dpAudioStreamOut()");
#ifdef DUMP_A2DPSTREAMOUT
    pA2dpinputFile = NULL;
#endif
    WriteMuteCounter = 0;
    // use any address by default
    strcpy(mA2dpAddress, "00:00:00:00:00:00");
    init();
}

status_t A2dpAudioInterface::A2dpAudioStreamOut::set(
        uint32_t device, int *pFormat, uint32_t *pChannels, uint32_t *pRate)
{
    int lFormat = pFormat ? *pFormat : 0;
    uint32_t lChannels = pChannels ? *pChannels : 0;
    uint32_t lRate = pRate ? *pRate : 0;

    LOGD("A2dpAudioStreamOut::set %x, %d, %d, %d\n", device, lFormat, lChannels, lRate);

    // fix up defaults
    if (lFormat == 0) lFormat = format();
    if (lChannels == 0) lChannels = channels();
    if (lRate == 0) lRate = sampleRate();

    // check values
    if ((lFormat != format()) ||
            (lChannels != channels()) ||
            (lRate != sampleRate())){
        if (pFormat) *pFormat = format();
        if (pChannels) *pChannels = channels();
        if (pRate) *pRate = sampleRate();
        return BAD_VALUE;
    }

    if (pFormat) *pFormat = lFormat;
    if (pChannels) *pChannels = lChannels;
    if (pRate) *pRate = lRate;

    mDevice = device;
    mBufferDurationUs = ((bufferSize() * 1000 )/ frameSize() / sampleRate()) * 1000;
    return NO_ERROR;
}

A2dpAudioInterface::A2dpAudioStreamOut::~A2dpAudioStreamOut()
{
    LOGD("A2dpAudioStreamOut destructor");
    close();
    LOGD("A2dpAudioStreamOut destructor returning from close()");
}

ssize_t A2dpAudioInterface::A2dpAudioStreamOut::write(const void* buffer, size_t bytes)
{
    status_t status = -1;
    size_t remaining = bytes;
    {
        if (!mBluetoothEnabled || mClosing || mSuspended) {
            LOGE("A2dpAudioStreamOut::write(), but bluetooth disabled \
                mBluetoothEnabled %d, mClosing %d, mSuspended %d",
                mBluetoothEnabled, mClosing, mSuspended);
           usleep(((bytes * 1000 )/ frameSize() / sampleRate()) * 1000);
           return status;
        }

        Mutex::Autolock lock(mLock);
#ifdef DUMP_A2DPSTREAMOUT
    if(pA2dpinputFile == NULL)
    {
        LOGD("A2dpAudioStreamOut pinputFile == NULL");
        struct tm *timeinfo;
        time_t rawtime;
        time(&rawtime);
        timeinfo=localtime(&rawtime);
        char path[80];
        memset((void*)path,0,80);
        strftime (path,80,"/sdcard/%a_%b_%Y__%H_%M_%S_A2dpstream_out.pcm",timeinfo);
        LOGD( "fopen path is : %s", path);
        pA2dpinputFile = fopen(path,"w");
        if(pA2dpinputFile ==  NULL)
        {
            LOGE("create pA2dpinputFile error");
        }
    }
#endif
        if (mStandby) {
            acquire_wake_lock (PARTIAL_WAKE_LOCK, sA2dpWakeLock);
            mStandby = false;
            mLastWriteTime = systemTime();
        }
#ifdef DUMP_A2DPSTREAMOUT
    if(pA2dpinputFile != NULL)
    {
        LOGD("A2dpAudioStreamOut::write bytes = %d",bytes);
        int written = fwrite(buffer,1,bytes,pA2dpinputFile);
    }
#endif

        status = init();
        if (status < 0)
            goto Error;

    if(WriteMuteCounter)
    {
        WriteMuteCounter--;
        memset((void *)buffer,0,bytes);
    }
        int retries = MAX_WRITE_RETRIES;
        while (remaining > 0 && retries) {
            status = a2dp_write(mData, buffer, remaining);
            if (status < 0) {
                LOGE("a2dp_write failed err: %d\n", status);
                goto Error;
            }
            if (status == 0) {
                retries--;
            }
            remaining -= status;
            buffer = (char *)buffer + status;
        }

        // if A2DP sink runs abnormally fast, sleep a little so that audioflinger mixer thread
        // does no spin and starve other threads.
        // NOTE: It is likely that the A2DP headset is being disconnected
        nsecs_t now = systemTime();
        if ((uint32_t)ns2us(now - mLastWriteTime) < (mBufferDurationUs >> 2)) {
            LOGV("A2DP sink runs too fast");
            usleep(mBufferDurationUs - (uint32_t)ns2us(now - mLastWriteTime));
        }
        mLastWriteTime = now;
        return bytes;

    }
Error:

    standby();

    // Simulate audio output timing in case of error
    usleep(mBufferDurationUs);

    return status;
}

status_t A2dpAudioInterface::A2dpAudioStreamOut::init()
{
    if (!mData) {
        status_t status = a2dp_init(44100, 2, &mData);
        if (status < 0) {
            LOGE("a2dp_init failed err: %d\n", status);
            mData = NULL;
            return status;
        }
        a2dp_set_sink(mData, mA2dpAddress);
    }

    return 0;
}

status_t A2dpAudioInterface::A2dpAudioStreamOut::setMuteModeNormal(int Mutecount)
{
    WriteMuteCounter = Mutecount;
    return NO_ERROR;
}

status_t A2dpAudioInterface::A2dpAudioStreamOut::standby()
{
    Mutex::Autolock lock(mLock);
    return standby_l();
}

status_t A2dpAudioInterface::A2dpAudioStreamOut::standby_l()
{
    int result = NO_ERROR;

    if (!mStandby) {
        LOGV_IF(mClosing || !mBluetoothEnabled, "Standby skip stop: closing %d enabled %d",
                mClosing, mBluetoothEnabled);
        if (!mClosing && mBluetoothEnabled) {
            result = a2dp_stop(mData);
        }
        release_wake_lock(sA2dpWakeLock);
        mStandby = true;
    }

    return result;
}

status_t A2dpAudioInterface::A2dpAudioStreamOut::setParameters(const String8& keyValuePairs)
{
    AudioParameter param = AudioParameter(keyValuePairs);
    String8 value;
    String8 key = String8("a2dp_sink_address");
    status_t status = NO_ERROR;
    int device;
    LOGD("A2dpAudioStreamOut::setParameters() %s", keyValuePairs.string());

    if (param.get(key, value) == NO_ERROR) {
        if (value.length() != strlen("00:00:00:00:00:00")) {
            status = BAD_VALUE;
        } else {
            setAddress(value.string());
        }
        param.remove(key);
    }
    key = String8("closing");
    if (param.get(key, value) == NO_ERROR) {
        mClosing = (value == "true");
        if (mClosing) {
            standby();
        }
        param.remove(key);
    }
    key = AudioParameter::keyRouting;
    if (param.getInt(key, device) == NO_ERROR) {
        if (AudioSystem::isA2dpDevice((AudioSystem::audio_devices)device)) {
            mDevice = device;
            status = NO_ERROR;
        } else {
            status = BAD_VALUE;
        }
        param.remove(key);
    }

    if (param.size()) {
        status = BAD_VALUE;
    }
    return status;
}

String8 A2dpAudioInterface::A2dpAudioStreamOut::getParameters(const String8& keys)
{
    AudioParameter param = AudioParameter(keys);
    String8 value;
    String8 key = String8("a2dp_sink_address");

    if (param.get(key, value) == NO_ERROR) {
        value = mA2dpAddress;
        param.add(key, value);
    }
    key = AudioParameter::keyRouting;
    if (param.get(key, value) == NO_ERROR) {
        param.addInt(key, (int)mDevice);
    }

    LOGV("A2dpAudioStreamOut::getParameters() %s", param.toString().string());
    return param.toString();
}

status_t A2dpAudioInterface::A2dpAudioStreamOut::setAddress(const char* address)
{
    Mutex::Autolock lock(mLock);

    if (strlen(address) != strlen("00:00:00:00:00:00"))
        return -EINVAL;

    strcpy(mA2dpAddress, address);
    if (mData)
        a2dp_set_sink(mData, mA2dpAddress);

    return NO_ERROR;
}

status_t A2dpAudioInterface::A2dpAudioStreamOut::setBluetoothEnabled(bool enabled)
{
    LOGD("setBluetoothEnabled %d", enabled);

    Mutex::Autolock lock(mLock);

    mBluetoothEnabled = enabled;
    if (!enabled) {
        return close_l();
    }
    return NO_ERROR;
}

status_t A2dpAudioInterface::A2dpAudioStreamOut::setSuspended(bool onOff)
{
    LOGD("setSuspended %d", onOff);
    mSuspended = onOff;
    standby();
    return NO_ERROR;
}

status_t A2dpAudioInterface::A2dpAudioStreamOut::close()
{
    Mutex::Autolock lock(mLock);
    LOGD("A2dpAudioStreamOut::close() calling close_l()");
    return close_l();
}

status_t A2dpAudioInterface::A2dpAudioStreamOut::close_l()
{
    standby_l();
    if (mData) {
        LOGD("A2dpAudioStreamOut::close_l() calling a2dp_cleanup(mData)");
        a2dp_cleanup(mData);
        mData = NULL;
    }
    return NO_ERROR;
}

status_t A2dpAudioInterface::A2dpAudioStreamOut::dump(int fd, const Vector<String16>& args)
{
    return NO_ERROR;
}

status_t A2dpAudioInterface::A2dpAudioStreamOut::getRenderPosition(uint32_t *driverFrames)
{
    //TODO: enable when supported by driver
    return INVALID_OPERATION;
}


/////////////////////////////////////////////////////////////////////////
//    for PCMxWay Interface API ...   Stan
/////////////////////////////////////////////////////////////////////////
int A2dpAudioInterface::xWayPlay_Start(int sample_rate)
{
    return 0;
}
int A2dpAudioInterface::xWayPlay_Stop()
{
    return 0;
}
int A2dpAudioInterface::xWayPlay_Write(void *buffer, int size_bytes)
{
    return 0;
}
int A2dpAudioInterface::xWayPlay_GetFreeBufferCount()
{
    return 0;
}
int A2dpAudioInterface::xWayRec_Start(int sample_rate)
{
    return 0;
}
int A2dpAudioInterface::xWayRec_Stop()
{
    return 0;
}
int A2dpAudioInterface::xWayRec_Read(void *buffer, int size_bytes)
{
    return 0;
}


// add by chipeng to add EM parameter
status_t A2dpAudioInterface::SetEMParameter(void *ptr , int len)
{
    return 0;
}
status_t A2dpAudioInterface::GetEMParameter(void *ptr , int len)
{
    return 0;
}

status_t A2dpAudioInterface::SetAudioCommand(int par1 ,int par2)
{
    return 0;
}

status_t A2dpAudioInterface::GetAudioCommand(int par1)
{
    return 0;
}

status_t A2dpAudioInterface::SetAudioData(int par1,size_t len,void *ptr)
{
    return 0;
}

status_t A2dpAudioInterface::GetAudioData(int par1,size_t len,void *ptr)
{
    return 0;
}

// add by Tina to set ACF Preview parameter
status_t A2dpAudioInterface::SetACFPreviewParameter(void *ptr , int len)
{
    return 0;
}
status_t A2dpAudioInterface::SetHCFPreviewParameter(void *ptr , int len)
{
    return 0;
}

extern "C" AudioHardwareInterface* createA2DPAudioHardware()
{
    LOGD("createA2DPAudioHardware");
    return A2dpAudioInterface::createA2dpInterface();
}


}; // namespace android
