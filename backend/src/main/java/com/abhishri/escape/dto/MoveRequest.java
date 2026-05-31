package com.abhishri.escape.dto;

import jakarta.validation.constraints.NotBlank;

public class MoveRequest {

    @NotBlank
    private String targetRoomId;

    public String getTargetRoomId() { return targetRoomId; }
    public void setTargetRoomId(String targetRoomId) { this.targetRoomId = targetRoomId; }
}
