package com.abhishri.escape.service;

import com.abhishri.escape.domain.GameSession;
import com.abhishri.escape.domain.PlayerInventory;
import com.abhishri.escape.dto.InventoryItemDTO;
import com.abhishri.escape.repository.InventoryItemRepository;
import com.abhishri.escape.repository.PlayerInventoryRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class InventoryService {

    private final PlayerInventoryRepository playerInventoryRepository;
    private final InventoryItemRepository inventoryItemRepository;

    public InventoryService(PlayerInventoryRepository playerInventoryRepository,
                            InventoryItemRepository inventoryItemRepository) {
        this.playerInventoryRepository = playerInventoryRepository;
        this.inventoryItemRepository = inventoryItemRepository;
    }

    public void addItem(GameSession session, String itemId) {
        PlayerInventory inv = session.getInventory();
        if (!inv.contains(itemId)) {
            inv.getHeldItemIds().add(itemId);
            playerInventoryRepository.save(inv);
        }
    }

    public boolean hasItem(GameSession session, String itemId) {
        return session.getInventory() != null && session.getInventory().contains(itemId);
    }

    public List<InventoryItemDTO> snapshot(GameSession session) {
        if (session.getInventory() == null
                || session.getInventory().getHeldItemIds().isEmpty()) {
            return new ArrayList<>();
        }
        List<InventoryItemDTO> result = new ArrayList<>();
        for (String itemId : session.getInventory().getHeldItemIds()) {
            inventoryItemRepository.findById(itemId).ifPresent(item ->
                result.add(new InventoryItemDTO(
                    item.getId(), item.getName(), item.getDescription(), item.getAssetKey())));
        }
        return result;
    }
}
