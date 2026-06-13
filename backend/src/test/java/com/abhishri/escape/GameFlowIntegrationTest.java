package com.abhishri.escape;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GameFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void goldenPath_solveAllPuzzles_winConditionFires() throws Exception {
        String gameId = createNewGame();

        // 1. Solve riddle (unlocks prereq riddle)
        attemptPuzzle(gameId, "test_puzzle", "{\"answer\":\"yes\"}");
        // 2. Solve combination (awards test_combo_reward needed for item-use puzzle)
        attemptPuzzle(gameId, "test_combo_puzzle", "{\"code\":\"123\"}");
        // 3. Solve prereq riddle (prereq: test_puzzle now satisfied)
        attemptPuzzle(gameId, "test_prereq_puzzle", "{\"answer\":\"maybe\"}");
        // 4. Solve sequence puzzle (iterates expectedSequence)
        attemptPuzzle(gameId, "test_seq_puzzle", "{\"sequence\":\"a,b,c\"}");
        // 5. Use test_combo_reward on some_target — solves test_item_use_puzzle (5th and final)
        MvcResult last = mockMvc.perform(post("/api/game/{gameId}/use-item", gameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":\"test_combo_reward\",\"targetObjectId\":\"some_target\"}"))
                .andExpect(status().isOk())
                .andReturn();

        ObjectNode body = objectMapper.readValue(last.getResponse().getContentAsString(), ObjectNode.class);
        assertThat(body.get("gameStatus").asText()).isEqualTo("COMPLETE");
    }

    private String createNewGame() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/game/new")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn();
        ObjectNode body = objectMapper.readValue(result.getResponse().getContentAsString(), ObjectNode.class);
        return body.get("gameId").asText();
    }

    private void attemptPuzzle(String gameId, String puzzleId, String inputsJson) throws Exception {
        mockMvc.perform(post("/api/game/{gameId}/attempt-puzzle", gameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"puzzleId\":\"" + puzzleId + "\",\"inputs\":" + inputsJson + "}"))
                .andExpect(status().isOk());
    }
}
