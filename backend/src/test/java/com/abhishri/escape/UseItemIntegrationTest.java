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
class UseItemIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void useItem_happyPath_returnsPuzzleSolved() throws Exception {
        String gameId = createNewGame();
        // Solve combination puzzle to acquire test_combo_reward
        solveCombo(gameId, "test_combo_puzzle", "123");

        mockMvc.perform(post("/api/game/{gameId}/use-item", gameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":\"test_combo_reward\",\"targetObjectId\":\"some_target\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastActionResult", is(LastActionResult.PUZZLE_SOLVED.name())))
                .andExpect(jsonPath("$.solvedPuzzleIds", hasItem("test_item_use_puzzle")));
    }

    @Test
    void useItem_itemNotInInventory_returns409() throws Exception {
        String gameId = createNewGame();
        // test_combo_reward has not been earned yet

        mockMvc.perform(post("/api/game/{gameId}/use-item", gameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":\"test_combo_reward\",\"targetObjectId\":\"some_target\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode", is("ITEM_NOT_IN_INVENTORY")));
    }

    @Test
    void useItem_noMatchingPuzzle_returns404() throws Exception {
        String gameId = createNewGame();
        // Earn test_combo_reward, but use it on a nonexistent target
        solveCombo(gameId, "test_combo_puzzle", "123");

        mockMvc.perform(post("/api/game/{gameId}/use-item", gameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":\"test_combo_reward\",\"targetObjectId\":\"nonexistent_xyz\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode", is("PUZZLE_NOT_FOUND")));
    }

    private String createNewGame() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/game/new")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn();
        ObjectNode body = objectMapper.readValue(result.getResponse().getContentAsString(), ObjectNode.class);
        return body.get("gameId").asText();
    }

    private void solveCombo(String gameId, String puzzleId, String code) throws Exception {
        mockMvc.perform(post("/api/game/{gameId}/attempt-puzzle", gameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"puzzleId\":\"" + puzzleId + "\",\"inputs\":{\"code\":\"" + code + "\"}}"))
                .andExpect(status().isOk());
    }
}
