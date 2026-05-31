package com.abhishri.escape.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "GAME_SESSION")
public class GameSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID")
    private UUID id;

    @Column(name = "CURRENT_ROOM_ID", nullable = false, length = 64)
    private String currentRoomId;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 16)
    private GameStatus status;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "LAST_UPDATED_AT", nullable = false)
    private LocalDateTime lastUpdatedAt;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "INVENTORY_ID")
    private PlayerInventory inventory;

    @ElementCollection
    @CollectionTable(name = "GAME_SESSION_SOLVED_PUZZLES", joinColumns = @JoinColumn(name = "GAME_SESSION_ID"))
    @Column(name = "SOLVED_PUZZLE_ID", length = 64)
    private List<String> solvedPuzzleIds = new ArrayList<>();

    public boolean isComplete() {
        return GameStatus.COMPLETE == this.status;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getCurrentRoomId() { return currentRoomId; }
    public void setCurrentRoomId(String currentRoomId) { this.currentRoomId = currentRoomId; }

    public GameStatus getStatus() { return status; }
    public void setStatus(GameStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastUpdatedAt() { return lastUpdatedAt; }
    public void setLastUpdatedAt(LocalDateTime lastUpdatedAt) { this.lastUpdatedAt = lastUpdatedAt; }

    public PlayerInventory getInventory() { return inventory; }
    public void setInventory(PlayerInventory inventory) { this.inventory = inventory; }

    public List<String> getSolvedPuzzleIds() { return solvedPuzzleIds; }
    public void setSolvedPuzzleIds(List<String> solvedPuzzleIds) { this.solvedPuzzleIds = solvedPuzzleIds; }
}
