/*
 * Copyright (C) 2010 The Android Open Source Project
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

#ifndef A_MPEG4_ELEM_ASSEMBLER_H_

#define A_MPEG4_ELEM_ASSEMBLER_H_

#include "ARTPAssembler.h"

#include <media/stagefright/foundation/AString.h>

#include <utils/List.h>
#include <utils/RefBase.h>

namespace android {

struct ABuffer;
struct AMessage;

struct AMPEG4ElementaryAssembler : public ARTPAssembler {
    AMPEG4ElementaryAssembler(
            const sp<AMessage> &notify, const AString &desc,
            const AString &params);

protected:
    virtual ~AMPEG4ElementaryAssembler();

#ifndef ANDROID_DEFAULT_CODE 
    virtual void evaluateDuration(const sp<ARTPSource> &source, 
            const sp<ABuffer> &buffer);
#endif // #ifndef ANDROID_DEFAULT_CODE
    virtual AssemblyStatus assembleMore(const sp<ARTPSource> &source);
    virtual void onByeReceived();
    virtual void packetLost();

private:
    sp<AMessage> mNotifyMsg;
    bool mIsGeneric;
    AString mParams;

#ifndef ANDROID_DEFAULT_CODE 
    bool mIsAAC;
#endif // #ifndef ANDROID_DEFAULT_CODE
    unsigned mSizeLength;
    unsigned mIndexLength;
    unsigned mIndexDeltaLength;
    unsigned mCTSDeltaLength;
    unsigned mDTSDeltaLength;
    bool mRandomAccessIndication;
    unsigned mStreamStateIndication;
    unsigned mAuxiliaryDataSizeLength;
    bool mHasAUHeader;

    uint32_t mAccessUnitRTPTime;
    bool mNextExpectedSeqNoValid;
    uint32_t mNextExpectedSeqNo;
    bool mAccessUnitDamaged;
    List<sp<ABuffer> > mPackets;
#ifndef ANDROID_DEFAULT_CODE
    unsigned mConstantSize;
#endif

    AssemblyStatus addPacket(const sp<ARTPSource> &source);
    void submitAccessUnit();

    DISALLOW_EVIL_CONSTRUCTORS(AMPEG4ElementaryAssembler);
#ifndef ANDROID_DEFAULT_CODE 
public:
    virtual void setNextExpectedSeqNo(uint32_t rtpSeq) {
        mNextExpectedSeqNo = rtpSeq;
        mNextExpectedSeqNoValid = true;
    }
#endif // #ifndef ANDROID_DEFAULT_CODE
};

}  // namespace android

#endif  // A_MPEG4_ELEM_ASSEMBLER_H_
