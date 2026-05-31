package com.abhishri.escape;

import com.abhishri.escape.repository.RoomRepository;
import com.abhishri.escape.service.WorldSeedService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class WorldSeedIdempotencyTest {

    @Autowired
    private WorldSeedService worldSeedService;

    @Autowired
    private RoomRepository roomRepository;

    @Test
    void seedIfEmpty_calledTwice_doesNotInsertDuplicates() {
        long countBefore = roomRepository.count();
        assertThat(countBefore).isGreaterThan(0); // already seeded at startup

        worldSeedService.seedIfEmpty(); // second invocation — should be a no-op

        assertThat(roomRepository.count()).isEqualTo(countBefore);
    }
}
