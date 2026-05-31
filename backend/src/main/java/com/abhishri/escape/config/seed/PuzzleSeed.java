package com.abhishri.escape.config.seed;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.ArrayList;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = CombinationPuzzleSeed.class, name = "COMBINATION"),
    @JsonSubTypes.Type(value = RiddlePuzzleSeed.class,      name = "RIDDLE"),
    @JsonSubTypes.Type(value = SequencePuzzleSeed.class,    name = "SEQUENCE"),
    @JsonSubTypes.Type(value = ItemUsePuzzleSeed.class,     name = "ITEM_USE")
})
public abstract class PuzzleSeed {
    public String id;
    public String roomId;
    public String description;
    public String rewardItemId;
    public List<String> prerequisitePuzzleIds = new ArrayList<>();
}
