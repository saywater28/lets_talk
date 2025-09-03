package com.example.letstalk.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.letstalk.R;
import com.example.letstalk.databinding.ActivityPhoneBinding;
import com.google.firebase.auth.FirebaseAuth;

public class PhoneActivity extends AppCompatActivity {

    ActivityPhoneBinding binding;
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPhoneBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();

        if(auth.getCurrentUser() != null){
            Intent intent = new Intent(PhoneActivity.this, SetupProfileActivity.class);
            startActivity(intent);
            finish();
        }

        binding.phoneBox.requestFocus();

        // Phone Auth Continue Button
        binding.continueBtn.setOnClickListener(view -> {
            String phone = binding.phoneBox.getText().toString().trim();
            if (TextUtils.isEmpty(phone)) {
                Toast.makeText(this, "Enter phone number", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(PhoneActivity.this, OTPActivity.class);
            intent.putExtra("phoneNumber", phone);
            startActivity(intent);
        });

        // Email Login/Signup Button
        binding.emailLoginBtn.setOnClickListener(view -> {
            String email = binding.emailBox.getText().toString().trim();
            String password = binding.passwordBox.getText().toString().trim();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show();
                return;
            }

            // Try login first
            auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show();
                            goToMain();
                        } else {
                            // If login fails, try creating a new account
                            auth.createUserWithEmailAndPassword(email, password)
                                    .addOnCompleteListener(signupTask -> {
                                        if (signupTask.isSuccessful()) {
                                            Toast.makeText(this, "Signup successful", Toast.LENGTH_SHORT).show();
                                            goToMain();
                                        } else {
                                            Toast.makeText(this, "Auth failed: " + signupTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                                        }
                                    });
                        }
                    });
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void goToMain() {
        Intent intent = new Intent(PhoneActivity.this, SetupProfileActivity.class);
        startActivity(intent);
        finish();
    }
}
