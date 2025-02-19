LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src) \
	src/com/android/music/IMediaPlaybackService.aidl \
	src/com/mediatek/bluetooth/avrcp/IBTAvrcpMusic.aidl \
	src/com/mediatek/bluetooth/avrcp/IBTAvrcpMusicCallback.aidl

LOCAL_PACKAGE_NAME := Music

LOCAL_CERTIFICATE := platform
#LOCAL_SDK_VERSION := current

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

include $(BUILD_PACKAGE)

# Use the folloing include to make our test apk.
include $(call all-makefiles-under,$(LOCAL_PATH))
