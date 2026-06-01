package com.abhishri.escape.dto;

import jakarta.validation.constraints.NotBlank;

public class UseItemRequest {

    @NotBlank
    private String itemId;

    @NotBlank
    private String targetObjectId;

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public String getTargetObjectId() { return targetObjectId; }
    public void setTargetObjectId(String targetObjectId) { this.targetObjectId = targetObjectId; }
}
