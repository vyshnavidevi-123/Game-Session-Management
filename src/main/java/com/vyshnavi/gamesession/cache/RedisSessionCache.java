package com.vyshnavi.gamesession.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vyshnavi.gamesession.session.SessionResponse;
import java.time.Duration;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis-backed cache. Failures degrade to a cache miss so a Redis outage never takes the API
 * down; the database stays the source of truth.
 */
@Component
@ConditionalOnProperty(name = "cache.type", havingValue = "redis", matchIfMissing = true)
public class RedisSessionCache implements SessionCache {

    private static final Logger log = LoggerFactory.getLogger(RedisSessionCache.class);
    private static final String PREFIX = "session:";

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final Duration ttl;

    public RedisSessionCache(StringRedisTemplate redis, ObjectMapper mapper,
                             @Value("${cache.session-ttl-seconds:600}") long ttlSeconds) {
        this.redis = redis;
        this.mapper = mapper;
        this.ttl = Duration.ofSeconds(ttlSeconds);
    }

    @Override
    public Optional<SessionResponse> get(String sessionId) {
        try {
            String json = redis.opsForValue().get(PREFIX + sessionId);
            return json == null ? Optional.empty()
                    : Optional.of(mapper.readValue(json, SessionResponse.class));
        } catch (RuntimeException | JsonProcessingException e) {
            log.warn("cache read failed for session {}: {}", sessionId, e.toString());
            return Optional.empty();
        }
    }

    @Override
    public void put(SessionResponse session) {
        try {
            redis.opsForValue().set(PREFIX + session.id(), mapper.writeValueAsString(session), ttl);
        } catch (RuntimeException | JsonProcessingException e) {
            log.warn("cache write failed for session {}: {}", session.id(), e.toString());
        }
    }

    @Override
    public void evict(String sessionId) {
        try {
            redis.delete(PREFIX + sessionId);
        } catch (RuntimeException e) {
            log.warn("cache evict failed for session {}: {}", sessionId, e.toString());
        }
    }
}
