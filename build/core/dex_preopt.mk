####################################
# Dexpreopt on the boot jars
#
####################################

# TODO: replace it with device's BOOTCLASSPATH
DEXPREOPT_BOOT_JARS := core:core-junit:bouncycastle:ext:framework:android.policy:services:apache-xml:filterfw:mediatek-framework:secondary_framework

# ONLY enable emma in eng build
ifeq ($(TARGET_BUILD_VARIANT),eng)
DEXPREOPT_BOOT_JARS := $(DEXPREOPT_BOOT_JARS):emma
endif

DEXPREOPT_BOOT_JARS_MODULES := $(subst :, ,$(DEXPREOPT_BOOT_JARS))

DEXPREOPT_BUILD_DIR := $(OUT_DIR)
DEXPREOPT_PRODUCT_DIR := $(patsubst $(DEXPREOPT_BUILD_DIR)/%,%,$(PRODUCT_OUT))/dex_bootjars
DEXPREOPT_BOOT_JAR_DIR := system/framework
DEXPREOPT_DEXOPT := $(patsubst $(DEXPREOPT_BUILD_DIR)/%,%,$(DEXOPT))

DEXPREOPT_BOOT_JAR_DIR_FULL_PATH := $(DEXPREOPT_BUILD_DIR)/$(DEXPREOPT_PRODUCT_DIR)/$(DEXPREOPT_BOOT_JAR_DIR)

DEXPREOPT_BOOT_ODEXS := $(foreach b,$(DEXPREOPT_BOOT_JARS_MODULES),\
    $(DEXPREOPT_BOOT_JAR_DIR_FULL_PATH)/$(b).odex)

# dexpreoption setting for binary released jars 
ifeq ($(PARTIAL_BUILD),true)
MTK_BINARY_RELEASE_JARS_MODULES := 
DEXPREOPT_BINARY_RELEASE_BOOT_JARS_MODULES := $(filter $(DEXPREOPT_BOOT_JARS_MODULES), $(MTK_BINARY_RELEASE_JARS_MODULES))
DEXPREOPT_BINARY_RELEASE_BOOT_JARS :=
endif

# If the target is a uniprocessor, then explicitly tell the preoptimizer
# that fact. (By default, it always optimizes for an SMP target.)
ifeq ($(TARGET_CPU_SMP),true)
DEXPREOPT_UNIPROCESSOR :=
else
DEXPREOPT_UNIPROCESSOR := --uniprocessor
endif

# $(1):binary released  boot jar module name
define install-dexpreopt-jars.binary-release
$(eval _installed_binary_release_jar := $(TARGET_OUT_JAVA_LIBRARIES)/$(1).jar)
$(eval _installed_dexpreopt_binary_release_jar := $(TARGET_OUT_JAVA_LIBRARIES)/$(1).odex)
$(eval _nodex_binary_release_jar := $(DEXPREOPT_BOOT_JAR_DIR_FULL_PATH)/$(1)_nodex.jar)
$(eval _dexpreopt_binary_release_jar := $(DEXPREOPT_BOOT_JAR_DIR_FULL_PATH)/$(1).odex)
$(_installed_binary_release_jar) : $(_nodex_binary_release_jar) | $(ACP)
	$$(call copy-file-to-target)

$(_installed_dexpreopt_binary_release_jar) : $(_dexpreopt_binary_release_jar) | $(ACP)
	$$(call copy-file-to-target)

$(eval _installed_binary_release_jar :=)
$(eval _installed_dexpreopt_binary_release_jar :=)
$(eval _nodex_binary_release_jar :=)
$(eval _dexpreopt_binary_release_jar :=)
endef

# $(1): the .jar or .apk to remove classes.dex
define dexpreopt-remove-classes.dex
$(hide) $(AAPT) remove $(1) classes.dex
endef

# $(1): the input .jar or .apk file
# $(2): the output .odex file
define dexpreopt-one-file
$(hide) $(DEXPREOPT) --dexopt=$(DEXPREOPT_DEXOPT) --build-dir=$(DEXPREOPT_BUILD_DIR) \
	--product-dir=$(DEXPREOPT_PRODUCT_DIR) --boot-dir=$(DEXPREOPT_BOOT_JAR_DIR) \
	--boot-jars=$(DEXPREOPT_BOOT_JARS) $(DEXPREOPT_UNIPROCESSOR) \
	$(patsubst $(DEXPREOPT_BUILD_DIR)/%,%,$(1)) \
	$(patsubst $(DEXPREOPT_BUILD_DIR)/%,%,$(2))
endef

# $(1): boot jar module name
define _dexpreopt-boot-jar
$(eval _dbj_jar := $(DEXPREOPT_BOOT_JAR_DIR_FULL_PATH)/$(1).jar)
$(eval _dbj_odex := $(DEXPREOPT_BOOT_JAR_DIR_FULL_PATH)/$(1).odex)
$(eval _dbj_jar_no_dex := $(DEXPREOPT_BOOT_JAR_DIR_FULL_PATH)/$(1)_nodex.jar)
$(eval _dbj_src_jar := $(call intermediates-dir-for,JAVA_LIBRARIES,$(1),,COMMON)/javalib.jar)
$(eval $(_dbj_odex): PRIVATE_DBJ_JAR := $(_dbj_jar))
$(_dbj_odex) : $(_dbj_src_jar) | $(ACP) $(DEXPREOPT) $(DEXOPT)
	@echo "Dexpreopt Boot Jar: $$@"
	$(hide) rm -f $$@
	$(hide) mkdir -p $$(dir $$@)
	$(hide) $(ACP) -fp $$< $$(PRIVATE_DBJ_JAR)
	$$(call dexpreopt-one-file,$$(PRIVATE_DBJ_JAR),$$@)

$(_dbj_jar_no_dex) : $(_dbj_src_jar) | $(ACP) $(AAPT)
	$$(call copy-file-to-target)
	$$(call dexpreopt-remove-classes.dex,$$@)

$(eval _dbj_jar :=)
$(eval _dbj_odex :=)
$(eval _dbj_jar_no_dex :=)
$(eval _dbj_src_jar :=)
endef

$(foreach b,$(DEXPREOPT_BOOT_JARS_MODULES),$(eval $(call _dexpreopt-boot-jar,$(b))))

# dexpreoption for binary released jars
# ONLY enabled in custom release
ifeq ($(PARTIAL_BUILD),true)
  ifeq ($(WITH_DEXPREOPT),true)
$(foreach m,$(DEXPREOPT_BINARY_RELEASE_BOOT_JARS_MODULES), \
    $(eval $(call install-dexpreopt-jars.binary-release,$(m))) \
    $(eval DEXPREOPT_BINARY_RELEASE_BOOT_JARS += \
        $(TARGET_OUT_JAVA_LIBRARIES)/$(m).jar \
        $(TARGET_OUT_JAVA_LIBRARIES)/$(m).odex \
     ) \
 )
  endif
endif

# $(1): the rest list of boot jars
define _build-dexpreopt-boot-jar-dependency-pair
$(if $(filter 1,$(words $(1)))$(filter 0,$(words $(1))),,\
	$(eval _bdbjdp_target := $(DEXPREOPT_BOOT_JAR_DIR_FULL_PATH)/$(word 2,$(1)).odex) \
	$(eval _bdbjdp_dep := $(DEXPREOPT_BOOT_JAR_DIR_FULL_PATH)/$(word 1,$(1)).odex) \
	$(eval $(call add-dependency,$(_bdbjdp_target),$(_bdbjdp_dep))) \
	$(eval $(call _build-dexpreopt-boot-jar-dependency-pair,$(wordlist 2,999,$(1)))))
endef

define _build-dexpreopt-boot-jar-dependency
$(call _build-dexpreopt-boot-jar-dependency-pair,$(DEXPREOPT_BOOT_JARS_MODULES))
endef

$(eval $(call _build-dexpreopt-boot-jar-dependency))
