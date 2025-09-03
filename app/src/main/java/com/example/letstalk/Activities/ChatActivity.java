package com.example.letstalk.Activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.letstalk.Adapters.MessagesAdapter;
import com.example.letstalk.Models.Message;
import com.example.letstalk.R;
import com.example.letstalk.databinding.ActivityChatBinding;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class ChatActivity extends AppCompatActivity {

    ActivityChatBinding binding;
    MessagesAdapter adapter;
    ArrayList<Message> messages;
    String senderRoom, receiverRoom;
    FirebaseDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String name = getIntent().getStringExtra("name");
        String senderUid = FirebaseAuth.getInstance().getUid();
        String receiverUid = getIntent().getStringExtra("uid");

        Log.d("CHAT_INIT", "SenderRoom: " + senderRoom + ", ReceiverRoom: " + receiverRoom);

        senderRoom = senderUid + receiverUid;
        receiverRoom = receiverUid + senderUid;

        messages = new ArrayList<>();
        adapter = new MessagesAdapter(this, messages, senderRoom, receiverRoom);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);

        database = FirebaseDatabase.getInstance();
        database.getReference().child("chats")
                .child(senderRoom)
                .child("messages")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        messages.clear();
                        for (DataSnapshot snapshot1: snapshot.getChildren()){
                            Message message = snapshot1.getValue(Message.class);
                            message.setMessageId(snapshot1.getKey());
                            messages.add(message);
                        }

                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        
                    }
                });

//        binding.sendBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                String messageTxt = binding.msgBox.getText().toString();
//
//                Date date = new Date();
//                Message message = new Message(messageTxt, senderUid, date.getTime(), detectedLanguage, translatedMessage);
//                binding.msgBox.setText("");
//
//                String randomKey = database.getReference().push().getKey();
//                HashMap<String, Object> lastMsgObj = new HashMap<>();
//                lastMsgObj.put("lastMsg", message.getMessage());
//                lastMsgObj.put("lastMsgTime", date.getTime());
//                database.getReference().child("chats").child(senderRoom).updateChildren(lastMsgObj);
//                database.getReference().child("chats").child(receiverRoom).updateChildren(lastMsgObj);
//
//
//                database.getReference().child("chats")
//                    .child(senderRoom)
//                    .child("messages")
//                    .child(randomKey)
//                    .setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
//                        @Override
//                        public void onSuccess(Void aVoid) {
//                            database.getReference().child("chats")
//                                    .child(receiverRoom)
//                                    .child("messages")
//                                    .child(randomKey)
//                                    .setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
//                                @Override
//                                public void onSuccess(Void aVoid) {
//                                }
//                            });
//
//                        }
//                    });
//            }
//        });

        binding.sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String messageTxt = binding.msgBox.getText().toString();
                if (messageTxt.isEmpty()) return;

                String senderUid = FirebaseAuth.getInstance().getUid();
                Date date = new Date();
                Message message = new Message(messageTxt, senderUid, date.getTime());
                binding.msgBox.setText("");

                String randomKey = database.getReference().push().getKey();
                HashMap<String, Object> lastMsgObj = new HashMap<>();
                lastMsgObj.put("lastMsg", message.getMessage());
                lastMsgObj.put("lastMsgTime", date.getTime());
                database.getReference().child("chats").child(senderRoom).updateChildren(lastMsgObj);
                database.getReference().child("chats").child(receiverRoom).updateChildren(lastMsgObj);


                database.getReference().child("chats")
                    .child(senderRoom)
                    .child("messages")
                    .child(randomKey)
                    .setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            database.getReference().child("chats")
                                    .child(receiverRoom)
                                    .child("messages")
                                    .child(randomKey)
                                    .setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                }
                            });

                        }
                    });
            }
        });

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(name);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

//        getSupportActionBar().setTitle(name);
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }
}