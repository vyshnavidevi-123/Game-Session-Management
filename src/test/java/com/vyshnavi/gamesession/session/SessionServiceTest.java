package com.vyshnavi.gamesession.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vyshnavi.gamesession.cache.SessionCache;
import com.vyshnavi.gamesession.common.ConflictException;
import com.vyshnavi.gamesession.common.NotFoundException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock
    GameSessionRepository repository;

    @Mock
    SessionCache cache;

    SimpleMeterRegistry registry;
    SessionService service;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        service = new SessionService(repository, cache, registry);
    }

    private static GameSession session(String id) {
        return new GameSession(id, List.of("p1", "p2"), Instant.now());
    }

    @Test
    void createPersistsAndCachesSession() {
        when(repository.save(any(GameSession.class))).thenAnswer(inv -> inv.getArgument(0));

        SessionResponse created = service.create(List.of("p1", "p2"));

        assertThat(created.status()).isEqualTo(SessionStatus.ACTIVE);
        assertThat(created.playerIds()).containsExactly("p1", "p2");
        verify(cache).put(created);
    }

    @Test
    void cacheHitSkipsDatabase() {
        SessionResponse cached = SessionResponse.from(session("s1"));
        when(cache.get("s1")).thenReturn(Optional.of(cached));

        assertThat(service.get("s1")).isEqualTo(cached);

        verifyNoInteractions(repository);
        assertThat(registry.counter("session.cache", "result", "hit").count()).isEqualTo(1.0);
    }

    @Test
    void cacheMissLoadsFromDatabaseAndRepopulatesCache() {
        when(cache.get("s1")).thenReturn(Optional.empty());
        when(repository.findById("s1")).thenReturn(Optional.of(session("s1")));

        SessionResponse response = service.get("s1");

        assertThat(response.id()).isEqualTo("s1");
        verify(cache).put(response);
        assertThat(registry.counter("session.cache", "result", "miss").count()).isEqualTo(1.0);
    }

    @Test
    void getThrowsWhenSessionMissing() {
        when(cache.get("nope")).thenReturn(Optional.empty());
        when(repository.findById("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get("nope")).isInstanceOf(NotFoundException.class);
        verify(cache, never()).put(any());
    }

    @Test
    void completeMarksSessionCompletedAndRefreshesCache() {
        when(repository.findById("s1")).thenReturn(Optional.of(session("s1")));
        when(repository.save(any(GameSession.class))).thenAnswer(inv -> inv.getArgument(0));

        SessionResponse response = service.complete("s1");

        assertThat(response.status()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(response.endedAt()).isNotNull();
        verify(cache).put(response);
    }

    @Test
    void completingTwiceIsAConflict() {
        GameSession done = session("s1");
        done.complete(Instant.now());
        when(repository.findById("s1")).thenReturn(Optional.of(done));

        assertThatThrownBy(() -> service.complete("s1")).isInstanceOf(ConflictException.class);
    }
}
