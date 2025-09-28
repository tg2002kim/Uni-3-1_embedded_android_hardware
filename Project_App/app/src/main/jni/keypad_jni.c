#include <jni.h>
#include <fcntl.h>
#include <unistd.h>
#include <linux/input.h>
#include <stdio.h>
#include <errno.h>
#include <android/log.h>
#include <string.h>

#define LOG_TAG "JNI_Keypad"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#define KEYPAD_DEVICE "/dev/input/event4"

// static fd → 한 번만 open
static int fd = -1;

JNIEXPORT jint JNICALL Java_com_example_test5_Device_getKeypadCode(JNIEnv* env, jclass clazz) {
    if (fd < 0) {
        fd = open(KEYPAD_DEVICE, O_RDONLY);
        if (fd < 0) {
            LOGE("키패드 디바이스 열기 실패 (%s): %s", KEYPAD_DEVICE, strerror(errno));
            return -2;
        }
        LOGD("키패드 디바이스 열기 성공: %s", KEYPAD_DEVICE);
    }

    struct input_event ev;
    int rawCode = -1;
    int mappedCode = -1;

    ssize_t bytes = read(fd, &ev, sizeof(ev));
    if (bytes == sizeof(ev)) {
        if (ev.type == EV_KEY && ev.value == 1) {
            rawCode = ev.code;

            // ✅ 매핑 처리
            switch (rawCode) {
                case 2:  mappedCode = 1; break;
                case 3:  mappedCode = 2; break;
                case 4:  mappedCode = 3; break;
                case 5:  mappedCode = 4; break;
                case 6:  mappedCode = 5; break;
                case 7:  mappedCode = 6; break;
                case 8:  mappedCode = 7; break;
                case 9:  mappedCode = 8; break;
                case 10: mappedCode = 9; break;
                case 11: mappedCode = 0; break;
                case 14: mappedCode = 11; break;
                default:
                    mappedCode = -1;  // 처리하지 않는 키
                    break;
            }

            LOGD("키 입력 감지됨: raw=%d → mapped=%d", rawCode, mappedCode);
        }
    } else if (bytes == -1 && (errno == EAGAIN || errno == EWOULDBLOCK)) {
        mappedCode = -1;  // 입력 없음
    } else if (bytes == -1) {
        LOGE("read 실패: %s", strerror(errno));
    } else {
        LOGE("예상치 못한 읽기 크기: %ld", bytes);
    }

    return mappedCode;
}


/*JNIEXPORT jint JNICALL
Java_com_example_test5_Device_getKeypadCode(JNIEnv* env, jclass clazz) {
    static int fd = -1;

    if (fd < 0) {
        fd = open(KEYPAD_DEVICE, O_RDONLY | O_NONBLOCK);
        if (fd < 0) {
            __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "디바이스 열기 실패 (%s): %s", KEYPAD_DEVICE, strerror(errno));
            return -2;
        }
    }

    struct input_event ev;
    int code = -1;

    ssize_t bytes = read(fd, &ev, sizeof(struct input_event));
    if (bytes == sizeof(struct input_event)) {
        if (ev.type == EV_KEY && ev.value == 1) {
            code = ev.code;
            __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "키 입력 감지됨: code = %d", code);
        }
    } else if (bytes == -1 && (errno == EAGAIN || errno == EWOULDBLOCK)) {
        // 입력 없음 → 정상상태
        code = -1;
    } else if (bytes == -1) {
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "read 실패: %s", strerror(errno));
    } else {
        __android_log_print(ANDROID_LOG_WARN, LOG_TAG, "예상치 못한 읽기 크기: %ld", bytes);
    }

    return code;
}*/