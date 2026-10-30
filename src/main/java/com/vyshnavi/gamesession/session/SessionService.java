package com.vyshnavi.gamesession.session;

import com.vyshnavi.gamesession.cache.SessionCache;
import com.vyshnavi.gamesession.common.ConflictException;
import com.vyshnavi.gamesession.common.NotFoundException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionService {

    private final GameSessionRepository repository;
    private final SessionCache cache;
    private final Counter cacheHits;
    private final Counter cacheMisses;

    public SessionService(GameSessionRepository repository, SessionCache cache,
                          MeterRegistry registry) {
        this.repository = repository;
        this.cache = cache;
        this.cacheHits = registry.counter("session.cache", "result", "hit");
        this.cacheMisses = registry.counter("session.cache", "result", "miss");
    }

    @Transactional
    public SessionResponse create(List<String> playerIds) {
        GameSession session = new GameSession(UUID.randomUUID().toString(), playerIds, Instant.now());
        SessionResponse response = SessionResponse.from(repository.save(session));
        cache.put(response);
        return response;
    }

    /** Cache-aside read: Redis first, database on a miss, then repopulate the cache. */
    @Transactional(readOnly = true)
    public SessionResponse get(String id) {
        var cached = cache.get(id);
        if (cached.isPresent()) {
            cacheHits.increment();
            return cached.get();
        }
        cacheMisses.increment();
        SessionResponse response = repository.findById(id)
                .map(SessionResponse::from)
                .orElseThrow(() -> new NotFoundException("session not found: " + id));
        cache.put(response);
        return response;
    }

    @Transactional
    public SessionResponse complete(String id) {
        GameSession session = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("session not found: " + id));
        if (session.getStatus() == SessionStatus.COMPLETED) {
            throw new ConflictException("session already completed: " + id);
        }
        session.complete(Instant.now());
        SessionResponse response = SessionResponse.from(repository.save(session));
        cache.put(response);
        return response;
    }
}
