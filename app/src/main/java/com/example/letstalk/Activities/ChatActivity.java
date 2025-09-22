package com.example.letstalk.Activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.firebase.database.*;
import com.google.mlkit.nl.translate.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class ChatActivity extends AppCompatActivity {

    private ActivityChatBinding binding;
    private MessagesAdapter adapter;
    private ArrayList<Message> messages;
    private ArrayList<String> messageKeys;
    private String senderRoom, receiverRoom;
    private DatabaseReference database;

    private String senderLang = "en";
    private String receiverLang = "en";

    private String senderUid, receiverUid;

    private Translator translator;
    private String chatLang = "en";   // 👈 will be set from DB

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("Settings", MODE_PRIVATE);
        String lang = prefs.getString("AppLang", "en");
        setLocale(lang, false);

        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Toolbar toolbar = binding.toolbar;

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        ImageView backArrow = findViewById(R.id.backArrow);
        TextView personName = findViewById(R.id.personName);

        senderUid = FirebaseAuth.getInstance().getUid();
        receiverUid = getIntent().getStringExtra("uid");
        if (senderUid == null || receiverUid == null) return;

        String name = getIntent().getStringExtra("name");
        if (name != null) {
            personName.setText(name);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        }

        backArrow.setOnClickListener(v -> onBackPressed());

        senderRoom = senderUid + receiverUid;
        receiverRoom = receiverUid + senderUid;

        messages = new ArrayList<>();
        messageKeys = new ArrayList<>();
        adapter = new MessagesAdapter(this, messages, "en", senderRoom, receiverRoom);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);

        database = FirebaseDatabase.getInstance().getReference();
        fetchLanguagesAndSetup();

        binding.sendBtn.setOnClickListener(v -> sendMessage());

        binding.recyclerView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            binding.recyclerView.post(() -> {
                if (adapter.getItemCount() > 0) {
                    binding.recyclerView.smoothScrollToPosition(adapter.getItemCount() - 1);
                }
            });
        });


        binding.msgBox.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                binding.sendBtn.performClick();
                return true;
            }
            return false;
        });
    }

    private void fetchLanguagesAndSetup() {
        database.child("users").child(senderUid).child("preferredLanguage")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) senderLang = mapToCode(snapshot.getValue(String.class));
                        setupTranslator(senderLang, chatLang);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });

        database.child("users").child(receiverUid).child("preferredLanguage")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            receiverLang = mapToCode(snapshot.getValue(String.class));
                            chatLang = receiverLang;   // 👈 load messages in receiver's preferred language
                            adapter.setPreferredLang(chatLang);
                            adapter.notifyDataSetChanged();
                            setupTranslator(senderLang, chatLang);
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void setupTranslator(String sourceLangCode, String targetLangCode) {
        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(getMlKitLangCode(sourceLangCode))
                .setTargetLanguage(getMlKitLangCode(targetLangCode))
                .build();
        translator = Translation.getClient(options);

        translator.downloadModelIfNeeded()
                .addOnSuccessListener(aVoid -> loadMessages())
                .addOnFailureListener(Throwable::printStackTrace);
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
                            if (message == null) continue;

                            messages.add(message);
                            messageKeys.add(snap.getKey());
                        }

                        adapter.notifyDataSetChanged();
                        binding.recyclerView.scrollToPosition(messages.size() - 1);

                        if (translator != null) {
                            for (Message message : messages) {
                                if (message.getTranslations() != null &&
                                        message.getTranslations().containsKey(chatLang)) continue;

                                String originalMessage = message.getMessageText();
                                if (originalMessage != null && !originalMessage.isEmpty()) {
                                    translator.translate(originalMessage)
                                            .addOnSuccessListener(translatedText -> {
                                                Map<String, String> translations =
                                                        message.getTranslations() != null
                                                                ? message.getTranslations()
                                                                : new HashMap<>();
                                                translations.put(chatLang, translatedText);
                                                message.setTranslations(translations);

                                                String messageId = message.getMessageId();
                                                if (messageId != null) {
                                                    database.child("chats").child(senderRoom)
                                                            .child("messages").child(messageId)
                                                            .child("translations")
                                                            .setValue(translations);
                                                    database.child("chats").child(receiverRoom)
                                                            .child("messages").child(messageId)
                                                            .child("translations")
                                                            .setValue(translations);
                                                }

                                                int pos = messages.indexOf(message);
                                                if (pos != -1) adapter.notifyItemChanged(pos);
                                            })
                                            .addOnFailureListener(Throwable::printStackTrace);
                                }
                            }
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void sendMessage() {
        String messageTxt = binding.msgBox.getText().toString().trim();
        if (messageTxt.isEmpty()) return;
        binding.msgBox.setText("");

        long timestamp = System.currentTimeMillis();
        String messageId = database.child("chats").child(senderRoom).child("messages").push().getKey();
        if (messageId == null) return;

        Message message = new Message(messageTxt, senderUid, timestamp);
        message.setMessageId(messageId);

        messages.add(message);
        messageKeys.add(messageId);
        adapter.notifyItemInserted(messages.size() - 1);
        binding.recyclerView.scrollToPosition(messages.size() - 1);

        DatabaseReference senderRef = database.child("chats").child(senderRoom).child("messages").child(messageId);
        DatabaseReference receiverRef = database.child("chats").child(receiverRoom).child("messages").child(messageId);

        senderRef.setValue(message);
        receiverRef.setValue(message);

        if (!senderLang.equals(receiverLang) && translator != null) {
            translator.translate(messageTxt)
                    .addOnSuccessListener(translatedText -> {
                        Map<String, String> translations = new HashMap<>();
                        translations.put(receiverLang, translatedText);
                        message.setTranslations(translations);

                        senderRef.child("translations").setValue(translations);
                        receiverRef.child("translations").setValue(translations);

                        int pos = messages.indexOf(message);
                        if (pos != -1) adapter.notifyItemChanged(pos);
                    })
                    .addOnFailureListener(Throwable::printStackTrace);
        }
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
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.chat_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull android.view.MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_view_contact) {
            Intent profileIntent = new Intent(this, ContactDetailsActivity.class);
            profileIntent.putExtra("name", getIntent().getStringExtra("name"));
            profileIntent.putExtra("email", getIntent().getStringExtra("email"));
            profileIntent.putExtra("phone", getIntent().getStringExtra("phone"));
            profileIntent.putExtra("profileImage", getIntent().getStringExtra("profileImage"));
            startActivity(profileIntent);
            return true;
        } else if (id == R.id.action_delete_chat) {
            showDeleteMessageOptions();
            return true;
        } else if (id == R.id.action_delete_all) {
            showDeleteAllMessagesOptions();
            return true;            
        } else if (id == R.id.action_lang_selection) showLanguageDialog();
        return super.onOptionsItemSelected(item);
    }

    private void showDeleteAllMessagesOptions() {
        String[] options = {"Delete all for me", "Delete all for everyone"};

        new AlertDialog.Builder(this)
                .setTitle("Delete All Messages")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        deleteAllForMe();
                    } else {
                        deleteAllForEveryone();
                    }
                })
                .show();
    }

    private void deleteAllForMe() {
        FirebaseDatabase.getInstance().getReference()
                .child("chats")
                .child(senderRoom)
                .child("messages")
                .removeValue();

        messages.clear();
        adapter.notifyDataSetChanged();

        Toast.makeText(this, "All messages deleted for me", Toast.LENGTH_SHORT).show();
    }

    private void deleteAllForEveryone() {
        for (Message m : messages) {
            if (m.getMessageId() != null) {
                database.child("chats").child(senderRoom)
                        .child("messages").child(m.getMessageId())
                        .child("deleted").setValue(true);

                database.child("chats").child(receiverRoom)
                        .child("messages").child(m.getMessageId())
                        .child("deleted").setValue(true);

                m.setDeleted(true);
            }
        }

        messages.clear();
        adapter.notifyDataSetChanged();
        Toast.makeText(this, "All messages deleted for everyone", Toast.LENGTH_SHORT).show();
    }



    private void showDeleteMessageOptions() {
        if (messages.isEmpty()) {
            Toast.makeText(this, "No messages to delete", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] options = {"Delete for me", "Delete for everyone"};

        new AlertDialog.Builder(this)
                .setTitle("Delete Message")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showDeleteForMeDialog();
                    } else {
                        showDeleteForEveryoneDialog();
                    }
                })
                .show();
    }

    private void showDeleteForMeDialog() {
        String[] msgArray = new String[messages.size()];
        for (int i = 0; i < messages.size(); i++) {
            msgArray[i] = messages.get(i).getMessageText();
        }

        new AlertDialog.Builder(this)
                .setTitle("Select message to delete (for me)")
                .setItems(msgArray, (dialog, which) -> {
                    String msgId = messages.get(which).getMessageId();

                    FirebaseDatabase.getInstance().getReference()
                            .child("chats")
                            .child(senderRoom)
                            .child("messages")
                            .child(msgId)
                            .removeValue();

                    messages.remove(which);
                    adapter.notifyItemRemoved(which);

                    Toast.makeText(this, "Deleted for me", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void showDeleteForEveryoneDialog() {
        String[] msgArray = new String[messages.size()];
        for (int i = 0; i < messages.size(); i++) {
            msgArray[i] = messages.get(i).getMessageText();
        }

        // Inside showDeleteForEveryoneDialog()
        new AlertDialog.Builder(this)
                .setTitle("Select message to delete (for everyone)")
                .setItems(msgArray, (dialog, which) -> {
                    String msgId = messages.get(which).getMessageId();

                    if (msgId != null) {
                        FirebaseDatabase.getInstance().getReference()
                                .child("chats").child(senderRoom).child("messages")
                                .child(msgId).child("deleted").setValue(true);

                        FirebaseDatabase.getInstance().getReference()
                                .child("chats").child(receiverRoom).child("messages")
                                .child(msgId).child("deleted").setValue(true);
                    }

                    messages.get(which).setDeleted(true);
                    adapter.notifyItemChanged(which);

                    Toast.makeText(this, "Deleted for everyone", Toast.LENGTH_SHORT).show();
                })
                .show();
    }



    private void showLanguageDialog() {
        final String[] languages = {"English", "हिन्दी", "Español", "Français"};
        final String[] langCodes = {"en", "hi", "es", "fr"};

        new AlertDialog.Builder(this)
                .setTitle("Choose Language")
                .setItems(languages, (dialog, which) -> {
                    chatLang = langCodes[which];
                    adapter.setPreferredLang(chatLang);
                    adapter.notifyDataSetChanged();
                    setupTranslator(senderLang, chatLang);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String getMlKitLangCode(String isoCode) {
        switch (isoCode) {
            case "hi": return TranslateLanguage.HINDI;
            case "es": return TranslateLanguage.SPANISH;
            case "fr": return TranslateLanguage.FRENCH;
            default: return TranslateLanguage.ENGLISH;
        }
    }

    private String mapToCode(String lang) {
        if (lang == null) return "en";
        switch (lang.toLowerCase()) {
            case "english": return "en";
            case "हिन्दी":
            case "hindi": return "hi";
            case "español":
            case "spanish": return "es";
            case "français":
            case "french": return "fr";
            default: return "en";
        }
    }
}
