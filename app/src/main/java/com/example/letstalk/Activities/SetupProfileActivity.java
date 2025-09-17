package com.example.letstalk.Activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Spinner;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.letstalk.Models.User;
import com.example.letstalk.databinding.ActivitySetupProfileBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class SetupProfileActivity extends AppCompatActivity {

    ActivitySetupProfileBinding binding;
    FirebaseAuth auth;
    FirebaseDatabase database;
    FirebaseStorage storage;
    Uri selectedImage;
    ProgressDialog dialog;

    Spinner langSpinner; // 🔹 NEW: Spinner reference

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySetupProfileBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());

        dialog = new ProgressDialog(this);
        dialog.setMessage("Updating Profile...");
        dialog.setCancelable(false);

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        storage = FirebaseStorage.getInstance();

        // 🔹 NEW: init spinner
        langSpinner = findViewById(binding.langSpinner.getId());

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            finish(); // safety check
            return;
        }

        // Check if user already exists in DB
        database.getReference().child("users").child(currentUser.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            // User exists → go to MainActivity
                            startActivity(new Intent(SetupProfileActivity.this, MainActivity.class));
                            finish();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) { }
                });

        // Pick profile image
        binding.imageView.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(intent, 45);
        });

        binding.continueBtn.setOnClickListener(view -> {
            String name = binding.nameBox.getText().toString().trim();
            if (name.isEmpty()) {
                binding.nameBox.setError("Please type a name");
                return;
            }
            dialog.show();

            String uid = currentUser.getUid();
            String phone = currentUser.getPhoneNumber() != null ? currentUser.getPhoneNumber() : "No Phone";
            String email = currentUser.getEmail() != null ? currentUser.getEmail() : "No Email";
            String preferredLang = langSpinner.getSelectedItem().toString(); // 🔹 NEW

            if (selectedImage != null) {
                StorageReference reference = storage.getReference().child("Profiles").child(uid);
                reference.putFile(selectedImage).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        reference.getDownloadUrl().addOnSuccessListener(uri -> {
                            String imageUrl = uri.toString();
                            saveUser(uid, name, phone, email, imageUrl, preferredLang); // 🔹 pass lang
                        });
                    }
                });
            } else {
                saveUser(uid, name, phone, email, "No Image", preferredLang); // 🔹 pass lang
            }
        });

    }

    // 🔹 UPDATED: Added preferredLang parameter
    private void saveUser(String uid, String name, String phone, String email, String profileImage, String preferredLang) {
        User user = new User(uid, name, phone, email, profileImage, preferredLang);
        user.setPreferredLanguage(preferredLang); // save language

        database.getReference().child("users").child(uid).setValue(user)
                .addOnSuccessListener(aVoid -> {
                    dialog.dismiss();
                    startActivity(new Intent(SetupProfileActivity.this, MainActivity.class));
                    finish();
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null && data.getData() != null) {
            binding.imageView.setImageURI(data.getData());
            selectedImage = data.getData();
        }
    }
}
