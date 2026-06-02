package com.abhishri.escape.ui;

import org.junit.jupiter.api.Test;

import java.awt.Rectangle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HotspotSolvedFieldTest {

    private static final Rectangle BOUNDS = new Rectangle(10, 10, 100, 50);

    @Test
    void sixArgConstructor_solvedTrue_isSolvedReturnsTrue() {
        Hotspot h = new Hotspot("h1", "PUZZLE", "Clock", BOUNDS, "wall_clock", true);
        assertTrue(h.isSolved());
    }

    @Test
    void sixArgConstructor_solvedFalse_isSolvedReturnsFalse() {
        Hotspot h = new Hotspot("h1", "PUZZLE", "Clock", BOUNDS, "wall_clock", false);
        assertFalse(h.isSolved());
    }

    @Test
    void sixArgConstructor_preservesAllOtherGetters() {
        Hotspot h = new Hotspot("h1", "PUZZLE", "Clock", BOUNDS, "wall_clock", true);
        assertEquals("h1",        h.getId());
        assertEquals("PUZZLE",    h.getType());
        assertEquals("Clock",     h.getLabel());
        assertEquals(BOUNDS,      h.getBounds());
        assertEquals("wall_clock", h.getObjectId());
    }

    @Test
    void fiveArgConstructor_isSolvedReturnsFalse() {
        Hotspot h = new Hotspot("h2", "EXIT", "→ Hall", BOUNDS, "room_reading_hall");
        assertFalse(h.isSolved());
    }

    @Test
    void fiveArgConstructor_existingCallSites_stillCompile() {
        // Mirrors the construction pattern used in ScenePanelInteractionTest —
        // confirms no regression in existing 5-arg call sites
        Hotspot h = new Hotspot("h3", "ITEM", "Old Key",
                new Rectangle(200, 150, 60, 40), "old_key");
        assertEquals("h3", h.getId());
        assertFalse(h.isSolved());
    }
}
