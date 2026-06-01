package com.abhishri.escape.ui;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.http.HttpClient;

public class GameApiClient {

    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GameApiClient(String baseUrl, ObjectMapper objectMapper) {
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = objectMapper;
    }
}
