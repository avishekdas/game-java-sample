package com.abhishri.escape.domain.puzzle;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "PUZZLE")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Puzzle {

    @Id
    @Column(name = "ID", length = 64)
    private String id;

    @Column(name = "ROOM_ID", nullable = false, length = 64)
    private String roomId;

    @Column(name = "DESCRIPTION", columnDefinition = "CLOB")
    private String description;

    @Column(name = "REWARD_ITEM_ID", length = 64)
    private String rewardItemId;

    @ElementCollection
    @CollectionTable(name = "PUZZLE_PREREQUISITE_IDS", joinColumns = @JoinColumn(name = "PUZZLE_ID"))
    @Column(name = "PREREQUISITE_ID", length = 64)
    private List<String> prerequisitePuzzleIds = new ArrayList<>();

    public abstract boolean attempt(Map<String, String> inputs);

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getRewardItemId() { return rewardItemId; }
    public void setRewardItemId(String rewardItemId) { this.rewardItemId = rewardItemId; }

    public List<String> getPrerequisitePuzzleIds() { return prerequisitePuzzleIds; }
    public void setPrerequisitePuzzleIds(List<String> prerequisitePuzzleIds) {
        this.prerequisitePuzzleIds = prerequisitePuzzleIds;
    }
}
