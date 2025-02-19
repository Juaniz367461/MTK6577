#
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
#

#
# This file should set PRODUCT_MAKEFILES to a list of product makefiles
# to expose to the build system.  LOCAL_DIR will already be set to
# the directory containing this file.
#
# This file may not rely on the value of any variable other than
# LOCAL_DIR; do not use any conditionals, and do not look up the
# value of any variable that isn't set in this file or in a file that
# it includes.
#

# Unbundled apps will be built with the default product config.
ifneq ($(TARGET_BUILD_APPS),)
PRODUCT_MAKEFILES := \
    $(LOCAL_DIR)/core.mk \
    $(LOCAL_DIR)/generic.mk \
    $(LOCAL_DIR)/full.mk
else
PRODUCT_MAKEFILES := \
    $(LOCAL_DIR)/core.mk \
    $(LOCAL_DIR)/generic.mk \
    $(LOCAL_DIR)/generic_armv5.mk \
    $(LOCAL_DIR)/generic_x86.mk \
    $(LOCAL_DIR)/full.mk \
    $(LOCAL_DIR)/full_x86.mk \
    $(LOCAL_DIR)/vbox_x86.mk \
    $(LOCAL_DIR)/sdk.mk \
    $(LOCAL_DIR)/sdk_x86.mk \
    $(LOCAL_DIR)/large_emu_hw.mk
#  ifneq ($(patsubst %_addon,_addon,$(TARGET_PRODUCT)),_addon)
  	 PRODUCT_MAKEFILES += \
       $(LOCAL_DIR)/$(TARGET_PRODUCT).mk
#  endif
endif
