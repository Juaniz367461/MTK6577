LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
	tritex.cpp

LOCAL_SHARED_LIBRARIES := \
	libcutils \
	libutils \
    libEGL \
    libGLESv1_CM \
	libsurfaceflinger \
	libandroid \
	libgui \
    libui

LOCAL_MODULE:= test-opengl-tritex

LOCAL_MODULE_TAGS := optional

include $(BUILD_EXECUTABLE)
