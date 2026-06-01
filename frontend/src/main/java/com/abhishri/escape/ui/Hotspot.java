package com.abhishri.escape.ui;

import java.awt.Rectangle;

public class Hotspot {

    private final String id;
    private final String type;
    private final String label;
    private final Rectangle bounds;
    private final String objectId;
    private final boolean solved;

    public Hotspot(String id, String type, String label, Rectangle bounds, String objectId, boolean solved) {
        this.id = id;
        this.type = type;
        this.label = label;
        this.bounds = bounds;
        this.objectId = objectId;
        this.solved = solved;
    }

    public Hotspot(String id, String type, String label, Rectangle bounds, String objectId) {
        this(id, type, label, bounds, objectId, false);
    }

    public String getId() { return id; }
    public String getType() { return type; }
    public String getLabel() { return label; }
    public Rectangle getBounds() { return bounds; }
    public String getObjectId() { return objectId; }
    public boolean isSolved() { return solved; }
}
