package com.abhishri.escape.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "PLAYER_INVENTORY")
public class PlayerInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID")
    private UUID id;

    @Column(name = "GAME_SESSION_ID")
    private UUID gameSessionId;

    @ElementCollection
    @CollectionTable(name = "PLAYER_INVENTORY_HELD_ITEMS", joinColumns = @JoinColumn(name = "PLAYER_INVENTORY_ID"))
    @Column(name = "HELD_ITEM_ID", length = 64)
    private List<String> heldItemIds = new ArrayList<>();

    public boolean contains(String itemId) {
        return heldItemIds.contains(itemId);
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getGameSessionId() { return gameSessionId; }
    public void setGameSessionId(UUID gameSessionId) { this.gameSessionId = gameSessionId; }

    public List<String> getHeldItemIds() { return heldItemIds; }
    public void setHeldItemIds(List<String> heldItemIds) { this.heldItemIds = heldItemIds; }
}
