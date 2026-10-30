package com.vyshnavi.gamesession.cache;

import com.vyshnavi.gamesession.session.SessionResponse;
import java.util.Optional;

/** Cache-aside store for hot session reads. */
public interface SessionCache {
    Optional<SessionResponse> get(String sessionId);

    void put(SessionResponse session);

    void evict(String sessionId);
}
