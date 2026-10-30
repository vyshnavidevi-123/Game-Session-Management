package com.vyshnavi.gamesession.matchmaking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import com.vyshnavi.gamesession.common.ConflictException;
import com.vyshnavi.gamesession.common.NotFoundException;
import com.vyshnavi.gamesession.matchmaking.MatchStatusResponse.State;
import com.vyshnavi.gamesession.player.Player;
import com.vyshnavi.gamesession.player.PlayerService;
import com.vyshnavi.gamesession.session.SessionResponse;
import com.vyshnavi.gamesession.session.SessionService;
import com.vyshnavi.gamesession.session.SessionStatus;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MatchmakingServiceTest {

    @Mock
    PlayerService playerService;

    @Mock
    SessionService sessionService;

    Matchmaker matchmaker;
    SimpleMeterRegistry registry;
    MatchmakingService service;

    @BeforeEach
    void setUp() {
        matchmaker = new Matchmaker(2, 100, 0, 100, System::currentTimeMillis);
        registry = new SimpleMeterRegistry();
        service = new MatchmakingService(matchmaker, playerService, sessionService, registry);
    }

    private void givenPlayer(String id, int rating) {
        when(playerService.get(id)).thenReturn(new Player(id, "user-" + id, rating, Instant.now()));
    }

    @Test
    void enqueueUnknownPlayerPropagatesNotFound() {
        when(playerService.get("ghost")).thenThrow(new NotFoundException("ghost"));

        assertThatThrownBy(() -> service.enqueue("ghost")).isInstanceOf(NotFoundException.class);
    }

    @Test
    void enqueueTwiceIsAConflict() {
        givenPlayer("a", 1000);
        service.enqueue("a");

        assertThatThrownBy(() -> service.enqueue("a")).isInstanceOf(ConflictException.class);
    }

    @Test
    void statusReflectsQueuedAndNotQueued() {
        givenPlayer("a", 1000);

        assertThat(service.status("a").state()).isEqualTo(State.NOT_QUEUED);
        service.enqueue("a");
        assertThat(service.status("a").state()).isEqualTo(State.QUEUED);
    }

    @Test
    void cancelUnknownPlayerIsNotFound() {
        assertThatThrownBy(() -> service.cancel("a")).isInstanceOf(NotFoundException.class);
    }

    @Test
    void runMatchmakingCreatesSessionAndAssignsBothPlayers() {
        givenPlayer("a", 1000);
        givenPlayer("b", 1050);
        service.enqueue("a");
        service.enqueue("b");
        when(sessionService.create(anyList())).thenReturn(
                new SessionResponse("s1", SessionStatus.ACTIVE, List.of("a", "b"), Instant.now(), null));

        int created = service.runMatchmaking();

        assertThat(created).isEqualTo(1);
        assertThat(service.status("a")).isEqualTo(new MatchStatusResponse("a", State.MATCHED, "s1"));
        assertThat(service.status("b").sessionId()).isEqualTo("s1");
        assertThat(registry.counter("matchmaking.matches.created").count()).isEqualTo(1.0);
    }

    @Test
    void failedSessionCreationRequeuesPlayers() {
        givenPlayer("a", 1000);
        givenPlayer("b", 1050);
        service.enqueue("a");
        service.enqueue("b");
        when(sessionService.create(anyList())).thenThrow(new IllegalStateException("db down"));

        int created = service.runMatchmaking();

        assertThat(created).isZero();
        assertThat(matchmaker.size()).isEqualTo(2);
        assertThat(service.status("a").state()).isEqualTo(State.QUEUED);
    }
}
