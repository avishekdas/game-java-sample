package com.abhishri.escape.exception;

import java.util.UUID;

public class GameNotFoundException extends RuntimeException {

    public GameNotFoundException(UUID id) {
        super("No game session with id " + id);
    }
}
