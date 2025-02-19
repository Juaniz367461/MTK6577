LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
	filter.cpp

LOCAL_SHARED_LIBRARIES := \
	libcutils \
	libutils \
    libEGL \
    libGLESv1_CM \
	libsurfaceflinger \
	libandroid \
	libgui \
    libui

LOCAL_MODULE:= test-opengl-filter

LOCAL_MODULE_TAGS := optional

LOCAL_CFLAGS := -DGL_GLEXT_PROTOTYPES

include $(BUILD_EXECUTABLE)
