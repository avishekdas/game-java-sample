package com.abhishri.escape.exception;

public class PuzzleNotFoundException extends RuntimeException {

    public PuzzleNotFoundException(String puzzleId) {
        super("Puzzle not found: " + puzzleId);
    }
}
