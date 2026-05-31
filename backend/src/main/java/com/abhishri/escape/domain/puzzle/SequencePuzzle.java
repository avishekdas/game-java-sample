package com.abhishri.escape.domain.puzzle;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "SEQUENCE_PUZZLE")
public class SequencePuzzle extends Puzzle {

    @ElementCollection
    @CollectionTable(name = "SEQUENCE_PUZZLE_EXPECTED", joinColumns = @JoinColumn(name = "SEQUENCE_PUZZLE_ID"))
    @Column(name = "EXPECTED_ITEM", length = 64)
    @OrderColumn(name = "POSITION")
    private List<String> expectedSequence = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "SEQUENCE_PUZZLE_AVAILABLE", joinColumns = @JoinColumn(name = "SEQUENCE_PUZZLE_ID"))
    @Column(name = "AVAILABLE_ITEM", length = 64)
    private List<String> availableItems = new ArrayList<>();

    @Override
    public boolean attempt(Map<String, String> inputs) {
        String sequenceInput = inputs.get("sequence");
        if (sequenceInput == null) return false;
        String[] submitted = sequenceInput.split(",");
        if (submitted.length != expectedSequence.size()) return false;
        for (int i = 0; i < expectedSequence.size(); i++) {
            if (!expectedSequence.get(i).equals(submitted[i].trim())) {
                return false;
            }
        }
        return true;
    }

    public List<String> getExpectedSequence() { return expectedSequence; }
    public void setExpectedSequence(List<String> expectedSequence) { this.expectedSequence = expectedSequence; }

    public List<String> getAvailableItems() { return availableItems; }
    public void setAvailableItems(List<String> availableItems) { this.availableItems = availableItems; }
}
