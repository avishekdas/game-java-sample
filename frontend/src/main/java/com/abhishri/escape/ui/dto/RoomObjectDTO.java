package com.abhishri.escape.ui.dto;

import java.util.List;

public class RoomObjectDTO {

    private String id;
    private String label;
    private boolean interactable;
    private String puzzleId;
    private String pickupItemId;
    private ObjectType objectType;
    private String puzzleType;
    private int digitCount;
    private List<String> availableItems;
    private String questionText;

    public RoomObjectDTO() {}

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

    public String getPuzzleType() { return puzzleType; }
    public void setPuzzleType(String puzzleType) { this.puzzleType = puzzleType; }

    public int getDigitCount() { return digitCount; }
    public void setDigitCount(int digitCount) { this.digitCount = digitCount; }

    public List<String> getAvailableItems() { return availableItems; }
    public void setAvailableItems(List<String> availableItems) { this.availableItems = availableItems; }

    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
}
