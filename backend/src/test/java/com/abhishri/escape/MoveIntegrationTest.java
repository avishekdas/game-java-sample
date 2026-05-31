package com.abhishri.escape;

import com.abhishri.escape.dto.LastActionResult;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MoveIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void move_toAdjacentRoom_returns200WithUpdatedRoom() throws Exception {
        String gameId = createNewGame();

        mockMvc.perform(post("/api/game/{gameId}/move", gameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetRoomId\":\"test_room_2\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentRoom.id", is("test_room_2")))
                .andExpect(jsonPath("$.currentRoom.name", is("Test Room Two")))
                .andExpect(jsonPath("$.lastActionResult", is(LastActionResult.MOVE_SUCCESS.name())));
    }

    @Test
    void move_toNonAdjacentRoom_returns409InvalidMove() throws Exception {
        String gameId = createNewGame();

        mockMvc.perform(post("/api/game/{gameId}/move", gameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetRoomId\":\"test_room_isolated\"}"))
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
