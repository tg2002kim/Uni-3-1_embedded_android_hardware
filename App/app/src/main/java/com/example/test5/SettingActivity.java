package com.example.test5;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;

public class SettingActivity extends Activity {

    private Button soundButton, vibrateButton, backButton;
    private View[] buttons;
    private int selectedIndex = 0;

    private SharedPreferences prefs;
    private boolean isSoundOn, isVibrateOn;

    private HardwareManager.KeypadCallback keypadListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences("GamePrefs", MODE_PRIVATE);

        // 기존 설정 불러오기
        isSoundOn = prefs.getBoolean("sound", true);
        isVibrateOn = prefs.getBoolean("vibrate", true);

        // 버튼 참조
        soundButton = findViewById(R.id.soundButton);
        vibrateButton = findViewById(R.id.vibrateButton);
        backButton = findViewById(R.id.backButton);

        // 버튼 배열 설정 (순서 중요)
        buttons = new View[]{soundButton, vibrateButton, backButton};

        // 초기 텍스트 설정
        updateSoundButtonText();
        updateVibrateButtonText();

        // 사운드 버튼 클릭 처리
        soundButton.setOnClickListener(v -> {
            isSoundOn = !isSoundOn;
            prefs.edit().putBoolean("sound", isSoundOn).apply();
            updateSoundButtonText();

            Device.lcd(isSoundOn ? "Sound ON" : "Sound OFF");

            if (isSoundOn) {
                Device.piezo("1");
                v.postDelayed(() -> {
                    if (!isFinishing()) Device.piezo("0");
                }, 1000);
            } else {
                Device.piezo("0");
            }
        });

        // 진동 버튼 클릭 처리
        vibrateButton.setOnClickListener(v -> {
            isVibrateOn = !isVibrateOn;
            prefs.edit().putBoolean("vibrate", isVibrateOn).apply();
            updateVibrateButtonText();

            Device.lcd(isVibrateOn ? "Vibration ON" : "Vibration OFF");

            if (isVibrateOn) {
                Vibrator vib = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                if (vib != null) vib.vibrate(500);
            }
        });

        // 뒤로가기 버튼
        backButton.setOnClickListener(v -> {
            Device.lcd("");
            Device.led("0");
            Device.segment("000000", "0");
            finish();
        });

        // 초기 안내 메시지
        Device.lcd("Settings");
    }

    @Override
    protected void onResume() {
        super.onResume();
        selectedIndex = 0;
        updateFocus();

        keypadListener = keyCode -> {
            if (keyCode == 0) {
                selectedIndex = (selectedIndex + 1) % buttons.length;
                updateFocus();
            } else if (keyCode == 11) {
                buttons[selectedIndex].performClick();
            }
        };

        HardwareManager.startKeypadListener(keypadListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        HardwareManager.stopKeypadListener();
    }

    private void updateFocus() {
        for (int i = 0; i < buttons.length; i++) {
            buttons[i].setSelected(i == selectedIndex);
        }
    }

    private void updateSoundButtonText() {
        soundButton.setText(isSoundOn ? "사운드 ON" : "사운드 OFF");
    }

    private void updateVibrateButtonText() {
        vibrateButton.setText(isVibrateOn ? "진동 ON" : "진동 OFF");
    }
}
