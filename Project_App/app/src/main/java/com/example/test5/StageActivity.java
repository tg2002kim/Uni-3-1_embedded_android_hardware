package com.example.test5;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.GridLayout;

public class StageActivity extends Activity {

    private GridLayout gridLayout;
    private static final int MAX_LEVEL = 16;
    private SharedPreferences prefs;

    private int selectedLevel = 1; // DIP 스위치 선택된 레벨
    private int cursorIndex = 0; // 0: 메인, 1: 설정, 2~17: lv1~lv16
    private boolean dipActive = false; // DIP 스위치 활성 여부

    private Thread dipThread;
    private Thread keyThread;
    private Device.dot dotThread;

    private Button btnMain, btnSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stage);

        gridLayout = findViewById(R.id.stageGrid);
        prefs = getSharedPreferences("GamePrefs", Context.MODE_PRIVATE);

        btnMain = findViewById(R.id.btnMain);
        btnSettings = findViewById(R.id.btnSettings);

        btnMain.setOnClickListener(v -> navigateTo(MainActivity.class));
        btnSettings.setOnClickListener(v -> navigateTo(SettingActivity.class));

        gridLayout.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        gridLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                        generateLevelButtons();
                    }
                });

        startDotDisplay();
        startDipThread();
        startKeyThread();
    }

    private void navigateTo(Class<?> targetActivity) {
        stopThreads();
        startActivity(new Intent(StageActivity.this, targetActivity));
        finish();
    }

    private void startDotDisplay() {
        if (dotThread != null) dotThread.stopThread();
        dotThread = new Device.dot(String.valueOf(selectedLevel));
        dotThread.start();
    }

    private void stopDotDisplay() {
        if (dotThread != null) {
            dotThread.stopThread();
        }
    }

    private void startDipThread() {
        dipThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                String state = Device.dip();
                if (state.equals("0000000000000000")) {
                    dipActive = false;
                    selectedLevel = 0;
                    runOnUiThread(() -> {
                        Device.lcd("");
                        stopDotDisplay();
                    });
                } else {
                    int level = parseDipLevel(state);
                    if (level >= 1 && level <= MAX_LEVEL && level != selectedLevel) {
                        dipActive = true;
                        selectedLevel = level;
                        cursorIndex = level + 1;
                        runOnUiThread(this::updateLCDandDot);
                    }
                }
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        dipThread.start();
    }

    private void startKeyThread() {
        keyThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                int key = Device.getKeypadCode();
                if (key == 0) {
                    runOnUiThread(() -> {
                        int maxCursor = dipActive ? (MAX_LEVEL + 2) : 2;
                        cursorIndex = (cursorIndex + 1) % maxCursor;
                        highlightCursor();
                    });
                } else if (key == 11) {
                    runOnUiThread(this::executeCursorAction);
                }
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        keyThread.start();
    }

    private void highlightCursor() {
        btnMain.setBackgroundColor(cursorIndex == 0 ? 0xFFAAAAFF : 0xFFFFFFFF);
        btnSettings.setBackgroundColor(cursorIndex == 1 ? 0xFFAAAAFF : 0xFFFFFFFF);
        for (int i = 0; i < gridLayout.getChildCount(); i++) {
            View v = gridLayout.getChildAt(i);
            v.setBackgroundColor(cursorIndex == i + 2 ? 0xFFAAAAFF : 0xFFFFFFFF);
        }
    }

    private void executeCursorAction() {
        if (cursorIndex == 0) navigateTo(MainActivity.class);
        else if (cursorIndex == 1) navigateTo(SettingActivity.class);
        else {
            int level = cursorIndex - 1;
            if (level == 1 || prefs.getBoolean("clear_level_" + (level - 1), false)) {
                stopThreads();
                Intent intent = new Intent(StageActivity.this, GameActivity.class);
                intent.putExtra("level", level);
                startActivity(intent);
                finish();
            } else {
                Device.lcd("Locked");
                Device.led("0");
            }
        }
    }

    private void updateLCDandDot() {
        boolean isUnlocked = (selectedLevel == 1 || prefs.getBoolean("clear_level_" + (selectedLevel - 1), false));
        Device.lcd(isUnlocked ? "Selected: Lv " + selectedLevel : "Locked");
        Device.led(isUnlocked ? "1" : "0");
        startDotDisplay();
    }

    private int parseDipLevel(String dipBinary) {
        for (int i = 0; i < 16; i++) {
            if (dipBinary.charAt(i) == '1') return i + 1;
        }
        return 0;
    }

    private void generateLevelButtons() {
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int margin = 8;
        int buttonSize = (screenWidth - (margin * 5)) / 4;

        for (int i = 1; i <= MAX_LEVEL; i++) {
            final int level = i;
            Button levelButton = new Button(this);
            levelButton.setTextSize(15f);
            levelButton.setBackgroundResource(android.R.drawable.btn_default);
            levelButton.setSingleLine(false);
            levelButton.setMaxLines(3);
            levelButton.setGravity(Gravity.CENTER);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = buttonSize;
            params.height = (int) (buttonSize * 1.2);
            params.setMargins(margin / 2, margin / 2, margin / 2, margin / 2);
            levelButton.setLayoutParams(params);

            long record = prefs.getLong("record_level_" + level, -1);
            int strikeRecord = prefs.getInt("strike_level_" + level, -1);
            String label = "lv " + level;
            if (strikeRecord >= 0) label += "\nX : " + strikeRecord + "개";
            if (record >= 0) label += "\n" + String.format("%.3f", record / 1000.0) + "s";
            levelButton.setText(label);

            if (level != 1 && !prefs.getBoolean("clear_level_" + (level - 1), false)) {
                levelButton.setEnabled(false);
            }

            levelButton.setOnClickListener(view -> {
                stopThreads();
                Intent intent = new Intent(StageActivity.this, GameActivity.class);
                intent.putExtra("level", level);
                startActivity(intent);
                finish();
            });

            gridLayout.addView(levelButton);
        }
    }

    private void stopThreads() {
        if (dipThread != null) dipThread.interrupt();
        if (keyThread != null) keyThread.interrupt();
        stopDotDisplay();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopThreads();
    }
}
