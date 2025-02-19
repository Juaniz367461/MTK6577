#
# Copyright (C) 2007 The Android Open Source Project
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

# This is a generic product that isn't specialized for a specific device.
# It includes the base Android platform.

PRODUCT_PACKAGES := \
    AccountAndSyncSettings \
    DeskClock \
    AlarmProvider \
    Bluetooth \
    Calculator \
    Calendar \
    CertInstaller \
    DrmProvider \
    Email \
    Exchange \
    LatinIME \
    Music \
    MusicFX \
    Protips \
    QuickSearchBox \
    Settings \
    ChangeLogo \
    Sync \
    SystemUI \
    Updater \
    CalendarProvider \
    SyncProvider \
    Launcher2 \
    mediatek-res \
    disableapplist.txt \
    AtciService \
    MTKThermalManager \
    thermal_manager \
    CDS_INFO

ifeq ($(strip $(MTK_DATAREG_APP)),yes)
  PRODUCT_PACKAGES += DataReg
  PRODUCT_PACKAGES += DataRegSecrets
  PRODUCT_PACKAGES += DataRegDefault.properties
endif

ifeq ($(strip $(MTK_EMULATOR_SUPPORT)),yes)
  PRODUCT_PACKAGES += SDKGallery
else
  PRODUCT_PACKAGES += Gallery2
endif

ifneq ($(strip $(MTK_EMULATOR_SUPPORT)),yes)
  PRODUCT_PACKAGES += Provision
endif

ifeq ($(strip $(HAVE_CMMB_FEATURE)), yes)
  PRODUCT_PACKAGES += CMMBPlayer 
endif

ifneq ($(findstring OP03, $(strip $(OPTR_SPEC_SEG_DEF))),)
  PRODUCT_PACKAGES += SimCardAuthenticationService
endif

ifdef MTK_WEATHER_PROVIDER_APP
  ifneq ($(strip $(MTK_WEATHER_PROVIDER_APP)), no)
    PRODUCT_PACKAGES += MtkWeatherProvider MtkWeatherSetting
  endif
endif

ifeq ($(strip $(MTK_CALENDAR_IMPORTER_APP)), yes)
  PRODUCT_PACKAGES += CalendarImporter 
endif

ifeq ($(strip $(MTK_THEMEMANAGER_APP)), yes)
  PRODUCT_PACKAGES += theme-res-mint \
                      theme-res-mocha \
                      theme-res-raspberry
endif

ifeq ($(strip $(MTK_VIDEOPLAYER_APP)), yes)
  PRODUCT_PACKAGES += VideoPlayer
endif

ifeq ($(strip $(MTK_VIDEOPLAYER2_APP)), yes)
  PRODUCT_PACKAGES += VideoPlayer2
endif

ifeq ($(strip $(MTK_GALLERY3D_APP)), yes)
  PRODUCT_PACKAGES += Gallery3D
endif

ifeq ($(strip $(MTK_ENABLE_VIDEO_EDITOR)),yes)
  PRODUCT_PACKAGES += VideoEditor
endif

ifeq ($(strip $(MTK_LOG2SERVER_APP)), yes)
  PRODUCT_PACKAGES += Log2Server
endif

ifeq ($(strip $(MTK_GALLERY_APP)), yes)
  PRODUCT_PACKAGES += Gallery
endif

ifeq ($(strip $(MTK_WEATHER3D_WIDGET)), yes)
  PRODUCT_PACKAGES += Weather3DWidget
endif

ifeq ($(strip $(MTK_INPUTMETHOD_PINYINIME_APP)), yes)
  PRODUCT_PACKAGES += PinyinIME
  PRODUCT_PACKAGES += libjni_pinyinime
endif

ifeq ($(strip $(MTK_CAMERA_APP)), yes)
  PRODUCT_PACKAGES += CameraOpen
else
  PRODUCT_PACKAGES += Camera
endif

ifeq ($(strip $(MTK_VIDEOWIDGET_APP)),yes)
  PRODUCT_PACKAGES += MtkVideoWidget
endif

ifeq ($(strip $(GEMINI)), yes)
  PRODUCT_PACKAGES += Stk1 \
                      Stk2
else
  PRODUCT_PACKAGES += Stk
endif

ifeq ($(strip $(MTK_ENGINEERMODE_APP)), yes)
  PRODUCT_PACKAGES += EngineerMode \
                      EngineerModeSim
endif

ifeq ($(strip $(MTK_GPS_SUPPORT)), yes)
  PRODUCT_PACKAGES += YGPS
endif

ifeq ($(strip $(MTK_LIVEWALLPAPER_APP)), yes)
  PRODUCT_PACKAGES += LiveWallpapers \
                      LiveWallpapersPicker \
                      MagicSmokeWallpapers \
                      VisualizationWallpapers \
                      Galaxy4 \
                      HoloSpiralWallpaper \
                      NoiseField \
                      PhaseBeam 
endif

ifeq ($(strip $(MTK_VLW_APP)), yes)
  PRODUCT_PACKAGES += MtkVideoLiveWallpaper
endif

ifeq ($(strip $(MTK_SNS_SUPPORT)), yes)
  PRODUCT_PACKAGES += SNSCommon \
                      SnsContentProvider \
                      SnsWidget \
                      SnsWidget24 \
                      SocialStream       
  ifeq ($(strip $(MTK_SNS_KAIXIN_APP)), yes)
    PRODUCT_PACKAGES += KaiXinAccountService
  endif
  ifeq ($(strip $(MTK_SNS_RENREN_APP)), yes)
    PRODUCT_PACKAGES += RenRenAccountService
  endif
  ifeq ($(strip $(MTK_SNS_FACEBOOK_APP)), yes)
    PRODUCT_PACKAGES += FacebookAccountService
  endif
  ifeq ($(strip $(MTK_SNS_FLICKR_APP)), yes)
    PRODUCT_PACKAGES += FlickrAccountService
  endif
  ifeq ($(strip $(MTK_SNS_TWITTER_APP)), yes)
    PRODUCT_PACKAGES += TwitterAccountService
  endif
  ifeq ($(strip $(MTK_SNS_SINAWEIBO_APP)), yes)
    PRODUCT_PACKAGES += WeiboAccountService
  endif
endif

ifeq ($(strip $(MTK_GOOGLEOTA_SUPPORT)), yes)
  PRODUCT_PACKAGES += GoogleOta \
                      GoogleOtaSysOper
endif

ifeq ($(strip $(MTK_DATADIALOG_APP)), yes)
  PRODUCT_PACKAGES += DataDialog
endif

ifeq ($(strip $(MTK_FM_SUPPORT)), yes)
  PRODUCT_PACKAGES += FMRadio
endif

ifeq (MT6620_FM,$(strip $(MTK_FM_CHIP)))
    PRODUCT_PROPERTY_OVERRIDES += \
        fmradio.driver.chip=1
endif

ifeq (MT6626_FM,$(strip $(MTK_FM_CHIP)))
    PRODUCT_PROPERTY_OVERRIDES += \
        fmradio.driver.chip=2
endif

ifeq (MT6628_FM,$(strip $(MTK_FM_CHIP)))
    PRODUCT_PROPERTY_OVERRIDES += \
        fmradio.driver.chip=3
endif

ifeq ($(strip $(MTK_BT_SUPPORT)), yes)
  PRODUCT_PACKAGES += MtkBt
endif

ifeq ($(strip $(MTK_DT_SUPPORT)),yes)
    ifneq ($(strip $(EVDO_DT_SUPPORT)),yes)
        ifeq ($(strip $(MTK_MDLOGGER_SUPPORT)),yes)
            PRODUCT_PACKAGES += \
                ExtModemLog
        endif
    endif
endif

ifeq ($(strip $(MTK_STEREO3D_WALLPAPER_APP)), yes)
  PRODUCT_PACKAGES += Stereo3DWallpaper                                          
endif

ifeq ($(strip $(MTK_EXTERNAL_MODEM_SLOT)),2)
  PRODUCT_PROPERTY_OVERRIDES += \
  ril.external.md=2
else
  ifeq ($(strip $(MTK_EXTERNAL_MODEM_SLOT)),1)
    PRODUCT_PROPERTY_OVERRIDES += \
    ril.external.md=1
  else
    PRODUCT_PROPERTY_OVERRIDES += \
    ril.external.md=0
  endif
endif

ifeq ($(strip $(MTK_ACWFDIALOG_APP)), yes)
  PRODUCT_PACKAGES += AcwfDialog
endif

ifeq ($(strip $(MTK_ENGINEERMODE_APP)), yes)
  PRODUCT_PACKAGES += EngineerMode \
                      MobileLog
endif

ifeq ($(strip $(MTK_RCSE_SUPPORT)), yes)
    PRODUCT_PACKAGES += Rcse
    PRODUCT_PACKAGES += Provisioning
endif

ifeq ($(strip $(HAVE_MATV_FEATURE)),yes)
  PRODUCT_PACKAGES += MtvPlayer \
                      MATVEM
endif

ifneq ($(strip $(MTK_LCM_PHYSICAL_ROTATION)),)
  PRODUCT_PROPERTY_OVERRIDES += \
    ro.sf.hwrotation=$(MTK_LCM_PHYSICAL_ROTATION)
endif

ifeq ($(strip $(MTK_SHARE_MODEM_CURRENT)),2)
  PRODUCT_PROPERTY_OVERRIDES += \
    ril.current.share_modem=2
else
  ifeq ($(strip $(MTK_SHARE_MODEM_CURRENT)),1)
    PRODUCT_PROPERTY_OVERRIDES += \
      ril.current.share_modem=1
  else
    PRODUCT_PROPERTY_OVERRIDES += \
      ril.current.share_modem=0
  endif
endif

ifeq ($(strip $(MTK_FM_TX_SUPPORT)), yes)
  PRODUCT_PACKAGES += FMTransmitter
endif

ifeq ($(strip $(MTK_SOUNDRECORDER_APP)),yes)
  PRODUCT_PACKAGES += SoundRecorder
endif

ifeq ($(strip $(MTK_DM_APP)),yes)
  PRODUCT_PACKAGES += dm 
endif

ifeq ($(strip $(MTK_LAUNCHERPLUS_APP)),yes)
  PRODUCT_PACKAGES += LauncherPlus \
                      MoreApp
  PRODUCT_PROPERTY_OVERRIDES += \
    launcherplus.allappsgrid=2d
endif

ifeq ($(strip $(MTK_LAUNCHER_ALLAPPSGRID)), yes)
  PRODUCT_PROPERTY_OVERRIDES += \
          launcher2.allappsgrid=3d_20
endif

ifeq ($(strip $(MTK_LOCKSCREEN_TYPE)),2)
  PRODUCT_PACKAGES += MtkWallPaper
endif

ifneq ($(strip $(MTK_LOCKSCREEN_TYPE)),)
  PRODUCT_PROPERTY_OVERRIDES += \
    curlockscreen=$(MTK_LOCKSCREEN_TYPE)
endif

ifeq ($(strip $(MTK_IME_SUPPORT)),yes)
  PRODUCT_PACKAGES += MediatekIME
endif

ifeq ($(strip $(MTK_ANDROIDFACTORYMODE_APP)),yes)
  PRODUCT_PACKAGES += AndroidFactoryMode
endif

ifeq ($(strip $(MTK_OMA_DOWNLOAD_SUPPORT)),yes)
  PRODUCT_PACKAGES += Browser \
                      DownloadProvider
endif

ifeq ($(strip $(MTK_OMACP_SUPPORT)),yes)
  PRODUCT_PACKAGES += Omacp
endif

ifeq ($(strip $(MTK_WIFI_P2P_SUPPORT)),yes)
  PRODUCT_PACKAGES += \
    WifiContactSync \
    WifiP2PWizardy \
    FileSharingServer \
    FileSharingClient \
    UPnPAV \
    WifiWsdsrv \
    bonjourExplorer
endif

ifeq ($(strip $(MTK_MDLOGGER_SUPPORT)),yes)
  PRODUCT_PACKAGES += \
    ModemLog
endif

ifeq ($(strip $(CUSTOM_KERNEL_TOUCHPANEL)),generic)
  PRODUCT_PACKAGES += Calibrator
endif

ifeq ($(strip $(MTK_FILEMANAGER_APP)), yes)
  PRODUCT_PACKAGES += FileManager
endif

ifeq ($(strip $(MTK_MEDIA3D_APP)), yes)
    PRODUCT_PACKAGES +=  Media3D
endif

ifeq ($(strip $(MTK_ENGINEERMODE_APP)), yes)
  PRODUCT_PACKAGES += ActivityNetwork
endif

ifeq ($(strip $(MTK_NFC_SUPPORT)), yes)
  PRODUCT_PACKAGES += NxpSecureElement
endif

ifeq ($(strip $(MTK_APKINSTALLER_APP)), yes)
  PRODUCT_PACKAGES += APKInstaller
endif

ifeq ($(strip $(MTK_SMSREG_APP)), yes)
  PRODUCT_PACKAGES += SmsReg
endif

ifeq ($(strip $(MTK_DT_SUPPORT)),yes)
  PRODUCT_PACKAGES += ip-up \
                      ip-down \
                      ppp_options \
                      chap-secrets \
                      init.gprs-pppd
endif

ifeq ($(strip $(GEMINI)),yes)
  PRODUCT_PROPERTY_OVERRIDES += \
    ro.mediatek.gemini_support=true
else
  PRODUCT_PROPERTY_OVERRIDES += \
    ro.mediatek.gemini_support=false
endif

ifeq ($(strip $(MTK_ENGINEERMODE_INTERNAL_APP)), yes)
  PRODUCT_PACKAGES += InternalEngineerMode
endif

ifeq ($(strip $(MTK_YMCAPROP_SUPPORT)),yes)
  PRODUCT_COPY_FILES += mediatek/source/packages/yahoo_tracking/ymca.properties:system/yahoo/com.yahoo.mobile.client.android.news/ymca.properties
endif

ifeq ($(MTK_BACKUPANDRESTORE_APP),yes)
  PRODUCT_PACKAGES += BackupAndRestore
endif

ifeq ($(strip $(MTK_NOTEBOOK_SUPPORT)),yes)
  PRODUCT_PACKAGES += NoteBook 
endif

ifeq ($(strip $(MTK_TODOS_APP)), yes)
  PRODUCT_PACKAGES += Todos
endif

ifeq ($(strip $(MTK_MDM_APP)),yes)
  PRODUCT_PACKAGES += MediatekDM
endif

ifneq ($(findstring OP03, $(strip $(OPTR_SPEC_SEG_DEF))),)
  PRODUCT_PACKAGES += SimCardAuthenticationService
endif

ifdef OPTR_SPEC_SEG_DEF
  ifneq ($(strip $(OPTR_SPEC_SEG_DEF)),NONE)
    OPTR := $(word 1,$(subst _,$(space),$(OPTR_SPEC_SEG_DEF)))
    SPEC := $(word 2,$(subst _,$(space),$(OPTR_SPEC_SEG_DEF)))
    SEG  := $(word 3,$(subst _,$(space),$(OPTR_SPEC_SEG_DEF)))

# Todo:
# obsolete this section's configuration for operator project resource overlay
# once all operator related overlay resource moved to custom folder
    PRODUCT_PACKAGE_OVERLAYS += mediatek/source/operator/$(OPTR)/$(SPEC)/$(SEG)/OverLayResource
# End

    PRODUCT_PROPERTY_OVERRIDES += \
      ro.operator.optr=$(OPTR) \
      ro.operator.spec=$(SPEC) \
      ro.operator.seg=$(SEG)
  endif
endif

ifeq (yes,$(strip $(MTK_FD_SUPPORT)))
    PRODUCT_PROPERTY_OVERRIDES += \
        persist.radio.fd.counter=20
    PRODUCT_PROPERTY_OVERRIDES += \
        persist.radio.fd.off.counter=20
endif

ifeq ($(strip $(MTK_WVDRM_SUPPORT)),yes)
  PRODUCT_PROPERTY_OVERRIDES += \
    drm.service.enabled=true
  PRODUCT_PACKAGES += \
    com.google.widevine.software.drm.xml \
    com.google.widevine.software.drm \
    libdrmwvmplugin \
    libwvm \
    libWVStreamControlAPI_L3 \
    libwvdrm_L3
else
  PRODUCT_PROPERTY_OVERRIDES += \
    drm.service.enabled=false
endif

ifeq (yes,$(strip $(MTK_FM_SUPPORT)))
    PRODUCT_PROPERTY_OVERRIDES += \
        fmradio.driver.enable=1
else
    PRODUCT_PROPERTY_OVERRIDES += \
        fmradio.driver.enable=0
endif

ifeq ($(EVDO_DT_VIA_SUPPORT),yes)
-include $(TOPDIR)viatelecom/via_config.mk
-include $(TOPDIR)hardware/ril/viatelecom/via_config.mk
endif

ifeq ($(strip $(EVDO_DT_VIA_SUPPORT)), yes)
    PRODUCT_PACKAGES += Utk
endif

ifeq ($(strip $(MTK_EXTERNAL_MODEM_SLOT)),2)
  PRODUCT_PROPERTY_OVERRIDES += \
  ril.external.md=2
else
  ifeq ($(strip $(MTK_EXTERNAL_MODEM_SLOT)),1)
    PRODUCT_PROPERTY_OVERRIDES += \
    ril.external.md=1
  else
    PRODUCT_PROPERTY_OVERRIDES += \
    ril.external.md=0
  endif
endif

# Get the TTS language packs
$(call inherit-product-if-exists, external/svox/pico/lang/all_pico_languages.mk)

#
# MediaTek resource overlay configuration
#
$(foreach cf,$(RESOURCE_OVERLAY_SUPPORT), \
  $(eval # do NOT modify the overlay resource paths order) \
  $(eval # 1. project level resource overlay) \
  $(eval _project_overlay_dir := $(MTK_ROOT_CUSTOM)/$(TARGET_PRODUCT)/resource_overlay/$(cf)) \
  $(if $(wildcard $(_project_overlay_dir)), \
    $(eval PRODUCT_PACKAGE_OVERLAYS += $(_project_overlay_dir)) \
    , \
   ) \
  $(eval # 2. operator spec. resource overlay) \
  $(eval _operator_overlay_dir := $(MTK_ROOT_CUSTOM)/$(word 1,$(subst _, ,$(OPTR_SPEC_SEG_DEF)))/resource_overlay/$(cf)) \
  $(if $(wildcard $(_operator_overlay_dir)), \
    $(eval PRODUCT_PACKAGE_OVERLAYS += $(_operator_overlay_dir)) \
    , \
   ) \
  $(eval # 3. product line level resource overlay) \
  $(eval _product_line_overlay_dir := $(MTK_ROOT_CUSTOM)/$(PRODUCT)/resource_overlay/$(cf)) \
  $(if $(wildcard $(_product_line_overlay_dir)), \
    $(eval PRODUCT_PACKAGE_OVERLAYS += $(_product_line_overlay_dir)) \
    , \
   ) \
  $(eval # 4. common level(v.s android default) resource overlay) \
  $(eval _common_overlay_dir := $(MTK_ROOT_CUSTOM)/common/resource_overlay/$(cf)) \
  $(if $(wildcard $(_common_overlay_dir)), \
    $(eval PRODUCT_PACKAGE_OVERLAYS += $(_common_overlay_dir)) \
    , \
   ) \
 )

ifeq (yes,$(strip $(MTK_NFC_SUPPORT)))
  PRODUCT_COPY_FILES += frameworks/base/data/etc/android.hardware.nfc.xml:system/etc/permissions/android.hardware.nfc.xml \
                        frameworks/base/nfc-extras/com.android.nfc_extras.xml:system/etc/permissions/com.android.nfc_extras.xml
  PRODUCT_PACKAGES += Nfc \
		      Tag \
                      nfc.default
  PRODUCT_PROPERTY_OVERRIDES += \
    ro.nfc.port=I2C
endif

ifeq ($(strip $(HAVE_SRSAUDIOEFFECT_FEATURE)),yes)
  PRODUCT_PACKAGES += SRSTruMedia
endif

ifeq ($(strip $(MTK_WEATHER_WIDGET_APP)), yes)
    PRODUCT_PACKAGES += MtkWeatherWidget
endif

ifeq ($(strip $(MTK_WORLD_CLOCK_WIDGET_APP)), yes)
    PRODUCT_PACKAGES += MtkWorldClockWidget
endif

ifeq ($(strip $(MTK_AGPS_APP)), yes)
  PRODUCT_PACKAGES += LocationEM
  PRODUCT_COPY_FILES += mediatek/source/frameworks/epo/etc/epo_conf.xml:system/etc/epo_conf.xml
  PRODUCT_COPY_FILES += mediatek/source/frameworks/agps/etc/agps_profiles_conf.xml:system/etc/agps_profiles_conf.xml
endif

PRODUCT_BRAND := alps
PRODUCT_MANUFACTURER := alps

PRODUCT_COPY_FILES += mediatek/source/frameworks/telephony/etc/apns-conf.xml:system/etc/apns-conf.xml
PRODUCT_COPY_FILES += mediatek/source/frameworks/telephony/etc/spn-conf.xml:system/etc/spn-conf.xml

# for location service
PRODUCT_COPY_FILES += mediatek/source/frameworks/base/location/location_service_conf.xml:system/etc/location_service_conf.xml
PRODUCT_COPY_FILES += mediatek/source/frameworks/base/location/com.mediatek.location.provider.xml:system/etc/permissions/com.mediatek.location.provider.xml
PRODUCT_PACKAGES += MediaTekLocationProvider
PRODUCT_PACKAGES += com.mediatek.location.provider
# for USB Accessory Library/permission
PRODUCT_COPY_FILES += frameworks/base/data/etc/android.hardware.usb.accessory.xml:system/etc/permissions/android.hardware.usb.accessory.xml
PRODUCT_PACKAGES += com.android.future.usb.accessory

# for Modem database
ifeq ($(strip $(MTK_INCLUDE_MODEM_DB_IN_IMAGE)), yes)
  ifeq ($(filter generic banyan_addon,$(PROJECT)),)
    MD_DATABASE_FILE := $(if $(wildcard mediatek/custom/common/modem/$(strip $(CUSTOM_MODEM))/BPLGUInfoCustomAppSrcP_*), $(wildcard mediatek/custom/common/modem/$(strip $(CUSTOM_MODEM))/BPLGUInfoCustomAppSrcP_*), $(wildcard mediatek/custom/common/modem/$(strip $(CUSTOM_MODEM))/BPLGUInfoCustomApp_*))
    ifneq ($(words $(MD_DATABASE_FILE)),1)
      $(error More than one modem database file: $(MD_DATABASE_FILE)!!)
    endif
    MD_DATABASE_FILENAME := $(notdir $(MD_DATABASE_FILE))
    PRODUCT_COPY_FILES += $(strip $(MD_DATABASE_FILE)):system/etc/mddb/$(strip $(MD_DATABASE_FILENAME))
  endif
endif

ifeq ($(strip $(MTK_WLAN_SUPPORT)),yes)
  PRODUCT_PROPERTY_OVERRIDES += \
    mediatek.wlan.chip=$(MTK_WLAN_CHIP)

  ifeq ($(strip $(MTK_WLAN_CHIP)),MT6620)
    PRODUCT_PROPERTY_OVERRIDES += \
      mediatek.wlan.module.postfix=""
  else
    PRODUCT_PROPERTY_OVERRIDES += \
      mediatek.wlan.module.postfix="_"$(shell echo $(strip $(MTK_WLAN_CHIP)) | tr A-Z a-z)
  endif
endif

ifeq ($(strip $(MTK_RILD_READ_IMSI)),yes)
  PRODUCT_PROPERTY_OVERRIDES += \
  ril.read.imsi=1
endif

# for ext modem database
ifeq ($(strip $(MTK_INCLUDE_EXT_MODEM_DB_IN_IMAGE)), yes)
  ifeq ($(filter generic banyan_addon,$(PROJECT)),)
    MD_DATABASE_FILE := $(if $(wildcard mediatek/custom/common/modem/$(strip $(CUSTOM_MODEM))/ExtModem/BPLGUInfoCustomAppSrcP_*), $(wildcard mediatek/custom/common/modem/$(strip $(CUSTOM_MODEM))/ExtModem/BPLGUInfoCustomAppSrcP_*), $(wildcard mediatek/custom/common/modem/$(strip $(CUSTOM_MODEM))/ExtModem/BPLGUInfoCustomApp_*))
    ifneq ($(words $(MD_DATABASE_FILE)),1)
      $(error More than one modem database file: $(MD_DATABASE_FILE)!!)
    endif
    MD_DATABASE_FILENAME := $(notdir $(MD_DATABASE_FILE))
    PRODUCT_COPY_FILES += $(strip $(MD_DATABASE_FILE)):system/etc/extmddb/$(strip $(MD_DATABASE_FILENAME))
  endif
endif

# System property for MediaTek ANR pre-dump.
PRODUCT_PROPERTY_OVERRIDES += dalvik.vm.mtk-stack-trace-file=/data/anr/mtk_traces.txt

$(call inherit-product, $(SRC_TARGET_DIR)/product/core.mk)
$(call inherit-product-if-exists, frameworks/base/data/fonts/fonts.mk)
$(call inherit-product-if-exists, external/lohit-fonts/fonts.mk)
$(call inherit-product-if-exists, frameworks/base/data/keyboards/keyboards.mk)
# Get the TTS language packs
$(call inherit-product-if-exists, external/svox/pico/lang/all_pico_languages.mk)
