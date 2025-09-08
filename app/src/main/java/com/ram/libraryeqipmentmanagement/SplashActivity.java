package com.ram.libraryeqipmentmanagement;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.ConnectionResult;

public class SplashActivity extends AppCompatActivity {
    private static final long SPLASH_DURATION = 3500; // 3.5 seconds total
    private boolean isAnimationDone = false;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private boolean isResolvingPlayServices = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Check Google Play Services first
        if (checkPlayServices()) {
            proceedWithSplash();
        }

        // Get references to views
        ImageView teamLogo = findViewById(R.id.teamLogo);
        ImageView collegeLogo = findViewById(R.id.collegeLogo);
        TextView appTitle = findViewById(R.id.appTitle);
        TextView presentedBy = findViewById(R.id.presentedBy);

        // Initially hide college logo and text
        collegeLogo.setVisibility(View.INVISIBLE);
        appTitle.setVisibility(View.INVISIBLE);
        presentedBy.setVisibility(View.INVISIBLE);

        // Load animations
        Animation fadeInScale = AnimationUtils.loadAnimation(this, R.anim.fade_in_scale);

        // Animation listener for team logo
        fadeInScale.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                // Show college logo with animation after team logo animation ends
                collegeLogo.setVisibility(View.VISIBLE);
                Animation collegeFadeIn = AnimationUtils.loadAnimation(SplashActivity.this, R.anim.fade_in_scale);
                collegeLogo.startAnimation(collegeFadeIn);

                // Show texts with delay
                new Handler().postDelayed(() -> {
                    appTitle.setVisibility(View.VISIBLE);
                    appTitle.startAnimation(AnimationUtils.loadAnimation(SplashActivity.this, android.R.anim.fade_in));
                }, 1000);

                new Handler().postDelayed(() -> {
                    presentedBy.setVisibility(View.VISIBLE);
                    presentedBy.startAnimation(AnimationUtils.loadAnimation(SplashActivity.this, android.R.anim.fade_in));
                }, 1500);

                // Start login activity after all animations
                new Handler().postDelayed(() -> {
                    if (!isFinishing()) {
                        Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
                        startActivity(intent);
                        overridePendingTransition(R.anim.slide_up, R.anim.fade_out);
                        finish();
                    }
                }, SPLASH_DURATION);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        // Start team logo animation
        teamLogo.startAnimation(fadeInScale);
    }

    private boolean checkPlayServices() {
        GoogleApiAvailability googleApi = GoogleApiAvailability.getInstance();
        int resultCode = googleApi.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (googleApi.isUserResolvableError(resultCode)) {
                googleApi.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST,
                        dialog -> {
                            isResolvingPlayServices = false;
                            finish();
                        })
                        .show();
                isResolvingPlayServices = true;
            } else {
                finish();
            }
            return false;
        }
        return true;
    }

    private void proceedWithSplash() {
        new Handler().postDelayed(() -> {
            // Your existing splash screen logic here
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }, 2000); // 2 seconds delay
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PLAY_SERVICES_RESOLUTION_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                proceedWithSplash();
            } else {
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove any pending handlers
        Handler handler = new Handler();
        handler.removeCallbacksAndMessages(null);
    }
}
