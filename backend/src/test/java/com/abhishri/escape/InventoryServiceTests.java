package com.abhishri.escape;

import com.abhishri.escape.domain.GameSession;
import com.abhishri.escape.domain.GameStatus;
import com.abhishri.escape.domain.PlayerInventory;
import com.abhishri.escape.repository.InventoryItemRepository;
import com.abhishri.escape.repository.PlayerInventoryRepository;
import com.abhishri.escape.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTests {

    @Mock private PlayerInventoryRepository playerInventoryRepository;
    @Mock private InventoryItemRepository inventoryItemRepository;

    private InventoryService service;

    @BeforeEach
    void setUp() {
        service = new InventoryService(playerInventoryRepository, inventoryItemRepository);
    }

    @Test
    void addItem_appendsToHeldItemIds() {
        when(playerInventoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        GameSession session = makeSession();

        service.addItem(session, "desk_key");

        assertThat(session.getInventory().getHeldItemIds()).containsExactly("desk_key");
    }

    @Test
    void addItem_duplicateRejected_doesNotDoubleInsert() {
        GameSession session = makeSession();
        session.getInventory().getHeldItemIds().add("desk_key");

        // save() should NOT be called — duplicate is a no-op; no stub needed
        service.addItem(session, "desk_key");

        assertThat(session.getInventory().getHeldItemIds()).containsExactly("desk_key");
    }

    @Test
    void hasItem_trueWhenHeld() {
        GameSession session = makeSession();
        session.getInventory().getHeldItemIds().add("desk_key");

        assertThat(service.hasItem(session, "desk_key")).isTrue();
    }

    @Test
    void hasItem_falseWhenNotHeld() {
        GameSession session = makeSession();

        assertThat(service.hasItem(session, "desk_key")).isFalse();
    }

    @Test
    void removeItem_removesFromHeldItemIds() {
        when(playerInventoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        GameSession session = makeSession();
        session.getInventory().getHeldItemIds().add("desk_key");

        service.removeItem(session, "desk_key");

        assertThat(session.getInventory().getHeldItemIds()).doesNotContain("desk_key");
    }

    private GameSession makeSession() {
        PlayerInventory inv = new PlayerInventory();
        inv.setId(UUID.randomUUID());

        GameSession session = new GameSession();
        session.setId(UUID.randomUUID());
        session.setCurrentRoomId("room_foyer");
        session.setStatus(GameStatus.IN_PROGRESS);
        session.setCreatedAt(LocalDateTime.now());
        session.setLastUpdatedAt(LocalDateTime.now());
        session.setInventory(inv);
        return session;
    }
}
