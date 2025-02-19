LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

mtklog_config_prop_file := $(TARGET_OUT)/etc/mtklog-config.prop
ifeq ($(TARGET_BUILD_VARIANT),eng)
mtklog_config_prop_src := $(LOCAL_PATH)/mtklog-config-eng.prop
else
mtklog_config_prop_src := $(LOCAL_PATH)/mtklog-config-user.prop
endif

$(mtklog_config_prop_file): PRIVATE_SRC_FILES := $(mtklog_config_prop_src)
$(mtklog_config_prop_file): $(mtklog_config_prop_file) | $(ACP)
	mkdir -p $(dir $@)
	$(ACP) $(PRIVATE_SRC_FILES) $(mtklog_config_prop_file)

xlog_filter_tags_file := $(TARGET_OUT)/etc/xlog-filter-tags
xlog_filter_default_file := $(TARGET_OUT)/etc/xlog-filter-default
xlog_filter_tags_src := $(LOCAL_PATH)/tags-default.xlog $(LOCAL_PATH)/tags-setting.xlog

$(xlog_filter_tags_file): PRIVATE_SRC_FILES := $(xlog_filter_tags_src)
$(xlog_filter_tags_file): $(xlog_filter_tags_src)
	mkdir -p $(dir $@)
	mediatek/source/external/xlog/tools/merge-xlog-filter-tags.py -t $@ $(PRIVATE_SRC_FILES)

$(xlog_filter_default_file): PRIVATE_SRC_FILES := $(xlog_filter_tags_src)
$(xlog_filter_default_file): $(xlog_filter_tags_src)
	mkdir -p $(dir $@)
	mediatek/source/external/xlog/tools/merge-xlog-filter-tags.py -f $@ $(PRIVATE_SRC_FILES)

ALL_DEFAULT_INSTALLED_MODULES += $(xlog_filter_tags_file) $(xlog_filter_default_file) $(mtklog_config_prop_file)
