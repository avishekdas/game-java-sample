package com.abhishri.escape;

import com.abhishri.escape.domain.PlayerInventory;
import com.abhishri.escape.repository.PlayerInventoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PlayerInventoryHeldItemsTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private PlayerInventoryRepository playerInventoryRepository;

    @Test
    void roundTrip_preservesHeldItems() {
        PlayerInventory inv = new PlayerInventory();
        inv.setId(UUID.randomUUID());
        inv.setHeldItemIds(List.of("desk_key", "brass_magnifying_glass"));

        em.persistAndFlush(inv);
        em.clear();

        PlayerInventory loaded = playerInventoryRepository.findById(inv.getId()).orElseThrow();
        assertThat(loaded.getHeldItemIds())
                .containsExactlyInAnyOrder("desk_key", "brass_magnifying_glass");
    }

    @Test
    void contains_returnsTrueWhenItemHeld() {
        PlayerInventory inv = new PlayerInventory();
        inv.setHeldItemIds(List.of("desk_key"));

        assertThat(inv.contains("desk_key")).isTrue();
        assertThat(inv.contains("brass_magnifying_glass")).isFalse();
    }
}
