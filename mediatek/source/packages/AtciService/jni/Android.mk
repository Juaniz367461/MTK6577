LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SHARED_LIBRARIES += libcutils

ifeq ($(MTK_BT_SUPPORT),yes)
LOCAL_SHARED_LIBRARIES += libexttestmode
LOCAL_CFLAGS += -D__MTK_BT_SUPPORT__
endif

LOCAL_SRC_FILES := atciserv_jni.cpp

LOCAL_C_INCLUDES := $(JNI_H_INCLUDE)

LOCAL_MODULE_TAGS := user

LOCAL_MODULE := libatciserv_jni

LOCAL_PRELINK_MODULE := false
include $(BUILD_SHARED_LIBRARY)


