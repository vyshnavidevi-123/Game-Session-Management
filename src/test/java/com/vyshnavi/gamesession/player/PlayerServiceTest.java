package com.vyshnavi.gamesession.player;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vyshnavi.gamesession.common.ConflictException;
import com.vyshnavi.gamesession.common.NotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlayerServiceTest {

    @Mock
    PlayerRepository repository;

    @InjectMocks
    PlayerService service;

    @Test
    void registerSavesNewPlayer() {
        when(repository.existsByUsername("neo")).thenReturn(false);
        when(repository.save(any(Player.class))).thenAnswer(inv -> inv.getArgument(0));

        Player saved = service.register("neo", 1500);

        assertThat(saved.getUsername()).isEqualTo("neo");
        assertThat(saved.getRating()).isEqualTo(1500);
        assertThat(saved.getId()).isNotBlank();
    }

    @Test
    void registerRejectsDuplicateUsername() {
        when(repository.existsByUsername("neo")).thenReturn(true);

        assertThatThrownBy(() -> service.register("neo", 1500))
                .isInstanceOf(ConflictException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void getThrowsWhenPlayerMissing() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get("missing")).isInstanceOf(NotFoundException.class);
    }
}
