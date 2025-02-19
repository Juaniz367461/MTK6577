#
# Copyright (C) 2011 The Android Open Source Project
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

LOCAL_PATH:= $(call my-dir)

#
# libvideofilters
#

include $(CLEAR_VARS)

LOCAL_MODULE:= libvideoeditor_videofilters

LOCAL_SRC_FILES:=          \
      M4VIFI_BGR565toYUV420.c \
      M4VIFI_ResizeRGB888toRGB888.c \
      M4VIFI_ResizeRGB565toRGB565.c \
      M4VIFI_Clip.c \
      M4VIFI_ResizeYUVtoBGR565.c \
      M4VIFI_RGB888toYUV420.c \
      M4VIFI_RGB565toYUV420.c \
      M4VFL_transition.c


LOCAL_SHARED_LIBRARIES := libcutils libutils

LOCAL_STATIC_LIBRARIES := \
    libvideoeditor_osal

LOCAL_C_INCLUDES += \
    $(TOP)/frameworks/media/libvideoeditor/osal/inc \
    $(TOP)/frameworks/media/libvideoeditor/vss/common/inc

LOCAL_MODULE_TAGS := user

LOCAL_SHARED_LIBRARIES += libdl

# All of the shared libraries we link against.
LOCAL_LDLIBS := \
    -lpthread -ldl

LOCAL_CFLAGS += -Wno-multichar

include $(BUILD_STATIC_LIBRARY)

