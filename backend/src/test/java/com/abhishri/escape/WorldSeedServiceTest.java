package com.abhishri.escape;

import com.abhishri.escape.repository.InventoryItemRepository;
import com.abhishri.escape.repository.PuzzleRepository;
import com.abhishri.escape.repository.RoomRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class WorldSeedServiceTest {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private PuzzleRepository puzzleRepository;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Test
    void afterContextLoad_worldTestDataIsSeeded() {
        assertThat(roomRepository.count()).isEqualTo(3);
        assertThat(puzzleRepository.count()).isEqualTo(1);
        assertThat(inventoryItemRepository.count()).isEqualTo(1);
    }
}
