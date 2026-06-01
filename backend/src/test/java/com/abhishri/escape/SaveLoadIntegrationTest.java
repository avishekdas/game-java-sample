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

import java.util.UUID;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SaveLoadIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void saveGame_returnsConfirmationWithFilenameAndPositiveSize() throws Exception {
        String gameId = createNewGame();

        mockMvc.perform(post("/api/game/{gameId}/save", gameId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filename").isString())
                .andExpect(jsonPath("$.sizeBytes", greaterThan(0)));
    }

    @Test
    void saveThenLoad_restoresInventoryAndSolvedPuzzles() throws Exception {
        String gameId = createNewGame();

        // Earn test_combo_reward by solving the combination puzzle
        mockMvc.perform(post("/api/game/{gameId}/attempt-puzzle", gameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"puzzleId\":\"test_combo_puzzle\",\"inputs\":{\"code\":\"123\"}}"))
                .andExpect(status().isOk());

        // Save state (solvedPuzzleIds=[test_combo_puzzle], inventory=[test_combo_reward])
        MvcResult saveResult = mockMvc.perform(post("/api/game/{gameId}/save", gameId))
                .andExpect(status().isOk())
                .andReturn();
        String filename = objectMapper.readValue(
                saveResult.getResponse().getContentAsString(), ObjectNode.class)
                .get("filename").asText();

        // Advance state: solve riddle (adds test_puzzle + test_item — beyond the save point)
        mockMvc.perform(post("/api/game/{gameId}/attempt-puzzle", gameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"puzzleId\":\"test_puzzle\",\"inputs\":{\"answer\":\"yes\"}}"))
                .andExpect(status().isOk());

        // Load from save — should revert to point of save
        mockMvc.perform(post("/api/game/{gameId}/load", gameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"filename\":\"" + filename + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastActionResult", is(LastActionResult.LOADED.name())))
                .andExpect(jsonPath("$.solvedPuzzleIds", hasItem("test_combo_puzzle")))
                .andExpect(jsonPath("$.solvedPuzzleIds", not(hasItem("test_puzzle"))))
                .andExpect(jsonPath("$.inventory[*].id", hasItem("test_combo_reward")))
                .andExpect(jsonPath("$.inventory[*].id", not(hasItem("test_item"))));
    }

    @Test
    void loadGame_pathTraversalFilename_returns400() throws Exception {
        String gameId = createNewGame();

        mockMvc.perform(post("/api/game/{gameId}/load", gameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"filename\":\"../../etc/passwd\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("INVALID_REQUEST")));
    }

    @Test
    void loadGame_unknownGameId_returns404() throws Exception {
        UUID unknownId = UUID.randomUUID();

        mockMvc.perform(post("/api/game/{gameId}/load", unknownId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"filename\":\"" + unknownId + "-20260531T120000.json\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode", is("GAME_NOT_FOUND")));
    }

    @Test
    void listSaves_afterSave_containsFilenameForThatGame() throws Exception {
        String gameId = createNewGame();

        MvcResult saveResult = mockMvc.perform(post("/api/game/{gameId}/save", gameId))
                .andExpect(status().isOk())
                .andReturn();
        String filename = objectMapper.readValue(
                saveResult.getResponse().getContentAsString(), ObjectNode.class)
                .get("filename").asText();

        mockMvc.perform(get("/api/game/{gameId}/saves", gameId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].filename", is(filename)));
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
