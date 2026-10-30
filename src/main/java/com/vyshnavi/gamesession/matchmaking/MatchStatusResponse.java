package com.vyshnavi.gamesession.matchmaking;

public record MatchStatusResponse(String playerId, State state, String sessionId) {

    public enum State { NOT_QUEUED, QUEUED, MATCHED }
}
