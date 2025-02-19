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

## ==> build this lib only when HAVE_MATV_FEATURE is yes

include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := user

LOCAL_SRC_FILES+= matvctrl.c \
                  fmctrl.c \
                  mATVdrv_ps_api.cpp


LOCAL_C_INCLUDES+= $(TOPDIR)$(MTK_PATH_SOURCE)/external/matvctrl

ifeq ($(HAVE_MATV_FEATURE),yes)
ifeq ($(MTK_ATV_CHIP), $(filter $(MTK_ATV_CHIP),MTK_MT5192 MTK_MT5193))
LOCAL_SRC_FILES += custom/mATVdrv_cust.c
LOCAL_C_INCLUDES += $(LOCAL_PATH)/custom/
ifeq ($(MTK_ATV_CHIP),MTK_MT5192)
LOCAL_C_INCLUDES += $(TOP)/mediatek/custom/$(MTK_PROJECT)/kernel/matv/mt5192
else
LOCAL_C_INCLUDES += $(TOP)/mediatek/custom/$(MTK_PROJECT)/kernel/matv/mt5193
endif

LOCAL_GENERATE_CUSTOM_FOLDER := custom:hal/matv

LOCAL_CFLAGS += -D__ATV_SUPPORT__
LOCAL_CFLAGS += -D__MTK_TARGET__
#for disabling the 6 dummy lines in image
LOCAL_CFLAGS += -D__ATV_SP_SUPPORT__
endif
endif


LOCAL_SHARED_LIBRARIES := \
	libnativehelper \
	libcutils \
	libutils \
	libbinder \

ifeq ($(HAVE_MATV_FEATURE),yes)
ifeq ($(MTK_ATV_CHIP),MTK_MT5192)
LOCAL_STATIC_LIBRARIES += libmatvctrl
else
LOCAL_STATIC_LIBRARIES += libmatvctrl_93
endif
endif

## ==> build this lib only when HAVE_MATV_FEATURE is yes
ifeq ($(HAVE_MATV_FEATURE),yes)

## build this lib for MT5192 only
ifeq ($(MTK_ATV_CHIP),MTK_MT5192)
#for disabling the 6 dummy lines in image
LOCAL_CFLAGS += -DHAVE_MTK_MT5192
endif

ifeq ($(MTK_ATV_CHIP),MTK_MT5193)
LOCAL_CFLAGS += -DHAVE_MTK_MT5193
endif

endif

LOCAL_MODULE := libmatv_cust

LOCAL_PRELINK_MODULE:=false 

include $(BUILD_SHARED_LIBRARY)
