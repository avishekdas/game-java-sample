package com.abhishri.escape.dto;

public class InventoryItemDTO {

    private String id;
    private String name;
    private String description;
    private String assetKey;

    public InventoryItemDTO() {}

    public InventoryItemDTO(String id, String name, String description, String assetKey) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.assetKey = assetKey;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAssetKey() { return assetKey; }
    public void setAssetKey(String assetKey) { this.assetKey = assetKey; }
}
