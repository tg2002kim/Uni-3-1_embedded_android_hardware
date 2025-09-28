LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := device-jni
LOCAL_SRC_FILES := device-jni.c \
                   keypad_jni.c
LOCAL_LDLIBS    := -llog

include $(BUILD_SHARED_LIBRARY)
