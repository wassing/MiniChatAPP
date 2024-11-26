// src/main/java/com/example/chatserver/model/ChatMessage.java
package com.example.chatserver.model;

import lombok.Data;
import java.time.Instant;

@Data
public class ChatMessage {
    private String id;
    private String senderId;
    private String content;
    private long timestamp;
    private MessageType type;

    public enum MessageType {
        TEXT,
        IMAGE
    }
    
    public ChatMessage() {
        this.id = String.valueOf(System.currentTimeMillis());
        this.timestamp = Instant.now().toEpochMilli();
    }
}