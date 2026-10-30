package com.vyshnavi.gamesession.cache;

import com.vyshnavi.gamesession.session.SessionResponse;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Local cache for tests and for running without Redis (cache.type=memory). */
@Component
@ConditionalOnProperty(name = "cache.type", havingValue = "memory")
public class InMemorySessionCache implements SessionCache {

    private final Map<String, SessionResponse> store = new ConcurrentHashMap<>();

    @Override
    public Optional<SessionResponse> get(String sessionId) {
        return Optional.ofNullable(store.get(sessionId));
    }

    @Override
    public void put(SessionResponse session) {
        store.put(session.id(), session);
    }

    @Override
    public void evict(String sessionId) {
        store.remove(sessionId);
    }
}
