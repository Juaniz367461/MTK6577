################################################################################
#
################################################################################

LOCAL_PATH := $(call my-dir)

################################################################################
#
################################################################################
include $(CLEAR_VARS)

#-----------------------------------------------------------
LOCAL_SRC_FILES += PVWDisplayAdapter.cpp
LOCAL_SRC_FILES += TgtBufQue.cpp
LOCAL_SRC_FILES += mHal/mHalPVWDisplayAdapter.cpp
LOCAL_SRC_FILES += mHal/ImgBufMapper.cpp

#-----------------------------------------------------------
LOCAL_C_INCLUDES += $(LOCAL_PATH)/../..
LOCAL_C_INCLUDES += $(LOCAL_PATH)/..
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/frameworks/base/include
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/external

LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/external/cameraprofile

#-----------------------------------------------------------
LOCAL_MODULE := camera.display.adapter.pvwdisplayadapter

#-----------------------------------------------------------
include $(BUILD_STATIC_LIBRARY)


################################################################################
#
################################################################################
include $(CLEAR_VARS)
include $(call all-makefiles-under,$(LOCAL_PATH))

