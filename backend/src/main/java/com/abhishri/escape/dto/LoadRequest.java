package com.abhishri.escape.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class LoadRequest {

    @NotBlank
    @Pattern(regexp = "^[a-zA-Z0-9_-]+\\.json$",
             message = "filename must contain only alphanumeric characters, hyphens, or underscores and end with .json")
    private String filename;

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
}
