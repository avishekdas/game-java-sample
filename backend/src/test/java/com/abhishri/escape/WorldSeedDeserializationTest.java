package com.abhishri.escape;

import com.abhishri.escape.config.seed.CombinationPuzzleSeed;
import com.abhishri.escape.config.seed.ItemUsePuzzleSeed;
import com.abhishri.escape.config.seed.PuzzleSeed;
import com.abhishri.escape.config.seed.RiddlePuzzleSeed;
import com.abhishri.escape.config.seed.SequencePuzzleSeed;
import com.abhishri.escape.config.seed.WorldSeed;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WorldSeedDeserializationTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void thornwickWorldJson_deserializesCorrectCounts() throws Exception {
        WorldSeed seed = mapper.readValue(
            getClass().getResourceAsStream("/world.json"), WorldSeed.class);

        assertThat(seed.rooms).hasSize(3);
        assertThat(seed.items).hasSize(5);
        assertThat(seed.puzzles).hasSize(6);
    }

    @Test
    void puzzleTypes_deserializeToCorrectSubclasses() throws Exception {
        WorldSeed seed = mapper.readValue(
            getClass().getResourceAsStream("/world.json"), WorldSeed.class);

        PuzzleSeed clock = findById(seed, "puzzle_clock");
        assertThat(clock).isInstanceOf(RiddlePuzzleSeed.class);
        assertThat(((RiddlePuzzleSeed) clock).expectedAnswer).isEqualTo("11:47");

        PuzzleSeed display = findById(seed, "puzzle_display_case");
        assertThat(display).isInstanceOf(CombinationPuzzleSeed.class);
        assertThat(((CombinationPuzzleSeed) display).expectedCode).isEqualTo("384");

        PuzzleSeed bookshelf = findById(seed, "puzzle_bookshelf");
        assertThat(bookshelf).isInstanceOf(SequencePuzzleSeed.class);
        assertThat(((SequencePuzzleSeed) bookshelf).expectedSequence)
            .containsExactly("510", "520", "610", "621", "720", "810");

        PuzzleSeed cipher = findById(seed, "puzzle_cipher_wheel");
        assertThat(cipher).isInstanceOf(ItemUsePuzzleSeed.class);
        assertThat(((ItemUsePuzzleSeed) cipher).requiredItemId).isEqualTo("brass_magnifying_glass");
    }

    private PuzzleSeed findById(WorldSeed seed, String id) {
        return seed.puzzles.stream()
            .filter(p -> id.equals(p.id))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Puzzle not found: " + id));
    }
}
