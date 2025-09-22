package com.example.letstalk.Adapters;

import android.content.Context;
import android.content.Intent;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.letstalk.Activities.ChatActivity;
import com.example.letstalk.R;
import com.example.letstalk.Models.User;
import com.example.letstalk.databinding.RowConversationBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Date;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.UsersViewHolder> {
    Context context;
    ArrayList<User> users;

    public UsersAdapter(Context context, ArrayList<User> users) {
        this.context = context;
        this.users = users;
    }

    @NonNull
    @Override
    public UsersViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_conversation, parent, false);
        return new UsersViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UsersViewHolder holder, int position) {
        User user = users.get(position);

        String senderId = FirebaseAuth.getInstance().getUid();
        String senderRoom = senderId + user.getUid();
        String receiverRoom = user.getUid() + senderId;

        DatabaseReference chatsRef = FirebaseDatabase.getInstance().getReference().child("chats");

        // ✅ First check senderRoom
        chatsRef.child(senderRoom).child("messages").limitToLast(1)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            showLastMessage(snapshot, holder);
                        } else {
                            // ✅ If no message in senderRoom, check receiverRoom
                            chatsRef.child(receiverRoom).child("messages").limitToLast(1)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot2) {
                                            if (snapshot2.exists()) {
                                                showLastMessage(snapshot2, holder);
                                            } else {
                                                holder.binding.lastMsg.setText("Tap to chat");
                                                holder.binding.msgTime.setText("");
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {}
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });

        holder.binding.username.setText(user.getName());

        String profileImage = user.getProfileImage();
        if (profileImage != null) {
            if (profileImage.matches("\\d+")) {
                int resId = Integer.parseInt(profileImage);
                holder.binding.profile.setImageResource(resId);
            } else {
                Glide.with(context)
                        .load(profileImage)
                        .placeholder(R.drawable.avatar)
                        .into(holder.binding.profile);
            }
        } else {
            holder.binding.profile.setImageResource(R.drawable.avatar);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ChatActivity.class);
            intent.putExtra("name", user.getName());
            intent.putExtra("uid", user.getUid());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public void updateList(ArrayList<User> newList) {
        this.users = newList;
        notifyDataSetChanged();
    }

    // ✅ Proper helper method OUTSIDE onDataChange
    private void showLastMessage(DataSnapshot snapshot, UsersViewHolder holder) {
        for (DataSnapshot data : snapshot.getChildren()) {

            String lastMsg = null;
            if (data.child("translations").exists()) {
                // try preferred language
                lastMsg = data.child("translations").child("en").getValue(String.class);
            }
            if (lastMsg == null) {
                lastMsg = data.child("messageText").getValue(String.class);
            }

            Long timeObj = data.child("timestamp").getValue(Long.class);

            if (lastMsg != null && !lastMsg.trim().isEmpty()) {
                holder.binding.lastMsg.setText(lastMsg);
            } else {
                holder.binding.lastMsg.setText("Tap to chat");
            }

            if (timeObj != null) {
                if (timeObj < 1000000000000L) {
                    timeObj *= 1000; // seconds → ms
                }
                String time = DateFormat.format("hh:mm a", new Date(timeObj)).toString();
                holder.binding.msgTime.setText(time);
            } else {
                holder.binding.msgTime.setText("");
            }
        }
    }

    public static class UsersViewHolder extends RecyclerView.ViewHolder {
        RowConversationBinding binding;

        public UsersViewHolder(@NonNull View itemview) {
            super(itemview);
            binding = RowConversationBinding.bind(itemview);
        }
    }
}
