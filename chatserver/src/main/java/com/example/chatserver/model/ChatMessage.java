// src/main/java/com/example/chatserver/model/ChatMessage.java
package com.example.chatserver.model;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatMessage {
    private Long id;
    private String roomId;
    private String senderId;
    private String content;
    private Long timestamp;
    private MessageType type;
    private MessageStatus status;

    // 无参构造函数
    public ChatMessage() {
        this.id = System.currentTimeMillis();
        this.timestamp = System.currentTimeMillis();
        this.type = MessageType.TEXT;
        this.status = MessageStatus.SENDING;
    }

    public enum MessageType {
        TEXT,
        IMAGE
    }

    public enum MessageStatus {
        SENDING,
        SENT,
        FAILED
    }
}