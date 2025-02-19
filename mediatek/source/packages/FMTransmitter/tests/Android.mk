ifeq ($(strip $(MTK_AUTO_TEST)), yes)

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)



# We only want this apk build for tests.
LOCAL_MODULE_TAGS := tests
LOCAL_CERTIFICATE := platform

LOCAL_JAVA_LIBRARIES := android.test.runner\
                        robotium

# Include all test java files.
LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_PACKAGE_NAME := FMTransmitterTests

LOCAL_INSTRUMENTATION_FOR := FMTransmitter


include $(BUILD_PACKAGE)

###############

# Use the folloing include to make our test apk.
#include $(call all-makefiles-under,$(LOCAL_PATH))
endif
