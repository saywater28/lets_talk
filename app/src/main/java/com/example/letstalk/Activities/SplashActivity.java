package com.example.letstalk.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.letstalk.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SplashActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private DatabaseReference usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        auth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        // Small delay so splash screen is visible
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            FirebaseUser currentUser = auth.getCurrentUser();

            if (currentUser != null) {
                // User logged in → check if profile exists
                checkUserProfile(currentUser.getUid());
            } else {
                // No logged in user → go to login/phone screen
                startActivity(new Intent(SplashActivity.this, PhoneActivity.class));
                finish();
            }
        }, 2000); // 2 second splash
    }

    private void checkUserProfile(String uid) {
        usersRef.child(uid).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                // Profile already exists → go to main
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
            } else {
                // Profile doesn’t exist yet → go to setup
                startActivity(new Intent(SplashActivity.this, SetupProfileActivity.class));
            }
            finish();
        });
    }
}
