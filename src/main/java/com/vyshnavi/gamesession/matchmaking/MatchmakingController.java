package com.vyshnavi.gamesession.matchmaking;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/matchmaking")
public class MatchmakingController {

    public record QueueRequest(@NotBlank String playerId) {}

    private final MatchmakingService service;

    public MatchmakingController(MatchmakingService service) {
        this.service = service;
    }

    @PostMapping("/queue")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MatchStatusResponse enqueue(@Valid @RequestBody QueueRequest request) {
        return service.enqueue(request.playerId());
    }

    @DeleteMapping("/queue/{playerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable String playerId) {
        service.cancel(playerId);
    }

    @GetMapping("/status/{playerId}")
    public MatchStatusResponse status(@PathVariable String playerId) {
        return service.status(playerId);
    }
}
