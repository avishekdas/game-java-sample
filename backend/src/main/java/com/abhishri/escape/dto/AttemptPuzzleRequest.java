package com.abhishri.escape.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public class AttemptPuzzleRequest {

    @NotBlank
    private String puzzleId;

    @NotNull
    private Map<String, String> inputs;

    public String getPuzzleId() { return puzzleId; }
    public void setPuzzleId(String puzzleId) { this.puzzleId = puzzleId; }

    public Map<String, String> getInputs() { return inputs; }
    public void setInputs(Map<String, String> inputs) { this.inputs = inputs; }
}
