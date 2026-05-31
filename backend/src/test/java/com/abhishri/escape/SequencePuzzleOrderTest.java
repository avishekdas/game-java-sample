package com.abhishri.escape;

import com.abhishri.escape.domain.puzzle.SequencePuzzle;
import com.abhishri.escape.repository.PuzzleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Canary test for @OrderColumn on SequencePuzzle.expectedSequence.
 * If @OrderColumn(name = "POSITION") is removed, the items may come back
 * in an arbitrary order and this test will fail.
 */
@DataJpaTest
class SequencePuzzleOrderTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private PuzzleRepository puzzleRepository;

    @Test
    void expectedSequence_preservesInsertionOrder_afterRoundTrip() {
        SequencePuzzle puzzle = new SequencePuzzle();
        puzzle.setId("order-canary");
        puzzle.setRoomId("room_reading_hall");
        puzzle.setDescription("Order test");
        puzzle.setExpectedSequence(List.of("510", "520", "610", "621", "720", "810"));
        puzzle.setAvailableItems(List.of("810", "510", "720", "520", "621", "610"));

        em.persistAndFlush(puzzle);
        em.clear();

        SequencePuzzle loaded = (SequencePuzzle) puzzleRepository.findById("order-canary").orElseThrow();
        assertThat(loaded.getExpectedSequence())
                .containsExactly("510", "520", "610", "621", "720", "810");
    }
}
