package com.vyshnavi.gamesession.player;

import com.vyshnavi.gamesession.player.PlayerDtos.PlayerResponse;
import com.vyshnavi.gamesession.player.PlayerDtos.RegisterPlayerRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/players")
public class PlayerController {

    private final PlayerService service;

    public PlayerController(PlayerService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlayerResponse register(@Valid @RequestBody RegisterPlayerRequest request) {
        return PlayerResponse.from(service.register(request.username(), request.rating()));
    }

    @GetMapping("/{id}")
    public PlayerResponse get(@PathVariable String id) {
        return PlayerResponse.from(service.get(id));
    }
}
