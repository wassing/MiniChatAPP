package com.example.chatserver.model;

import lombok.Data;
import java.time.Instant;

@Data
public class ChatMessage {
    private String id;
    private String roomId;  // 添加roomId字段
    private String senderId;
    private String content;
    private MessageType type;
    private long timestamp;

    public enum MessageType {
        TEXT,
        IMAGE
    }

    public ChatMessage() {
        this.id = String.valueOf(System.currentTimeMillis());
        this.timestamp = Instant.now().toEpochMilli();
        this.type = MessageType.TEXT;
    }
}