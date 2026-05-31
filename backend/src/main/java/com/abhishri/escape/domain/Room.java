package com.abhishri.escape.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ROOM")
public class Room {

    @Id
    @Column(name = "ID", length = 64)
    private String id;

    @Column(name = "NAME", nullable = false, length = 128)
    private String name;

    @Column(name = "DESCRIPTION", columnDefinition = "CLOB")
    private String description;

    @ElementCollection
    @CollectionTable(name = "ROOM_CONNECTED_ROOMS", joinColumns = @JoinColumn(name = "ROOM_ID"))
    @Column(name = "CONNECTED_ROOM_ID", length = 64)
    private List<String> connectedRoomIds = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "ROOM_OBJECT_IDS", joinColumns = @JoinColumn(name = "ROOM_ID"))
    @Column(name = "OBJECT_ID", length = 64)
    private List<String> objectIds = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "ROOM_PUZZLE_IDS", joinColumns = @JoinColumn(name = "ROOM_ID"))
    @Column(name = "PUZZLE_ID", length = 64)
    private List<String> puzzleIds = new ArrayList<>();

    public boolean isConnectedTo(String roomId) {
        return connectedRoomIds.contains(roomId);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<String> getConnectedRoomIds() { return connectedRoomIds; }
    public void setConnectedRoomIds(List<String> connectedRoomIds) { this.connectedRoomIds = connectedRoomIds; }

    public List<String> getObjectIds() { return objectIds; }
    public void setObjectIds(List<String> objectIds) { this.objectIds = objectIds; }

    public List<String> getPuzzleIds() { return puzzleIds; }
    public void setPuzzleIds(List<String> puzzleIds) { this.puzzleIds = puzzleIds; }
}
