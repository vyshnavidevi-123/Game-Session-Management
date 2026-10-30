package com.vyshnavi.gamesession.session;

import java.time.Instant;
import java.util.List;

public record SessionResponse(String id, SessionStatus status, List<String> playerIds,
                              Instant createdAt, Instant endedAt) {
    public static SessionResponse from(GameSession s) {
        return new SessionResponse(s.getId(), s.getStatus(), s.getPlayerIds(),
                s.getCreatedAt(), s.getEndedAt());
    }
}
