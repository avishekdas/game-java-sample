package com.abhishri.escape.ui.dto;

import java.util.List;

public class RoomDTO {

    private String id;
    private String name;
    private String description;
    private List<RoomObjectDTO> objects;
    private List<String> exits;

    public RoomDTO() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<RoomObjectDTO> getObjects() { return objects; }
    public void setObjects(List<RoomObjectDTO> objects) { this.objects = objects; }

    public List<String> getExits() { return exits; }
    public void setExits(List<String> exits) { this.exits = exits; }
}
