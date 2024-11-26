// src/main/java/com/example/chatserver/handler/ChatWebSocketHandler.java
package com.example.chatserver.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import com.example.chatserver.model.ChatMessage;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {
    
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // 从URL参数中获取用户名
        String username = extractUsername(session);
        sessions.put(username, session);
        
        // 广播用户加入消息
        broadcastMessage(createSystemMessage(username + " 加入了聊天室"));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            ChatMessage chatMessage = objectMapper.readValue(message.getPayload(), ChatMessage.class);
            broadcastMessage(chatMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String username = extractUsername(session);
        sessions.remove(username);
        
        // 广播用户离开消息
        broadcastMessage(createSystemMessage(username + " 离开了聊天室"));
    }

    private void broadcastMessage(ChatMessage message) {
        try {
            String messageJson = objectMapper.writeValueAsString(message);
            TextMessage textMessage = new TextMessage(messageJson);
            
            sessions.values().forEach(session -> {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(textMessage);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String extractUsername(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query != null && query.startsWith("username=")) {
            return query.substring(9); // "username=".length() == 9
        }
        return "anonymous-" + session.getId();
    }

    private ChatMessage createSystemMessage(String content) {
        ChatMessage message = new ChatMessage();
        message.setSenderId("System");
        message.setContent(content);
        message.setType(ChatMessage.MessageType.TEXT);
        return message;
    }
}