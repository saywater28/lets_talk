package com.example.letstalk.Activities;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.letstalk.R;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings); // ✅ must be called first

        View view = findViewById(R.id.main); // make sure R.id.main exists in activity_settings.xml
        if (view != null) {
            ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
                // handle insets
                return insets;
            });
        }
    }

}