package com.example.test5;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

public class GuideActivity extends Activity {

    private ImageView guideImage;
    private Button prevButton, nextButton, endButton, mainButton;
    private int currentPage = 0;

    private int selectedIndex = 0;
    private Button[] buttons;

    private int[] guideImages = {
            R.drawable.guide_1,
            R.drawable.guide_2,
            R.drawable.guide_3,
            R.drawable.guide_4,
            R.drawable.guide_5
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide);

        guideImage = findViewById(R.id.guideImage);
        prevButton = findViewById(R.id.prevButton);
        nextButton = findViewById(R.id.nextButton);
        endButton = findViewById(R.id.endButton);
        mainButton = findViewById(R.id.mainButton);

        updatePage();

        prevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentPage > 0) {
                    currentPage--;
                    updatePage();
                }
            }
        });

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentPage < guideImages.length - 1) {
                    currentPage++;
                    updatePage();
                }
            }
        });

        endButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(GuideActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

        mainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(GuideActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

        // ✅ LCD 표시
        Device.lcd("Guide Page 1");

        // ✅ 키패드 입력 리스너 시작
        HardwareManager.startKeypadListener(code -> runOnUiThread(() -> handleKeypadInput(code)));
    }

    private void handleKeypadInput(int code) {
        if (buttons == null) return;

        int len = buttons.length;
        if (code == 0) {
            selectedIndex = (selectedIndex + 1) % len;
        } else if (code == 11) {
            buttons[selectedIndex].performClick();
        }

        for (int i = 0; i < len; i++) {
            buttons[i].setSelected(i == selectedIndex);
        }
    }

    private void updatePage() {
        guideImage.setImageResource(guideImages[currentPage]);
        Device.lcd("Guide Page " + (currentPage + 1));

        if (currentPage == 0) {
            prevButton.setVisibility(View.GONE);
            mainButton.setVisibility(View.VISIBLE);
        } else {
            prevButton.setVisibility(View.VISIBLE);
            mainButton.setVisibility(View.GONE);
        }

        if (currentPage == guideImages.length - 1) {
            nextButton.setVisibility(View.GONE);
            endButton.setVisibility(View.VISIBLE);
        } else {
            nextButton.setVisibility(View.VISIBLE);
            endButton.setVisibility(View.GONE);
        }

        buttons = getVisibleButtons();
        selectedIndex = 0;
        for (int i = 0; i < buttons.length; i++) {
            buttons[i].setSelected(i == selectedIndex);
        }
    }

    private Button[] getVisibleButtons() {
        if (currentPage == 0) {
            return new Button[]{mainButton, nextButton};
        } else if (currentPage == guideImages.length - 1) {
            return new Button[]{prevButton, endButton};
        } else {
            return new Button[]{prevButton, nextButton};
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        HardwareManager.stopKeypadListener();
    }
}
