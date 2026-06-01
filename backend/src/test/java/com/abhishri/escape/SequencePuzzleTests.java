package com.abhishri.escape;

import com.abhishri.escape.domain.puzzle.SequencePuzzle;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SequencePuzzleTests {

    @Test
    void sequencePuzzle_correctOrder_returnsTrue() {
        SequencePuzzle p = seq("a", "b", "c");
        assertThat(p.attempt(Map.of("sequence", "a,b,c"))).isTrue();
    }

    @Test
    void sequencePuzzle_wrongOrder_returnsFalse() {
        SequencePuzzle p = seq("a", "b", "c");
        assertThat(p.attempt(Map.of("sequence", "c,b,a"))).isFalse();
    }

    @Test
    void sequencePuzzle_extraItems_returnsFalse() {
        SequencePuzzle p = seq("a", "b", "c");
        assertThat(p.attempt(Map.of("sequence", "a,b,c,d"))).isFalse();
    }

    @Test
    void sequencePuzzle_tooFewItems_returnsFalse() {
        SequencePuzzle p = seq("a", "b", "c");
        assertThat(p.attempt(Map.of("sequence", "a,b"))).isFalse();
    }

    @Test
    void sequencePuzzle_missingInput_returnsFalse() {
        SequencePuzzle p = seq("a", "b", "c");
        assertThat(p.attempt(Map.of())).isFalse();
    }

    @Test
    void sequencePuzzle_whitespaceAroundItems_isTrimmed() {
        SequencePuzzle p = seq("a", "b", "c");
        assertThat(p.attempt(Map.of("sequence", "a, b, c"))).isTrue();
    }

    private SequencePuzzle seq(String... items) {
        SequencePuzzle p = new SequencePuzzle();
        p.setId("sp");
        p.setRoomId("room");
        p.setDescription("seq puzzle");
        p.setExpectedSequence(new ArrayList<>(List.of(items)));
        p.setAvailableItems(new ArrayList<>(List.of(items)));
        return p;
    }
}
