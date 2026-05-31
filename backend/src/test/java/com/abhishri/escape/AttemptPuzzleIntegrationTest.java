package com.abhishri.escape;

import com.abhishri.escape.dto.LastActionResult;
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

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AttemptPuzzleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void attemptPuzzle_riddleCorrectAnswer_returnsPuzzleSolvedWithReward() throws Exception {
        String gameId = createNewGame();

        mockMvc.perform(post("/api/game/{gameId}/attempt-puzzle", gameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"puzzleId\":\"test_puzzle\",\"inputs\":{\"answer\":\"yes\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastActionResult", is(LastActionResult.PUZZLE_SOLVED.name())))
                .andExpect(jsonPath("$.solvedPuzzleIds", hasItem("test_puzzle")))
                .andExpect(jsonPath("$.inventory[*].id", hasItem("test_item")));
    }

    @Test
    void attemptPuzzle_riddleWrongAnswer_returnsPuzzleFailed() throws Exception {
        String gameId = createNewGame();

        mockMvc.perform(post("/api/game/{gameId}/attempt-puzzle", gameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"puzzleId\":\"test_puzzle\",\"inputs\":{\"answer\":\"nope\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastActionResult", is(LastActionResult.PUZZLE_FAILED.name())))
                .andExpect(jsonPath("$.solvedPuzzleIds").isArray())
                .andExpect(jsonPath("$.solvedPuzzleIds.length()", is(0)));
    }

    @Test
    void attemptPuzzle_combinationCorrectCode_returnsPuzzleSolved() throws Exception {
        String gameId = createNewGame();

        mockMvc.perform(post("/api/game/{gameId}/attempt-puzzle", gameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"puzzleId\":\"test_combo_puzzle\",\"inputs\":{\"code\":\"123\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastActionResult", is(LastActionResult.PUZZLE_SOLVED.name())))
                .andExpect(jsonPath("$.solvedPuzzleIds", hasItem("test_combo_puzzle")))
                .andExpect(jsonPath("$.inventory[*].id", hasItem("test_combo_reward")));
    }

    @Test
    void attemptPuzzle_unknownPuzzleId_returns404() throws Exception {
        String gameId = createNewGame();

        mockMvc.perform(post("/api/game/{gameId}/attempt-puzzle", gameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"puzzleId\":\"puzzle_nope\",\"inputs\":{}}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode", is("PUZZLE_NOT_FOUND")));
    }

    @Test
    void attemptPuzzle_prereqNotMet_returns409() throws Exception {
        String gameId = createNewGame();

        // test_prereq_puzzle requires test_puzzle — not solved yet
        mockMvc.perform(post("/api/game/{gameId}/attempt-puzzle", gameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"puzzleId\":\"test_prereq_puzzle\",\"inputs\":{\"answer\":\"maybe\"}}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode", is("PREREQUISITE_NOT_MET")));
    }

    private String createNewGame() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/game/new")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn();
        ObjectNode body = objectMapper.readValue(result.getResponse().getContentAsString(), ObjectNode.class);
        return body.get("gameId").asText();
    }
}
