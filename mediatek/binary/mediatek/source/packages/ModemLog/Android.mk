LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_MODULE := ModemLog
LOCAL_SRC_FILES := $(LOCAL_MODULE).apk
LOCAL_MODULE_SUFFIX := $(COMMON_ANDROID_PACKAGE_SUFFIX)
LOCAL_MODULE_CLASS := APPS

LOCAL_SHARED_LIBRARIES := libmdlogger_ctrl_jni

LOCAL_CERTIFICATE := platform


include $(BUILD_PREBUILT)
