package com.abhishri.escape.exception;

public class PrerequisiteNotMetException extends RuntimeException {

    public PrerequisiteNotMetException(String puzzleId) {
        super("Prerequisite not met for puzzle: " + puzzleId);
    }
}
