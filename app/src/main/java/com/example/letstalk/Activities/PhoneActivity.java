package com.example.letstalk.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.letstalk.R;
import com.example.letstalk.databinding.ActivityPhoneBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class PhoneActivity extends AppCompatActivity {

    ActivityPhoneBinding binding;
    FirebaseAuth auth;
    DatabaseReference usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPhoneBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        // If already logged in, check if profile exists
        if (auth.getCurrentUser() != null) {
            checkUserProfile(auth.getCurrentUser().getUid());
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
                            checkUserProfile(auth.getCurrentUser().getUid());
                        } else {
                            // If login fails, try creating a new account
                            auth.createUserWithEmailAndPassword(email, password)
                                    .addOnCompleteListener(signupTask -> {
                                        if (signupTask.isSuccessful()) {
                                            Toast.makeText(this, "Signup successful", Toast.LENGTH_SHORT).show();
                                            goToSetupProfile();
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

    // ✅ Check if user profile exists in Firebase
    private void checkUserProfile(String uid) {
        usersRef.child(uid).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                // Profile already exists → go to MainActivity
                goToMain();
            } else {
                // First-time login → go to SetupProfileActivity
                goToSetupProfile();
            }
        });
    }

    private void goToSetupProfile() {
        Intent intent = new Intent(PhoneActivity.this, SetupProfileActivity.class);
        startActivity(intent);
        finish();
    }

    private void goToMain() {
        Intent intent = new Intent(PhoneActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
