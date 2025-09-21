package com.example.letstalk.Models;

import com.google.firebase.database.Exclude;

import java.util.Map;

public class Message {
    private String messageId;
    private String messageText;
    private String senderId;
    private long timestamp;
    private int feeling = -1;

    private Map<String, String> translations;
    private boolean isDeleted;

    public Message() {}

    public Message(String message, String senderId, long timestamp) {
        this.messageText = message;
        this.senderId = senderId;
        this.timestamp = timestamp;
    }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getMessageText() { return messageText; }
    public void setMessageText(String messageText) { this.messageText = messageText; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public int getFeeling() { return feeling; }
    public void setFeeling(int feeling) { this.feeling = feeling; }

    public Map<String, String> getTranslations() { return translations; }
    public void setTranslations(Map<String, String> translations) { this.translations = translations; }

    @Exclude
    public String getDisplayMessage(String preferredLang) {
        if (translations != null && translations.containsKey(preferredLang)) {
            return translations.get(preferredLang);
        }
        return messageText != null ? messageText : "";
    }

    public boolean isDeleted() { return isDeleted; }
    public void setDeleted(boolean deleted) { isDeleted = deleted; }
}
