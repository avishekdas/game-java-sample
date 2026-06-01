package com.abhishri.escape.dto;

public class SaveConfirmationDTO {

    private String filename;
    private String savedAt;
    private long sizeBytes;

    public SaveConfirmationDTO(String filename, String savedAt, long sizeBytes) {
        this.filename = filename;
        this.savedAt = savedAt;
        this.sizeBytes = sizeBytes;
    }

    public String getFilename() { return filename; }
    public String getSavedAt() { return savedAt; }
    public long getSizeBytes() { return sizeBytes; }
}
