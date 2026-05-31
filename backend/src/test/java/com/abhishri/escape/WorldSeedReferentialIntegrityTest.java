package com.abhishri.escape;

import com.abhishri.escape.config.seed.WorldSeed;
import com.abhishri.escape.service.WorldSeedValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies that a world.json with a broken room reference causes a loud
 * IllegalStateException from WorldSeedValidator rather than silently seeding
 * corrupt data.
 */
class WorldSeedReferentialIntegrityTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final WorldSeedValidator validator = new WorldSeedValidator();

    @Test
    void puzzleWithUnknownRoomId_throwsIllegalStateException() throws Exception {
        WorldSeed broken = mapper.readValue(
            getClass().getResourceAsStream("/world-broken.json"), WorldSeed.class);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> validator.validate(broken));

        assertThat(ex.getMessage()).contains("puzzle_bad");
        assertThat(ex.getMessage()).contains("room_nonexistent");
    }
}
