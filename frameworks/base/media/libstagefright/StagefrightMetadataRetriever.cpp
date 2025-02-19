/*
 * Copyright (C) 2009 The Android Open Source Project
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

//#define LOG_NDEBUG 0
#define LOG_TAG "StagefrightMetadataRetriever"
#include <utils/Log.h>

#include "include/StagefrightMetadataRetriever.h"

#include <media/stagefright/ColorConverter.h>
#include <media/stagefright/DataSource.h>
#include <media/stagefright/FileSource.h>
#include <media/stagefright/MediaDebug.h>
#include <media/stagefright/MediaExtractor.h>
#include <media/stagefright/MetaData.h>
#include <media/stagefright/OMXCodec.h>
#include <media/stagefright/MediaDefs.h>

#include <drm/DrmMtkDef.h>

namespace android {

StagefrightMetadataRetriever::StagefrightMetadataRetriever()
    : mParsedMetaData(false),
      mAlbumArt(NULL) {
    LOGI("StagefrightMetadataRetriever()");

    DataSource::RegisterDefaultSniffers();
    CHECK_EQ(mClient.connect(), OK);
}

StagefrightMetadataRetriever::~StagefrightMetadataRetriever() {
    LOGI("~StagefrightMetadataRetriever()");

    delete mAlbumArt;
    mAlbumArt = NULL;

    mClient.disconnect();
}

status_t StagefrightMetadataRetriever::setDataSource(
        const char *uri, const KeyedVector<String8, String8> *headers) {
    LOGI("setDataSource(%s)", uri);

    mParsedMetaData = false;
    mMetaData.clear();
    delete mAlbumArt;
    mAlbumArt = NULL;

    mSource = DataSource::CreateFromURI(uri, headers);

    if (mSource == NULL) {
#ifndef ANDROID_DEFAULT_CODE
				LOGW("mSource == NULL");
#endif
        return UNKNOWN_ERROR;
    }

#ifndef ANDROID_DEFAULT_CODE
    const char * sniffedMIME = NULL;
    if ((!strncasecmp("/system/media/audio/", uri, 20)) && (strcasestr(uri,".ogg") != NULL))
    {
         sniffedMIME = MEDIA_MIMETYPE_CONTAINER_OGG;
    }
    mExtractor = MediaExtractor::Create(mSource, sniffedMIME);
#else
    mExtractor = MediaExtractor::Create(mSource);
#endif

#ifndef ANDROID_DEFAULT_CODE
#ifdef MTK_DRM_APP
    // OMA DRM v1 implementation:
    // for .dcf file without valid rights, the mExtractor will be NULL so return OK here
    // mSource is created by "CreateFromURI"
    if ((mSource->flags() & OMADrmFlag) != 0) {
        LOGD("setDataSource() : it is a OMA DRM v1 .dcf file. return OK");
        return OK;
    }
#endif
#endif

    if (mExtractor == NULL) {
        mSource.clear();
#ifndef ANDROID_DEFAULT_CODE
				LOGW("no related extractor");
#endif
        return UNKNOWN_ERROR;
    }

#ifndef ANDROID_DEFAULT_CODE
    if (mExtractor->countTracks() == 0) {
    	LOGW("Track number is 0");
    	return UNKNOWN_ERROR;
    }
#endif

    return OK;
}

// Warning caller retains ownership of the filedescriptor! Dup it if necessary.
status_t StagefrightMetadataRetriever::setDataSource(
        int fd, int64_t offset, int64_t length) {
    fd = dup(fd);

    LOGI("setDataSource(%d, %lld, %lld)", fd, offset, length);

    mParsedMetaData = false;
    mMetaData.clear();
    delete mAlbumArt;
    mAlbumArt = NULL;

    mSource = new FileSource(fd, offset, length);

    status_t err;
    if ((err = mSource->initCheck()) != OK) {
        mSource.clear();
#ifndef ANDROID_DEFAULT_CODE
				LOGW("mSource initCheck fail err=%d",err);
#endif
        return err;
    }

    mExtractor = MediaExtractor::Create(mSource);

#ifndef ANDROID_DEFAULT_CODE
#ifdef MTK_DRM_APP
    // OMA DRM v1 implementation:
    // for .dcf file without valid rights, the mExtractor will be NULL so return OK here
    // mSource is created by "CreateFromURI"
    if ((mSource->flags() & OMADrmFlag) != 0) {
        LOGD("setDataSource() : it is a OMA DRM v1 .dcf file. return OK");
        return OK;
    }
#endif
#endif

    if (mExtractor == NULL) {
        mSource.clear();
#ifndef ANDROID_DEFAULT_CODE
				LOGW("no related extractor");
#endif
        return UNKNOWN_ERROR;
    }

#ifndef ANDROID_DEFAULT_CODE
    if (mExtractor->countTracks() == 0) {
    	LOGW("Track number is 0");
    	return UNKNOWN_ERROR;
    }
#endif

    return OK;
}

static VideoFrame *extractVideoFrameWithCodecFlags(
        OMXClient *client,
        const sp<MetaData> &trackMeta,
        const sp<MediaSource> &source,
        uint32_t flags,
        int64_t frameTimeUs,
        int seekMode) {
    sp<MediaSource> decoder =
        OMXCodec::Create(
                client->interface(), source->getFormat(), false, source,
                NULL, flags | OMXCodec::kClientNeedsFramebuffer);

    if (decoder.get() == NULL) {
        LOGW("unable to instantiate video decoder.");

        return NULL;
    }

    status_t err = decoder->start();
    if (err != OK) {
        LOGW("OMXCodec::start returned error %d (0x%08x)\n", err, err);
        return NULL;
    }

    // Read one output buffer, ignore format change notifications
    // and spurious empty buffers.

    MediaSource::ReadOptions options;
    if (seekMode < MediaSource::ReadOptions::SEEK_PREVIOUS_SYNC ||
        seekMode > MediaSource::ReadOptions::SEEK_CLOSEST) {

        LOGE("Unknown seek mode: %d", seekMode);
        return NULL;
    }

    MediaSource::ReadOptions::SeekMode mode =
            static_cast<MediaSource::ReadOptions::SeekMode>(seekMode);

    int64_t thumbNailTime;
    if (frameTimeUs < 0) {
        if (!trackMeta->findInt64(kKeyThumbnailTime, &thumbNailTime)
                || thumbNailTime < 0) {
            thumbNailTime = 0;
        }
        options.setSeekTo(thumbNailTime, mode);
    } else {
        thumbNailTime = -1;
        options.setSeekTo(frameTimeUs, mode);
    }

    MediaBuffer *buffer = NULL;
    do {
        if (buffer != NULL) {
            buffer->release();
            buffer = NULL;
        }
        err = decoder->read(&buffer, &options);
        options.clearSeekTo();
    } while (err == INFO_FORMAT_CHANGED
             || (buffer != NULL && buffer->range_length() == 0));

    if (err != OK) {
        CHECK_EQ(buffer, NULL);

        LOGE("decoding frame failed.");
        decoder->stop();

        return NULL;
    }

    LOGI("successfully decoded video frame.");

    int32_t unreadable;
    if (buffer->meta_data()->findInt32(kKeyIsUnreadable, &unreadable)
            && unreadable != 0) {
        LOGE("video frame is unreadable, decoder does not give us access "
             "to the video data.");

        buffer->release();
        buffer = NULL;

        decoder->stop();

        return NULL;
    }

    int64_t timeUs;
    CHECK(buffer->meta_data()->findInt64(kKeyTime, &timeUs));
    if (thumbNailTime >= 0) {
        if (timeUs != thumbNailTime) {
            const char *mime;
            CHECK(trackMeta->findCString(kKeyMIMEType, &mime));

            LOGW("thumbNailTime = %lld us, timeUs = %lld us, mime = %s",
                 thumbNailTime, timeUs, mime);
        }
    }

    sp<MetaData> meta = decoder->getFormat();

    int32_t width, height;
    CHECK(meta->findInt32(kKeyWidth, &width));
    CHECK(meta->findInt32(kKeyHeight, &height));
#ifndef ANDROID_DEFAULT_CODE
    {
        sp<MetaData> inputFormat = source->getFormat();
        const char *mime;
        if (inputFormat->findCString(kKeyMIMEType, &mime)) {
            LOGD("width=%d, height=%d", width, height);
            if (strcasecmp(mime, MEDIA_MIMETYPE_VIDEO_VPX)) {//VP8 is software decode, no need to 16 byte align
                width = (width+15) & (~(0xf));
                height = (height+15) & (~(0xf));
            } else { // VP8
#if defined(MT6575)  || defined(MT6577)
                width = (width+15) & (~(0xf));
                height = (height+15) & (~(0xf));
#endif
            }

				if (!strcasecmp(mime, MEDIA_MIMETYPE_VIDEO_REAL_VIDEO)) { //get the actual resolution for RV frame
					int32_t actual_width, actual_height;
		        		bool success = meta->findInt32(kKeyRvActualWidth, &actual_width);
		        		success = success && meta->findInt32(kKeyRvActualHeight, &actual_height);
		        		if (success) {
						width = actual_width;
						height = actual_height;
		        		}
				}	

            LOGD("Video frame width=%d, height=%d, mime=%s", width, height, mime);
        }
    }
#endif

    int32_t crop_left, crop_top, crop_right, crop_bottom;
    if (!meta->findRect(
                kKeyCropRect,
                &crop_left, &crop_top, &crop_right, &crop_bottom)) {
        crop_left = crop_top = 0;
        crop_right = width - 1;
        crop_bottom = height - 1;
    }

    int32_t rotationAngle;
    if (!trackMeta->findInt32(kKeyRotation, &rotationAngle)) {
        rotationAngle = 0;  // By default, no rotation
    }

    VideoFrame *frame = new VideoFrame;
    frame->mWidth = crop_right - crop_left + 1;
    frame->mHeight = crop_bottom - crop_top + 1;
    frame->mDisplayWidth = frame->mWidth;
    frame->mDisplayHeight = frame->mHeight;

#ifdef MTK_HIGH_QUALITY_THUMBNAIL
    frame->mSize = frame->mWidth * frame->mHeight * 4;
#else
    frame->mSize = frame->mWidth * frame->mHeight * 2;
#endif

    frame->mData = new uint8_t[frame->mSize];
    frame->mRotationAngle = rotationAngle;

    int32_t displayWidth, displayHeight;
    if (meta->findInt32(kKeyDisplayWidth, &displayWidth)) {
        frame->mDisplayWidth = displayWidth;
    }
    if (meta->findInt32(kKeyDisplayHeight, &displayHeight)) {
        frame->mDisplayHeight = displayHeight;
    }

    int32_t srcFormat;
    CHECK(meta->findInt32(kKeyColorFormat, &srcFormat));

#ifdef MTK_HIGH_QUALITY_THUMBNAIL
    ColorConverter converter((OMX_COLOR_FORMATTYPE)srcFormat, OMX_COLOR_Format32bitARGB8888);
#else
    ColorConverter converter((OMX_COLOR_FORMATTYPE)srcFormat, OMX_COLOR_Format16bitRGB565);
#endif

    CHECK(converter.isValid());

    err = converter.convert(
            (const uint8_t *)buffer->data() + buffer->range_offset(),
            width, height,
            crop_left, crop_top, crop_right, crop_bottom,
            frame->mData,
            frame->mWidth,
            frame->mHeight,
            0, 0, frame->mWidth - 1, frame->mHeight - 1);

#if 0 // Morris Yang dump bitmap buffer
    if (frame->mWidth == 176 && frame->mHeight == 144) {
            char buf[255];
            sprintf (buf, "/sdcard/%d_%d.bin", frame->mWidth, frame->mHeight);
            FILE *fp = fopen(buf, "ab");
            if (fp) {
                fwrite((void *)frame->mData, 1, frame->mSize, fp);
                fclose(fp);
            }
    }
#endif

    buffer->release();
    buffer = NULL;

    decoder->stop();

    if (err != OK) {
        LOGE("Colorconverter failed to convert frame.");

        delete frame;
        frame = NULL;
    }

    return frame;
}

VideoFrame *StagefrightMetadataRetriever::getFrameAtTime(
        int64_t timeUs, int option) {

    LOGI("getFrameAtTime: %lld us option: %d", timeUs, option);

    if (mExtractor.get() == NULL) {
        LOGW("no extractor.");
        return NULL;
    }

    sp<MetaData> fileMeta = mExtractor->getMetaData();

    if (fileMeta == NULL) {
        LOGW("extractor doesn't publish metadata, failed to initialize?");
        return NULL;
    }

    int32_t drm = 0;
    if (fileMeta->findInt32(kKeyIsDRM, &drm) && drm != 0) {
        LOGE("frame grab not allowed.");
        return NULL;
    }

    size_t n = mExtractor->countTracks();
    size_t i;
    for (i = 0; i < n; ++i) {
        sp<MetaData> meta = mExtractor->getTrackMetaData(i);
#ifndef ANDROID_DEFAULT_CODE
        if(meta.get() == NULL) return NULL;
#endif // #ifndef ANDROID_DEFAULT_CODE
        const char *mime;
        CHECK(meta->findCString(kKeyMIMEType, &mime));

        if (!strncasecmp(mime, "video/", 6)) {
            break;
        }
    }

    if (i == n) {
        LOGW("no video track found.");
        return NULL;
    }

    sp<MetaData> trackMeta = mExtractor->getTrackMetaData(
            i, MediaExtractor::kIncludeExtensiveMetaData);

    sp<MediaSource> source = mExtractor->getTrack(i);

    if (source.get() == NULL) {
        LOGW("unable to instantiate video track.");
        return NULL;
    }

    const void *data;
    uint32_t type;
    size_t dataSize;
    if (fileMeta->findData(kKeyAlbumArt, &type, &data, &dataSize)
            && mAlbumArt == NULL) {
        mAlbumArt = new MediaAlbumArt;
        mAlbumArt->mSize = dataSize;
        mAlbumArt->mData = new uint8_t[dataSize];
        memcpy(mAlbumArt->mData, data, dataSize);
    }

    VideoFrame *frame =
        extractVideoFrameWithCodecFlags(
                &mClient, trackMeta, source, OMXCodec::kPreferSoftwareCodecs,
                timeUs, option);

    if (frame == NULL) {
        LOGE("Software decoder failed to extract thumbnail, "
             "trying hardware decoder.");

        frame = extractVideoFrameWithCodecFlags(&mClient, trackMeta, source, 0,
                        timeUs, option);
    }

    return frame;
}

MediaAlbumArt *StagefrightMetadataRetriever::extractAlbumArt() {
    LOGI("extractAlbumArt (extractor: %s)", mExtractor.get() != NULL ? "YES" : "NO");

    if (mExtractor == NULL) {
        return NULL;
    }

    if (!mParsedMetaData) {
        parseMetaData();

        mParsedMetaData = true;
    }

    if (mAlbumArt) {
        return new MediaAlbumArt(*mAlbumArt);
    }

    return NULL;
}

const char *StagefrightMetadataRetriever::extractMetadata(int keyCode) {
    if (mExtractor == NULL) {
        return NULL;
    }

    if (!mParsedMetaData) {
        parseMetaData();

        mParsedMetaData = true;
    }

    ssize_t index = mMetaData.indexOfKey(keyCode);

    if (index < 0) {
        return NULL;
    }

#ifndef ANDROID_DEFAULT_CODE
    // const char, return strdup will cause a memory leak
    return mMetaData.valueAt(index).string();
#else
    return strdup(mMetaData.valueAt(index).string());
#endif
}

void StagefrightMetadataRetriever::parseMetaData() {
#ifndef ANDROID_DEFAULT_CODE
#ifdef MTK_DRM_APP
    // OMA DRM v1 implementation: NULL extractor means .dcf without valid rights
    if (mExtractor.get() == NULL) {
        LOGD("NULL extractor because of invalid rights for OMA DRM v1 .dcf file");
        LOGD("cannot parse Meta data");
        return;
    }
#endif
#endif

    sp<MetaData> meta = mExtractor->getMetaData();

    if (meta == NULL) {
        LOGW("extractor doesn't publish metadata, failed to initialize?");
        return;
    }

    struct Map {
        int from;
        int to;
    };
    static const Map kMap[] = {
        { kKeyMIMEType, METADATA_KEY_MIMETYPE },
        { kKeyCDTrackNumber, METADATA_KEY_CD_TRACK_NUMBER },
        { kKeyDiscNumber, METADATA_KEY_DISC_NUMBER },
        { kKeyAlbum, METADATA_KEY_ALBUM },
        { kKeyArtist, METADATA_KEY_ARTIST },
        { kKeyAlbumArtist, METADATA_KEY_ALBUMARTIST },
        { kKeyAuthor, METADATA_KEY_AUTHOR },
        { kKeyComposer, METADATA_KEY_COMPOSER },
        { kKeyDate, METADATA_KEY_DATE },
        { kKeyGenre, METADATA_KEY_GENRE },
        { kKeyTitle, METADATA_KEY_TITLE },
        { kKeyYear, METADATA_KEY_YEAR },
        { kKeyWriter, METADATA_KEY_WRITER },
        { kKeyCompilation, METADATA_KEY_COMPILATION },
        { kKeyLocation, METADATA_KEY_LOCATION },
    };
    static const size_t kNumMapEntries = sizeof(kMap) / sizeof(kMap[0]);

    for (size_t i = 0; i < kNumMapEntries; ++i) {
        const char *value;
        if (meta->findCString(kMap[i].from, &value)) {
            mMetaData.add(kMap[i].to, String8(value));
        }
    }

    const void *data;
    uint32_t type;
    size_t dataSize;
    if (meta->findData(kKeyAlbumArt, &type, &data, &dataSize)
            && mAlbumArt == NULL) {
        mAlbumArt = new MediaAlbumArt;
        mAlbumArt->mSize = dataSize;
        mAlbumArt->mData = new uint8_t[dataSize];
        memcpy(mAlbumArt->mData, data, dataSize);
    }

    size_t numTracks = mExtractor->countTracks();

    char tmp[32];
    sprintf(tmp, "%d", numTracks);

    mMetaData.add(METADATA_KEY_NUM_TRACKS, String8(tmp));

    bool hasAudio = false;
    bool hasVideo = false;
    int32_t videoWidth = -1;
    int32_t videoHeight = -1;
    int32_t audioBitrate = -1;

#ifdef MTK_S3D_SUPPORT
	//add for check stereo 3D
	int32_t video_3d_mode = 	VIDEO_STEREO_2D;
#endif
    // The overall duration is the duration of the longest track.
    int64_t maxDurationUs = 0;
    String8 timedTextLang;
    for (size_t i = 0; i < numTracks; ++i) {
        sp<MetaData> trackMeta = mExtractor->getTrackMetaData(i);

#ifndef ANDROID_DEFAULT_CODE
        if (trackMeta.get() == NULL) return ;
#endif // #ifndef ANDROID_DEFAULT_CODE
        int64_t durationUs;
        if (trackMeta->findInt64(kKeyDuration, &durationUs)) {
            if (durationUs > maxDurationUs) {
                maxDurationUs = durationUs;
            }
        }

        const char *mime;
        if (trackMeta->findCString(kKeyMIMEType, &mime)) {
            if (!hasAudio && !strncasecmp("audio/", mime, 6)) {
                hasAudio = true;

                if (!trackMeta->findInt32(kKeyBitRate, &audioBitrate)) {
                    audioBitrate = -1;
                }
            } else if (!hasVideo && !strncasecmp("video/", mime, 6)) {
                hasVideo = true;

                CHECK(trackMeta->findInt32(kKeyWidth, &videoWidth));
                CHECK(trackMeta->findInt32(kKeyHeight, &videoHeight));
				
#ifdef MTK_S3D_SUPPORT
				//check whether video is 3D from parser
				trackMeta->findInt32(kKeyVideoStereoMode,&video_3d_mode);
				LOGD("parseMetaData: video 3d mode=%d",video_3d_mode);				
#endif			
            } else if (!strcasecmp(mime, MEDIA_MIMETYPE_TEXT_3GPP)) {
                const char *lang;
                trackMeta->findCString(kKeyMediaLanguage, &lang);
                timedTextLang.append(String8(lang));
                timedTextLang.append(String8(":"));
            }
        }
    }

    // To save the language codes for all timed text tracks
    // If multiple text tracks present, the format will look
    // like "eng:chi"
    if (!timedTextLang.isEmpty()) {
        mMetaData.add(METADATA_KEY_TIMED_TEXT_LANGUAGES, timedTextLang);
    }

    // The duration value is a string representing the duration in ms.
    sprintf(tmp, "%lld", (maxDurationUs + 500) / 1000);
    mMetaData.add(METADATA_KEY_DURATION, String8(tmp));

    if (hasAudio) {
        mMetaData.add(METADATA_KEY_HAS_AUDIO, String8("yes"));
    }

    if (hasVideo) {
        mMetaData.add(METADATA_KEY_HAS_VIDEO, String8("yes"));

        sprintf(tmp, "%d", videoWidth);
        mMetaData.add(METADATA_KEY_VIDEO_WIDTH, String8(tmp));

        sprintf(tmp, "%d", videoHeight);
        mMetaData.add(METADATA_KEY_VIDEO_HEIGHT, String8(tmp));
		
#ifdef MTK_S3D_SUPPORT
		sprintf(tmp,"%d",video_3d_mode);
		mMetaData.add(METADATA_KEY_STEREO_3D, String8(tmp));
#endif
		
    }

    if (numTracks == 1 && hasAudio && audioBitrate >= 0) {
        sprintf(tmp, "%d", audioBitrate);
        mMetaData.add(METADATA_KEY_BITRATE, String8(tmp));
    } else {
        off64_t sourceSize;
        if (mSource->getSize(&sourceSize) == OK) {
            int64_t avgBitRate = (int64_t)(sourceSize * 8E6 / maxDurationUs);

            sprintf(tmp, "%lld", avgBitRate);
            mMetaData.add(METADATA_KEY_BITRATE, String8(tmp));
        }
    }

    if (numTracks == 1) {
        const char *fileMIME;
        CHECK(meta->findCString(kKeyMIMEType, &fileMIME));

        if (!strcasecmp(fileMIME, "video/x-matroska")) {
            sp<MetaData> trackMeta = mExtractor->getTrackMetaData(0);
#ifndef ANDROID_DEFAULT_CODE
            if (trackMeta.get() == NULL) return ;
#endif // #ifndef ANDROID_DEFAULT_CODE
            const char *trackMIME;
            CHECK(trackMeta->findCString(kKeyMIMEType, &trackMIME));

            if (!strncasecmp("audio/", trackMIME, 6)) {
                // The matroska file only contains a single audio track,
                // rewrite its mime type.
                mMetaData.add(
                        METADATA_KEY_MIMETYPE, String8("audio/x-matroska"));
            }
        }
    }

    // To check whether the media file is drm-protected
    if (mExtractor->getDrmFlag()) {
        mMetaData.add(METADATA_KEY_IS_DRM, String8("1"));
    }
}

}  // namespace android
