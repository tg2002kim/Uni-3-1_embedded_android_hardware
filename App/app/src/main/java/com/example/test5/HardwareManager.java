package com.example.test5;

public class HardwareManager {

    private static Device.dot dotThread; // Dot Matrix 출력용 스레드
    private static KeypadThread keypadThread; // Keypad 입력 감지 스레드

    // ─────────────────────────────────────────────
    // ✅ 1. LCD 출력
    public static void showLcdText(String text) {
        Device.lcd(text);
    }

    // ─────────────────────────────────────────────
    // ✅ 2. Segment 제어
    public static void showCountdown(String number) {
        Device.segment(number, "0"); // 고정 모드
    }

    public static void startSegmentCountUp() {
        Device.segment("000000", "1"); // 카운트업
    }

    // ─────────────────────────────────────────────
    // ✅ 3. LED 제어
    public static void showStrikeCount(int count) {
        Device.led(String.valueOf(count));
    }

    // ─────────────────────────────────────────────
    // ✅ 4. Dot Matrix 제어 (스레드 기반)
    public static void startDot(String pattern) {
        stopDot(); // 중복 방지
        dotThread = new Device.dot(pattern);
        dotThread.start();
    }

    public static void stopDot() {
        if (dotThread != null) {
            dotThread.stopThread();
            dotThread = null;
        }
    }

    // ─────────────────────────────────────────────
    // ✅ 5. DIP 스위치 상태 (왼쪽부터 ON인 비트의 위치 반환)
    public static int getHighestDipSwitchOn() {
        String state = Device.dip(); // 예: "0010000000000000"
        for (int i = 0; i < state.length(); i++) {
            if (state.charAt(i) == '1') return i + 1;
        }
        return 0;
    }

    // ─────────────────────────────────────────────
    // ✅ 6. Piezo 사운드 출력 (비동기)
    public static void playSuccessSound() {
        Device.piezo("1");
        new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            Device.piezo("0");
        }).start();
    }

    public static void playFailSound() {
        Device.piezo("2");
        new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            Device.piezo("0");
        }).start();
    }

    // ─────────────────────────────────────────────
    // ✅ 7. Keypad 감지 스레드 관련 정의

    /** 키패드 입력 감지 시작 */
    public static void startKeypadListener(KeypadCallback callback) {
        if (keypadThread != null) stopKeypadListener();
        keypadThread = new KeypadThread(callback);
        keypadThread.start();
    }

    /** 키패드 입력 시 호출되는 콜백 인터페이스 */
    public interface KeypadCallback {
        void onKeyPressed(int keyCode);  // JNI로부터 받은 키코드 전달
    }

    /** 키패드 입력 감지 종료 */
    public static void stopKeypadListener() {
        if (keypadThread != null) {
            keypadThread.interrupt();
            keypadThread = null;
        }
    }

    /** JNI에서 키패드 코드를 주기적으로 읽는 스레드 */
    private static class KeypadThread extends Thread {
        private final KeypadCallback callback;

        public KeypadThread(KeypadCallback callback) {
            this.callback = callback;
        }

        @Override
        public void run() {
            while (true) {
                int key = Device.getKeypadCode();  // JNI 호출
                //if (key >= 0 && key <= 11) {
                //    callback.onKeyPressed(key);
                //}
                callback.onKeyPressed(key);
                try {
                    Thread.sleep(50);  // 50ms 간격으로 폴링
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }
}
