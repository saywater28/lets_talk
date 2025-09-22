package com.example.letstalk.Activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.letstalk.Adapters.AvatarAdapter;
import com.example.letstalk.Models.User;
import com.example.letstalk.R;
import com.example.letstalk.databinding.ActivitySetupProfileBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class SetupProfileActivity extends AppCompatActivity {

    ActivitySetupProfileBinding binding;
    FirebaseAuth auth;
    FirebaseDatabase database;
    FirebaseStorage storage;
    Uri selectedImage;
    ProgressDialog dialog;

    Spinner langSpinner;
    private int selectedAvatarResId = -1;

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

        // use binding directly
        langSpinner = binding.langSpinner;

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            finish();
            return;
        }

        database.getReference().child("users").child(currentUser.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            startActivity(new Intent(SetupProfileActivity.this, MainActivity.class));
                            finish();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) { }
                });

        binding.imageView.setOnClickListener(v -> {
            Integer[] avatars = {
                    R.drawable.avatar1,
                    R.drawable.avatar2,
                    R.drawable.avatar3,
                    R.drawable.avatar4,
                    R.drawable.avatar5,
                    R.drawable.avatar6
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(SetupProfileActivity.this);
            builder.setTitle("Choose a profile picture");

            AvatarAdapter adapter = new AvatarAdapter(SetupProfileActivity.this, avatars);

            builder.setAdapter(adapter, (dialog, which) -> {
                selectedAvatarResId = avatars[which];
                binding.imageView.setImageResource(selectedAvatarResId);
            });


            builder.show();
        });


        // Continue button
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
            String preferredLang = langSpinner.getSelectedItem().toString();

            if (selectedAvatarResId != -1) {
                // save avatar resource ID as string
                saveUser(uid, name, phone, email, String.valueOf(selectedAvatarResId), preferredLang);

            }
            else if (selectedImage != null) {
                if (selectedImage.toString().startsWith("android.resource://")) {
                    saveUser(uid, name, phone, email, selectedImage.toString(), preferredLang);
                } else {
                    StorageReference reference = storage.getReference().child("Profiles").child(uid);
                    reference.putFile(selectedImage).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            reference.getDownloadUrl().addOnSuccessListener(uri -> {
                                saveUser(uid, name, phone, email, uri.toString(), preferredLang);
                            });
                        }
                    });
                }
            } else {
                saveUser(uid, name, phone, email, "No Image", preferredLang);
            }
        });
    }

    private void saveUser(String uid, String name, String phone, String email, String profileImage, String preferredLang) {
        User user = new User(uid, name, phone, email, profileImage, preferredLang);
        user.setPreferredLanguage(preferredLang);

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
