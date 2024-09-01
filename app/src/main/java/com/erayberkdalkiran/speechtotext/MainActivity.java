package com.erayberkdalkiran.speechtotext;

import android.content.Intent;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;


import androidx.appcompat.app.AppCompatActivity;
import android.animation.ObjectAnimator;
import android.animation.AnimatorSet;
import android.widget.ImageView;

import com.erayberkdalkiran.speechtotext.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private ImageView storybookButton_1;
    Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View activityView = binding.getRoot();
        setContentView(activityView);

        storybookButton_1 = binding.storybookButton1;

        storybookButton_1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonClickAnimation(storybookButton_1);
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(MainActivity.this, StoryBook.class);
                        startActivity(intent);
                    }
                }, 300);
            }
        });

        binding.voskButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, VoskActivity.class);
                startActivity(intent);
            }
        });

        binding.googleCloudButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, GoogleCloudActivity.class);
                startActivity(intent);

            }
        });
    }

    public void buttonClickAnimation(ImageView imageView) {
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(imageView, "alpha", 1f, 0.3f);
        fadeOut.setDuration(300);
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(imageView, "alpha", 0.3f, 1f);
        fadeIn.setDuration(300);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.play(fadeOut).before(fadeIn);
        animatorSet.start();
    }
}