package com.abhishri.escape;

import com.abhishri.escape.domain.GameStatus;
import com.abhishri.escape.dto.LastActionResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NewGameIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void postNewGame_returns201WithValidGameStateDTO() throws Exception {
        mockMvc.perform(post("/api/game/new").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.gameId").isNotEmpty())
                .andExpect(jsonPath("$.gameStatus").value(GameStatus.IN_PROGRESS.name()))
                .andExpect(jsonPath("$.currentRoom.id").value("test_room"))
                .andExpect(jsonPath("$.currentRoom.name").value("Test Room"))
                .andExpect(jsonPath("$.inventory").isArray())
                .andExpect(jsonPath("$.solvedPuzzleIds").isArray())
                .andExpect(jsonPath("$.lastActionResult").value(LastActionResult.NEW_GAME.name()));
    }
}
