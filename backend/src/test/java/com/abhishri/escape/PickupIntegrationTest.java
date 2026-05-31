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
class PickupIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void pickup_pickupableObject_addsItemToInventory() throws Exception {
        String gameId = createNewGame();

        mockMvc.perform(post("/api/game/{gameId}/pickup", gameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"objectId\":\"floor_coin\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastActionResult", is(LastActionResult.PICKUP_OK.name())))
                .andExpect(jsonPath("$.inventory[*].id", hasItem("test_pickup_item")));
    }

    @Test
    void pickup_alreadyHeld_returns409() throws Exception {
        String gameId = createNewGame();

        // First pickup succeeds
        mockMvc.perform(post("/api/game/{gameId}/pickup", gameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"objectId\":\"floor_coin\"}"))
                .andExpect(status().isOk());

        // Second pickup returns 409
        mockMvc.perform(post("/api/game/{gameId}/pickup", gameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"objectId\":\"floor_coin\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode", is("INVALID_MOVE")));
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
