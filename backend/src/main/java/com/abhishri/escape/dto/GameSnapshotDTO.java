package com.abhishri.escape.dto;

import com.abhishri.escape.domain.GameStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GameSnapshotDTO {

    private int schemaVersion;
    private UUID gameId;
    private String currentRoomId;
    private GameStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdatedAt;
    private List<String> solvedPuzzleIds = new ArrayList<>();
    private List<String> heldItemIds = new ArrayList<>();

    public int getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(int schemaVersion) { this.schemaVersion = schemaVersion; }

    public UUID getGameId() { return gameId; }
    public void setGameId(UUID gameId) { this.gameId = gameId; }

    public String getCurrentRoomId() { return currentRoomId; }
    public void setCurrentRoomId(String currentRoomId) { this.currentRoomId = currentRoomId; }

    public GameStatus getStatus() { return status; }
    public void setStatus(GameStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastUpdatedAt() { return lastUpdatedAt; }
    public void setLastUpdatedAt(LocalDateTime lastUpdatedAt) { this.lastUpdatedAt = lastUpdatedAt; }

    public List<String> getSolvedPuzzleIds() { return solvedPuzzleIds; }
    public void setSolvedPuzzleIds(List<String> solvedPuzzleIds) { this.solvedPuzzleIds = solvedPuzzleIds; }

    public List<String> getHeldItemIds() { return heldItemIds; }
    public void setHeldItemIds(List<String> heldItemIds) { this.heldItemIds = heldItemIds; }
}
