LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := native
LOCAL_SRC_FILES := libnative.a

include $(PREBUILT_STATIC_LIBRARY) 

LOCAL_PATH := $(call my-dir)  
  
include $(CLEAR_VARS)  	 
LOCAL_MODULE := usbthermapp 
	 
LOCAL_LDLIBS := -llog
LOCAL_WHOLE_STATIC_LIBRARIES := libnative libusb

include $(BUILD_SHARED_LIBRARY)  

