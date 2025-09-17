package com.example.letstalk.Activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.letstalk.Adapters.MessagesAdapter;
import com.example.letstalk.Models.Message;
import com.example.letstalk.R;
import com.example.letstalk.databinding.ActivityChatBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class ChatActivity extends AppCompatActivity {

    private ActivityChatBinding binding;
    private MessagesAdapter adapter;
    private ArrayList<Message> messages;
    private ArrayList<String> messageKeys; // store Firebase keys
    private String senderRoom, receiverRoom;
    private DatabaseReference database;

    private String senderLang = "en";
    private String receiverLang = "en";

    private String senderUid, receiverUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("Settings", MODE_PRIVATE);
        String lang = prefs.getString("AppLang", "en");
        setLocale(lang, false);

        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.msgBox.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                binding.sendBtn.performClick();
                return true;
            }
            return false;
        });

        Toolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);

        ImageView backArrow = findViewById(R.id.backArrow);
        TextView personName = findViewById(R.id.personName);

        senderUid = FirebaseAuth.getInstance().getUid();
        receiverUid = getIntent().getStringExtra("uid");

        if (senderUid == null || receiverUid == null) return;

        String name = getIntent().getStringExtra("name");
        if (name != null) {
            personName.setText(name);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(name);
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        }

        backArrow.setOnClickListener(v -> onBackPressed());

        senderRoom = senderUid + receiverUid;
        receiverRoom = receiverUid + senderUid;

        messages = new ArrayList<>();
        messageKeys = new ArrayList<>();
        adapter = new MessagesAdapter(this, messages, messageKeys, senderRoom, receiverRoom);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);

        database = FirebaseDatabase.getInstance().getReference();

        loadMessages();

        binding.sendBtn.setOnClickListener(v -> sendMessage());
    }

    private void loadMessages() {
        database.child("chats").child(senderRoom).child("messages")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        messages.clear();
                        messageKeys.clear();
                        for (DataSnapshot snap : snapshot.getChildren()) {
                            Message message = snap.getValue(Message.class);
                            if (message != null) {
                                messages.add(message);
                                messageKeys.add(snap.getKey());
                            }
                        }
                        adapter.notifyDataSetChanged();
                        binding.recyclerView.scrollToPosition(messages.size() - 1);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void sendMessage() {
        String messageTxt = binding.msgBox.getText().toString().trim();
        if (messageTxt.isEmpty()) return;

        binding.msgBox.setText("");

        Message message = new Message(messageTxt, senderUid, new Date().getTime());
        String key = database.child("chats").child(senderRoom).child("messages").push().getKey();
        if (key == null) return;

        message.setMessageId(key);

        // Save message to both rooms
        database.child("chats").child(senderRoom).child("messages").child(key).setValue(message);
        database.child("chats").child(receiverRoom).child("messages").child(key).setValue(message);
    }

    private void setLocale(String langCode, boolean recreateActivity) {
        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());

        SharedPreferences prefs = getSharedPreferences("Settings", MODE_PRIVATE);
        prefs.edit().putString("AppLang", langCode).apply();

        if (recreateActivity) recreate();
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chat_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_view_contact) {
            Intent profileIntent = new Intent(this, ContactDetailsActivity.class);
            profileIntent.putExtra("name", getIntent().getStringExtra("name"));
            profileIntent.putExtra("email", getIntent().getStringExtra("email"));
            profileIntent.putExtra("phone", getIntent().getStringExtra("phone"));
            profileIntent.putExtra("profileImage", getIntent().getStringExtra("profileImage"));
            startActivity(profileIntent);
            return true;
        } else if (id == R.id.action_delete_chat) return true;
        else if (id == R.id.action_lang_selection) showLanguageDialog();
        return super.onOptionsItemSelected(item);
    }

    private void showLanguageDialog() {
        final String[] languages = {"English", "हिन्दी", "Español", "Français"};
        final String[] langCodes = {"en", "hi", "es", "fr"};

        new AlertDialog.Builder(this)
                .setTitle("Choose Language")
                .setItems(languages, (dialog, which) -> setLocale(langCodes[which], true))
                .setNegativeButton("Cancel", null)
                .show();
    }
}
