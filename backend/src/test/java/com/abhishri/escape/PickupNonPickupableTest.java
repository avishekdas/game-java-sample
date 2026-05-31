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

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PickupNonPickupableTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void pickup_sceneryObject_returns409() throws Exception {
        String gameId = createNewGame();

        // test_object is SCENERY — pickupItemId is null
        mockMvc.perform(post("/api/game/{gameId}/pickup", gameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"objectId\":\"test_object\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode", is("INVALID_MOVE")));
    }

    @Test
    void pickup_objectNotInRoom_returns409() throws Exception {
        String gameId = createNewGame();

        // test_object_2 lives in test_room_2, not the starting test_room
        mockMvc.perform(post("/api/game/{gameId}/pickup", gameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"objectId\":\"test_object_2\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode", is("INVALID_MOVE")));
    }

    @Test
    void pickup_emptyBody_returns400() throws Exception {
        String gameId = createNewGame();

        mockMvc.perform(post("/api/game/{gameId}/pickup", gameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("INVALID_REQUEST")));
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
