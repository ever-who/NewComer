####################################################################
LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

$(warning external/libshowlogo/ enter)


LOCAL_SRC_FILES  := show_logo_common.c\
                  decompress_common.c\
                  show_animation_common.c\
                  charging_animation.cpp

ifeq ($(MTK_PUMP_EXPRESS_SUPPORT), yes)
LOCAL_CFLAGS += -DMTK_PUMP_EXPRESS_SUPPORT
endif
ifeq ($(MTK_PUMP_EXPRESS_PLUS_SUPPORT), yes)
LOCAL_CFLAGS += -DMTK_PUMP_EXPRESS_PLUS_SUPPORT
endif
ifeq ($(MTK_GAUGE_VERSION), 30)
LOCAL_CFLAGS += -DMTK_GM_30
endif

ifdef MTK_CARRIEREXPRESS_PACK
ifneq ($(MTK_CARRIEREXPRESS_PACK), no)
	LOCAL_CFLAGS += -DMTK_CARRIEREXPRESS_PACK
	LOCAL_CPPFLAGS += -DMTK_CARRIEREXPRESS_PACK

ifeq ($(filter OP01, $(subst _, $(space), $(MTK_REGIONAL_OP_PACK))), OP01)
	LOCAL_CFLAGS += -DMTK_CARRIEREXPRESS_PACK_OP01
endif

ifeq ($(filter OP02, $(subst _, $(space), $(MTK_REGIONAL_OP_PACK))), OP02)
	LOCAL_CFLAGS += -DMTK_CARRIEREXPRESS_PACK_OP02
endif

ifeq ($(filter OP09, $(subst _, $(space), $(MTK_REGIONAL_OP_PACK))), OP09)
	LOCAL_CFLAGS += -DMTK_CARRIEREXPRESS_PACK_OP09
endif

ifneq ($(filter NONE, $(subst _, $(space), $(OPTR_SPEC_SEG_DEF))), NONE)
ifeq ($(filter OP01, $(subst _, $(space), $(OPTR_SPEC_SEG_DEF))), OP01)
	LOCAL_CFLAGS += -DGLOBAL_DEVICE_DEFAULT_OPTR=1
endif
ifeq ($(filter OP02, $(subst _, $(space), $(OPTR_SPEC_SEG_DEF))), OP02)
	LOCAL_CFLAGS += -DGLOBAL_DEVICE_DEFAULT_OPTR=2
endif
ifeq ($(filter OP09, $(subst _, $(space), $(OPTR_SPEC_SEG_DEF))), OP09)
	LOCAL_CFLAGS += -DGLOBAL_DEVICE_DEFAULT_OPTR=9
endif
endif

endif
endif

# add by BIRD@hujingcheng 20190829 custom boot logo and animation by secret code
ifeq ($(strip $(BIRD_BOOT_SWITCH)),yes)
$(warning libshowlogo-- makefile BIRD_BOOT_SWITCH=$(BIRD_BOOT_SWITCH))
LOCAL_CFLAGS += -DBIRD_BOOT_SWITCH
endif

LOCAL_SHARED_LIBRARIES := libcutils libutils libc libstdc++ libz libdl liblog libgui libui libsysenv_system libbase libnvram vendor.mediatek.hardware.nvram@1.0_vendor

LOCAL_STATIC_LIBRARIES += libfs_mgr

$(warning libshowlogo MTK_PATH_CUSTOM = $(MTK_PATH_CUSTOM)  MTK_PATH_PLATFORM=$(MTK_PATH_PLATFORM))
LOCAL_C_INCLUDES += $(MTK_PATH_CUSTOM)/lk/include/target
LOCAL_C_INCLUDES += $(MTK_PATH_PLATFORM)/lk/include/target
LOCAL_C_INCLUDES += $(LOCAL_PATH)/include
LOCAL_C_INCLUDES += $(TOP)/external/zlib/
LOCAL_C_INCLUDES += $(TOP)/frameworks/native/libs/nativewindow/include
LOCAL_C_INCLUDES += system/core/fs_mgr/include
LOCAL_C_INCLUDES += $(TOP)/vendor/mediatek/proprietary/external/libsysenv
LOCAL_C_INCLUDES += $(MTK_PATH_SOURCE)/external/nvram/libnvram
LOCAL_C_INCLUDES += $(MTK_PATH_SOURCE)/external/nvram/nvramagentclient

LOCAL_MODULE := libshowlogo
#LOCAL_PROPRIETARY_MODULE := true
#LOCAL_MODULE_OWNER := mtk
LOCAL_MULTILIB := 32

LOCAL_PRELINK_MODULE := false

include $(MTK_SHARED_LIBRARY)
