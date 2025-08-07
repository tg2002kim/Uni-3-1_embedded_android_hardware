package com.example.test5;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {

    private Button startButton;
    private Button guideButton;
    private Button settingsButton;
    private Button exitButton;

    private Button[] buttons; // 버튼 배열
    private int selectedIndex = 0; // 현재 선택된 버튼 인덱스

    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    private static boolean isFirstLaunch = true; // ✅ 최초 실행 체크 플래그

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startButton = findViewById(R.id.btnStart);
        guideButton = findViewById(R.id.btnGuide);
        settingsButton = findViewById(R.id.btnSettings);
        exitButton = findViewById(R.id.btnExit);

        // 버튼 배열 구성
        buttons = new Button[]{startButton, guideButton, settingsButton, exitButton};

        // 초기 포커스 설정
        updateButtonFocus();

        // 버튼 클릭 리스너들 유지
        startButton.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, StageActivity.class);
            startActivity(intent);
        });

        guideButton.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, GuideActivity.class);
            startActivity(intent);
        });

        settingsButton.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, SettingActivity.class);
            startActivity(intent);
        });

        exitButton.setOnClickListener(view -> {
            Device.lcd("");
            Device.led("0");
            Device.segment("000000", "0");
            finish();
        });

        // 하드웨어 연동 초기화
        Device.lcd("Welcome!");
        Device.piezo("0"); // 짧은 사운드 재생

        // ✅ 최초 실행 시에만 su 권한 요청
        if (isFirstLaunch) {
            isFirstLaunch = false;
            requestSuPermission();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 키패드 리스너 시작
        HardwareManager.startKeypadListener(keyCode -> {
            if (keyCode == 0) { // 다음 버튼으로 이동
                selectedIndex = (selectedIndex + 1) % buttons.length;
                uiHandler.post(this::updateButtonFocus);
            } else if (keyCode == 11) { // 현재 버튼 실행
                uiHandler.post(() -> buttons[selectedIndex].performClick());
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        HardwareManager.stopKeypadListener();
    }

    // 버튼 포커스 표시 함수
    private void updateButtonFocus() {
        for (int i = 0; i < buttons.length; i++) {
            buttons[i].setSelected(i == selectedIndex);
        }
    }

    // ✅ su 권한 요청 함수 (최초 1회만)
    private void requestSuPermission() {
        new Thread(() -> {
            try {
                Process su = Runtime.getRuntime().exec("su");
                su.getOutputStream().write("chmod 666 /dev/input/event4\n".getBytes());
                su.getOutputStream().write("chmod 666 /dev/fpga_*\n".getBytes());
                su.getOutputStream().flush();
                su.getOutputStream().close();
                su.waitFor();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
