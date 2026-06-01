package com.abhishri.escape.ui;

import org.junit.jupiter.api.Test;

import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ScenePanelInteractionTest {

    @Test
    void findHotspotAt_insideBounds_returnsHotspot() {
        ScenePanel panel = new ScenePanel(new PlaceholderAssetManager());
        Hotspot hotspot = new Hotspot("h1", "PUZZLE", "Bookshelf",
                new Rectangle(100, 100, 80, 60), "bookshelf_obj");
        panel.setHotspots(List.of(hotspot));

        Hotspot found = panel.findHotspotAt(130, 130);
        assertEquals("h1", found.getId());
    }

    @Test
    void findHotspotAt_outsideBounds_returnsNull() {
        ScenePanel panel = new ScenePanel(new PlaceholderAssetManager());
        Hotspot hotspot = new Hotspot("h1", "PUZZLE", "Bookshelf",
                new Rectangle(100, 100, 80, 60), "bookshelf_obj");
        panel.setHotspots(List.of(hotspot));

        assertNull(panel.findHotspotAt(50, 50));
    }

    @Test
    void mouseClick_insideHotspotBounds_firesListener() {
        ScenePanel panel = new ScenePanel(new PlaceholderAssetManager());
        Hotspot hotspot = new Hotspot("h2", "ITEM", "Old Key",
                new Rectangle(200, 150, 60, 40), "old_key");
        panel.setHotspots(List.of(hotspot));

        Hotspot[] clicked = new Hotspot[1];
        panel.setHotspotClickListener(h -> clicked[0] = h);

        MouseEvent event = new MouseEvent(panel, MouseEvent.MOUSE_CLICKED,
                System.currentTimeMillis(), 0, 220, 165, 1, false);
        for (MouseListener l : panel.getMouseListeners()) {
            l.mouseClicked(event);
        }

        assertEquals("h2", clicked[0].getId());
    }
}
