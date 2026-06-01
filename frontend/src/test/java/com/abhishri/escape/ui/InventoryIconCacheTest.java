package com.abhishri.escape.ui;

import com.abhishri.escape.ui.dto.InventoryItemDTO;
import org.junit.jupiter.api.Test;

import javax.swing.ListCellRenderer;
import java.awt.Image;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InventoryIconCacheTest {

    /** Counting stub — tracks how many times getItemIcon() is called per key. */
    private static class CountingAssetManager implements AssetManager {
        final Map<String, Integer> callCounts = new HashMap<>();
        private final PlaceholderAssetManager delegate = new PlaceholderAssetManager();

        @Override
        public Image getBackground(String roomId) { return delegate.getBackground(roomId); }

        @Override
        public Image getItemIcon(String assetKey) {
            callCounts.merge(assetKey, 1, Integer::sum);
            return delegate.getItemIcon(assetKey);
        }
    }

    @Test
    void renderer_callsGetItemIconAtMostOncePerKey() {
        CountingAssetManager stub = new CountingAssetManager();
        InventoryPanel panel = new InventoryPanel(stub);

        InventoryItemDTO key1  = new InventoryItemDTO("k1", "Key",  "", "item_key");
        InventoryItemDTO key2  = new InventoryItemDTO("k2", "Key2", "", "item_key");   // same assetKey
        InventoryItemDTO lens1 = new InventoryItemDTO("l1", "Lens", "", "item_lens");
        InventoryItemDTO lens2 = new InventoryItemDTO("l2", "Lens2","", "item_lens"); // same assetKey

        panel.setItems(List.of(key1, key2, lens1, lens2));

        // Trigger cell rendering for every item in the list
        @SuppressWarnings("unchecked")
        ListCellRenderer<InventoryItemDTO> renderer =
                (ListCellRenderer<InventoryItemDTO>) panel.getItemList().getCellRenderer();

        for (int i = 0; i < 4; i++) {
            renderer.getListCellRendererComponent(
                    panel.getItemList(),
                    panel.getItemList().getModel().getElementAt(i),
                    i, false, false);
        }

        assertEquals(1, stub.callCounts.getOrDefault("item_key",  0),
                "getItemIcon('item_key') must be called exactly once (cache hit on second render)");
        assertEquals(1, stub.callCounts.getOrDefault("item_lens", 0),
                "getItemIcon('item_lens') must be called exactly once (cache hit on second render)");
    }
}
