package com.vyshnavi.gamesession.player;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class PlayerDtos {
    private PlayerDtos() {}

    public record RegisterPlayerRequest(
            @NotBlank @Size(max = 32) String username,
            @Min(0) @Max(5000) int rating) {}

    public record PlayerResponse(String id, String username, int rating) {
        public static PlayerResponse from(Player p) {
            return new PlayerResponse(p.getId(), p.getUsername(), p.getRating());
        }
    }
}
