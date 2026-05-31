package com.abhishri.escape;

import com.abhishri.escape.domain.puzzle.CombinationPuzzle;
import com.abhishri.escape.domain.puzzle.RiddlePuzzle;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PuzzleAttemptTests {

    // --- CombinationPuzzle ---

    @Test
    void combinationPuzzle_correctCode_returnsTrue() {
        CombinationPuzzle p = combo("384");
        assertThat(p.attempt(Map.of("code", "384"))).isTrue();
    }

    @Test
    void combinationPuzzle_wrongCode_returnsFalse() {
        CombinationPuzzle p = combo("384");
        assertThat(p.attempt(Map.of("code", "000"))).isFalse();
    }

    @Test
    void combinationPuzzle_missingInput_returnsFalse() {
        CombinationPuzzle p = combo("384");
        assertThat(p.attempt(Map.of())).isFalse();
    }

    // --- RiddlePuzzle ---

    @Test
    void riddlePuzzle_correctAnswer_caseInsensitive() {
        RiddlePuzzle p = riddle("eleven forty-seven", false);
        assertThat(p.attempt(Map.of("answer", "ELEVEN FORTY-SEVEN"))).isTrue();
    }

    @Test
    void riddlePuzzle_correctAnswer_caseSensitive() {
        RiddlePuzzle p = riddle("Eleven", true);
        assertThat(p.attempt(Map.of("answer", "Eleven"))).isTrue();
        assertThat(p.attempt(Map.of("answer", "eleven"))).isFalse();  // wrong case
    }

    @Test
    void riddlePuzzle_whitespaceTrimmed() {
        RiddlePuzzle p = riddle("11:47", false);
        assertThat(p.attempt(Map.of("answer", "  11:47  "))).isTrue();
    }

    // --- helpers ---

    private CombinationPuzzle combo(String expectedCode) {
        CombinationPuzzle p = new CombinationPuzzle();
        p.setId("cp");
        p.setExpectedCode(expectedCode);
        p.setDigitCount(expectedCode.length());
        p.setRoomId("room");
        p.setDescription("combo");
        return p;
    }

    private RiddlePuzzle riddle(String answer, boolean caseSensitive) {
        RiddlePuzzle p = new RiddlePuzzle();
        p.setId("rp");
        p.setQuestionText("What time?");
        p.setExpectedAnswer(answer);
        p.setCaseSensitive(caseSensitive);
        p.setRoomId("room");
        p.setDescription("riddle");
        return p;
    }
}
