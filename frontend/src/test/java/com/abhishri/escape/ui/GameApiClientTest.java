package com.abhishri.escape.ui;

import com.abhishri.escape.ui.dto.GameStateDTO;
import com.abhishri.escape.ui.dto.GameStatus;
import com.abhishri.escape.ui.dto.RoomDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GameApiClientTest {

    private HttpServer server;
    private GameApiClient client;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() throws IOException {
        mapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();

        server = HttpServer.create(new InetSocketAddress(0), 0);
        int port = server.getAddress().getPort();
        server.start();

        client = new GameApiClient("http://localhost:" + port + "/api/game", mapper);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void newGame_postsToNewEndpoint_returnsGameStateWithGameId() throws IOException {
        UUID gameId = UUID.randomUUID();
        byte[] responseBytes = mapper.writeValueAsBytes(buildState(gameId));

        server.createContext("/api/game/new", exchange -> {
            exchange.sendResponseHeaders(201, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        });

        GameStateDTO result = client.newGame();
        assertEquals(gameId, result.getGameId());
    }

    @Test
    void getState_getsFromStateEndpoint_returnsGameState() throws IOException {
        UUID gameId = UUID.randomUUID();
        byte[] responseBytes = mapper.writeValueAsBytes(buildState(gameId));

        server.createContext("/api/game/" + gameId, exchange -> {
            exchange.sendResponseHeaders(200, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        });

        GameStateDTO result = client.getState(gameId);
        assertNotNull(result);
        assertEquals(gameId, result.getGameId());
    }

    @Test
    void move_postsToMoveEndpoint_returnsUpdatedStateWithNewRoom() throws IOException {
        UUID gameId = UUID.randomUUID();
        String targetRoomId = "reading_room";
        GameStateDTO expectedState = buildState(gameId);
        RoomDTO room = new RoomDTO();
        room.setId(targetRoomId);
        room.setName("Reading Room");
        expectedState.setCurrentRoom(room);
        byte[] responseBytes = mapper.writeValueAsBytes(expectedState);

        server.createContext("/api/game/" + gameId + "/move", exchange -> {
            exchange.sendResponseHeaders(200, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        });

        GameStateDTO result = client.move(gameId, targetRoomId);
        assertEquals(targetRoomId, result.getCurrentRoom().getId());
    }

    private GameStateDTO buildState(UUID gameId) {
        GameStateDTO state = new GameStateDTO();
        state.setGameId(gameId);
        state.setGameStatus(GameStatus.IN_PROGRESS);
        return state;
    }
}
