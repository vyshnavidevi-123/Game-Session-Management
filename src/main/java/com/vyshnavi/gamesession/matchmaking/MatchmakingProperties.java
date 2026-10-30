package com.vyshnavi.gamesession.matchmaking;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "matchmaking")
public record MatchmakingProperties(
        @DefaultValue("2") int playersPerMatch,
        @DefaultValue("100") int baseWindow,
        @DefaultValue("50") int widenPerSecond,
        @DefaultValue("1000") int maxWindow,
        @DefaultValue("500") long intervalMs,
        @DefaultValue("true") boolean schedulerEnabled) {}
