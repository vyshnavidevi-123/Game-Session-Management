package com.vyshnavi.gamesession.matchmaking;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "matchmaking.scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class MatchmakingScheduler {

    private final MatchmakingService service;

    public MatchmakingScheduler(MatchmakingService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${matchmaking.interval-ms:500}")
    public void tick() {
        service.runMatchmaking();
    }
}
