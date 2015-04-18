LOCAL_PATH := $(call my-dir)  

include $(CLEAR_VARS)  

LOCAL_MODULE := usb

LOCAL_SRC_FILES:= \
	 core.c \
	 descriptor.c \
	 io.c \
	 sync.c \
	 os/linux_usbfs.c \
	 os/threads_posix.c \

include $(BUILD_STATIC_LIBRARY) 