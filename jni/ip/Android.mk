LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE:= ipthermapp
LOCAL_SRC_FILES := libipthermapp.so
include $(PREBUILT_SHARED_LIBRARY)