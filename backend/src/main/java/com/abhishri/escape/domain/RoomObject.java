package com.abhishri.escape.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable
public class RoomObject {

    @Column(name = "OBJECT_ID", length = 64)
    private String id;

    @Column(name = "OBJECT_LABEL", length = 128)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(name = "OBJECT_TYPE", length = 16)
    private ObjectType objectType;

    @Column(name = "PUZZLE_ID", length = 64)
    private String puzzleId;

    @Column(name = "PICKUP_ITEM_ID", length = 64)
    private String pickupItemId;

    @Column(name = "INTERACTABLE")
    private boolean interactable;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public ObjectType getObjectType() { return objectType; }
    public void setObjectType(ObjectType objectType) { this.objectType = objectType; }

    public String getPuzzleId() { return puzzleId; }
    public void setPuzzleId(String puzzleId) { this.puzzleId = puzzleId; }

    public String getPickupItemId() { return pickupItemId; }
    public void setPickupItemId(String pickupItemId) { this.pickupItemId = pickupItemId; }

    public boolean isInteractable() { return interactable; }
    public void setInteractable(boolean interactable) { this.interactable = interactable; }
}
