/*
 * Copyright (C) 2006-2007 The Android Open Source Project
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

#define LOG_TAG "AudioSystem"
//#define LOG_NDEBUG 0

#include <utils/Log.h>
#include <cutils/xlog.h>
#include <binder/IServiceManager.h>
#include <media/AudioSystem.h>
#include <media/IAudioPolicyService.h>
#include <math.h>

#include <system/audio.h>
#include <media/AudioParameter.h>
#ifdef MTK_AUDIO
#include <AudioIoctl.h>
#endif

// ----------------------------------------------------------------------------

namespace android {

// client singleton for AudioFlinger binder interface
Mutex AudioSystem::gLock;
sp<IAudioFlinger> AudioSystem::gAudioFlinger;
sp<AudioSystem::AudioFlingerClient> AudioSystem::gAudioFlingerClient;
audio_error_callback AudioSystem::gAudioErrorCallback = NULL;
// Cached values
DefaultKeyedVector<int, audio_io_handle_t> AudioSystem::gStreamOutputMap(0);
DefaultKeyedVector<audio_io_handle_t, AudioSystem::OutputDescriptor *> AudioSystem::gOutputs(0);

// Cached values for recording queries
uint32_t AudioSystem::gPrevInSamplingRate = 16000;
int AudioSystem::gPrevInFormat = AUDIO_FORMAT_PCM_16_BIT;
int AudioSystem::gPrevInChannelCount = 1;
size_t AudioSystem::gInBuffSize = 0;

#ifdef MTK_AUDIO
static const char* ForceToSpeaker = "AudioSetForceToSpeaker";
static String8 keyFM = String8(ForceToSpeaker);
static bool Parameter_to_manager_FM_Speaker(audio_io_handle_t ioHandle, const String8 keyValuePairs)
{
    //LOGD("Parameter_to_manager_FM_Speaker ioHandle = %d %s",ioHandle,keyValuePairs.string ());
    bool ret = false;
    int value =0;
    AudioParameter param = AudioParameter(keyValuePairs);

    // check
    if(ioHandle != 0){
        return ret;
    }
    if (param.getInt(keyFM, value) == NO_ERROR){
        return  true;
    }
    return ret;
}

static bool GetFmSpeakerStatus(const String8 keyValuePairs)
{
    //LOGD("GetFmSpeakerStatus %s",keyValuePairs.string ());
    bool ret = false;
    int value =0;
    AudioParameter param = AudioParameter(keyValuePairs);
    if (param.getInt(keyFM, value) == NO_ERROR){
        return  value;
    }
    return ret;
}
#endif

// establish binder interface to AudioFlinger service
const sp<IAudioFlinger>& AudioSystem::get_audio_flinger()
{
    Mutex::Autolock _l(gLock);
    if (gAudioFlinger.get() == 0) {
        sp<IServiceManager> sm = defaultServiceManager();
        sp<IBinder> binder;
        do {
            binder = sm->getService(String16("media.audio_flinger"));
            if (binder != 0)
                break;
            LOGW("AudioFlinger not published, waiting...");
            usleep(500000); // 0.5 s
        } while(true);
        if (gAudioFlingerClient == NULL) {
            gAudioFlingerClient = new AudioFlingerClient();
        } else {
            if (gAudioErrorCallback) {
                gAudioErrorCallback(NO_ERROR);
            }
         }
        binder->linkToDeath(gAudioFlingerClient);
        gAudioFlinger = interface_cast<IAudioFlinger>(binder);
        gAudioFlinger->registerClient(gAudioFlingerClient);
    }
    LOGE_IF(gAudioFlinger==0, "no AudioFlinger!?");

    return gAudioFlinger;
}

/////////////////////////////////////////////////////////////////////////
//    for PCMxWay Interface API ...   Stan
/////////////////////////////////////////////////////////////////////////
int AudioSystem::xWayPlay_Start(int sample_rate)
{
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af == 0) return PERMISSION_DENIED;
    return af->xWayPlay_Start(sample_rate);
}
int AudioSystem::xWayPlay_Stop()
{
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af == 0) return PERMISSION_DENIED;
    return af->xWayPlay_Stop();
}
int AudioSystem::xWayPlay_Write(void *buffer, int size_bytes)
{
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af == 0) return PERMISSION_DENIED;
    return af->xWayPlay_Write(buffer,size_bytes);
}
int AudioSystem::xWayPlay_GetFreeBufferCount()
{
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af == 0) return PERMISSION_DENIED;
    return af->xWayPlay_GetFreeBufferCount();
}
int AudioSystem::xWayRec_Start(int sample_rate)
{
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af == 0) return PERMISSION_DENIED;
    return af->xWayRec_Start(sample_rate);
}
int AudioSystem::xWayRec_Stop()
{
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af == 0) return PERMISSION_DENIED;
    return af->xWayRec_Stop();
}
int AudioSystem::xWayRec_Read(void *buffer, int size_bytes)
{
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af == 0) return PERMISSION_DENIED;
    return af->xWayRec_Read(buffer,size_bytes);
}



//add by chipeng , for EM mode
status_t AudioSystem::GetEMParameter(void *ptr,size_t len)
{
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af == 0) return PERMISSION_DENIED;
    return af->GetEMParameter(ptr,len);
}

status_t AudioSystem::SetEMParameter(void *ptr,size_t len)
{
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af == 0) return PERMISSION_DENIED;
    return af->SetEMParameter(ptr,len);
}

status_t AudioSystem::SetACFPreviewParameter(void *ptr,size_t len)
{
    SXLOGD("AudioSystem::SetACFPreviewParameter!! 01");
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af == 0)
    {
        SXLOGE("AudioSystem::SetACFPreviewParameter Error!! PERMISSION_DENIED");
        return PERMISSION_DENIED;
    }
    return af->SetACFPreviewParameter(ptr,len);
}

status_t AudioSystem::SetHCFPreviewParameter(void *ptr,size_t len)
{
    SXLOGD("AudioSystem::SetHCFPreviewParameter!! 01");
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af == 0)
    {
        SXLOGE("AudioSystem::SetHCFPreviewParameter Error!! PERMISSION_DENIED");
        return PERMISSION_DENIED;
    }
    return af->SetHCFPreviewParameter(ptr,len);
}

status_t AudioSystem::SetAudioCommand(int par1,int par2)
{
    SXLOGD("AudioSystem::SetAAudioCommand");
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af == 0)
    {
        SXLOGE("AudioSystem::SetAudioCommand Error!! PERMISSION_DENIED");
        return PERMISSION_DENIED;
    }
    return af->SetAudioCommand(par1,par2);
}

status_t AudioSystem::GetAudioCommand(int par1,int* par2)
{
    SXLOGD("AudioSystem::GetAudioCommand");
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af == 0)
    {
        SXLOGE("AudioSystem::GetAudioCommand Error!! PERMISSION_DENIED");
        return PERMISSION_DENIED;
    }
    *par2 =  af->GetAudioCommand(par1);
    return NO_ERROR;
}

status_t AudioSystem::SetAudioData(int par1,size_t byte_len,void *ptr)
{
    SXLOGD("SetAudioData");
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af == 0)
    {
        SXLOGE("AudioSystem::SetAAudioData Error!! PERMISSION_DENIED");
        return PERMISSION_DENIED;
    }
    return af->SetAudioData(par1,byte_len,ptr);
}

status_t AudioSystem::GetAudioData(int par1,size_t byte_len,void *ptr)
{
    SXLOGD("GetAudioData");
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af == 0)
    {
        SXLOGE("AudioSystem::GetAAudioData Error!! PERMISSION_DENIED");
        return PERMISSION_DENIED;
    }
    return af->GetAudioData(par1,byte_len,ptr);
}

status_t AudioSystem::muteMicrophone(bool state) {
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af == 0) return PERMISSION_DENIED;
    return af->setMicMute(state);
}

status_t AudioSystem::isMicrophoneMuted(bool* state) {
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af == 0) return PERMISSION_DENIED;
    *state = af->getMicMute();
    return NO_ERROR;
}

status_t AudioSystem::setMasterVolume(float value)
{
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af == 0) return PERMISSION_DENIED;
    af->setMasterVolume(value);
    return NO_ERROR;
}

status_t AudioSystem::setMasterMute(bool mute)
{
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af == 0) return PERMISSION_DENIED;
    af->setMasterMute(mute);
    return NO_ERROR;
}

status_t AudioSystem::getMasterVolume(float* volume)
{
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af == 0) return PERMISSION_DENIED;
    *volume = af->masterVolume();
    return NO_ERROR;
}

status_t AudioSystem::getMasterMute(bool* mute)
{
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af == 0) return PERMISSION_DENIED;
    *mute = af->masterMute();
    return NO_ERROR;
}

status_t AudioSystem::setStreamVolume(int stream, float value, int output)
{
    if (uint32_t(stream) >= AUDIO_STREAM_CNT) return BAD_VALUE;
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af == 0) return PERMISSION_DENIED;
    af->setStreamVolume(stream, value, output);
    return NO_ERROR;
}

status_t AudioSystem::setStreamMute(int stream, bool mute)
{
    if (uint32_t(stream) >= AUDIO_STREAM_CNT) return BAD_VALUE;
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af == 0) return PERMISSION_DENIED;
    af->setStreamMute(stream, mute);
    return NO_ERROR;
}

status_t AudioSystem::getStreamVolume(int stream, float* volume, int output)
{
    if (uint32_t(stream) >= AUDIO_STREAM_CNT) return BAD_VALUE;
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af == 0) return PERMISSION_DENIED;
    *volume = af->streamVolume(stream, output);
    return NO_ERROR;
}

status_t AudioSystem::getStreamMute(int stream, bool* mute)
{
    if (uint32_t(stream) >= AUDIO_STREAM_CNT) return BAD_VALUE;
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af == 0) return PERMISSION_DENIED;
    *mute = af->streamMute(stream);
    return NO_ERROR;
}

status_t AudioSystem::setMode(int mode)
{
    if (mode >= AUDIO_MODE_CNT) return BAD_VALUE;
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af == 0) return PERMISSION_DENIED;
    return af->setMode(mode);
}

status_t AudioSystem::setParameters(audio_io_handle_t ioHandle, const String8& keyValuePairs) {
    status_t ret =NO_ERROR;
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af == 0) return PERMISSION_DENIED;
    ret = af->setParameters(ioHandle, keyValuePairs);

    #ifdef MTK_AUDIO
    if(Parameter_to_manager_FM_Speaker(ioHandle,keyValuePairs)){
      const sp<IAudioPolicyService>& aps = AudioSystem::get_audio_policy_service();
        if (aps != 0){
            int FMspeaker = GetFmSpeakerStatus(keyValuePairs);
            aps->SetPolicyManagerParameters (POLICY_SET_FM_SPEAKER,FMspeaker,0,0);
        }
    }
    #endif

    return ret;
}

String8 AudioSystem::getParameters(audio_io_handle_t ioHandle, const String8& keys) {
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    String8 result = String8("");
    if (af == 0) return result;

    result = af->getParameters(ioHandle, keys);
    return result;
}

// convert volume steps to natural log scale

// change this value to change volume scaling
#ifdef VOLUME_NEWMAP
static const float dBPerStep = 0.25f;
static const float VOLUME_MAPPING_STEP = 256.0f;
#else
static const float dBPerStep = 0.5f;
static const float VOLUME_MAPPING_STEP = 100.0f;
#endif
// shouldn't need to touch these
static const float dBConvert = -dBPerStep * 2.302585093f / 20.0f;
static const float dBConvertInverse = 1.0f / dBConvert;

float AudioSystem::linearToLog(int volume)
{
    // float v = volume ? exp(float(100 - volume) * dBConvert) : 0;
    // LOGD("linearToLog(%d)=%f", volume, v);
    // return v;
    return volume ? exp(float(VOLUME_MAPPING_STEP - volume) * dBConvert) : 0;
}

int AudioSystem::logToLinear(float volume)
{
    // int v = volume ? 100 - int(dBConvertInverse * log(volume) + 0.5) : 0;
    // LOGD("logTolinear(%d)=%f", v, volume);
    // return v;
    return volume ? VOLUME_MAPPING_STEP - int(dBConvertInverse * log(volume) + 0.5) : 0;
}

status_t AudioSystem::getOutputSamplingRate(int* samplingRate, int streamType)
{
    OutputDescriptor *outputDesc;
    audio_io_handle_t output;

    if (streamType == AUDIO_STREAM_DEFAULT) {
        streamType = AUDIO_STREAM_MUSIC;
    }

    output = getOutput((audio_stream_type_t)streamType);
    if (output == 0) {
        return PERMISSION_DENIED;
    }

    gLock.lock();
    outputDesc = AudioSystem::gOutputs.valueFor(output);
    if (outputDesc == 0) {
        LOGV("getOutputSamplingRate() no output descriptor for output %d in gOutputs", output);
        gLock.unlock();
        const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
        if (af == 0) return PERMISSION_DENIED;
        *samplingRate = af->sampleRate(output);
    } else {
        LOGV("getOutputSamplingRate() reading from output desc");
        *samplingRate = outputDesc->samplingRate;
        gLock.unlock();
    }

    LOGV("getOutputSamplingRate() streamType %d, output %d, sampling rate %d", streamType, output, *samplingRate);

    return NO_ERROR;
}

status_t AudioSystem::getOutputFrameCount(int* frameCount, int streamType)
{
    OutputDescriptor *outputDesc;
    audio_io_handle_t output;

    if (streamType == AUDIO_STREAM_DEFAULT) {
        streamType = AUDIO_STREAM_MUSIC;
    }

    output = getOutput((audio_stream_type_t)streamType);
    if (output == 0) {
        return PERMISSION_DENIED;
    }

    gLock.lock();
    outputDesc = AudioSystem::gOutputs.valueFor(output);
    if (outputDesc == 0) {
        gLock.unlock();
        const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
        if (af == 0) return PERMISSION_DENIED;
        *frameCount = af->frameCount(output);
    } else {
        *frameCount = outputDesc->frameCount;
        gLock.unlock();
    }

    LOGV("getOutputFrameCount() streamType %d, output %d, frameCount %d", streamType, output, *frameCount);

    return NO_ERROR;
}

status_t AudioSystem::getOutputLatency(uint32_t* latency, int streamType)
{
    OutputDescriptor *outputDesc;
    audio_io_handle_t output;

    if (streamType == AUDIO_STREAM_DEFAULT) {
        streamType = AUDIO_STREAM_MUSIC;
    }

    output = getOutput((audio_stream_type_t)streamType);
    if (output == 0) {
        return PERMISSION_DENIED;
    }

    gLock.lock();
    outputDesc = AudioSystem::gOutputs.valueFor(output);
    if (outputDesc == 0) {
        gLock.unlock();
        const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
        if (af == 0) return PERMISSION_DENIED;
        *latency = af->latency(output);
    } else {
        *latency = outputDesc->latency;
        gLock.unlock();
    }

    LOGV("getOutputLatency() streamType %d, output %d, latency %d", streamType, output, *latency);

    return NO_ERROR;
}

status_t AudioSystem::getInputBufferSize(uint32_t sampleRate, int format, int channelCount,
    size_t* buffSize)
{
    // Do we have a stale gInBufferSize or are we requesting the input buffer size for new values
    if ((gInBuffSize == 0) || (sampleRate != gPrevInSamplingRate) || (format != gPrevInFormat)
        || (channelCount != gPrevInChannelCount)) {
        // save the request params
        gPrevInSamplingRate = sampleRate;
        gPrevInFormat = format;
        gPrevInChannelCount = channelCount;

        gInBuffSize = 0;
        const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
        if (af == 0) {
            return PERMISSION_DENIED;
        }
        gInBuffSize = af->getInputBufferSize(sampleRate, format, channelCount);
    }
    *buffSize = gInBuffSize;

    return NO_ERROR;
}

status_t AudioSystem::setVoiceVolume(float value)
{
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af == 0) return PERMISSION_DENIED;
    return af->setVoiceVolume(value);
}

status_t AudioSystem::getRenderPosition(uint32_t *halFrames, uint32_t *dspFrames, int stream)
{
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af == 0) return PERMISSION_DENIED;

    if (stream == AUDIO_STREAM_DEFAULT) {
        stream = AUDIO_STREAM_MUSIC;
    }

    return af->getRenderPosition(halFrames, dspFrames, getOutput((audio_stream_type_t)stream));
}

unsigned int AudioSystem::getInputFramesLost(audio_io_handle_t ioHandle) {
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    unsigned int result = 0;
    if (af == 0) return result;
    if (ioHandle == 0) return result;

    result = af->getInputFramesLost(ioHandle);
    return result;
}

int AudioSystem::newAudioSessionId() {
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af == 0) return 0;
    return af->newAudioSessionId();
}

void AudioSystem::acquireAudioSessionId(int audioSession) {
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af != 0) {
        af->acquireAudioSessionId(audioSession);
    }
}

void AudioSystem::releaseAudioSessionId(int audioSession) {
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af != 0) {
        af->releaseAudioSessionId(audioSession);
    }
}

// ---------------------------------------------------------------------------

void AudioSystem::AudioFlingerClient::binderDied(const wp<IBinder>& who) {
    Mutex::Autolock _l(AudioSystem::gLock);

    AudioSystem::gAudioFlinger.clear();
    // clear output handles and stream to output map caches
    AudioSystem::gStreamOutputMap.clear();
    AudioSystem::gOutputs.clear();

    if (gAudioErrorCallback) {
        gAudioErrorCallback(DEAD_OBJECT);
    }
    LOGW("AudioFlinger server died!");
}

void AudioSystem::AudioFlingerClient::ioConfigChanged(int event, int ioHandle, void *param2) {
    LOGV("ioConfigChanged() event %d", event);
    OutputDescriptor *desc;
    uint32_t stream;

    if (ioHandle == 0) return;

    Mutex::Autolock _l(AudioSystem::gLock);

    switch (event) {
    case STREAM_CONFIG_CHANGED:
        if (param2 == 0) break;
        stream = *(uint32_t *)param2;
        LOGV("ioConfigChanged() STREAM_CONFIG_CHANGED stream %d, output %d", stream, ioHandle);
        if (gStreamOutputMap.indexOfKey(stream) >= 0) {
            gStreamOutputMap.replaceValueFor(stream, ioHandle);
        }
        break;
    case OUTPUT_OPENED: {
        if (gOutputs.indexOfKey(ioHandle) >= 0) {
            LOGV("ioConfigChanged() opening already existing output! %d", ioHandle);
            break;
        }
        if (param2 == 0) break;
        desc = (OutputDescriptor *)param2;

        OutputDescriptor *outputDesc =  new OutputDescriptor(*desc);
        gOutputs.add(ioHandle, outputDesc);
        LOGV("ioConfigChanged() new output samplingRate %d, format %d channels %d frameCount %d latency %d",
                outputDesc->samplingRate, outputDesc->format, outputDesc->channels, outputDesc->frameCount, outputDesc->latency);
        } break;
    case OUTPUT_CLOSED: {
        if (gOutputs.indexOfKey(ioHandle) < 0) {
            LOGW("ioConfigChanged() closing unknow output! %d", ioHandle);
            break;
        }
        LOGV("ioConfigChanged() output %d closed", ioHandle);

        gOutputs.removeItem(ioHandle);
        for (int i = gStreamOutputMap.size() - 1; i >= 0 ; i--) {
            if (gStreamOutputMap.valueAt(i) == ioHandle) {
                gStreamOutputMap.removeItemsAt(i);
            }
        }
        } break;

    case OUTPUT_CONFIG_CHANGED: {
        int index = gOutputs.indexOfKey(ioHandle);
        if (index < 0) {
            LOGW("ioConfigChanged() modifying unknow output! %d", ioHandle);
            break;
        }
        if (param2 == 0) break;
        desc = (OutputDescriptor *)param2;

        LOGV("ioConfigChanged() new config for output %d samplingRate %d, format %d channels %d frameCount %d latency %d",
                ioHandle, desc->samplingRate, desc->format,
                desc->channels, desc->frameCount, desc->latency);
        OutputDescriptor *outputDesc = gOutputs.valueAt(index);
        delete outputDesc;
        outputDesc =  new OutputDescriptor(*desc);
        gOutputs.replaceValueFor(ioHandle, outputDesc);
    } break;
    case INPUT_OPENED:
    case INPUT_CLOSED:
    case INPUT_CONFIG_CHANGED:
        break;

    }
}

void AudioSystem::setErrorCallback(audio_error_callback cb) {
    Mutex::Autolock _l(gLock);
    gAudioErrorCallback = cb;
}

bool AudioSystem::routedToA2dpOutput(int streamType) {
    switch(streamType) {
    case AUDIO_STREAM_MUSIC:
    case AUDIO_STREAM_VOICE_CALL:
    case AUDIO_STREAM_BLUETOOTH_SCO:
    case AUDIO_STREAM_SYSTEM:
        return true;
    default:
        return false;
    }
}


// client singleton for AudioPolicyService binder interface
sp<IAudioPolicyService> AudioSystem::gAudioPolicyService;
sp<AudioSystem::AudioPolicyServiceClient> AudioSystem::gAudioPolicyServiceClient;


// establish binder interface to AudioFlinger service
const sp<IAudioPolicyService>& AudioSystem::get_audio_policy_service()
{
    gLock.lock();
    if (gAudioPolicyService.get() == 0) {
        sp<IServiceManager> sm = defaultServiceManager();
        sp<IBinder> binder;
        do {
            binder = sm->getService(String16("media.audio_policy"));
            if (binder != 0)
                break;
            LOGW("AudioPolicyService not published, waiting...");
            usleep(500000); // 0.5 s
        } while(true);
        if (gAudioPolicyServiceClient == NULL) {
            gAudioPolicyServiceClient = new AudioPolicyServiceClient();
        }
        binder->linkToDeath(gAudioPolicyServiceClient);
        gAudioPolicyService = interface_cast<IAudioPolicyService>(binder);
        gLock.unlock();
    } else {
        gLock.unlock();
    }
    return gAudioPolicyService;
}

status_t AudioSystem::setDeviceConnectionState(audio_devices_t device,
                                               audio_policy_dev_state_t state,
                                               const char *device_address)
{
    const sp<IAudioPolicyService>& aps = AudioSystem::get_audio_policy_service();
    const char *address = "";

    if (aps == 0) return PERMISSION_DENIED;

    if (device_address != NULL) {
        address = device_address;
    }

    return aps->setDeviceConnectionState(device, state, address);
}

audio_policy_dev_state_t AudioSystem::getDeviceConnectionState(audio_devices_t device,
                                                  const char *device_address)
{
    const sp<IAudioPolicyService>& aps = AudioSystem::get_audio_policy_service();
    if (aps == 0) return AUDIO_POLICY_DEVICE_STATE_UNAVAILABLE;

    return aps->getDeviceConnectionState(device, device_address);
}

status_t AudioSystem::setPhoneState(int state)
{
    const sp<IAudioPolicyService>& aps = AudioSystem::get_audio_policy_service();
    if (aps == 0) return PERMISSION_DENIED;

    return aps->setPhoneState(state);
}

status_t AudioSystem::setRingerMode(uint32_t mode, uint32_t mask)
{
    const sp<IAudioPolicyService>& aps = AudioSystem::get_audio_policy_service();
    if (aps == 0) return PERMISSION_DENIED;
    return aps->setRingerMode(mode, mask);
}

status_t AudioSystem::setForceUse(audio_policy_force_use_t usage, audio_policy_forced_cfg_t config)
{
    const sp<IAudioPolicyService>& aps = AudioSystem::get_audio_policy_service();
    if (aps == 0) return PERMISSION_DENIED;
    return aps->setForceUse(usage, config);
}

audio_policy_forced_cfg_t AudioSystem::getForceUse(audio_policy_force_use_t usage)
{
    const sp<IAudioPolicyService>& aps = AudioSystem::get_audio_policy_service();
    if (aps == 0) return AUDIO_POLICY_FORCE_NONE;
    return aps->getForceUse(usage);
}


audio_io_handle_t AudioSystem::getOutput(audio_stream_type_t stream,
                                    uint32_t samplingRate,
                                    uint32_t format,
                                    uint32_t channels,
                                    audio_policy_output_flags_t flags)
{
    audio_io_handle_t output = 0;
    // Do not use stream to output map cache if the direct output
    // flag is set or if we are likely to use a direct output
    // (e.g voice call stream @ 8kHz could use BT SCO device and be routed to
    // a direct output on some platforms).
    // TODO: the output cache and stream to output mapping implementation needs to
    // be reworked for proper operation with direct outputs. This code is too specific
    // to the first use case we want to cover (Voice Recognition and Voice Dialer over
    // Bluetooth SCO
    if ((flags & AUDIO_POLICY_OUTPUT_FLAG_DIRECT) == 0 &&
        ((stream != AUDIO_STREAM_VOICE_CALL && stream != AUDIO_STREAM_BLUETOOTH_SCO) ||
         channels != AUDIO_CHANNEL_OUT_MONO ||
         (samplingRate != 8000 && samplingRate != 16000))) {
        Mutex::Autolock _l(gLock);
        output = AudioSystem::gStreamOutputMap.valueFor(stream);
        LOGV_IF((output != 0), "getOutput() read %d from cache for stream %d", output, stream);
    }
    if (output == 0) {
        const sp<IAudioPolicyService>& aps = AudioSystem::get_audio_policy_service();
        if (aps == 0) return 0;
        output = aps->getOutput(stream, samplingRate, format, channels, flags);
        if ((flags & AUDIO_POLICY_OUTPUT_FLAG_DIRECT) == 0) {
            Mutex::Autolock _l(gLock);
            AudioSystem::gStreamOutputMap.add(stream, output);
        }
    }
    return output;
}

status_t AudioSystem::startOutput(audio_io_handle_t output,
                                  audio_stream_type_t stream,
                                  int session)
{
    const sp<IAudioPolicyService>& aps = AudioSystem::get_audio_policy_service();
    if (aps == 0) return PERMISSION_DENIED;
    return aps->startOutput(output, stream, session);
}

status_t AudioSystem::stopOutput(audio_io_handle_t output,
                                 audio_stream_type_t stream,
                                 int session)
{
    const sp<IAudioPolicyService>& aps = AudioSystem::get_audio_policy_service();
    if (aps == 0) return PERMISSION_DENIED;
    return aps->stopOutput(output, stream, session);
}

void AudioSystem::releaseOutput(audio_io_handle_t output)
{
    const sp<IAudioPolicyService>& aps = AudioSystem::get_audio_policy_service();
    if (aps == 0) return;
    aps->releaseOutput(output);
}

audio_io_handle_t AudioSystem::getInput(int inputSource,
                                    uint32_t samplingRate,
                                    uint32_t format,
                                    uint32_t channels,
                                    audio_in_acoustics_t acoustics,
                                    int sessionId)
{
    const sp<IAudioPolicyService>& aps = AudioSystem::get_audio_policy_service();
    if (aps == 0) return 0;
    return aps->getInput(inputSource, samplingRate, format, channels, acoustics, sessionId);
}

status_t AudioSystem::startInput(audio_io_handle_t input)
{
    const sp<IAudioPolicyService>& aps = AudioSystem::get_audio_policy_service();
    if (aps == 0) return PERMISSION_DENIED;
    return aps->startInput(input);
}

status_t AudioSystem::stopInput(audio_io_handle_t input)
{
    const sp<IAudioPolicyService>& aps = AudioSystem::get_audio_policy_service();
    if (aps == 0) return PERMISSION_DENIED;
    return aps->stopInput(input);
}

void AudioSystem::releaseInput(audio_io_handle_t input)
{
    const sp<IAudioPolicyService>& aps = AudioSystem::get_audio_policy_service();
    if (aps == 0) return;
    aps->releaseInput(input);
}

status_t AudioSystem::initStreamVolume(audio_stream_type_t stream,
                                    int indexMin,
                                    int indexMax)
{
    const sp<IAudioPolicyService>& aps = AudioSystem::get_audio_policy_service();
    if (aps == 0) return PERMISSION_DENIED;
    return aps->initStreamVolume(stream, indexMin, indexMax);
}

status_t AudioSystem::setStreamVolumeIndex(audio_stream_type_t stream, int index)
{
    const sp<IAudioPolicyService>& aps = AudioSystem::get_audio_policy_service();
    if (aps == 0) return PERMISSION_DENIED;
    return aps->setStreamVolumeIndex(stream, index);
}

status_t AudioSystem::getStreamVolumeIndex(audio_stream_type_t stream, int *index)
{
    const sp<IAudioPolicyService>& aps = AudioSystem::get_audio_policy_service();
    if (aps == 0) return PERMISSION_DENIED;
    return aps->getStreamVolumeIndex(stream, index);
}

uint32_t AudioSystem::getStrategyForStream(audio_stream_type_t stream)
{
    const sp<IAudioPolicyService>& aps = AudioSystem::get_audio_policy_service();
    if (aps == 0) return 0;
    return aps->getStrategyForStream(stream);
}

uint32_t AudioSystem::getDevicesForStream(audio_stream_type_t stream)
{
    const sp<IAudioPolicyService>& aps = AudioSystem::get_audio_policy_service();
    if (aps == 0) return 0;
    return aps->getDevicesForStream(stream);
}

audio_io_handle_t AudioSystem::getOutputForEffect(effect_descriptor_t *desc)
{
    const sp<IAudioPolicyService>& aps = AudioSystem::get_audio_policy_service();
    if (aps == 0) return PERMISSION_DENIED;
    return aps->getOutputForEffect(desc);
}

status_t AudioSystem::registerEffect(effect_descriptor_t *desc,
                                audio_io_handle_t io,
                                uint32_t strategy,
                                int session,
                                int id)
{
    const sp<IAudioPolicyService>& aps = AudioSystem::get_audio_policy_service();
    if (aps == 0) return PERMISSION_DENIED;
    return aps->registerEffect(desc, io, strategy, session, id);
}

status_t AudioSystem::unregisterEffect(int id)
{
    const sp<IAudioPolicyService>& aps = AudioSystem::get_audio_policy_service();
    if (aps == 0) return PERMISSION_DENIED;
    return aps->unregisterEffect(id);
}

status_t AudioSystem::setEffectEnabled(int id, bool enabled)
{
    const sp<IAudioPolicyService>& aps = AudioSystem::get_audio_policy_service();
    if (aps == 0) return PERMISSION_DENIED;
    return aps->setEffectEnabled(id, enabled);
}

status_t AudioSystem::isStreamActive(int stream, bool* state, uint32_t inPastMs)
{
    const sp<IAudioPolicyService>& aps = AudioSystem::get_audio_policy_service();
    if (aps == 0) return PERMISSION_DENIED;
    if (state == NULL) return BAD_VALUE;
    *state = aps->isStreamActive(stream, inPastMs);
    return NO_ERROR;
}


void AudioSystem::clearAudioConfigCache()
{
    Mutex::Autolock _l(gLock);
    LOGV("clearAudioConfigCache()");
    gStreamOutputMap.clear();
    gOutputs.clear();
}

// ---------------------------------------------------------------------------

void AudioSystem::AudioPolicyServiceClient::binderDied(const wp<IBinder>& who) {
    Mutex::Autolock _l(AudioSystem::gLock);
    AudioSystem::gAudioPolicyService.clear();

    LOGW("AudioPolicyService server died!");
}

}; // namespace android

