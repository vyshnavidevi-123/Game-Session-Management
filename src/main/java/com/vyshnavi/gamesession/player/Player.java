package com.vyshnavi.gamesession.player;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "players")
public class Player {

    @Id
    private String id;

    @Column(nullable = false, unique = true, length = 32)
    private String username;

    @Column(nullable = false)
    private int rating;

    @Column(nullable = false)
    private Instant createdAt;

    protected Player() {}

    public Player(String id, String username, int rating, Instant createdAt) {
        this.id = id;
        this.username = username;
        this.rating = rating;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getUsername() { return username; }
    public int getRating() { return rating; }
    public Instant getCreatedAt() { return createdAt; }
}
