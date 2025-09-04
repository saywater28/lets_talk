package com.example.letstalk.Activities;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.letstalk.R;

public class ContactDetailsActivity extends AppCompatActivity {

    private TextView nameText, emailText, phoneText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_details);

        nameText = findViewById(R.id.nameText);
        emailText = findViewById(R.id.emailText);
        phoneText = findViewById(R.id.phoneText);

        // Get data from intent
        String name = getIntent().getStringExtra("name");
        String email = getIntent().getStringExtra("email");
        String phone = getIntent().getStringExtra("phone");

        nameText.setText(name != null ? name : "N/A");
        emailText.setText(email != null ? email : "N/A");
        phoneText.setText(phone != null ? phone : "N/A");
    }
}
