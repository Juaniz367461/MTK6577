# Only use ANDROID_BUILD_SHELL to wrap around bash.
# DO NOT use other shells such as zsh.
ifdef ANDROID_BUILD_SHELL
SHELL := $(ANDROID_BUILD_SHELL)
else
# Use bash, not whatever shell somebody has installed as /bin/sh
# This is repeated in config.mk, since envsetup.sh runs that file
# directly.
SHELL := /bin/bash
endif

# this turns off the suffix rules built into make
.SUFFIXES:

# this turns off the RCS / SCCS implicit rules of GNU Make
% : RCS/%,v
% : RCS/%
% : %,v
% : s.%
% : SCCS/s.%

# If a rule fails, delete $@.
.DELETE_ON_ERROR:

# Figure out where we are.
#TOP := $(dir $(word $(words $(MAKEFILE_LIST)),$(MAKEFILE_LIST)))
#TOP := $(patsubst %/,%,$(TOP))

# TOPDIR is the normal variable you should use, because
# if we are executing relative to the current directory
# it can be "", whereas TOP must be "." which causes
# pattern matching probles when make strips off the
# trailing "./" from paths in various places.
#ifeq ($(TOP),.)
#TOPDIR :=
#else
#TOPDIR := $(TOP)/
#endif

# check for broken versions of make
ifeq (0,$(shell expr $$(echo $(MAKE_VERSION) | sed "s/[^0-9\.].*//") = 3.81))
$(warning ********************************************************************************)
$(warning *  You are using version $(MAKE_VERSION) of make.)
$(warning *  Android can only be built by version 3.81.)
$(warning *  see http://source.android.com/source/download.html)
$(warning ********************************************************************************)
$(error stopping)
endif

TOP := .
TOPDIR :=

BUILD_SYSTEM := $(TOPDIR)build/core
BUILD_SYSTEM_MTK_EXTENSION := $(TOPDIR)mediatek/build/android


# This is the default target.  It must be the first declared target.
.PHONY: droid
DEFAULT_GOAL := droid
$(DEFAULT_GOAL):

# Used to force goals to build.  Only use for conditionally defined goals.
.PHONY: FORCE
FORCE:

# Targets that provide quick help on the build system.
include $(BUILD_SYSTEM)/help.mk

# Set up various standard variables based on configuration
# and host information.
include $(BUILD_SYSTEM)/config.mk

# This allows us to force a clean build - included after the config.make
# environment setup is done, but before we generate any dependencies.  This
# file does the rm -rf inline so the deps which are all done below will
# be generated correctly
include $(BUILD_SYSTEM)/cleanbuild.mk

VERSION_CHECK_SEQUENCE_NUMBER := 2
-include $(OUT_DIR)/versions_checked.mk
ifneq ($(VERSION_CHECK_SEQUENCE_NUMBER),$(VERSIONS_CHECKED))

$(info Checking build tools versions...)

ifneq ($(HOST_OS),windows)
ifneq ($(HOST_OS)-$(HOST_ARCH),darwin-ppc)
# check for a case sensitive file system
ifneq (a,$(shell mkdir -p $(OUT_DIR) ; \
                echo a > $(OUT_DIR)/casecheck.txt; \
                    echo B > $(OUT_DIR)/CaseCheck.txt; \
                cat $(OUT_DIR)/casecheck.txt))
$(warning ************************************************************)
$(warning You are building on a case-insensitive filesystem.)
$(warning Please move your source tree to a case-sensitive filesystem.)
$(warning ************************************************************)
$(error Case-insensitive filesystems not supported)
endif
endif
endif

# Make sure that there are no spaces in the absolute path; the
# build system can't deal with them.
ifneq ($(words $(shell pwd)),1)
$(warning ************************************************************)
$(warning You are building in a directory whose absolute path contains)
$(warning a space character:)
$(warning $(space))
$(warning "$(shell pwd)")
$(warning $(space))
$(warning Please move your source tree to a path that does not contain)
$(warning any spaces.)
$(warning ************************************************************)
$(error Directory names containing spaces not supported)
endif


# Check for the correct version of java
java_version := $(shell java -version 2>&1 | head -n 1 | grep '^java .*[ "]1\.6[\. "$$]')
ifneq ($(shell java -version 2>&1 | grep -i openjdk),)
java_version :=
endif
ifeq ($(strip $(java_version)),)
$(info ************************************************************)
$(info You are attempting to build with the incorrect version)
$(info of java.)
$(info $(space))
$(info Your version is: $(shell java -version 2>&1 | head -n 1).)
$(info The correct version is: Java SE 1.6.)
$(info $(space))
$(info Please follow the machine setup instructions at)
$(info $(space)$(space)$(space)$(space)http://source.android.com/source/download.html)
$(info ************************************************************)
$(error stop)
endif

# Check for the correct version of javac
javac_version := $(shell javac -version 2>&1 | head -n 1 | grep '[ "]1\.6[\. "$$]')
ifeq ($(strip $(javac_version)),)
$(info ************************************************************)
$(info You are attempting to build with the incorrect version)
$(info of javac.)
$(info $(space))
$(info Your version is: $(shell javac -version 2>&1 | head -n 1).)
$(info The correct version is: 1.6.)
$(info $(space))
$(info Please follow the machine setup instructions at)
$(info $(space)$(space)$(space)$(space)http://source.android.com/source/download.html)
$(info ************************************************************)
$(error stop)
endif

$(shell echo 'VERSIONS_CHECKED := $(VERSION_CHECK_SEQUENCE_NUMBER)' \
        > $(OUT_DIR)/versions_checked.mk)
endif

# These are the modifier targets that don't do anything themselves, but
# change the behavior of the build.
# (must be defined before including definitions.make)
INTERNAL_MODIFIER_TARGETS := showcommands checkbuild all incrementaljavac

.PHONY: incrementaljavac
incrementaljavac: ;

# WARNING:
# ENABLE_INCREMENTALJAVAC should NOT be enabled by default, because change of
# a Java source file won't trigger rebuild of its dependent Java files.
# You can only enable it by adding "incrementaljavac" to your make command line.
# You are responsible for the correctness of the incremental build.
# This may decrease incremental build time dramatically for large Java libraries,
# such as core.jar, framework.jar, etc.
ENABLE_INCREMENTALJAVAC :=
ifneq (,$(filter incrementaljavac, $(MAKECMDGOALS)))
ENABLE_INCREMENTALJAVAC := true
MAKECMDGOALS := $(filter-out incrementaljavac, $(MAKECMDGOALS))
endif

# Bring in standard build system definitions.
include $(BUILD_SYSTEM)/definitions.mk

ifeq (user, $(TARGET_BUILD_VARIANT))
    WITH_DEXPREOPT := true
endif 
# Bring in dex_preopt.mk
include $(BUILD_SYSTEM)/dex_preopt.mk

ifneq ($(filter eng user userdebug tests,$(MAKECMDGOALS)),)
$(info ***************************************************************)
$(info ***************************************************************)
$(info Don't pass '$(filter eng user userdebug tests,$(MAKECMDGOALS))' on \
		the make command line.)
# XXX The single quote on this line fixes gvim's syntax highlighting.
# Without which, the rest of this file is impossible to read.
$(info Set TARGET_BUILD_VARIANT in buildspec.mk, or use lunch or)
$(info choosecombo.)
$(info ***************************************************************)
$(info ***************************************************************)
$(error stopping)
endif

ifneq ($(filter-out $(INTERNAL_VALID_VARIANTS),$(TARGET_BUILD_VARIANT)),)
$(info ***************************************************************)
$(info ***************************************************************)
$(info Invalid variant: $(TARGET_BUILD_VARIANT)
$(info Valid values are: $(INTERNAL_VALID_VARIANTS)
$(info ***************************************************************)
$(info ***************************************************************)
$(error stopping)
endif

###
### In this section we set up the things that are different
### between the build variants
###

is_sdk_build :=

ifneq ($(filter sdk win_sdk sdk_addon,$(MAKECMDGOALS)),)
is_sdk_build := true
endif


## user/userdebug ##

user_variant := $(filter userdebug user,$(TARGET_BUILD_VARIANT))
enable_target_debugging := true
ifneq (,$(user_variant))
  # Target is secure in user builds.
  ADDITIONAL_DEFAULT_PROPERTIES += ro.secure=1

  tags_to_install := user
  ifeq ($(user_variant),userdebug)
    # Pick up some extra useful tools
    tags_to_install += debug

    # Enable Dalvik lock contention logging for userdebug builds.
    ADDITIONAL_BUILD_PROPERTIES += dalvik.vm.lockprof.threshold=500
  else
    # Disable debugging in plain user builds.
    enable_target_debugging :=
  endif

  # enable dex pre-optimization for all TARGET projects in default to 
  # speed up device first boot-up
  WITH_DEXPREOPT := true

  # Turn on Dalvik preoptimization for user builds, but only if not
  # explicitly disabled and the build is running on Linux (since host
  # Dalvik isn't built for non-Linux hosts).
  ifneq (true,$(DISABLE_DEXPREOPT))
    ifeq ($(user_variant),user)
      ifeq ($(HOST_OS),linux)
        WITH_DEXPREOPT := true
      endif
    endif
  endif

  # Disallow mock locations by default for user builds
  ADDITIONAL_DEFAULT_PROPERTIES += ro.allow.mock.location=0

else # !user_variant
  # Turn on checkjni for non-user builds.
  # Kirby: turn off it temporarily to gain performance {
  # ADDITIONAL_BUILD_PROPERTIES += ro.kernel.android.checkjni=1
  ADDITIONAL_BUILD_PROPERTIES += ro.kernel.android.checkjni=0
  # } Kirby
  # Set device insecure for non-user builds.
  ADDITIONAL_DEFAULT_PROPERTIES += ro.secure=0
  # Allow mock locations by default for non user builds
  ADDITIONAL_DEFAULT_PROPERTIES += ro.allow.mock.location=1
endif # !user_variant


# always enable aed
ADDITIONAL_DEFAULT_PROPERTIES += persist.mtk.aee.aed=on

ifeq (true,$(strip $(enable_target_debugging)))
  # Target is more debuggable and adbd is on by default
  ADDITIONAL_DEFAULT_PROPERTIES += ro.debuggable=1
  # Include the debugging/testing OTA keys in this build.
  INCLUDE_TEST_OTA_KEYS := true
else # !enable_target_debugging
  # Target is less debuggable and adbd is off by default
  ADDITIONAL_DEFAULT_PROPERTIES += ro.debuggable=0
endif # !enable_target_debugging

# default usb function
ifeq ($(strip $(MTK_MASS_STORAGE)),yes)
ADDITIONAL_DEFAULT_PROPERTIES += persist.sys.usb.config=mass_storage
else
  ifeq ($(strip $(MTK_BICR_SUPPORT)),yes)
    ADDITIONAL_DEFAULT_PROPERTIES += persist.sys.usb.config=mtp,mass_storage
  else
    ADDITIONAL_DEFAULT_PROPERTIES += persist.sys.usb.config=mtp
  endif
endif

# serial port open or not
ifeq ($(strip $(MTK_SERIAL_PORT_DEFAULT_ON)),yes)
ADDITIONAL_DEFAULT_PROPERTIES += persist.service.acm.enable=1
else
ADDITIONAL_DEFAULT_PROPERTIES += persist.service.acm.enable=0
endif

## eng ##

ifeq ($(TARGET_BUILD_VARIANT),eng)
tags_to_install := user debug eng
  # Don't require the setup wizard on eng builds
  ADDITIONAL_BUILD_PROPERTIES := $(filter-out ro.setupwizard.mode=%,\
          $(call collapse-pairs, $(ADDITIONAL_BUILD_PROPERTIES))) \
          ro.setupwizard.mode=OPTIONAL
endif

## tests ##

ifeq ($(TARGET_BUILD_VARIANT),tests)
tags_to_install := user debug eng tests
endif

## sdk ##

ifdef is_sdk_build

# Detect if we want to build a repository for the SDK
sdk_repo_goal := $(strip $(filter sdk_repo,$(MAKECMDGOALS)))
MAKECMDGOALS := $(strip $(filter-out sdk_repo,$(MAKECMDGOALS)))

ifneq ($(words $(filter-out $(INTERNAL_MODIFIER_TARGETS),$(MAKECMDGOALS))),1)
$(error The 'sdk' target may not be specified with any other targets)
endif

# TODO: this should be eng I think.  Since the sdk is built from the eng
# variant.
tags_to_install := user debug eng
ADDITIONAL_BUILD_PROPERTIES += xmpp.auto-presence=true
ADDITIONAL_BUILD_PROPERTIES += ro.config.nocheckin=yes
else # !sdk
endif

BUILD_WITHOUT_PV := true

## precise GC ##

ifneq ($(filter dalvik.gc.type-precise,$(PRODUCT_TAGS)),)
  # Enabling type-precise GC results in larger optimized DEX files.  The
  # additional storage requirements for ".odex" files can cause /system
  # to overflow on some devices, so this is configured separately for
  # each product.
  ADDITIONAL_BUILD_PROPERTIES += dalvik.vm.dexopt-flags=m=y
endif

ifneq ($(BUILD_TINY_ANDROID),true)
# Install an apns-conf.xml file if one's not already being installed.
ifeq (,$(filter %:system/etc/apns-conf.xml, $(PRODUCT_COPY_FILES)))
  PRODUCT_COPY_FILES += \
        development/data/etc/apns-conf_sdk.xml:system/etc/apns-conf.xml
  ifeq ($(filter eng tests,$(TARGET_BUILD_VARIANT)),)
    $(warning implicitly installing apns-conf_sdk.xml)
  endif
endif
endif

ADDITIONAL_BUILD_PROPERTIES += net.bt.name=Android

# enable vm tracing in files for now to help track
# the cause of ANRs in the content process
ADDITIONAL_BUILD_PROPERTIES += dalvik.vm.stack-trace-file=/data/anr/traces.txt

# ------------------------------------------------------------
# Define a function that, given a list of module tags, returns
# non-empty if that module should be installed in /system.

# For most goals, anything not tagged with the "tests" tag should
# be installed in /system.
define should-install-to-system
$(if $(filter tests,$(1)),,true)
endef

ifdef is_sdk_build
# For the sdk goal, anything with the "samples" tag should be
# installed in /data even if that module also has "eng"/"debug"/"user".
define should-install-to-system
$(if $(filter samples tests,$(1)),,true)
endef
endif


# If they only used the modifier goals (showcommands, checkbuild), we'll actually
# build the default target.
ifeq ($(filter-out $(INTERNAL_MODIFIER_TARGETS),$(MAKECMDGOALS)),)
.PHONY: $(INTERNAL_MODIFIER_TARGETS)
$(INTERNAL_MODIFIER_TARGETS): $(DEFAULT_GOAL)
endif

# These targets are going to delete stuff, don't bother including
# the whole directory tree if that's all we're going to do
ifeq ($(MAKECMDGOALS),clean)
dont_bother := true
endif
ifeq ($(MAKECMDGOALS),clobber)
dont_bother := true
endif
ifeq ($(MAKECMDGOALS),dataclean)
dont_bother := true
endif
ifeq ($(MAKECMDGOALS),installclean)
dont_bother := true
endif

# Bring in all modules that need to be built.
ifneq ($(dont_bother),true)

ifeq ($(HOST_OS)-$(HOST_ARCH),darwin-ppc)
SDK_ONLY := true
$(info Building the SDK under darwin-ppc is actually obsolete and unsupported.)
$(error stop)
endif

ifeq ($(HOST_OS),windows)
SDK_ONLY := true
endif

ifeq ($(SDK_ONLY),true)

# ----- SDK for Windows ------
# These configure the build targets that are available for the SDK under Windows.
# The first section defines all the C/C++ tools that can be compiled in C/C++,
# the second section defines all the Java ones (assuming javac is available.)

subdirs := \
	prebuilt \
	build/libs/host \
	build/tools/zipalign \
	dalvik/dexdump \
	dalvik/libdex \
	dalvik/tools/dmtracedump \
	dalvik/tools/hprof-conv \
	development/host \
	development/tools/etc1tool \
	development/tools/line_endings \
	development/tools/emulator/opengl \
	external/clang \
	external/easymock \
	external/expat \
	external/libpng \
	external/llvm \
	external/qemu \
	external/sqlite/dist \
	external/zlib \
	frameworks/base \
	frameworks/compile \
	sdk/avdlauncher \
	sdk/emulator/mksdcard \
	sdk/sdklauncher \
	system/core/adb \
	system/core/fastboot \
	system/core/libcutils \
	system/core/liblog \
	system/core/libzipfile \
	mediatek/source/external/ale/libxlog \
	mediatek/source/external/ale/map_reader

# The following can only be built if "javac" is available.
# This check is used when building parts of the SDK under Cygwin.
ifneq (,$(shell which javac 2>/dev/null))
subdirs += \
	build/tools/signapk \
	dalvik/dx \
	libcore \
	sdk/archquery \
	sdk/androidprefs \
	sdk/apkbuilder \
	sdk/assetstudio \
	sdk/common \
	sdk/ddms \
	sdk/hierarchyviewer2 \
	sdk/ide_common \
	sdk/jarutils \
	sdk/layoutlib_api \
	sdk/layoutopt \
	sdk/ninepatch \
	sdk/rule_api \
	sdk/lint \
	sdk/sdkstats \
	sdk/sdkmanager \
	sdk/swtmenubar \
	sdk/traceview \
	development/apps \
	development/tools/mkstubs \
	packages
else
$(warning SDK_ONLY: javac not available.)
endif

# Exclude tools/acp when cross-compiling windows under linux
ifeq ($(findstring Linux,$(UNAME)),)
subdirs += build/tools/acp
endif

else	# !SDK_ONLY
ifeq ($(BUILD_TINY_ANDROID), true)

# TINY_ANDROID is a super-minimal build configuration, handy for board
# bringup and very low level debugging

subdirs := \
	bionic \
	system/core \
	system/extras/ext4_utils \
	system/extras/su \
	build/libs \
	build/target \
	build/tools/acp \
	external/mksh \
	external/yaffs2 \
	external/zlib \
        mediatek/source/external/xlog/libxlog

ifeq ($(BUILD_MTK_LDVT), true)
subdirs += $(MTK_PATH_PLATFORM)/external/ldvt
subdirs += $(MTK_PATH_PLATFORM)/external/mhal/src/core/drv
subdirs += $(MTK_PATH_PLATFORM)/external/mhal/src/core/pipe
subdirs += $(MTK_PATH_PLATFORM)/external/mhal/src/custom
endif

else	# !BUILD_TINY_ANDROID

#
# Typical build; include any Android.mk files we can find.
#
subdirs := $(TOP)

FULL_BUILD := true

endif	# !BUILD_TINY_ANDROID

endif	# !SDK_ONLY

# Before we go and include all of the module makefiles, stash away
# the PRODUCT_* values so that later we can verify they are not modified.
stash_product_vars:=true
ifeq ($(stash_product_vars),true)
  $(call stash-product-vars, __STASHED)
endif

ifneq ($(ONE_SHOT_MAKEFILE),)
# We've probably been invoked by the "mm" shell function
# with a subdirectory's makefile.
include $(ONE_SHOT_MAKEFILE)
# Change CUSTOM_MODULES to include only modules that were
# defined by this makefile; this will install all of those
# modules as a side-effect.  Do this after including ONE_SHOT_MAKEFILE
# so that the modules will be installed in the same place they
# would have been with a normal make.
CUSTOM_MODULES := $(sort $(call get-tagged-modules,$(ALL_MODULE_TAGS)))
FULL_BUILD :=
# Stub out the notice targets, which probably aren't defined
# when using ONE_SHOT_MAKEFILE.
NOTICE-HOST-%: ;
NOTICE-TARGET-%: ;

else # ONE_SHOT_MAKEFILE

#
# Include all of the makefiles in the system
#

# Can't use first-makefiles-under here because
# --mindepth=2 makes the prunes not work.
subdir_makefiles := \
	$(shell build/tools/findleaves.py --prune=out --prune=.repo --prune=.git $(subdirs) Android.mk)

include $(subdir_makefiles)
endif # ONE_SHOT_MAKEFILE

ifeq ($(stash_product_vars),true)
#  $(call assert-product-vars, __STASHED)
endif

include $(BUILD_SYSTEM)/legacy_prebuilts.mk
ifneq ($(filter-out $(GRANDFATHERED_ALL_PREBUILT),$(strip $(notdir $(ALL_PREBUILT)))),)
  $(warning *** Some files have been added to ALL_PREBUILT.)
  $(warning *)
  $(warning * ALL_PREBUILT is a deprecated mechanism that)
  $(warning * should not be used for new files.)
  $(warning * As an alternative, use PRODUCT_COPY_FILES in)
  $(warning * the appropriate product definition.)
  $(warning * build/target/product/core.mk is the product)
  $(warning * definition used in all products.)
  $(warning *)
  $(foreach bad_prebuilt,$(filter-out $(GRANDFATHERED_ALL_PREBUILT),$(strip $(notdir $(ALL_PREBUILT)))),$(warning * unexpected $(bad_prebuilt) in ALL_PREBUILT))
  $(warning *)
  $(error ALL_PREBUILT contains unexpected files)
endif

# -------------------------------------------------------------------
# All module makefiles have been included at this point.
# -------------------------------------------------------------------

# -------------------------------------------------------------------
# Include any makefiles that must happen after the module makefiles
# have been included.
# TODO: have these files register themselves via a global var rather
# than hard-coding the list here.
ifdef FULL_BUILD
  # Only include this during a full build, otherwise we can't be
  # guaranteed that any policies were included.
  -include frameworks/policies/base/PolicyConfig.mk
endif

# -------------------------------------------------------------------
# Fix up CUSTOM_MODULES to refer to installed files rather than
# just bare module names.  Leave unknown modules alone in case
# they're actually full paths to a particular file.
known_custom_modules := $(filter $(ALL_MODULES),$(CUSTOM_MODULES))
unknown_custom_modules := $(filter-out $(ALL_MODULES),$(CUSTOM_MODULES))
CUSTOM_MODULES := \
	$(call module-installed-files,$(known_custom_modules)) \
	$(unknown_custom_modules)

# -------------------------------------------------------------------
# Define dependencies for modules that require other modules.
# This can only happen now, after we've read in all module makefiles.
#
# TODO: deal with the fact that a bare module name isn't
# unambiguous enough.  Maybe declare short targets like
# APPS:Quake or HOST:SHARED_LIBRARIES:libutils.
# BUG: the system image won't know to depend on modules that are
# brought in as requirements of other modules.
define add-required-deps
$(1): $(2)
endef
$(foreach m,$(ALL_MODULES), \
  $(eval r := $(ALL_MODULES.$(m).REQUIRED)) \
  $(if $(r), \
    $(eval r := $(call module-installed-files,$(r))) \
    $(eval $(call add-required-deps,$(ALL_MODULES.$(m).INSTALLED),$(r))) \
   ) \
 )
m :=
r :=
i :=
add-required-deps :=

# -------------------------------------------------------------------
# Figure out our module sets.

# Of the modules defined by the component makefiles,
# determine what we actually want to build.
Default_MODULES := $(sort $(ALL_DEFAULT_INSTALLED_MODULES) \
                          $(CUSTOM_MODULES))
# TODO: Remove the 3 places in the tree that use
# ALL_DEFAULT_INSTALLED_MODULES and get rid of it from this list.

ifdef FULL_BUILD
  # The base list of modules to build for this product is specified
  # by the appropriate product definition file, which was included
  # by product_config.make.
  user_PACKAGES := $(call module-installed-files, \
                       $(PRODUCTS.$(INTERNAL_PRODUCT).PRODUCT_PACKAGES))
  ifeq (0,1)
    $(info user packages for $(TARGET_DEVICE) ($(INTERNAL_PRODUCT)):)
    $(foreach p,$(user_PACKAGES),$(info :   $(p)))
    $(error done)
  endif
else
  # We're not doing a full build, and are probably only including
  # a subset of the module makefiles.  Don't try to build any modules
  # requested by the product, because we probably won't have rules
  # to build them.
  user_PACKAGES :=
endif
# Use tags to get the non-APPS user modules.  Use the product
# definition files to get the APPS user modules.
user_MODULES := $(sort $(call get-tagged-modules,user shell_$(TARGET_SHELL)))
user_MODULES := $(user_MODULES) $(user_PACKAGES)

eng_MODULES := $(sort $(call get-tagged-modules,eng))
debug_MODULES := $(sort $(call get-tagged-modules,debug))
tests_MODULES := $(sort $(call get-tagged-modules,tests))

ifeq ($(strip $(tags_to_install)),)
$(error ASSERTION FAILED: tags_to_install should not be empty)
endif
modules_to_install := $(sort $(Default_MODULES) \
          $(foreach tag,$(tags_to_install),$($(tag)_MODULES)))

# Some packages may override others using LOCAL_OVERRIDES_PACKAGES.
# Filter out (do not install) any overridden packages.
overridden_packages := $(call get-package-overrides,$(modules_to_install))
ifdef overridden_packages
#  old_modules_to_install := $(modules_to_install)
  modules_to_install := \
      $(filter-out $(foreach p,$(overridden_packages),$(p) %/$(p).apk), \
          $(modules_to_install))
endif
#$(error filtered out
#           $(filter-out $(modules_to_install),$(old_modules_to_install)))

# Don't include any GNU targets in the SDK.  It's ok (and necessary)
# to build the host tools, but nothing that's going to be installed
# on the target (including static libraries).
ifdef is_sdk_build
  target_gnu_MODULES := \
              $(filter \
                      $(TARGET_OUT_INTERMEDIATES)/% \
                      $(TARGET_OUT)/% \
                      $(TARGET_OUT_DATA)/%, \
                              $(sort $(call get-tagged-modules,gnu)))
  $(info Removing from sdk:)$(foreach d,$(target_gnu_MODULES),$(info : $(d)))
  modules_to_install := \
              $(filter-out $(target_gnu_MODULES),$(modules_to_install))

  # Ensure every module listed in PRODUCT_PACKAGES gets something installed
  $(foreach m, $(PRODUCTS.$(INTERNAL_PRODUCT).PRODUCT_PACKAGES), \
      $(if $(strip $(ALL_MODULES.$(m).INSTALLED)),,\
          $(warning Module '$(m)' in PRODUCT_PACKAGES has nothing to install!)))
endif


# build/core/Makefile contains extra stuff that we don't want to pollute this
# top-level makefile with.  It expects that ALL_DEFAULT_INSTALLED_MODULES
# contains everything that's built during the current make, but it also further
# extends ALL_DEFAULT_INSTALLED_MODULES.
ALL_DEFAULT_INSTALLED_MODULES := $(modules_to_install)
include $(BUILD_SYSTEM_MTK_EXTENSION)/extension.mk
include $(BUILD_SYSTEM)/Makefile
modules_to_install := $(sort $(ALL_DEFAULT_INSTALLED_MODULES))
ALL_DEFAULT_INSTALLED_MODULES :=

endif # dont_bother

# These are additional goals that we build, in order to make sure that there
# is as little code as possible in the tree that doesn't build.
modules_to_check := $(foreach m,$(ALL_MODULES),$(ALL_MODULES.$(m).CHECKED))

# If you would like to build all goals, and not skip any intermediate
# steps, you can pass the "all" modifier goal on the commandline.
ifneq ($(filter all,$(MAKECMDGOALS)),)
modules_to_check += $(foreach m,$(ALL_MODULES),$(ALL_MODULES.$(m).BUILT))
endif

# for easier debugging
modules_to_check := $(sort $(modules_to_check))
#$(error modules_to_check $(modules_to_check))

# -------------------------------------------------------------------
# This is used to to get the ordering right, you can also use these,
# but they're considered undocumented, so don't complain if their
# behavior changes.
.PHONY: prebuilt
prebuilt: $(ALL_PREBUILT)

# An internal target that depends on all copied headers
# (see copy_headers.make).  Other targets that need the
# headers to be copied first can depend on this target.
.PHONY: all_copied_headers
all_copied_headers: ;

$(ALL_C_CPP_ETC_OBJECTS): | all_copied_headers

# All the droid stuff, in directories
.PHONY: files
files: prebuilt \
        $(modules_to_install) \
        $(modules_to_check) \
        $(INSTALLED_ANDROID_INFO_TXT_TARGET)

# -------------------------------------------------------------------

.PHONY: checkbuild
checkbuild: $(modules_to_check)

.PHONY: ramdisk
ramdisk: $(INSTALLED_RAMDISK_TARGET)

.PHONY: systemtarball
systemtarball: $(INSTALLED_SYSTEMTARBALL_TARGET)

.PHONY: cacheimage
ifeq ($(TARGET_USERIMAGES_USE_EXT4),true)
cacheimage: $(INSTALLED_CACHEIMAGE_TARGET)
else
cacheimage: 
	@echo "EMMC feature NOT supported/disabled now(MTK_EMMC_SUPPORT=$(MTK_EMMC_SUPPORT)"
endif

.PHONY: boottarball
boottarball: $(INSTALLED_BOOTTARBALL_TARGET)

.PHONY: userdataimage
userdataimage: $(INSTALLED_USERDATAIMAGE_TARGET)

.PHONY: secroimage
secroimage: $(INSTALLED_SECROIMAGE_TARGET)

ifneq (,$(filter userdataimage, $(MAKECMDGOALS)))
$(call dist-for-goals, userdataimage, $(BUILT_USERDATAIMAGE_TARGET))
endif

.PHONY: userdatatarball
userdatatarball: $(INSTALLED_USERDATATARBALL_TARGET)

.PHONY: bootimage
bootimage: $(INSTALLED_BOOTIMAGE_TARGET)

ifeq ($(BUILD_TINY_ANDROID), true)
INSTALLED_RECOVERYIMAGE_TARGET :=
endif

# Build files and then package it into the rom formats
.PHONY: droidcore
droidcore: files \
	systemimage \
	$(INSTALLED_BOOTIMAGE_TARGET) \
	$(INSTALLED_RECOVERYIMAGE_TARGET) \
	$(INSTALLED_USERDATAIMAGE_TARGET) \
	$(INSTALLED_SECROIMAGE_TARGET) \
	$(INSTALLED_FACTORYIMAGE_TARGET) \
	$(INSTALLED_FILES_FILE)

ifeq ($(TARGET_USERIMAGES_USE_EXT4),true)
droidcore: $(INSTALLED_CACHEIMAGE_TARGET)
endif

# dist_files only for putting your library into the dist directory with a full build.
.PHONY: dist_files

ifeq ($(EMMA_INSTRUMENT),true)
  $(call dist-for-goals, dist_files, $(EMMA_META_ZIP))
endif

# Dist for droid if droid is among the cmd goals, or no cmd goal is given.
ifneq ($(filter droid,$(MAKECMDGOALS))$(filter ||,|$(filter-out $(INTERNAL_MODIFIER_TARGETS),$(MAKECMDGOALS))|),)

ifneq ($(TARGET_BUILD_APPS),)
  # If this build is just for apps, only build apps and not the full system by default.

  unbundled_build_modules :=
  ifneq ($(filter all,$(TARGET_BUILD_APPS)),)
    # If they used the magic goal "all" then build all apps in the source tree.
    unbundled_build_modules := $(foreach m,$(sort $(ALL_MODULES)),$(if $(filter APPS,$(ALL_MODULES.$(m).CLASS)),$(m)))
  else
    unbundled_build_modules := $(TARGET_BUILD_APPS)
  endif

  # dist the unbundled app.
  $(call dist-for-goals,apps_only, \
    $(foreach m,$(unbundled_build_modules),$(ALL_MODULES.$(m).INSTALLED)) \
  )

.PHONY: apps_only
apps_only: $(unbundled_build_modules)

droid: apps_only

else # TARGET_BUILD_APPS
  $(call dist-for-goals, droidcore, \
    $(INTERNAL_UPDATE_PACKAGE_TARGET) \
    $(INTERNAL_OTA_PACKAGE_TARGET) \
    $(SYMBOLS_ZIP) \
    $(APPS_ZIP) \
    $(INTERNAL_EMULATOR_PACKAGE_TARGET) \
    $(PACKAGE_STATS_FILE) \
    $(INSTALLED_FILES_FILE) \
    $(INSTALLED_BUILD_PROP_TARGET) \
    $(BUILT_TARGET_FILES_PACKAGE) \
    $(INSTALLED_ANDROID_INFO_TXT_TARGET) \
    $(INSTALLED_RAMDISK_TARGET) \
   )

# Building a full system-- the default is to build droidcore
droid: droidcore dist_files

endif # TARGET_BUILD_APPS
endif # droid in $(MAKECMDGOALS)


.PHONY: droid tests
tests: droidcore

# phony target that include any targets in $(ALL_MODULES)
.PHONY: all_modules
all_modules: $(ALL_MODULES)

.PHONY: docs
docs: $(ALL_DOCS)

.PHONY: sdk
ALL_SDK_TARGETS := $(INTERNAL_SDK_TARGET)
sdk: $(ALL_SDK_TARGETS)
ifneq ($(filter sdk win_sdk,$(MAKECMDGOALS)),)
$(call dist-for-goals,sdk win_sdk, \
    $(ALL_SDK_TARGETS) \
    $(SYMBOLS_ZIP) \
    $(INSTALLED_BUILD_PROP_TARGET) \
)
endif

.PHONY: samplecode
sample_MODULES := $(sort $(call get-tagged-modules,samples))
sample_APKS_DEST_PATH := $(TARGET_COMMON_OUT_ROOT)/samples
sample_APKS_COLLECTION := \
        $(foreach module,$(sample_MODULES),$(sample_APKS_DEST_PATH)/$(notdir $(module)))
$(foreach module,$(sample_MODULES),$(eval $(call \
        copy-one-file,$(module),$(sample_APKS_DEST_PATH)/$(notdir $(module)))))
sample_ADDITIONAL_INSTALLED := \
        $(filter-out $(modules_to_install) $(modules_to_check) $(ALL_PREBUILT),$(sample_MODULES))
samplecode: $(sample_APKS_COLLECTION)
	@echo "Collect sample code apks: $^"
	# remove apks that are not intended to be installed.
	rm -f $(sample_ADDITIONAL_INSTALLED)

.PHONY: findbugs
findbugs: $(INTERNAL_FINDBUGS_HTML_TARGET) $(INTERNAL_FINDBUGS_XML_TARGET)

.PHONY: clean
dirs_to_clean := \
	$(PRODUCT_OUT) \
	$(TARGET_COMMON_OUT_ROOT) \
	$(HOST_OUT) \
	$(HOST_COMMON_OUT_ROOT)
clean:
	@for dir in $(dirs_to_clean) ; do \
	    echo "Cleaning $$dir..."; \
	    rm -rf $$dir; \
	done
	@echo "Clean."; \

.PHONY: clobber
clobber:
	@rm -rf $(OUT_DIR)
	@echo "Entire build directory removed."

#.PHONY: clean
#clean:
#	@rm -rf $(out_dir)
#	@echo "Entire build directory removed."

#.PHONY: clobber
#clobber: clean

# The rules for dataclean and installclean are defined in cleanbuild.mk.

#xxx scrape this from ALL_MODULE_NAME_TAGS
.PHONY: modules
modules:
	@echo "Available sub-modules:"
	@echo "$(call module-names-for-tag-list,$(ALL_MODULE_TAGS))" | \
	      tr -s ' ' '\n' | sort -u | $(COLUMN)

.PHONY: showcommands
showcommands:
	@echo >/dev/null

# dump comp. build info index (local path <--> module id)
ifeq ($(DUMP_COMP_BUILD_INFO),true)
  $(shell rm -f $(COMP_INDEX_TABLE))
  $(shell rm -f $(COMP_INDEX_TABLE_PLUS))
  $(call dump-words-to-file.mtk, $(COMP_INDEX_INFO), $(COMP_INDEX_TABLE)) 
  $(call dump-words-to-file.mtk, $(COMP_INDEX_INFO_PLUS), $(COMP_INDEX_TABLE_PLUS))
endif

