#xunfei tts engine
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_DEX_PREOPT:=false
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := xunfei
LOCAL_SRC_FILES := ./xunfei.apk
LOCAL_MODULE_CLASS := APPS
LOCAL_MODULE_SUFFIX := $(COMMON_ANDROID_PACKAGE_SUFFIX)
LOCAL_CERTIFICATE := PRESIGNED
LOCAL_MODULE_PATH := $(TARGET_OUT)/vendor/operator/app
include $(BUILD_PREBUILT)
