# Android.mk

ifeq ($(MTK_NFC_SUPPORT), never_happen)

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-subdir-java-files)

LOCAL_PACKAGE_NAME := NxpSecureElement

LOCAL_CERTIFICATE := platform

include $(BUILD_PACKAGE)

endif
