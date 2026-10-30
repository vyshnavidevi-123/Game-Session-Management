package com.vyshnavi.gamesession.player;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vyshnavi.gamesession.common.ConflictException;
import com.vyshnavi.gamesession.common.NotFoundException;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PlayerController.class)
class PlayerControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    PlayerService service;

    @Test
    void registerReturns201WithBody() throws Exception {
        when(service.register("neo", 1500))
                .thenReturn(new Player("id-1", "neo", 1500, Instant.now()));

        mvc.perform(post("/api/v1/players").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"neo\",\"rating\":1500}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("id-1"))
                .andExpect(jsonPath("$.rating").value(1500));
    }

    @Test
    void blankUsernameReturns400() throws Exception {
        mvc.perform(post("/api/v1/players").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"rating\":1500}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void outOfRangeRatingReturns400() throws Exception {
        mvc.perform(post("/api/v1/players").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"neo\",\"rating\":99999}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void duplicateUsernameReturns409() throws Exception {
        when(service.register("neo", 1500)).thenThrow(new ConflictException("taken"));

        mvc.perform(post("/api/v1/players").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"neo\",\"rating\":1500}"))
                .andExpect(status().isConflict());
    }

    @Test
    void unknownPlayerReturns404() throws Exception {
        when(service.get("nope")).thenThrow(new NotFoundException("nope"));

        mvc.perform(get("/api/v1/players/nope")).andExpect(status().isNotFound());
    }
}
