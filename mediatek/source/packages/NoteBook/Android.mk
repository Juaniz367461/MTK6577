ifeq ($(strip $(MTK_NOTEBOOK_SUPPORT)), yes)
LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional


LOCAL_SRC_FILES := \
 	$(call all-subdir-java-files)

LOCAL_PACKAGE_NAME := NoteBook
LOCAL_CERTIFICATE := platform

include $(BUILD_PACKAGE)

endif
