package com.zixiang.yanmanus.memory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
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

    public List<Message> getMessages(Long userId, String sessionId) {
        String cacheKey = buildCacheKey(userId, sessionId);
        return localCache.computeIfAbsent(cacheKey, k -> loadFromRedis(userId, sessionId));
    }

    public void saveMessages(Long userId, String sessionId, List<Message> messages) {
        List<Message> trimmed = trimMessages(messages);
        String cacheKey = buildCacheKey(userId, sessionId);
        localCache.put(cacheKey, new ArrayList<>(trimmed));
        saveToRedis(userId, sessionId, trimmed);
    }

    public void clearSession(Long userId, String sessionId) {
        String cacheKey = buildCacheKey(userId, sessionId);
        localCache.remove(cacheKey);
        redisTemplate.delete(buildRedisKey(userId, sessionId));
    }

    public boolean sessionExists(Long userId, String sessionId) {
        String cacheKey = buildCacheKey(userId, sessionId);
        if (localCache.containsKey(cacheKey)) {
            return true;
        }
        return redisTemplate.hasKey(buildRedisKey(userId, sessionId));
    }

    private List<Message> loadFromRedis(Long userId, String sessionId) {
        try {
            String json = redisTemplate.opsForValue().get(buildRedisKey(userId, sessionId));
            if (json == null) {
                return new ArrayList<>();
            }
            List<Message> messages = objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Message.class));
            log.info("从 Redis 加载用户 {} 会话 {} 的 {} 条消息", userId, sessionId, messages.size());
            return new ArrayList<>(messages);
        } catch (JsonProcessingException e) {
            log.error("反序列化会话消息失败, userId={}, sessionId={}", userId, sessionId, e);
            return new ArrayList<>();
        }
    }

    private void saveToRedis(Long userId, String sessionId, List<Message> messages) {
        try {
            String json = objectMapper.writeValueAsString(messages);
            redisTemplate.opsForValue().set(buildRedisKey(userId, sessionId), json, TTL_HOURS, TimeUnit.HOURS);
        } catch (JsonProcessingException e) {
            log.error("序列化会话消息失败, userId={}, sessionId={}", userId, sessionId, e);
        }
    }

    private List<Message> trimMessages(List<Message> messages) {
        if (messages.size() <= MAX_MESSAGES) {
            return messages;
        }
        log.info("消息数量 {} 超过上限 {}，截断早期消息", messages.size(), MAX_MESSAGES);
        return new ArrayList<>(messages.subList(messages.size() - MAX_MESSAGES, messages.size()));
    }

    private String buildRedisKey(Long userId, String sessionId) {
        return KEY_PREFIX + userId + ":" + sessionId;
    }

    private String buildCacheKey(Long userId, String sessionId) {
        return userId + ":" + sessionId;
    }
}
