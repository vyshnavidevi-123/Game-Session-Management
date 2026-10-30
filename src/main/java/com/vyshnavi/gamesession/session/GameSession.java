package com.vyshnavi.gamesession.session;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "game_sessions")
public class GameSession {

    @Id
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "session_players", joinColumns = @JoinColumn(name = "session_id"))
    @Column(name = "player_id")
    private List<String> playerIds = new ArrayList<>();

    @Column(nullable = false)
    private Instant createdAt;

    private Instant endedAt;

    protected GameSession() {}

    public GameSession(String id, List<String> playerIds, Instant createdAt) {
        this.id = id;
        this.status = SessionStatus.ACTIVE;
        this.playerIds = new ArrayList<>(playerIds);
        this.createdAt = createdAt;
    }

    public void complete(Instant when) {
        this.status = SessionStatus.COMPLETED;
        this.endedAt = when;
    }

    public String getId() { return id; }
    public SessionStatus getStatus() { return status; }
    public List<String> getPlayerIds() { return List.copyOf(playerIds); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getEndedAt() { return endedAt; }
}
