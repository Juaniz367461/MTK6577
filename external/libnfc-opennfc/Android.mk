ifeq ($(MTK_NFC_INSIDE), yes)
# flag to enable OpenNFCExtensions
EXTENDED_OPEN_NFC := true

###########"Hardware

# Copyright (C) 2008 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License. 

$(info  "Build Hardware")

LOCAL_PATH := $(call my-dir)

$(info $(LOCAL_PATH))

include $(CLEAR_VARS)

LOCAL_SHARED_LIBRARIES := liblog

LOCAL_PRELINK_MODULE := false
LOCAL_MODULE_TAGS = optional eng
LOCAL_ARM_MODE := arm

LOCAL_MODULE_PATH := $(TARGET_OUT_SHARED_LIBRARIES)

LOCAL_SRC_FILES := $(call all-c-files-under, open_nfc/hardware/libhardware/modules/nfcc/nfc_hal_microread/sources) \
						 $(call all-c-files-under, open_nfc/hardware/libhardware/modules/nfcc/nfc_hal_microread/porting)

LOCAL_C_INCLUDES := $(LOCAL_PATH)/open_nfc/hardware/libhardware/modules/nfcc/nfc_hal_microread/interfaces	\
						$(LOCAL_PATH)/open_nfc/hardware/libhardware/modules/nfcc/nfc_hal_microread/porting	\
						$(LOCAL_PATH)/open_nfc/open_nfc/interfaces \
						$(LOCAL_PATH)/open_nfc/open_nfc/porting/common \
						$(LOCAL_PATH)/open_nfc/open_nfc/porting/server \
						$(LOCAL_PATH)/open_nfc/hardware/libhardware/modules/include	\
						$(LOCAL_PATH)/open_nfc/hardware/libhardware/modules/nfcc/nfc_hal_microread \
						kernel/drivers/nfc
					
$(info  "C path")
$(info $(LOCAL_C_INCLUDES))					
						
LOCAL_MODULE := nfc_hal_microread
PRODUCT_COPY_FILES += $(LOCAL_PATH)/microread_fw.bin:/system/vendor/firmware/microread_fw.bin
include $(BUILD_SHARED_LIBRARY)

################""SIMULATOR
# Copyright (C) 2008 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

include $(CLEAR_VARS)

LOCAL_SHARED_LIBRARIES := liblog

LOCAL_PRELINK_MODULE := false
LOCAL_MODULE_TAGS = optional eng

LOCAL_MODULE_PATH := $(TARGET_OUT_SHARED_LIBRARIES)

LOCAL_SRC_FILES := $(call all-c-files-under, open_nfc/hardware/libhardware/modules/nfcc/nfc_hal_simulator/sources) \
						 $(call all-c-files-under, open_nfc/hardware/libhardware/modules/nfcc/nfc_hal_simulator/porting)


LOCAL_C_INCLUDES := $(LOCAL_PATH)/open_nfc/hardware/libhardware/modules/nfcc/nfc_hal_simulator/interfaces	\
						  $(LOCAL_PATH)/open_nfc/hardware/libhardware/modules/nfcc/nfc_hal_simulator/porting	\
						  $(LOCAL_PATH)/open_nfc/hardware/libhardware/modules/nfcc/nfc_hal_simulator/driver	\
						$(LOCAL_PATH)/open_nfc/open_nfc/interfaces \
						$(LOCAL_PATH)/open_nfc/open_nfc/porting/common \
						$(LOCAL_PATH)/open_nfc/open_nfc/porting/server \
						$(LOCAL_PATH)/open_nfc/hardware/libhardware/modules/include	

LOCAL_CFLAGS += -DDO_NOT_USE_LWRAP_UNICODE

LOCAL_MODULE := nfc_hal_simulator


include $(BUILD_SHARED_LIBRARY)
################""NFCC

# Copyright (C) 2008 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

$(info "Build NFCC")

#LOCAL_PATH := $(call my-dir)

# HAL module implemenation, not prelinked and stored in
# hw/<OVERLAY_HARDWARE_MODULE_ID>.<ro.product.board>.so
include $(CLEAR_VARS)

LOCAL_SHARED_LIBRARIES := liblog libdl

LOCAL_PRELINK_MODULE := false
LOCAL_MODULE_TAGS = optional eng

LOCAL_MODULE_PATH := $(TARGET_OUT_SHARED_LIBRARIES)/hw
LOCAL_ARM_MODE := arm

LOCAL_SRC_FILES := open_nfc/hardware/libhardware/modules/nfcc/nfcc/nfcc.c


LOCAL_C_INCLUDES :=	$(LOCAL_PATH)/open_nfc/open_nfc/interfaces		\
					$(LOCAL_PATH)/open_nfc/open_nfc/porting/common	\
					$(LOCAL_PATH)/open_nfc/open_nfc/porting/server	\
					$(LOCAL_PATH)/open_nfc/hardware/libhardware/include

LOCAL_MODULE := nfcc.default


include $(BUILD_SHARED_LIBRARY)



##############CLIENT

#
# Copyright (C) 2012 Inside Contactless
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

$(info "Build Client")
#LOCAL_PATH:= $(call my-dir)

#---------------------------------------------------------------------------------
# Open NFC client dynamic library
#---------------------------------------------------------------------------------

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional eng
LOCAL_ARM_MODE := arm

# This is the target being built.
LOCAL_MODULE:= libopen_nfc_client_jni

# All of the source files that we will compile.
LOCAL_SRC_FILES:= \
	$(call all-c-files-under, open_nfc/open_nfc/sources) \
	$(call all-c-files-under, open_nfc/open_nfc/porting/common) \
	$(call all-c-files-under, open_nfc/open_nfc/porting/client)
	
#Additionnal headers

LOCAL_C_INCLUDES := \
	$(LOCAL_PATH)/open_nfc/open_nfc/interfaces \
	$(LOCAL_PATH)/open_nfc/open_nfc/sources \
	$(LOCAL_PATH)/open_nfc/open_nfc/porting/common \
	$(LOCAL_PATH)/open_nfc/open_nfc/porting/client

LOCAL_CFLAGS := -D DO_NOT_USE_LWRAP_UNICODE=1

# All of the shared libraries we link against.
LOCAL_SHARED_LIBRARIES := \
	libandroid_runtime \
	libnativehelper \
	libcutils \
	libutils


# Don't prelink this library.  For more efficient code, you may want
# to add this library to the prelink map and set this to true.
LOCAL_PRELINK_MODULE := false

include $(BUILD_SHARED_LIBRARY)

#---------------------------------------------------------------------------------
# Open NFC server dynamic library
#---------------------------------------------------------------------------------

$(info "Build Server")

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional eng

# This is the target being built.
LOCAL_MODULE:= libopen_nfc_server_jni

# All of the source files that we will compile.
LOCAL_SRC_FILES:= \
	$(call all-c-files-under, open_nfc/open_nfc/sources) \
	$(call all-c-files-under, open_nfc/open_nfc/porting/common) \
	$(call all-c-files-under, open_nfc/open_nfc/porting/server)

#Additionnal headers
LOCAL_C_INCLUDES := \
	$(LOCAL_PATH)/open_nfc/open_nfc/interfaces \
	$(LOCAL_PATH)/open_nfc/open_nfc/porting/common \
	$(LOCAL_PATH)/open_nfc/open_nfc/porting/server \
	$(LOCAL_PATH)/open_nfc/hardware/libhardware/include \
	$(LOCAL_PATH)/open_nfc/hardware/libhardware/include/hardware \
	$(LOCAL_PATH)/open_nfc/hardware/libhardware/modules/nfcc/porting \
    $(LOCAL_PATH)/open_nfc/hardware/libhardware/modules/nfcc/nfc_hal_microread/porting

LOCAL_CFLAGS += -D DO_NOT_USE_LWRAP_UNICODE=1

# All of the shared libraries we link against.
LOCAL_SHARED_LIBRARIES := \
	libhardware \
	libandroid_runtime \
	libnativehelper \
	libcutils \
	libutils

# Don't prelink this library.  For more efficient code, you may want
# to add this library to the prelink map and set this to true.
LOCAL_PRELINK_MODULE := false

include $(BUILD_SHARED_LIBRARY)


########################SERVER

#
# Copyright (C) 2012 Inside Contactless
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

$(info "Build Standalone server")
#LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)    

LOCAL_MODULE_TAGS	:=	optional	eng
LOCAL_ARM_MODE := arm

LOCAL_SRC_FILES		:=	open_nfc/standalone_server/src/standalone_server.c

LOCAL_C_INCLUDES	:=	$(LOCAL_PATH)/open_nfc/open_nfc/porting/common
LOCAL_C_INCLUDES	+=	$(LOCAL_PATH)/open_nfc/open_nfc/porting/server
LOCAL_C_INCLUDES	+=	$(LOCAL_PATH)/open_nfc/open_nfc/interfaces
LOCAL_C_INCLUDES	+=	$(LOCAL_PATH)/open_nfc/hardware/libhardware/include
LOCAL_C_INCLUDES	+=	$(LOCAL_PATH)/open_nfc/hardware/libhardware/include/hardware
LOCAL_C_INCLUDES	+=	$(LOCAL_PATH)/open_nfc/hardware/libhardware/modules/nfcc/porting
LOCAL_C_INCLUDES	+=	$(LOCAL_PATH)/open_nfc/hardware/libhardware/modules/nfcc/nfc_hal_simulator/porting


LOCAL_MODULE := server_open_nfc

LOCAL_STATIC_LIBRARIES := libcutils libc 

LOCAL_SHARED_LIBRARIES	:=	libopen_nfc_server_jni  \
	libhardware \
	libandroid_runtime \
	libnativehelper \
	libcutils \
	libutils


PRODUCT_COPY_FILES += $(LOCAL_PATH)/connection_center_access:data/connection_center_access

include $(BUILD_EXECUTABLE)


#$(info "Build libnfc_ndef")
#
# libnfc_ndef
#

#include $(CLEAR_VARS)

#LOCAL_PRELINK_MODULE := false

#LOCAL_SRC_FILES := ndef/phFriNfc_NdefRecord.c

#LOCAL_CFLAGS := -I$(LOCAL_PATH)/ndef

#LOCAL_MODULE:= libnfc_ndef
#LOCAL_MODULE_TAGS := optional eng
#LOCAL_SHARED_LIBRARIES := libcutils

#include $(BUILD_SHARED_LIBRARY)

############################## Test launcher
#ifdef TEST_OPENNFC
#$(info "Build Open NFC test launcher")
#LOCAL_PATH:= $(call my-dir)
#include $(CLEAR_VARS)

#LOCAL_PRELINK_MODULE := false

#LOCAL_MODULE_TAGS	:=	optional	eng
#LOCAL_ARM_MODE := arm

#LOCAL_MODULE := wrapper_test_open_nfc

#LOCAL_SRC_FILES		:=	test/wrapper_test_launcher.c \
#	open_nfc_extension/open_nfc_ext_common.c \
#	open_nfc_extension/open_nfc_ext_client.c

#LOCAL_C_INCLUDES := \
#	$(LOCAL_PATH)/src \
#	$(LOCAL_PATH)/inc \
#	$(LOCAL_PATH)/open_nfc_extension

# libnfc
#LOCAL_SHARED_LIBRARIES := libcutils

#include $(BUILD_EXECUTABLE)
#endif


############################## JNI for OpenNFCExtensions

ifdef EXTENDED_OPEN_NFC

$(info "Build JNI for OpenNFCExtensions")

include $(CLEAR_VARS)

LOCAL_PRELINK_MODULE	:=	false

LOCAL_MODULE := libnfc_ext_jni
LOCAL_MODULE_TAGS := optional eng

LOCAL_SRC_FILES		:=	java/jni/com_opennfc_extension_nfc_api_OpenNFCExtManager.c \
	open_nfc_extension/open_nfc_ext_common.c \
	open_nfc_extension/open_nfc_ext_client.c
	
LOCAL_C_INCLUDES		:=	$(JNI_H_INCLUDE) \
							$(LOCAL_PATH)/inc \
							$(LOCAL_PATH)/ndef \
							$(LOCAL_PATH)/open_nfc/open_nfc/interfaces \
							$(LOCAL_PATH)/open_nfc/open_nfc/porting/common \
							$(LOCAL_PATH)/open_nfc_extension \
							$(LOCAL_PATH)/../../packages/apps/Nfc/jni_inside

LOCAL_SHARED_LIBRARIES	:=	libnativehelper		\
							libcutils			\
							libutils			\
							libopen_nfc_ext
#							libnfc

LOCAL_CFLAGS += -O0 -g

include $(BUILD_SHARED_LIBRARY)

endif

############################## Java for OpenNFCExtensions (to be used by external Java applications)
ifdef EXTENDED_OPEN_NFC

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional eng

LOCAL_SUB_PATH			:= java/src/com/opennfc/extension/nfc/api

LOCAL_SRC_FILES			:=	$(LOCAL_SUB_PATH)/CardEmulation.java \
							$(LOCAL_SUB_PATH)/OpenNFCExtManager.java \
							$(LOCAL_SUB_PATH)/OpenNFCExtReplyMessage.java \
							$(LOCAL_SUB_PATH)/CardEmulationException.java \
							$(LOCAL_SUB_PATH)/SecureElementPolicy.java \
							$(LOCAL_SUB_PATH)/Utils.java \
							$(LOCAL_SUB_PATH)/ConstantAutogen.java 
 							
LOCAL_MODULE := NfcExt
LOCAL_CERTIFICATE := platform

LOCAL_JNI_SHARED_LIBRARIES := nfc_ext_jni

LOCAL_PROGUARD_ENABLED := disabled

#Manually copy the optional library XML files in the system image.

PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/NfcExt.xml:system/etc/permissions/NfcExt.xml

include $(BUILD_JAVA_LIBRARY)
endif


############################## Libary for OpenNFCExtensions (to be embedded into engine)
ifdef EXTENDED_OPEN_NFC
include $(CLEAR_VARS)

$(info "Build library for OpenNFCExtensions server")

LOCAL_MODULE_TAGS := optional eng
LOCAL_ARM_MODE := arm

# This is the target being built.
LOCAL_MODULE:= libopen_nfc_ext


# All of the source files that we will compile.
LOCAL_SRC_FILES := \
		open_nfc_extension/open_nfc_ext_server.c \
		open_nfc_extension/open_nfc_ext_common.c \
		open_nfc_extension/queue.c \
		open_nfc_extension/card_emulation_ext.c
		
#Additionnal headers
LOCAL_C_INCLUDES := \
	$(LOCAL_PATH)/open_nfc/open_nfc/interfaces \
	$(LOCAL_PATH)/open_nfc/open_nfc/sources \
	$(LOCAL_PATH)/open_nfc/open_nfc/porting/common \
	$(LOCAL_PATH)/open_nfc/open_nfc/porting/client \
	$(LOCAL_PATH)/open_nfc_extension \
	$(LOCAL_PATH)/../../packages/apps/Nfc/jni_inside
	
LOCAL_CFLAGS := -DEXTENDED_OPEN_NFC

# All of the shared libraries we link against.
LOCAL_SHARED_LIBRARIES := \
	libc \
	libcutils \
	libopen_nfc_client_jni

# Don't prelink this library.  For more efficient code, you may want
# to add this library to the prelink map and set this to true.
LOCAL_PRELINK_MODULE := false

include $(BUILD_SHARED_LIBRARY)
endif

###   Distant debug   ###
#########################

#ifdef EXTENDED_OPEN_NFC
#	include $(CLEAR_VARS)    
#	LOCAL_MODULE_TAGS	:=	optional	eng
#	LOCAL_ARM_MODE := arm
	
#	LOCAL_SRC_FILES		:=	distant_debug/launch_distant_debug.c
	
#	LOCAL_MODULE := launch_distant_debug
	
#	LOCAL_STATIC_LIBRARIES := libcutils libc 
	
#	LOCAL_SHARED_LIBRARIES	:=	\
#		libhardware \
#		libandroid_runtime \
#		libnativehelper \
#		libcutils \
#		libutils
#	include $(BUILD_EXECUTABLE)
#endif
endif