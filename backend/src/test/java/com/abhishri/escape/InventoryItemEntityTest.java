package com.abhishri.escape;

import com.abhishri.escape.domain.InventoryItem;
import com.abhishri.escape.repository.InventoryItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class InventoryItemEntityTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Test
    void roundTrip_persistsAllFields() {
        InventoryItem item = new InventoryItem();
        item.setId("brass_magnifying_glass");
        item.setName("Brass Magnifying Glass");
        item.setDescription("Heavier than it looks.");
        item.setAssetKey("item_lens");

        em.persistAndFlush(item);
        em.clear();

        InventoryItem loaded = inventoryItemRepository.findById("brass_magnifying_glass").orElseThrow();
        assertThat(loaded.getName()).isEqualTo("Brass Magnifying Glass");
        assertThat(loaded.getDescription()).isEqualTo("Heavier than it looks.");
        assertThat(loaded.getAssetKey()).isEqualTo("item_lens");
    }
}
