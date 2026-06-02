package com.abhishri.escape.ui;

import com.abhishri.escape.ui.dto.InventoryItemDTO;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryUseModeTest {

    @Test
    void selectItem_changesTitleToUsingMode() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Requires display");

        InventoryPanel[] ref = {null};
        SwingUtilities.invokeAndWait(() -> {
            ref[0] = new InventoryPanel(new PlaceholderAssetManager());
            ref[0].setItems(List.of(
                    new InventoryItemDTO("k1", "Desk Key", "", "item_key"),
                    new InventoryItemDTO("l1", "Lens",     "", "item_lens")
            ));
        });
        InventoryPanel panel = ref[0];

        SwingUtilities.invokeAndWait(() -> panel.getItemList().setSelectedIndex(0));

        assertTrue(panel.getPanelTitle().contains("USING:"),
                "Title must contain 'USING:' when an item is selected; got: " + panel.getPanelTitle());
        assertTrue(panel.getPanelTitle().contains("Desk Key"),
                "Title must contain the item name; got: " + panel.getPanelTitle());
    }

    @Test
    void clearSelection_revertsTitleToInventory() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Requires display");

        InventoryPanel[] ref = {null};
        SwingUtilities.invokeAndWait(() -> {
            ref[0] = new InventoryPanel(new PlaceholderAssetManager());
            ref[0].setItems(List.of(new InventoryItemDTO("k1", "Desk Key", "", "item_key")));
        });
        InventoryPanel panel = ref[0];

        SwingUtilities.invokeAndWait(() -> panel.getItemList().setSelectedIndex(0));
        SwingUtilities.invokeAndWait(() -> panel.clearSelection());

        assertEquals("INVENTORY", panel.getPanelTitle(),
                "Title must revert to 'INVENTORY' after clearSelection()");
    }
}
