package com.example.test5;

public class Device {
    static {
        System.loadLibrary("device-jni");
    }

    // JNI에서 키패드 입력값을 가져오는 함수
    public static native int getKeypadCode();

    // LED 제어: 0~8까지 왼쪽부터 점등, "-1"이면 전체 깜빡임
    public static native void led(String ledValue);

    // DIP Switch 16비트 문자열 반환 ("0010000000000000" 등)
    public static native String dip();

    // Segment 출력: 고정 모드("0") 또는 카운트업("1")
    public static native void segment(String value, String mode);

    // LCD 출력: 최대 32자
    public static native void lcd(String text);

    // Piezo 출력: "0"=정지, "1"=성공음, "2"=실패음
    public static native void piezo(String musicType);

    // Dot Matrix 1회 출력
    public static native void dotWrite(String pattern);

    // Dot Matrix 지속 출력용 Thread
    public static class dot extends Thread {
        private volatile boolean running = true;
        private final String pattern;

        public dot(String pattern) {
            this.pattern = pattern;
        }

        public void stopThread() {
            running = false;
        }

        @Override
        public void run() {
            while (running) {
                dotWrite(pattern);
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
