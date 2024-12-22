package com.example.chatserver.handler;

import com.example.chatserver.model.ChatMessage;
import com.example.chatserver.model.ChatMessage.MessageStatus;
import com.example.chatserver.model.ChatMessage.MessageType;
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
        
        // 首先发送私人欢迎消息
        sendPrivateWelcomeMessage(session);
        // 然后广播用户加入消息
        broadcastUserJoinMessage(username);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            String username = extractUsername(session);
            logger.info("收到来自用户 {} 的消息: {}", username, message.getPayload());
            
            ChatMessage chatMessage = objectMapper.readValue(message.getPayload(), ChatMessage.class);
            
            // 如果是加入消息，直接返回
            if ("joined".equals(chatMessage.getContent())) {
                logger.debug("忽略加入消息");
                return;
            }
            
            // 更新消息状态为已发送
            chatMessage.setStatus(MessageStatus.SENT);
            
            // 根据roomId处理消息
            if ("public".equals(chatMessage.getRoomId())) {
                broadcastMessage(chatMessage);
            } else {
                handlePrivateMessage(chatMessage);
            }
        } catch (Exception e) {
            logger.error("处理消息时发生错误", e);
            sendErrorMessage(session, "消息处理失败: " + e.getMessage());
        }
    }


    private void broadcastMessage(ChatMessage message) {
        try {
            String messageJson = objectMapper.writeValueAsString(message);
            logger.debug("广播消息: {}", messageJson);
            
            TextMessage textMessage = new TextMessage(messageJson);
            for (WebSocketSession session : sessions.values()) {
                if (session != null && session.isOpen()) {
                    try {
                        session.sendMessage(textMessage);
                        logger.debug("消息已发送到session: {}", extractUsername(session));
                    } catch (IOException e) {
                        logger.error("发送消息到用户失败: {}", extractUsername(session), e);
                    }
                }
            }
        } catch (JsonProcessingException e) {
            logger.error("消息序列化失败", e);
        }
    }

    private void handlePrivateMessage(ChatMessage message) {
        try {
            String messageJson = objectMapper.writeValueAsString(message);
            TextMessage textMessage = new TextMessage(messageJson);
            
            String[] participants = message.getRoomId().split("-");
            for (String participant : participants) {
                WebSocketSession session = sessions.get(participant);
                if (session != null && session.isOpen()) {
                    try {
                        session.sendMessage(textMessage);
                        logger.debug("私聊消息已发送到用户: {}", participant);
                    } catch (IOException e) {
                        logger.error("发送私聊消息失败: {}", e);
                    }
                }
            }
        } catch (JsonProcessingException e) {
            logger.error("消息序列化失败", e);
        }
    }

    private void sendErrorMessage(WebSocketSession session, String errorMessage) {
        try {
            ChatMessage error = new ChatMessage();
            error.setSenderId("System");
            error.setContent(errorMessage);
            error.setRoomId("public");
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(error)));
        } catch (IOException e) {
            logger.error("发送错误消息失败", e);
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
        
        // 通知其他用户
        ChatMessage leaveMessage = new ChatMessage();
        leaveMessage.setSenderId("System");
        leaveMessage.setContent("用户 " + username + " 离开了聊天室");
        leaveMessage.setRoomId("public");
        broadcastMessage(leaveMessage);
    }

    private void sendPrivateWelcomeMessage(WebSocketSession session) {
        ChatMessage welcomeMessage = new ChatMessage();
        welcomeMessage.setId(System.currentTimeMillis());
        welcomeMessage.setSenderId("System");
        welcomeMessage.setContent("欢迎加入聊天室");
        welcomeMessage.setRoomId("public");
        welcomeMessage.setTimestamp(System.currentTimeMillis());
        welcomeMessage.setType(MessageType.TEXT);
        welcomeMessage.setStatus(MessageStatus.SENT);

        try {
            String welcomeJson = objectMapper.writeValueAsString(welcomeMessage);
            session.sendMessage(new TextMessage(welcomeJson));
        } catch (IOException e) {
            logger.error("Failed to send welcome message", e);
        }
    }

    private void broadcastUserJoinMessage(String username) {
        ChatMessage joinMessage = new ChatMessage();
        joinMessage.setId(System.currentTimeMillis() + 1);
        joinMessage.setSenderId("System");
        joinMessage.setContent("用户 " + username + " 加入了聊天室");
        joinMessage.setRoomId("public");
        joinMessage.setTimestamp(System.currentTimeMillis());
        joinMessage.setType(MessageType.TEXT);
        joinMessage.setStatus(MessageStatus.SENT);

        broadcastMessage(joinMessage);
    }
}