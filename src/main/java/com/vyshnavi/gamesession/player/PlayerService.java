package com.vyshnavi.gamesession.player;

import com.vyshnavi.gamesession.common.ConflictException;
import com.vyshnavi.gamesession.common.NotFoundException;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PlayerService {

    private final PlayerRepository repository;

    public PlayerService(PlayerRepository repository) {
        this.repository = repository;
    }

    public Player register(String username, int rating) {
        if (repository.existsByUsername(username)) {
            throw new ConflictException("username already taken: " + username);
        }
        Player player = new Player(UUID.randomUUID().toString(), username, rating, Instant.now());
        return repository.save(player);
    }

    public Player get(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("player not found: " + id));
    }
}
