

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
LOCAL_SRC_FILES = \
    libsched.c  \

LOCAL_C_INCLUDES += \
 	$(MTK_PATH_SOURCE)/kernel/include \

LOCAL_MODULE_TAGS := user
LOCAL_MODULE := libsched

LOCAL_SHARED_LIBRARIES := libcutils libc
LOCAL_CFLAGS += -g
include $(BUILD_SHARED_LIBRARY)


