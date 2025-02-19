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

#ifndef MEDIA_EXTRACTOR_H_

#define MEDIA_EXTRACTOR_H_

#include <utils/RefBase.h>

namespace android {

class DataSource;
class MediaSource;
class MetaData;

class MediaExtractor : public RefBase {
public:
    static sp<MediaExtractor> Create(
            const sp<DataSource> &source, const char *mime = NULL);

    virtual size_t countTracks() = 0;
    virtual sp<MediaSource> getTrack(size_t index) = 0;

    enum GetTrackMetaDataFlags {
#ifndef ANDROID_DEFAULT_CODE
        kIncludeExtensiveMetaData = 1,
		kIncludeInterleaveInfo = 2,
#else
        kIncludeExtensiveMetaData = 1
#endif
    };
    virtual sp<MetaData> getTrackMetaData(
            size_t index, uint32_t flags = 0) = 0;

    // Return container specific meta-data. The default implementation
    // returns an empty metadata object.
    virtual sp<MetaData> getMetaData();

    enum Flags {
        CAN_SEEK_BACKWARD  = 1,  // the "seek 10secs back button"
        CAN_SEEK_FORWARD   = 2,  // the "seek 10secs forward button"
        CAN_PAUSE          = 4,
        CAN_SEEK           = 8,  // the "seek bar"
#ifndef ANDROID_DEFAULT_CODE
        MAY_PARSE_TOO_LONG = 256,
#endif // #ifndef ANDROID_DEFAULT_CODE
    };

    // If subclasses do _not_ override this, the default is
    // CAN_SEEK_BACKWARD | CAN_SEEK_FORWARD | CAN_SEEK | CAN_PAUSE
    virtual uint32_t flags() const;

    // for DRM
    virtual void setDrmFlag(bool flag) {
        mIsDrm = flag;
    };
    virtual bool getDrmFlag() {
        return mIsDrm;
    }
    virtual char* getDrmTrackInfo(size_t trackID, int *len) {
        return NULL;
    }

#ifndef ANDROID_DEFAULT_CODE
    virtual int stopParsing() { return 0; }
    virtual int finishParsing(){return 0;}
#endif // #ifndef ANDROID_DEFAULT_CODE
protected:
    MediaExtractor() {}
    virtual ~MediaExtractor() {}

private:
    bool mIsDrm;

    MediaExtractor(const MediaExtractor &);
    MediaExtractor &operator=(const MediaExtractor &);
};

}  // namespace android

#endif  // MEDIA_EXTRACTOR_H_
