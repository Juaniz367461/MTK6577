#
# Copyright (C) 2008 The Android Open Source Project
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

LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := MediaTekLocationProvider
LOCAL_SRC_FILES := $(LOCAL_MODULE).apk
LOCAL_MODULE_SUFFIX := $(COMMON_ANDROID_PACKAGE_SUFFIX)
LOCAL_MODULE_CLASS := APPS
LOCAL_SDK_VERSION := current
LOCAL_PROGUARD_ENABLED := custom
LOCAL_PROGUARD_FLAG_FILES := proguard.flags
LOCAL_JAVA_LIBRARIES += com.mediatek.location.provider
LOCAL_JAVA_LIBRARIES += com.android.location.provider


ifeq ($(strip $(MTK_SIGNATURE_CUSTOMIZATION)),yes)
  LOCAL_CERTIFICATE := releasekey
else
  LOCAL_CERTIFICATE := testkey
endif
include $(BUILD_PREBUILT)
include $(CLEAR_VARS)

