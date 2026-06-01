package com.abhishri.escape.ui.dto;

public class SaveMetadataDTO {

    private String filename;
    private String savedAt;
    private long sizeBytes;

    public SaveMetadataDTO() {}

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public String getSavedAt() { return savedAt; }
    public void setSavedAt(String savedAt) { this.savedAt = savedAt; }

    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }
}
