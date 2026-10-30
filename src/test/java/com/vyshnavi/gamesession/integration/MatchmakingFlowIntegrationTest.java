package com.vyshnavi.gamesession.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.vyshnavi.gamesession.matchmaking.MatchmakingService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Exercises controllers, services, JPA (H2) and the cache end to end. */
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureObservability
@ActiveProfiles("test")
class MatchmakingFlowIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    MatchmakingService matchmakingService;

    private String register(int rating) throws Exception {
        String name = "u" + UUID.randomUUID().toString().substring(0, 8);
        String body = mvc.perform(post("/api/v1/players").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + name + "\",\"rating\":" + rating + "}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private void queue(String playerId) throws Exception {
        mvc.perform(post("/api/v1/matchmaking/queue").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerId\":\"" + playerId + "\"}"))
                .andExpect(status().isAccepted());
    }

    private String sessionOf(String playerId) throws Exception {
        String body = mvc.perform(get("/api/v1/matchmaking/status/" + playerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("MATCHED"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.sessionId");
    }

    @Test
    void closelyRatedPlayersAreMatchedIntoActiveSessionsThatCanBeCompleted() throws Exception {
        String a = register(1000);
        String b = register(1020);
        String c = register(2000);
        String d = register(2030);
        for (String id : new String[] {a, b, c, d}) {
            queue(id);
        }

        assertThat(matchmakingService.runMatchmaking()).isEqualTo(2);

        String low = sessionOf(a);
        assertThat(sessionOf(b)).isEqualTo(low);
        String high = sessionOf(c);
        assertThat(sessionOf(d)).isEqualTo(high);
        assertThat(low).isNotEqualTo(high);

        mvc.perform(get("/api/v1/sessions/" + low))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.playerIds.length()").value(2));

        mvc.perform(post("/api/v1/sessions/" + low + "/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
        mvc.perform(post("/api/v1/sessions/" + low + "/complete"))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/v1/sessions/" + low))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void farApartPlayersStayQueuedAndCanCancel() throws Exception {
        String low = register(3500);
        String high = register(4900);
        queue(low);
        queue(high);

        matchmakingService.runMatchmaking();

        mvc.perform(get("/api/v1/matchmaking/status/" + low))
                .andExpect(jsonPath("$.state").value("QUEUED"));
        mvc.perform(delete("/api/v1/matchmaking/queue/" + low)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/v1/matchmaking/queue/" + high)).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/matchmaking/status/" + low))
                .andExpect(jsonPath("$.state").value("NOT_QUEUED"));
    }

    @Test
    void queueingAnUnknownPlayerReturns404() throws Exception {
        mvc.perform(post("/api/v1/matchmaking/queue").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerId\":\"does-not-exist\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void unknownSessionReturns404() throws Exception {
        mvc.perform(get("/api/v1/sessions/does-not-exist")).andExpect(status().isNotFound());
    }

    @Test
    void prometheusEndpointExposesCustomMatchmakingMetrics() throws Exception {
        String body = mvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(body).contains("matchmaking_queue_size");
    }
}
