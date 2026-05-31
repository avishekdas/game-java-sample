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

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExamineIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void examine_objectInCurrentRoom_returns200WithDialogue() throws Exception {
        String gameId = createNewGame();

        // test_object is in test_room (the starting room)
        mockMvc.perform(post("/api/game/{gameId}/examine", gameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"objectId\":\"test_object\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastActionResult", is(LastActionResult.EXAMINE_OK.name())))
                .andExpect(jsonPath("$.dialogueMessage", notNullValue()));
    }

    @Test
    void examine_objectNotInCurrentRoom_returns409InvalidMove() throws Exception {
        String gameId = createNewGame();

        // test_object_2 is in test_room_2, not in the starting test_room
        mockMvc.perform(post("/api/game/{gameId}/examine", gameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"objectId\":\"test_object_2\"}"))
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
