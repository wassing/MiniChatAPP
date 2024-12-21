package com.example.chatserver.handler;

import com.example.chatserver.model.ChatMessage;
import com.example.chatserver.model.ChatMessage.MessageStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketHandler.class);
    
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String username = extractUsername(session);
        logger.info("用户 {} 已连接", username);
        sessions.put(username, session);
        
        // 发送连接成功消息
        sendSystemMessage(session, "连接成功");
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            String username = extractUsername(session);
            ChatMessage chatMessage = objectMapper.readValue(message.getPayload(), ChatMessage.class);
            
            // 更新消息状态为已发送
            chatMessage.setStatus(MessageStatus.SENT);
            
            if ("public".equals(chatMessage.getRoomId())) {
                // 广播到所有连接的客户端
                broadcastMessage(chatMessage);
            } else {
                // 处理私聊消息
                handlePrivateMessage(chatMessage);
            }
            
            logger.debug("处理消息: {}", objectMapper.writeValueAsString(chatMessage));
            
        } catch (Exception e) {
            logger.error("处理消息时发生错误", e);
            try {
                ChatMessage errorMessage = new ChatMessage();
                errorMessage.setSenderId("System");
                errorMessage.setContent("消息处理失败: " + e.getMessage());
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(errorMessage)));
            } catch (IOException ex) {
                logger.error("发送错误消息失败", ex);
            }
        }
    }

    private void broadcastMessage(ChatMessage message) {
        String messageJson;
        try {
            messageJson = objectMapper.writeValueAsString(message);
            TextMessage textMessage = new TextMessage(messageJson);
            
            for (WebSocketSession session : sessions.values()) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(textMessage);
                    } catch (IOException e) {
                        logger.error("发送消息失败: {}", e.getMessage());
                    }
                }
            }
        } catch (JsonProcessingException e) {
            logger.error("消息序列化失败", e);
        }
    }

    private void handlePrivateMessage(ChatMessage message) {
        String[] participants = message.getRoomId().split("-");
        for (String participant : participants) {
            WebSocketSession session = sessions.get(participant);
            if (session != null && session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
                } catch (IOException e) {
                    logger.error("发送私聊消息失败: {}", e.getMessage());
                }
            }
        }
    }

    private void sendSystemMessage(WebSocketSession session, String content) {
        try {
            ChatMessage systemMessage = new ChatMessage();
            systemMessage.setSenderId("System");
            systemMessage.setContent(content);
            systemMessage.setRoomId("public");
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(systemMessage)));
        } catch (IOException e) {
            logger.error("发送系统消息失败", e);
        }
    }

    private String extractUsername(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query != null && query.startsWith("username=")) {
            return query.substring(9);
        }
        return "anonymous-" + session.getId();
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String username = extractUsername(session);
        sessions.remove(username);
        logger.info("用户 {} 断开连接, 状态: {}", username, status);
    }
}