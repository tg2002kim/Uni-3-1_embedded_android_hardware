package com.example.test5;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GameActivity extends Activity {

    private int level;
    private int totalTargets;
    private long timeLimitPerPress;
    private List<Integer> sequence = new ArrayList<>();
    private int currentIndex = 0;
    private int strikeCount = 0;
    private long startTime;
    private boolean isRunning = true;
    private boolean waitingForInput = false;
    private boolean awaitingRetry = false;

    private Handler handler = new Handler();
    private Runnable timeoutRunnable;
    private Runnable timerRunnable;

    private Button[] gridButtons = new Button[9];
    private TextView timerText, levelText, strikeText, countdownText;
    private GridLayout gridLayout;
    private RelativeLayout rootLayout;
    private Vibrator vibrator;

    private Button btnPause, btnExit;
    private int currentNavIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        level = getIntent().getIntExtra("level", 1);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        timerText = findViewById(R.id.timerText);
        levelText = findViewById(R.id.levelText);
        strikeText = findViewById(R.id.strikeText);
        gridLayout = findViewById(R.id.gridLayout);
        rootLayout = findViewById(R.id.rootLayout);

        btnPause = findViewById(R.id.btnPause);
        btnExit = findViewById(R.id.btnExit);

        btnPause.setOnClickListener(v -> togglePause());
        btnExit.setOnClickListener(v -> exitToStage());

        countdownText = new TextView(this);
        countdownText.setTextSize(72);
        countdownText.setTextColor(Color.BLACK);
        countdownText.setGravity(Gravity.CENTER);
        rootLayout.addView(countdownText, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        levelText.setText("LEVEL " + level);
        strikeText.setText("Strike: 0/8");

        Device.led("0");
        setupLevelDifficulty();
        setupGridButtons();
        startCountdown();
        startKeypadPolling();
    }

    private void togglePause() {
        if (isRunning) {
            isRunning = false;
            handler.removeCallbacks(timeoutRunnable);
            handler.removeCallbacks(timerRunnable);
            btnPause.setText("이어하기");
        } else {
            isRunning = true;
            btnPause.setText("일시정지");
            startTimer();
            if (waitingForInput || awaitingRetry) {
                handler.postDelayed(timeoutRunnable, timeLimitPerPress);
            }
        }
    }

    private void exitToStage() {
        endGame(false);
        Intent intent = new Intent(GameActivity.this, StageActivity.class);
        startActivity(intent);
        finish();
    }

    private void setupLevelDifficulty() {
        if (level <= 5) {
            totalTargets = 6 + level;
            timeLimitPerPress = 700;
        } else if (level <= 10) {
            totalTargets = 10 + level;
            timeLimitPerPress = 600;
        } else if (level <= 15) {
            totalTargets = 5 + level;
            timeLimitPerPress = 500;
        } else {
            totalTargets = 21;
            timeLimitPerPress = 400;
        }

        List<Integer> base = new ArrayList<>();
        for (int i = 0; i < 9; i++) base.add(i);
        while (sequence.size() < totalTargets) {
            Collections.shuffle(base);
            for (int b : base) {
                if (sequence.size() >= totalTargets) break;
                if (sequence.isEmpty() || sequence.get(sequence.size() - 1) != b) {
                    sequence.add(b);
                }
            }
        }
    }

    private void setupGridButtons() {
        gridLayout.post(() -> {
            int layoutWidth = gridLayout.getWidth();
            int layoutHeight = gridLayout.getHeight();
            int size = Math.min(layoutWidth, layoutHeight) / 3;
            int margin = dpToPx(8);

            for (int i = 0; i < 9; i++) {
                final int index = i;
                Button btn = new Button(GameActivity.this);
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = size - margin;
                params.height = size - margin;
                params.setMargins(margin / 2, margin / 2, margin / 2, margin / 2);
                btn.setLayoutParams(params);
                btn.setBackgroundColor(Color.LTGRAY);
                btn.setText("");
                btn.setId(1000 + i);
                gridButtons[i] = btn;
                gridLayout.addView(btn);
            }
        });
    }

    private void startCountdown() {
        final int[] countdown = {3};
        countdownText.setVisibility(View.VISIBLE);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (countdown[0] > 0) {
                    Device.lcd("Ready");
                    for (int i = 0; i < 3; i++) {
                        Device.segment(countdown[0] + "" + countdown[0] + countdown[0] + countdown[0] + countdown[0] + countdown[0], "0");
                    }
                    countdownText.setText(String.valueOf(countdown[0]));
                    countdown[0]--;
                    handler.postDelayed(this, 1000);
                } else {
                    countdownText.setVisibility(View.GONE);
                    Device.lcd("Go!");
                    startGame();
                }
            }
        }, 0);
    }

    private void startGame() {
        startTime = System.currentTimeMillis();
        startTimer();
        showNextButton();
    }

    private void showNextButton() {
        if (currentIndex >= sequence.size()) {
            endGame(true);
            return;
        }

        for (Button b : gridButtons) b.setBackgroundColor(Color.LTGRAY);

        final int targetIndex = sequence.get(currentIndex);
        gridButtons[targetIndex].setBackgroundColor(Color.YELLOW);
        waitingForInput = true;
        awaitingRetry = false;

        timeoutRunnable = () -> {
            if (waitingForInput && !awaitingRetry) {
                waitingForInput = false;
                strikeCount++;
                strikeText.setText("Strike: " + strikeCount + "/8");
                updateLEDs();

                Device.lcd("Miss");  // ✅ 시간 초과 메시지 출력

                gridButtons[targetIndex].setBackgroundColor(Color.RED);
                handler.postDelayed(() -> {
                    gridButtons[targetIndex].setBackgroundColor(Color.LTGRAY);
                    if (strikeCount >= 8) endGame(false);
                    else {
                        currentIndex++;
                        showNextButton();
                    }
                }, 300);
            }
        };
        handler.postDelayed(timeoutRunnable, timeLimitPerPress);
    }

    private void handleGridInput(int index) {
        final int targetIndex = sequence.get(currentIndex);
        final Button btn = gridButtons[index];

        if (index == targetIndex && (waitingForInput || awaitingRetry)) {
            handler.removeCallbacks(timeoutRunnable);
            waitingForInput = false;
            awaitingRetry = false;
            btn.setBackgroundColor(Color.BLUE);

            Device.lcd("Good");  // ✅ 정답 메시지 출력

            handler.postDelayed(() -> {
                btn.setBackgroundColor(Color.LTGRAY);
                currentIndex++;
                if (strikeCount >= 8) endGame(false);
                else showNextButton();
            }, 300);
        } else if (index != targetIndex && waitingForInput) {
            handler.removeCallbacks(timeoutRunnable);
            strikeCount++;
            strikeText.setText("Strike: " + strikeCount + "/8");
            updateLEDs();
            awaitingRetry = true;
            waitingForInput = true;
            btn.setBackgroundColor(Color.RED);

            Device.lcd("Wrong");  // ✅ 오답 메시지 출력

            handler.postDelayed(() -> {
                btn.setBackgroundColor(Color.LTGRAY);
                gridButtons[targetIndex].setBackgroundColor(Color.YELLOW);
                if (strikeCount >= 8) endGame(false);
            }, 300);
        }
    }


    private void handleKeypadInput(int code) {
        if (code >= 1 && code <= 9) {
            handleGridInput(code - 1);
        } else if (code == 0) {
            currentNavIndex = (currentNavIndex + 1) % 2;
            btnPause.setBackgroundColor(currentNavIndex == 0 ? Color.CYAN : Color.LTGRAY);
            btnExit.setBackgroundColor(currentNavIndex == 1 ? Color.CYAN : Color.LTGRAY);
        } else if (code == 11) {
            if (currentNavIndex == 0) btnPause.performClick();
            else btnExit.performClick();
        }
    }

    private void updateLEDs() {
        if (strikeCount < 8) {
            Device.led(String.valueOf(strikeCount));
        } else {
            Device.led("-1");
        }
    }

    private void endGame(boolean success) {
        isRunning = false;
        waitingForInput = false;
        awaitingRetry = false;

        // 🔴 타이머 강제 정지
        handler.removeCallbacksAndMessages(null); // 모든 handler 메시지 제거

        long elapsedTime = System.currentTimeMillis() - startTime;

        SharedPreferences prefs = getSharedPreferences("GamePrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        if (success) {
            long best = prefs.getLong("record_level_" + level, Long.MAX_VALUE);
            if (elapsedTime < best) {
                editor.putLong("record_level_" + level, elapsedTime);
                vibrate(500);
                handler.postDelayed(() -> vibrate(500), 600);
                handler.postDelayed(() -> vibrate(500), 1200);
            } else {
                vibrate(1000);
            }
            editor.putBoolean("clear_level_" + level, true);
        } else {
            vibrate(3000);
        }

        editor.apply();

        // ✅ 결과 화면으로 이동
        Intent intent = new Intent(GameActivity.this, ResultActivity.class);
        intent.putExtra("result", success);
        intent.putExtra("level", level);
        intent.putExtra("time", elapsedTime);
        intent.putExtra("strike", strikeCount);
        startActivity(intent);

        finish();
    }

    private void startTimer() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isRunning) return;

                long elapsed = System.currentTimeMillis() - startTime;
                timerText.setText("TIME: " + elapsed + "ms");

                // ⏱ 0.01초 단위로 센티초 표시 (예: 1.23초 -> "000123")
                String segmentStr = String.format("%06d", elapsed / 10);
                Device.segment(segmentStr, "0");

                handler.postDelayed(this, 10); // 10ms 간격으로 실행
            }
        };
        handler.post(timerRunnable);
    }

    private void vibrate(int millis) {
        SharedPreferences prefs = getSharedPreferences("GamePrefs", MODE_PRIVATE);
        if (prefs.getBoolean("vibrate", true)) {
            vibrator.vibrate(millis);
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void startKeypadPolling() {
        new Thread(() -> {
            while (true) {
                int code = Device.getKeypadCode();
                if (code >= 0) {
                    runOnUiThread(() -> handleKeypadInput(code));
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
