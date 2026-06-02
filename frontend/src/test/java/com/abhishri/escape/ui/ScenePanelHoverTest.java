package com.abhishri.escape.ui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Rectangle;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ScenePanelHoverTest {

    private ScenePanel panel;
    private Hotspot h1;
    private Hotspot h2;

    @BeforeEach
    void setUp() {
        panel = new ScenePanel(new PlaceholderAssetManager());
        h1 = new Hotspot("h1", "PUZZLE", "Clock",    new Rectangle(100, 100, 80, 60), "wall_clock");
        h2 = new Hotspot("h2", "EXIT",   "→ Hall",   new Rectangle(300, 200, 80, 60), "room_reading_hall");
        panel.setHotspots(List.of(h1, h2));
    }

    // Existing behavior (regression pins) ------------------------------------------------

    @Test
    void findHotspotAt_insideBounds_returnsHotspot() {
        assertEquals("h1", panel.findHotspotAt(130, 130).getId());
    }

    @Test
    void findHotspotAt_outsideBounds_returnsNull() {
        assertNull(panel.findHotspotAt(50, 50));
    }

    // New hover-state assertions (Red) ----------------------------------------------------

    @Test
    void updateHover_insideHotspot_setsHoveredHotspotId() {
        panel.updateHover(130, 130);   // inside h1 bounds
        assertEquals("h1", panel.getHoveredHotspotId());
    }

    @Test
    void updateHover_insideSecondHotspot_setsCorrectId() {
        panel.updateHover(330, 230);   // inside h2 bounds
        assertEquals("h2", panel.getHoveredHotspotId());
    }

    @Test
    void updateHover_emptySpace_setsHoveredHotspotIdToNull() {
        panel.updateHover(130, 130);   // over h1 first
        panel.updateHover(10, 10);     // then off all hotspots
        assertNull(panel.getHoveredHotspotId());
    }
}
