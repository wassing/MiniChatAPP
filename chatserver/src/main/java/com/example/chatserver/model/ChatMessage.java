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
        TEXT,           // 普通文本消息
        IMAGE,          // 图片消息
        CHECK_USER,     // 检查用户是否存在
        USER_RESPONSE,  // 用户查询响应
        CONTACT_ADDED,   // 添加联系人通知
        SYSTEM_NOTIFICATION // 系统通知
    }

    public enum MessageStatus {
        SENDING,
        SENT,
        FAILED
    }
}