package com.abhishri.escape.dto;

import jakarta.validation.constraints.NotBlank;

public class PickupRequest {

    @NotBlank
    private String objectId;

    public String getObjectId() { return objectId; }
    public void setObjectId(String objectId) { this.objectId = objectId; }
}
