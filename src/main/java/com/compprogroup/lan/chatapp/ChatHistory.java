package com.compprogroup.lan.chatapp;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ChatHistory {
    private final List<ChatMessage> messages;
    private final int maxHistorySize;

    public ChatHistory() {
        this(1000); // Default to storing last 1000 messages
    }

    public ChatHistory(int maxHistorySize) {
        this.messages = Collections.synchronizedList(new ArrayList<>());
        this.maxHistorySize = maxHistorySize;
    }

    public void addMessage(String username, String content) {
        ChatMessage message = new ChatMessage(username, content, LocalDateTime.now());
        
        synchronized (messages) {
            messages.add(message);
            
            // Remove oldest messages if we exceed max size
            while (messages.size() > maxHistorySize) {
                messages.remove(0);
            }
        }
        
        System.out.println("Message logged: [" + message.getFormattedTimestamp() + "] " + username + ": " + content);
    }

    public List<ChatMessage> getRecentMessages(int count) {
        synchronized (messages) {
            int start = Math.max(0, messages.size() - count);
            return new ArrayList<>(messages.subList(start, messages.size()));
        }
    }

    public List<ChatMessage> getAllMessages() {
        synchronized (messages) {
            return new ArrayList<>(messages);
        }
    }

    public int getMessageCount() {
        return messages.size();
    }

    public void clearHistory() {
        synchronized (messages) {
            messages.clear();
        }
        System.out.println("Chat history cleared");
    }

    public static class ChatMessage {
        private final String username;
        private final String content;
        private final LocalDateTime timestamp;

        public ChatMessage(String username, String content, LocalDateTime timestamp) {
            this.username = username;
            this.content = content;
            this.timestamp = timestamp;
        }

        public String getUsername() { return username; }
        public String getContent() { return content; }
        public LocalDateTime getTimestamp() { return timestamp; }
        
        public String getFormattedTimestamp() {
            return timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        }

        @Override
        public String toString() {
            return String.format("[%s] %s: %s", getFormattedTimestamp(), username, content);
        }
    }
}