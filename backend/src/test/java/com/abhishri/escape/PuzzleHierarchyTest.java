package com.abhishri.escape;

import com.abhishri.escape.domain.puzzle.CombinationPuzzle;
import com.abhishri.escape.domain.puzzle.ItemUsePuzzle;
import com.abhishri.escape.domain.puzzle.Puzzle;
import com.abhishri.escape.domain.puzzle.RiddlePuzzle;
import com.abhishri.escape.domain.puzzle.SequencePuzzle;
import com.abhishri.escape.repository.PuzzleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PuzzleHierarchyTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private PuzzleRepository puzzleRepository;

    @Test
    void combinationPuzzle_roundTrip_correctSubclass() {
        CombinationPuzzle p = new CombinationPuzzle();
        p.setId("comb1");
        p.setRoomId("room_reading_hall");
        p.setDescription("Three-digit lock");
        p.setExpectedCode("384");
        p.setDigitCount(3);

        em.persistAndFlush(p);
        em.clear();

        Puzzle loaded = puzzleRepository.findById("comb1").orElseThrow();
        assertThat(loaded).isInstanceOf(CombinationPuzzle.class);
        assertThat(((CombinationPuzzle) loaded).getExpectedCode()).isEqualTo("384");
        assertThat(((CombinationPuzzle) loaded).getDigitCount()).isEqualTo(3);
    }

    @Test
    void riddlePuzzle_roundTrip_correctSubclass() {
        RiddlePuzzle p = new RiddlePuzzle();
        p.setId("riddle1");
        p.setRoomId("room_foyer");
        p.setDescription("Clock riddle");
        p.setQuestionText("When does the library close?");
        p.setExpectedAnswer("11:47");
        p.setCaseSensitive(false);

        em.persistAndFlush(p);
        em.clear();

        Puzzle loaded = puzzleRepository.findById("riddle1").orElseThrow();
        assertThat(loaded).isInstanceOf(RiddlePuzzle.class);
        assertThat(((RiddlePuzzle) loaded).getExpectedAnswer()).isEqualTo("11:47");
        assertThat(((RiddlePuzzle) loaded).isCaseSensitive()).isFalse();
    }

    @Test
    void sequencePuzzle_roundTrip_correctSubclass() {
        SequencePuzzle p = new SequencePuzzle();
        p.setId("seq1");
        p.setRoomId("room_reading_hall");
        p.setDescription("Bookshelf order");
        p.setExpectedSequence(List.of("510", "520", "610"));
        p.setAvailableItems(List.of("610", "510", "520"));

        em.persistAndFlush(p);
        em.clear();

        Puzzle loaded = puzzleRepository.findById("seq1").orElseThrow();
        assertThat(loaded).isInstanceOf(SequencePuzzle.class);
        assertThat(((SequencePuzzle) loaded).getAvailableItems())
                .containsExactlyInAnyOrder("510", "520", "610");
    }

    @Test
    void itemUsePuzzle_roundTrip_correctSubclass() {
        ItemUsePuzzle p = new ItemUsePuzzle();
        p.setId("use1");
        p.setRoomId("room_archives");
        p.setDescription("Cipher wheel");
        p.setRequiredItemId("brass_magnifying_glass");
        p.setTargetObjectId("cipher_wheel");
        p.setOutcomeMessage("THORNWICK revealed.");

        em.persistAndFlush(p);
        em.clear();

        Puzzle loaded = puzzleRepository.findById("use1").orElseThrow();
        assertThat(loaded).isInstanceOf(ItemUsePuzzle.class);
        assertThat(((ItemUsePuzzle) loaded).getRequiredItemId()).isEqualTo("brass_magnifying_glass");
        assertThat(((ItemUsePuzzle) loaded).getTargetObjectId()).isEqualTo("cipher_wheel");
    }
}
