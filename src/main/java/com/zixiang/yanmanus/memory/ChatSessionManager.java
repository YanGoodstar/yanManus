package com.zixiang.yanmanus.memory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class ChatSessionManager {

    private static final String KEY_PREFIX = "yanmanus:session:";
    private static final long TTL_HOURS = 2;
    private static final int MAX_MESSAGES = 50;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, List<Message>> localCache = new ConcurrentHashMap<>();

    public ChatSessionManager(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("org.springframework.ai.chat.messages.")
                .allowIfSubType("java.util.")
                .allowIfSubType("java.lang.")
                .allowIfSubType("java.util.LinkedHashMap")
                .build();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.OBJECT_AND_NON_CONCRETE);
    }

    public List<Message> getMessages(String sessionId) {
        return localCache.computeIfAbsent(sessionId, this::loadFromRedis);
    }

    public void saveMessages(String sessionId, List<Message> messages) {
        List<Message> trimmed = trimMessages(messages);
        localCache.put(sessionId, new ArrayList<>(trimmed));
        saveToRedis(sessionId, trimmed);
    }

    public void clearSession(String sessionId) {
        localCache.remove(sessionId);
        redisTemplate.delete(KEY_PREFIX + sessionId);
    }

    public boolean sessionExists(String sessionId) {
        if (localCache.containsKey(sessionId)) {
            return true;
        }
        return redisTemplate.hasKey(KEY_PREFIX + sessionId);
    }

    private List<Message> loadFromRedis(String sessionId) {
        try {
            String json = redisTemplate.opsForValue().get(KEY_PREFIX + sessionId);
            if (json == null) {
                return new ArrayList<>();
            }
            List<Message> messages = objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Message.class));
            log.info("从 Redis 加载会话 {} 的 {} 条消息", sessionId, messages.size());
            return new ArrayList<>(messages);
        } catch (JsonProcessingException e) {
            log.error("反序列化会话消息失败, sessionId={}", sessionId, e);
            return new ArrayList<>();
        }
    }

    private void saveToRedis(String sessionId, List<Message> messages) {
        try {
            String json = objectMapper.writeValueAsString(messages);
            redisTemplate.opsForValue().set(KEY_PREFIX + sessionId, json, TTL_HOURS, TimeUnit.HOURS);
        } catch (JsonProcessingException e) {
            log.error("序列化会话消息失败, sessionId={}", sessionId, e);
        }
    }

    private List<Message> trimMessages(List<Message> messages) {
        if (messages.size() <= MAX_MESSAGES) {
            return messages;
        }
        log.info("消息数量 {} 超过上限 {}，截断早期消息", messages.size(), MAX_MESSAGES);
        return new ArrayList<>(messages.subList(messages.size() - MAX_MESSAGES, messages.size()));
    }
}
