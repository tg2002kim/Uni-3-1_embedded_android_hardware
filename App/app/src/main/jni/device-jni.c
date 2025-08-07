#include <jni.h>
#include <fcntl.h>
#include <unistd.h>
#include <stdio.h>
#include <string.h>
#include <stdint.h>
#include <sys/mman.h>
#include <linux/input.h>
#include <pthread.h>
#include <stdlib.h>

#define LED_PATH    "/dev/fpga_led"
#define DIP_PATH    "/dev/fpga_dipsw"
#define SEG_PATH    "/dev/fpga_segment"
#define DOT_PATH    "/dev/fpga_dotmatrix"
#define LCD_PATH    "/dev/fpga_textlcd"
#define PIEZO_PATH  "/dev/fpga_piezo"

static JavaVM* g_vm;
static jobject g_listener = NULL;
static pthread_t key_thread;
static int running = 0;

unsigned char numbers[16][10] = {
        {0x00,0x3E,0x22,0x3E,0x00,0x00,0x00,0x00,0x3E,0x00},
        {0x00,0x3E,0x22,0x3E,0x00,0x00,0x3A,0x2A,0x2E,0x00},
        {0x00,0x3E,0x22,0x3E,0x00,0x00,0x2A,0x2A,0x3E,0x00},
        {0x00,0x3E,0x22,0x3E,0x00,0x00,0x0E,0x08,0x3E,0x00},
        {0x00,0x3E,0x22,0x3E,0x00,0x00,0x2E,0x2A,0x3A,0x00},
        {0x00,0x3E,0x22,0x3E,0x00,0x00,0x3E,0x2A,0x3A,0x00},
        {0x00,0x3E,0x22,0x3E,0x00,0x00,0x02,0x02,0x3E,0x00},
        {0x00,0x3E,0x22,0x3E,0x00,0x00,0x3E,0x2A,0x3E,0x00},
        {0x00,0x3E,0x22,0x3E,0x00,0x00,0x2E,0x2A,0x3E,0x00},
        {0x00,0x00,0x00,0x3E,0x00,0x00,0x3E,0x22,0x3E,0x00},
        {0x00,0x00,0x00,0x3E,0x00,0x00,0x00,0x00,0x3E,0x00},
        {0x00,0x00,0x00,0x3E,0x00,0x00,0x3A,0x2A,0x2E,0x00},
        {0x00,0x00,0x00,0x3E,0x00,0x00,0x2A,0x2A,0x3E,0x00},
        {0x00,0x00,0x00,0x3E,0x00,0x00,0x0E,0x08,0x3E,0x00},
        {0x00,0x00,0x00,0x3E,0x00,0x00,0x2E,0x2A,0x3A,0x00},
        {0x00,0x00,0x00,0x3E,0x00,0x00,0x3E,0x2A,0x3A,0x00}
};

JNIEXPORT void JNICALL Java_com_example_test5_Device_led(JNIEnv* env, jclass clazz, jstring jstr) {
    const char* val = (*env)->GetStringUTFChars(env, jstr, NULL);
    int level = atoi(val);
    unsigned char output = 0;
    for (int i = 0; i < level; i++) output |= (1 << i);  // 왼쪽부터 점등
    int fd = open(LED_PATH, O_WRONLY);
    if (fd >= 0) {
        write(fd, &output, 1);
        close(fd);
    }
    (*env)->ReleaseStringUTFChars(env, jstr, val);
}

JNIEXPORT jstring JNICALL Java_com_example_test5_Device_dip(JNIEnv* env, jclass clazz) {
    int fd = open(DIP_PATH, O_RDONLY);
    if (fd < 0) return (*env)->NewStringUTF(env, "0000000000000000");
    unsigned short value[2] = {0};
    read(fd, &value, 4);
    close(fd);
    uint16_t dip = (value[1] << 8) | value[0];
    int idx_map[16] = {4,5,6,7, 0,1,2,3, 12,13,14,15, 8,9,10,11};
    char buf[17] = {0};
    for (int i = 0; i < 16; i++) buf[i] = ((dip >> idx_map[i]) & 1) ? '1' : '0';
    return (*env)->NewStringUTF(env, buf);
}

void countUpSegment() {
    int fd = open(SEG_PATH, O_WRONLY);
    if (fd < 0) return;
    for (int i = 0; i <= 999999; i++) {
        write(fd, &i, sizeof(int));
        usleep(50000); // 0.05초 간격
    }
    close(fd);
}

JNIEXPORT void JNICALL Java_com_example_test5_Device_segment(JNIEnv* env, jclass clazz, jstring jstr, jstring modeStr) {
    const char* data = (*env)->GetStringUTFChars(env, jstr, NULL);
    const char* mode = (*env)->GetStringUTFChars(env, modeStr, NULL);
    if (strcmp(mode, "1") == 0) {
        countUpSegment();
    } else {
        int value = atoi(data);
        int fd = open(SEG_PATH, O_WRONLY);
        if (fd >= 0) {
            write(fd, &value, sizeof(int));
            close(fd);
        }
    }
    (*env)->ReleaseStringUTFChars(env, jstr, data);
    (*env)->ReleaseStringUTFChars(env, modeStr, mode);
}

JNIEXPORT void JNICALL Java_com_example_test5_Device_dotWrite(JNIEnv* env, jclass clazz, jstring jstr) {
    const char* input = (*env)->GetStringUTFChars(env, jstr, NULL);
    char output[21] = {0};
    int num = atoi(input);
    if (num == 0) {
        memset(output, '0', 20);  // all off
    } else if (num >= 1 && num <= 16) {
        unsigned char* d = numbers[num - 1];
        for (int i = 0; i < 10; i++) sprintf(&output[i * 2], "%02X", d[i]);
    }
    int fd = open(DOT_PATH, O_WRONLY);
    if (fd >= 0) {
        write(fd, output, 20);
        close(fd);
    }
    (*env)->ReleaseStringUTFChars(env, jstr, input);
}

JNIEXPORT void JNICALL Java_com_example_test5_Device_lcd(JNIEnv* env, jclass clazz, jstring jstr) {
    const char* text = (*env)->GetStringUTFChars(env, jstr, NULL);
    int fd = open(LCD_PATH, O_WRONLY);
    if (fd >= 0) {
        write(fd, text, strlen(text));
        close(fd);
    }
    (*env)->ReleaseStringUTFChars(env, jstr, text);
}

JNIEXPORT void JNICALL Java_com_example_test5_Device_piezo(JNIEnv* env, jclass clazz, jstring jstr) {
    const char* value = (*env)->GetStringUTFChars(env, jstr, NULL);
    int fd = open(PIEZO_PATH, O_WRONLY);
    if (fd >= 0) {
        write(fd, value, 1);
        close(fd);
    }
    (*env)->ReleaseStringUTFChars(env, jstr, value);
}


