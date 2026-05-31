package com.abhishri.escape;

import com.abhishri.escape.domain.GameStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GetStateIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getState_returnsIdenticalStateToNewGame() throws Exception {
        // Create a new game and capture the gameId
        MvcResult newResult = mockMvc.perform(post("/api/game/new").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn();

        String body = newResult.getResponse().getContentAsString();
        // Extract gameId from JSON (simple substring — avoids pulling in extra parser)
        String gameId = body.split("\"gameId\"\\s*:\\s*\"")[1].split("\"")[0];

        // GET the same state
        mockMvc.perform(get("/api/game/" + gameId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId").value(gameId))
                .andExpect(jsonPath("$.gameStatus").value(GameStatus.IN_PROGRESS.name()))
                .andExpect(jsonPath("$.currentRoom.id").value("test_room"));

        assertThat(gameId).isNotBlank();
    }
}
