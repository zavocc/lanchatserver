package com.compprogroup.lan.chatapp;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.time.LocalDateTime;

/**
 * Base class for all JSON messages exchanged between client and server
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = ChatMessage.class, name = "chat"),
    @JsonSubTypes.Type(value = StatusMessage.class, name = "status"),
    @JsonSubTypes.Type(value = AuthMessage.class, name = "auth"),
    @JsonSubTypes.Type(value = UserMessage.class, name = "user")
})
public abstract class Message {
    @JsonProperty("type")
    public abstract String getType();
}

/**
 * Chat message sent by users
 */
class ChatMessage extends Message {
    @JsonProperty("username")
    private String username;
    
    @JsonProperty("content")
    private String content;
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    // Default constructor for Jackson
    public ChatMessage() {}

    public ChatMessage(String username, String content) {
        this.username = username;
        this.content = content;
        this.timestamp = LocalDateTime.now();
    }

    public ChatMessage(String username, String content, LocalDateTime timestamp) {
        this.username = username;
        this.content = content;
        this.timestamp = timestamp;
    }

    @Override
    public String getType() {
        return "chat";
    }

    // Getters and setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}

/**
 * Status/system messages from server
 */
class StatusMessage extends Message {
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("level")
    private String level; // INFO, WARNING, ERROR

    public StatusMessage() {}

    public StatusMessage(String message) {
        this(message, "INFO");
    }

    public StatusMessage(String message, String level) {
        this.message = message;
        this.level = level;
    }

    @Override
    public String getType() {
        return "status";
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
}

/**
 * Authentication messages
 */
class AuthMessage extends Message {
    @JsonProperty("action")
    private String action; // REQUIRED, SUCCESS, FAILED
    
    @JsonProperty("key")
    private String key;

    public AuthMessage() {}

    public AuthMessage(String action) {
        this.action = action;
    }

    public AuthMessage(String action, String key) {
        this.action = action;
        this.key = key;
    }

    @Override
    public String getType() {
        return "auth";
    }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
}

/**
 * User-related messages (join, leave, username validation)
 */
class UserMessage extends Message {
    @JsonProperty("action")
    private String action; // REQUIRED, SUCCESS, TAKEN, INVALID, JOIN, LEAVE
    
    @JsonProperty("username")
    private String username;

    public UserMessage() {}

    public UserMessage(String action) {
        this.action = action;
    }

    public UserMessage(String action, String username) {
        this.action = action;
        this.username = username;
    }

    @Override
    public String getType() {
        return "user";
    }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}