package com.example.test5;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.test5.Device.dot;

public class ResultActivity extends Activity {

    private TextView resultText, timeText, strikeText;
    private Button retryButton, nextButton, homeButton;
    private Vibrator vibrator;
    private Handler handler = new Handler();

    private Button[] buttons;
    private int selectedIndex = 0;

    private dot dotThread; // DotMatrix 스레드

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        resultText = findViewById(R.id.resultText);
        timeText = findViewById(R.id.timeText);
        strikeText = findViewById(R.id.strikeText);
        retryButton = findViewById(R.id.retryButton);
        nextButton = findViewById(R.id.nextButton);
        homeButton = findViewById(R.id.homeButton);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        Intent intent = getIntent();
        boolean success = intent.getBooleanExtra("result", false);
        int level = intent.getIntExtra("level", 1);
        long time = intent.getLongExtra("time", 0);
        int strike = intent.getIntExtra("strike", 0);

        double timeInSeconds = time / 1000.0;
        String timeFormatted = String.format("%.4f", timeInSeconds);

        resultText.setText(success ? "성공!" : "실패");
        timeText.setText("클리어 시간: " + timeFormatted + "초");
        strikeText.setText("오답 수: " + strike);

        SharedPreferences prefs = getSharedPreferences("GamePrefs", MODE_PRIVATE);
        long best = prefs.getLong("record_level_" + level, Long.MAX_VALUE);

        if (success) {
            Device.lcd("Clear");
            Device.led(String.valueOf(strike));
            if (time < best) vibratePattern(500, 3);
            else vibrator.vibrate(500);
        } else {
            Device.lcd("Fail");
            Device.led("-1"); // 3초간 점멸
            handler.postDelayed(() -> Device.led("0"), 500);
            nextButton.setVisibility(View.GONE);
        }

        displaySegmentTime(timeFormatted);

        retryButton.setOnClickListener(v -> {
            clearAllDevices();
            Intent i = new Intent(ResultActivity.this, GameActivity.class);
            i.putExtra("level", level);
            startActivity(i);
            finish();
        });

        nextButton.setOnClickListener(v -> {
            clearAllDevices();
            Intent i = new Intent(ResultActivity.this, GameActivity.class);
            i.putExtra("level", level + 1);
            startActivity(i);
            finish();
        });

        homeButton.setOnClickListener(v -> {
            clearAllDevices();
            Intent i = new Intent(ResultActivity.this, MainActivity.class);
            startActivity(i);
            finish();
        });

        buttons = nextButton.getVisibility() == View.VISIBLE
                ? new Button[]{retryButton, nextButton, homeButton}
                : new Button[]{retryButton, homeButton};
        updateButtonFocus();

        HardwareManager.startKeypadListener(code -> {
            runOnUiThread(() -> {
                if (code == 0) {
                    selectedIndex = (selectedIndex + 1) % buttons.length;
                    updateButtonFocus();
                    clearAllDevices();
                } else if (code == 11) {
                    buttons[selectedIndex].performClick();
                }
            });
        });

        HardwareManager.stopDot();
    }

    private void updateButtonFocus() {
        for (int i = 0; i < buttons.length; i++) {
            buttons[i].setBackgroundResource(i == selectedIndex
                    ? R.drawable.selectable_button
                    : android.R.drawable.btn_default);
        }
    }

    private void vibratePattern(int duration, int repeat) {
        long[] pattern = new long[repeat * 2];
        for (int i = 0; i < pattern.length; i++) {
            pattern[i] = duration;
        }
        vibrator.vibrate(pattern, -1);
    }

    private void displaySegmentTime(String timeFormatted) {
        final String seconds = timeFormatted.split("\\.")[0];
        final int repeatCount = 50;
        for (int i = 0; i < repeatCount; i++) {
            handler.postDelayed(() -> Device.segment(String.format("%06d", Integer.parseInt(seconds)), "0"), i * 50);
        }
    }

    private void clearAllDevices() {
        Device.lcd("");
        Device.led("0");
        Device.segment("000000", "0");
    }
}
