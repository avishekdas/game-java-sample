package com.abhishri.escape.ui;

import com.abhishri.escape.ui.dto.GameStateDTO;
import com.abhishri.escape.ui.dto.SaveMetadataDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GameApiClient {

    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GameApiClient(String baseUrl, ObjectMapper objectMapper) {
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = objectMapper;
    }

    public GameStateDTO newGame() {
        String json = post(baseUrl + "/new", "{}");
        return deserialize(json, GameStateDTO.class);
    }

    public GameStateDTO getState(UUID gameId) {
        String json = get(baseUrl + "/" + gameId);
        return deserialize(json, GameStateDTO.class);
    }

    public GameStateDTO move(UUID gameId, String targetRoomId) {
        String body = serialize(Map.of("targetRoomId", targetRoomId));
        String json = post(baseUrl + "/" + gameId + "/move", body);
        return deserialize(json, GameStateDTO.class);
    }

    public GameStateDTO examine(UUID gameId, String objectId) {
        String body = serialize(Map.of("objectId", objectId));
        String json = post(baseUrl + "/" + gameId + "/examine", body);
        return deserialize(json, GameStateDTO.class);
    }

    public GameStateDTO pickup(UUID gameId, String objectId) {
        String body = serialize(Map.of("objectId", objectId));
        String json = post(baseUrl + "/" + gameId + "/pickup", body);
        return deserialize(json, GameStateDTO.class);
    }

    public GameStateDTO attemptPuzzle(UUID gameId, String puzzleId, Map<String, String> inputs) {
        String body = serialize(Map.of("puzzleId", puzzleId, "inputs", inputs));
        String json = post(baseUrl + "/" + gameId + "/attempt-puzzle", body);
        return deserialize(json, GameStateDTO.class);
    }

    public GameStateDTO useItem(UUID gameId, String itemId, String targetObjectId) {
        String body = serialize(Map.of("itemId", itemId, "targetObjectId", targetObjectId));
        String json = post(baseUrl + "/" + gameId + "/use-item", body);
        return deserialize(json, GameStateDTO.class);
    }

    public String saveGame(UUID gameId) {
        String json = post(baseUrl + "/" + gameId + "/save", "{}");
        try {
            return objectMapper.readTree(json).get("filename").asText();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse save response", e);
        }
    }

    public GameStateDTO loadGame(UUID gameId, String filename) {
        String body = serialize(Map.of("filename", filename));
        String json = post(baseUrl + "/" + gameId + "/load", body);
        return deserialize(json, GameStateDTO.class);
    }

    public List<SaveMetadataDTO> listSaves(UUID gameId) {
        String json = get(baseUrl + "/" + gameId + "/saves");
        try {
            return objectMapper.readValue(json, new TypeReference<List<SaveMetadataDTO>>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse saves list", e);
        }
    }

    private String post(String url, String jsonBody) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new RuntimeException("Server error " + response.statusCode()
                        + " at " + url + ": " + response.body());
            }
            return response.body();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("HTTP POST failed: " + url, e);
        }
    }

    private String get(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new RuntimeException("Server error " + response.statusCode()
                        + " at " + url + ": " + response.body());
            }
            return response.body();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("HTTP GET failed: " + url, e);
        }
    }

    private String serialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Serialization error", e);
        }
    }

    private <T> T deserialize(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse response: " + json, e);
        }
    }
}
