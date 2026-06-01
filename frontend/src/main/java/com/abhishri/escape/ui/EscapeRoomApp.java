package com.abhishri.escape.ui;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import javax.swing.SwingUtilities;

public class EscapeRoomApp {

    private static final String BASE_URL = "http://127.0.0.1:8080/api/game";

    public static void main(String[] args) {
        var mapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
        GameApiClient client = new GameApiClient(BASE_URL, mapper);

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame(new FileAssetManager(), client);
            frame.setVisible(true);
        });
    }
}
