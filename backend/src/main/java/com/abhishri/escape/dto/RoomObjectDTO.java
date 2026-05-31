package com.abhishri.escape.dto;

import com.abhishri.escape.domain.ObjectType;

public class RoomObjectDTO {

    private String id;
    private String label;
    private boolean interactable;
    private String puzzleId;
    private String pickupItemId;
    private ObjectType objectType;

    public RoomObjectDTO() {}

    public RoomObjectDTO(String id, String label, boolean interactable,
                         String puzzleId, String pickupItemId, ObjectType objectType) {
        this.id = id;
        this.label = label;
        this.interactable = interactable;
        this.puzzleId = puzzleId;
        this.pickupItemId = pickupItemId;
        this.objectType = objectType;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public boolean isInteractable() { return interactable; }
    public void setInteractable(boolean interactable) { this.interactable = interactable; }

    public String getPuzzleId() { return puzzleId; }
    public void setPuzzleId(String puzzleId) { this.puzzleId = puzzleId; }

    public String getPickupItemId() { return pickupItemId; }
    public void setPickupItemId(String pickupItemId) { this.pickupItemId = pickupItemId; }

    public ObjectType getObjectType() { return objectType; }
    public void setObjectType(ObjectType objectType) { this.objectType = objectType; }
}
