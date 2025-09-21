package com.example.letstalk.Adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.letstalk.Models.Message;
import com.example.letstalk.R;
import com.example.letstalk.databinding.ItemReceiveBinding;
import com.example.letstalk.databinding.ItemSendBinding;
import com.github.pgreze.reactions.ReactionPopup;
import com.github.pgreze.reactions.ReactionsConfig;
import com.github.pgreze.reactions.ReactionsConfigBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Objects;

public class MessagesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final Context context;
    private final ArrayList<Message> messages;

    private final int ITEM_SENT = 1;
    private final int ITEM_RECEIVE = 2;
    private final String senderRoom;
    private final String receiverRoom;
    private String preferredLang;

    public MessagesAdapter(Context context, ArrayList<Message> messages,
                           String preferredLang, String senderRoom, String receiverRoom){
        this.context = context;
        this.messages = messages;
        this.preferredLang = preferredLang;
        this.senderRoom = senderRoom;
        this.receiverRoom = receiverRoom;
    }

    public void setPreferredLang(String lang) {
        this.preferredLang = lang;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == ITEM_SENT) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_send, parent, false);
            return new SentViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_receive, parent, false);
            return new ReceiveViewHolder(view);
        }
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);
        if (Objects.equals(FirebaseAuth.getInstance().getUid(), message.getSenderId())) {
            return ITEM_SENT;
        } else {
            return ITEM_RECEIVE;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messages.get(position);

        String displayText = message.isDeleted()
                ? "This message was deleted"
                : message.getDisplayMessage(preferredLang);

        int[] reactions = new int[]{
                R.drawable.ic_fb_like,
                R.drawable.ic_fb_love,
                R.drawable.ic_fb_laugh,
                R.drawable.ic_fb_wow,
                R.drawable.ic_fb_sad,
                R.drawable.ic_fb_angry
        };

        ReactionsConfig config = new ReactionsConfigBuilder(context)
                .withReactions(reactions)
                .build();

        ReactionPopup popup = new ReactionPopup(context, config, (pos) -> {
            if (holder instanceof SentViewHolder) {
                ((SentViewHolder) holder).binding.feeling.setImageResource(reactions[pos]);
                ((SentViewHolder) holder).binding.feeling.setVisibility(View.VISIBLE);
            } else if (holder instanceof ReceiveViewHolder) {
                ((ReceiveViewHolder) holder).binding.feeling.setImageResource(reactions[pos]);
                ((ReceiveViewHolder) holder).binding.feeling.setVisibility(View.VISIBLE);
            }

            message.setFeeling(pos);

            if (senderRoom != null && receiverRoom != null && message.getMessageId() != null) {
                FirebaseDatabase.getInstance().getReference()
                        .child("chats").child(senderRoom).child("messages")
                        .child(message.getMessageId()).setValue(message);

                FirebaseDatabase.getInstance().getReference()
                        .child("chats").child(receiverRoom).child("messages")
                        .child(message.getMessageId()).setValue(message);
            }
            return true;
        });

        if (holder instanceof SentViewHolder) {
            SentViewHolder viewHolder = (SentViewHolder) holder;
            viewHolder.binding.message.setText(displayText);

            if (message.isDeleted()) {
                viewHolder.binding.message.setTypeface(null, Typeface.ITALIC);
                viewHolder.binding.message.setTextColor(Color.GRAY);
                viewHolder.binding.feeling.setVisibility(View.GONE);
            } else {
                viewHolder.binding.message.setTypeface(null, Typeface.NORMAL);
                viewHolder.binding.message.setTextColor(Color.BLACK);

                if (message.getFeeling() >= 0) {
                    viewHolder.binding.feeling.setImageResource(reactions[message.getFeeling()]);
                    viewHolder.binding.feeling.setVisibility(View.VISIBLE);
                } else {
                    viewHolder.binding.feeling.setVisibility(View.GONE);
                }
            }

            viewHolder.binding.message.setOnTouchListener((v, motionEvent) -> {
                if (!message.isDeleted())
                    popup.onTouch(v, motionEvent);
                return false;
            });

        } else if (holder instanceof ReceiveViewHolder) {
            ReceiveViewHolder viewHolder = (ReceiveViewHolder) holder;
            viewHolder.binding.message.setText(displayText);

            if (message.isDeleted()) {
                viewHolder.binding.message.setTypeface(null, Typeface.ITALIC);
                viewHolder.binding.message.setTextColor(Color.GRAY);
                viewHolder.binding.feeling.setVisibility(View.GONE);
            } else {
                viewHolder.binding.message.setTypeface(null, Typeface.NORMAL);
                viewHolder.binding.message.setTextColor(Color.BLACK);

                if (message.getFeeling() >= 0) {
                    viewHolder.binding.feeling.setImageResource(reactions[message.getFeeling()]);
                    viewHolder.binding.feeling.setVisibility(View.VISIBLE);
                } else {
                    viewHolder.binding.feeling.setVisibility(View.GONE);
                }
            }

            viewHolder.binding.message.setOnTouchListener((v, motionEvent) -> {
                if (!message.isDeleted())
                    popup.onTouch(v, motionEvent);
                return false;
            });
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public static class SentViewHolder extends RecyclerView.ViewHolder {
        ItemSendBinding binding;
        public SentViewHolder(@NonNull View itemView) {
            super(itemView);
            binding = ItemSendBinding.bind(itemView);
        }
    }

    public static class ReceiveViewHolder extends RecyclerView.ViewHolder {
        ItemReceiveBinding binding;
        public ReceiveViewHolder(@NonNull View itemView) {
            super(itemView);
            binding = ItemReceiveBinding.bind(itemView);
        }
    }
}
