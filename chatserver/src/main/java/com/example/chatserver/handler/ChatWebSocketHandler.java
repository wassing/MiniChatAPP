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
//        sendPrivateWelcomeMessage(session);
        // 然后广播用户加入消息
        broadcastUserJoinMessage(username);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            String username = extractUsername(session);
            
            ChatMessage chatMessage = objectMapper.readValue(message.getPayload(), ChatMessage.class);
            if (MessageType.IMAGE.equals(chatMessage.getType())) {
                logger.info("收到来自用户 {} 的图片消息，长度: {}", username, chatMessage.getContent().length());
            }
            else {
                logger.info("收到来自用户 {} 的消息: {}", username, chatMessage.getContent());
            }

            // 处理认证消息
            if (chatMessage.getType() == MessageType.LOGIN) {
                handleLogin(session, chatMessage);
                return;
            } else if (chatMessage.getType() == MessageType.REGISTER) {
                handleRegistration(session, chatMessage);
                return;
            }

            // 如果是加入消息，直接返回
            if ("joined".equals(chatMessage.getContent())) {
                return;
            }

            // 如果是系统信息，直接返回
            if (MessageType.SYSTEM_NOTIFICATION.equals(chatMessage.getType())) {
                return;
            }

            // 如果是检查用户消息，则处理并返回
            if (MessageType.CHECK_USER.equals(chatMessage.getType())) {
                handleCheckUserMessage(session, chatMessage);
                return;
            }

            // 如果是添加联系人消息，发送通知给被添加的用户
            if (MessageType.CONTACT_ADDED.equals(chatMessage.getType())) {
                handleContactAdded(session, chatMessage);
            }

            // 如果是图片消息，记录图片长度
            if (MessageType.IMAGE.equals(chatMessage.getType())) {
                // TODO: 处理图片消息
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
            if (MessageType.IMAGE.equals(message.getType())) {
                logger.debug("广播图片消息");
            } else {
                logger.debug("广播消息: {}", messageJson);
            }
            
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
        leaveMessage.setTimestamp(System.currentTimeMillis());
        leaveMessage.setType(MessageType.SYSTEM_NOTIFICATION);
        leaveMessage.setStatus(MessageStatus.SENT);
        broadcastMessage(leaveMessage);
    }

//    private void sendPrivateWelcomeMessage(WebSocketSession session) {
//        ChatMessage welcomeMessage = new ChatMessage();
//        welcomeMessage.setId(System.currentTimeMillis());
//        welcomeMessage.setSenderId("System");
//        welcomeMessage.setContent("欢迎加入聊天室");
//        welcomeMessage.setRoomId("public");
//        welcomeMessage.setTimestamp(System.currentTimeMillis());
//        welcomeMessage.setType(MessageType.TEXT);
//        welcomeMessage.setStatus(MessageStatus.SENT);
//
//        try {
//            String welcomeJson = objectMapper.writeValueAsString(welcomeMessage);
//            session.sendMessage(new TextMessage(welcomeJson));
//        } catch (IOException e) {
//            logger.error("Failed to send welcome message", e);
//        }
//    }

    private void broadcastUserJoinMessage(String username) {
        ChatMessage joinMessage = new ChatMessage();
        joinMessage.setId(System.currentTimeMillis() + 1);
        joinMessage.setSenderId("System");
        joinMessage.setContent("用户 " + username + " 加入了聊天室");
        joinMessage.setRoomId("public");
        joinMessage.setTimestamp(System.currentTimeMillis());
        joinMessage.setType(MessageType.SYSTEM_NOTIFICATION);
        joinMessage.setStatus(MessageStatus.SENT);

        broadcastMessage(joinMessage);
    }

    ////////////////////////////////////////////////////////////////////
    /////////////////////////// 用户信息存储 ///////////////////////////
    ////////////////////////////////////////////////////////////////////

    // 添加用户存储
    private final ConcurrentHashMap<String, UserInfo> registeredUsers = new ConcurrentHashMap<>();

    // 添加用户信息类
    public static class UserInfo {
        private final String username;
        private final String password;
        
        public UserInfo(String username, String password) {
            this.username = username;
            this.password = password;
        }
        
        public String getUsername() { return username; }
        public String getPassword() { return password; }
    }

    // 添加用户查询方法
    public boolean isUserExists(String username) {
        return registeredUsers.containsKey(username) || sessions.containsKey(username);
    }

    private void sendAuthResponse(WebSocketSession session, String response) {
        try {
            ChatMessage authResponse = new ChatMessage();
            authResponse.setType(MessageType.AUTH_RESPONSE);
            authResponse.setContent(response);
            authResponse.setSenderId("System");
            
            String responseJson = objectMapper.writeValueAsString(authResponse);
            session.sendMessage(new TextMessage(responseJson));
        } catch (IOException e) {
            logger.error("发送认证响应失败", e);
        }
    }

    private void handleRegistration(WebSocketSession session, ChatMessage message) {
        try {
            String[] parts = message.getContent().split(":");
            String username = parts[0];
            String password = parts[1];
            
            if (isUserExists(username)) {
                sendAuthResponse(session, "USERNAME_EXISTS");
                return;
            }
            
            registeredUsers.put(username, new UserInfo(username, password));
            sendAuthResponse(session, "REGISTRATION_SUCCESS");
            logger.info("用户 {} 注册成功", username);
            
        } catch (Exception e) {
            logger.error("注册失败", e);
            sendAuthResponse(session, "REGISTRATION_FAILED");
        }
    }

    private void handleLogin(WebSocketSession session, ChatMessage message) {
        try {
            String[] parts = message.getContent().split(":");
            String username = parts[0];
            String password = parts[1];
            
            UserInfo userInfo = registeredUsers.get(username);
            if (userInfo != null && userInfo.getPassword().equals(password)) {
                sendAuthResponse(session, "LOGIN_SUCCESS");
                logger.info("用户 {} 登录成功", username);
            } else {
                sendAuthResponse(session, "LOGIN_FAILED");
                logger.info("用户 {} 登录失败", username);
            }
        } catch (Exception e) {
            logger.error("登录失败", e);
            sendAuthResponse(session, "LOGIN_FAILED");
        }
    }


    private void handleCheckUserMessage(WebSocketSession session, ChatMessage message) {
        String targetUsername = message.getContent();
        boolean exists = sessions.containsKey(targetUsername);
        
        ChatMessage response = new ChatMessage();
        response.setSenderId("System");
        response.setType(MessageType.USER_RESPONSE);
        response.setContent(Boolean.toString(exists));
    
        try {
            String responseJson = objectMapper.writeValueAsString(response);
            session.sendMessage(new TextMessage(responseJson));
        } catch (Exception e) {
            logger.error("Error sending user check response", e);
        }
    }
    
    private void handleContactAdded(WebSocketSession session, ChatMessage chatMessage) {
        // 获取被添加者用户名
        String targetUsername = chatMessage.getContent();
        // 获取发送者用户名
        String adderUsername = chatMessage.getSenderId();
        
        // 获取被添加者的session
        WebSocketSession targetSession = sessions.get(targetUsername);
        
        // 创建通知消息 - 发给被添加的用户
        ChatMessage notification = new ChatMessage();
        notification.setId(System.currentTimeMillis());
        notification.setSenderId(adderUsername);
        notification.setContent(String.format("用户 %s 已将您添加为联系人", adderUsername));
        notification.setRoomId("public");
        notification.setTimestamp(System.currentTimeMillis());
        notification.setType(MessageType.CONTACT_ADDED);
        notification.setStatus(MessageStatus.SENT);

        if (targetSession != null && targetSession.isOpen()) {
            try {
                String notificationJson = objectMapper.writeValueAsString(notification);
                targetSession.sendMessage(new TextMessage(notificationJson));
                logger.info("已向{}发送联系人添加通知: {}", targetUsername, notificationJson);
            } catch (IOException e) {
                logger.error("发送联系人通知失败: {}", e.getMessage());
            }
        } else {
            logger.warn("目标用户 {} 不在线或session无效", targetUsername);
        }

        // 给发送者一个确认消息
        ChatMessage confirmMessage = new ChatMessage();
        confirmMessage.setId(System.currentTimeMillis() + 1);
        confirmMessage.setSenderId("System");
        confirmMessage.setContent(String.format("您已成功添加 %s 为联系人", targetUsername));
        confirmMessage.setRoomId("public");
        confirmMessage.setTimestamp(System.currentTimeMillis());
        confirmMessage.setType(MessageType.SYSTEM_NOTIFICATION);
        confirmMessage.setStatus(MessageStatus.SENT);

        WebSocketSession senderSession = sessions.get(adderUsername);
        if (senderSession != null && senderSession.isOpen()) {
            try {
                String confirmJson = objectMapper.writeValueAsString(confirmMessage);
                senderSession.sendMessage(new TextMessage(confirmJson));
                logger.info("已向{}发送添加确认消息: {}", adderUsername, confirmJson);
            } catch (IOException e) {
                logger.error("发送确认消息失败: {}", e.getMessage());
            }
        }
        
        return;
    }
}