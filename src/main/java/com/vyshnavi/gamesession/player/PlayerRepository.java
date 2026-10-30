package com.vyshnavi.gamesession.player;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerRepository extends JpaRepository<Player, String> {
    boolean existsByUsername(String username);
}
