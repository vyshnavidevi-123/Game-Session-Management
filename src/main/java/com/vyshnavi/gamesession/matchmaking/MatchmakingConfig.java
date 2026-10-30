package com.vyshnavi.gamesession.matchmaking;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MatchmakingConfig {

    @Bean
    public Matchmaker matchmaker(MatchmakingProperties p) {
        return new Matchmaker(p.playersPerMatch(), p.baseWindow(), p.widenPerSecond(),
                p.maxWindow(), System::currentTimeMillis);
    }
}
