package com.abhishri.escape.domain.puzzle;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.Map;

@Entity
@Table(name = "ITEM_USE_PUZZLE")
public class ItemUsePuzzle extends Puzzle {

    @Column(name = "REQUIRED_ITEM_ID", nullable = false, length = 64)
    private String requiredItemId;

    @Column(name = "TARGET_OBJECT_ID", nullable = false, length = 64)
    private String targetObjectId;

    @Column(name = "OUTCOME_MESSAGE", columnDefinition = "CLOB")
    private String outcomeMessage;

    @Override
    public boolean attempt(Map<String, String> inputs) {
        String itemId = inputs.get("itemId");
        String target = inputs.get("targetObjectId");
        if (itemId == null || target == null) return false;
        return requiredItemId.equals(itemId) && targetObjectId.equals(target);
    }

    public String getRequiredItemId() { return requiredItemId; }
    public void setRequiredItemId(String requiredItemId) { this.requiredItemId = requiredItemId; }

    public String getTargetObjectId() { return targetObjectId; }
    public void setTargetObjectId(String targetObjectId) { this.targetObjectId = targetObjectId; }

    public String getOutcomeMessage() { return outcomeMessage; }
    public void setOutcomeMessage(String outcomeMessage) { this.outcomeMessage = outcomeMessage; }
}
