package com.abhishri.escape;

import com.abhishri.escape.domain.GameSession;
import com.abhishri.escape.domain.GameStatus;
import com.abhishri.escape.domain.PlayerInventory;
import com.abhishri.escape.domain.puzzle.RiddlePuzzle;
import com.abhishri.escape.dto.AttemptPuzzleRequest;
import com.abhishri.escape.dto.GameStateDTO;
import com.abhishri.escape.dto.LastActionResult;
import com.abhishri.escape.exception.PrerequisiteNotMetException;
import com.abhishri.escape.repository.GameSessionRepository;
import com.abhishri.escape.repository.PuzzleRepository;
import com.abhishri.escape.service.GameSessionService;
import com.abhishri.escape.service.InventoryService;
import com.abhishri.escape.service.PuzzleEvaluationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PuzzleEvaluationServiceTest {

    @Mock private GameSessionRepository gameSessionRepository;
    @Mock private PuzzleRepository puzzleRepository;
    @Mock private InventoryService inventoryService;
    @Mock private GameSessionService gameSessionService;

    private PuzzleEvaluationService service;

    @BeforeEach
    void setUp() {
        service = new PuzzleEvaluationService(
                gameSessionRepository, puzzleRepository, inventoryService, gameSessionService);
    }

    @Test
    void prereqNotMet_throwsPrerequisiteNotMetException() {
        UUID gameId = UUID.randomUUID();
        GameSession session = makeSession(gameId, new ArrayList<>());  // no solved puzzles

        RiddlePuzzle puzzle = riddle("puzzle_b", "yes", false, List.of("puzzle_a"), "reward");
        when(gameSessionRepository.findById(gameId)).thenReturn(Optional.of(session));
        when(puzzleRepository.findById("puzzle_b")).thenReturn(Optional.of(puzzle));

        AttemptPuzzleRequest req = req("puzzle_b", Map.of("answer", "yes"));

        assertThatThrownBy(() -> service.attempt(gameId, req))
                .isInstanceOf(PrerequisiteNotMetException.class);
    }

    @Test
    void rewardItem_addedToInventoryOnFirstSolve() {
        UUID gameId = UUID.randomUUID();
        GameSession session = makeSession(gameId, new ArrayList<>());

        RiddlePuzzle puzzle = riddle("puzzle_a", "yes", false, List.of(), "reward_item");
        when(gameSessionRepository.findById(gameId)).thenReturn(Optional.of(session));
        when(puzzleRepository.findById("puzzle_a")).thenReturn(Optional.of(puzzle));
        when(gameSessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        stubBuildStateDTO(LastActionResult.PUZZLE_SOLVED);

        GameStateDTO dto = service.attempt(gameId, req("puzzle_a", Map.of("answer", "yes")));

        assertThat(dto.getLastActionResult()).isEqualTo(LastActionResult.PUZZLE_SOLVED);
        assertThat(session.getSolvedPuzzleIds()).containsExactly("puzzle_a");
        verify(inventoryService).addItem(session, "reward_item");
    }

    @Test
    void idempotency_solveAlreadySolvedPuzzle_doesNotDoubleInsertReward() {
        UUID gameId = UUID.randomUUID();
        // puzzle_a is already solved
        GameSession session = makeSession(gameId, new ArrayList<>(List.of("puzzle_a")));

        RiddlePuzzle puzzle = riddle("puzzle_a", "yes", false, List.of(), "reward_item");
        when(gameSessionRepository.findById(gameId)).thenReturn(Optional.of(session));
        when(puzzleRepository.findById("puzzle_a")).thenReturn(Optional.of(puzzle));
        stubBuildStateDTO(LastActionResult.PUZZLE_SOLVED);

        service.attempt(gameId, req("puzzle_a", Map.of("answer", "yes")));

        // save and addItem must NOT be called — puzzle was already solved
        verify(inventoryService, never()).addItem(any(), any());
        verify(gameSessionRepository, never()).save(any());
        assertThat(session.getSolvedPuzzleIds()).containsExactly("puzzle_a");
    }

    // --- helpers ---

    private void stubBuildStateDTO(LastActionResult result) {
        when(gameSessionService.buildStateDTO(any(), any(), any())).thenAnswer(inv -> {
            GameStateDTO dto = new GameStateDTO();
            dto.setLastActionResult(inv.getArgument(2));
            return dto;
        });
    }

    private RiddlePuzzle riddle(String id, String answer, boolean caseSensitive,
                                 List<String> prereqs, String rewardItemId) {
        RiddlePuzzle p = new RiddlePuzzle();
        p.setId(id);
        p.setQuestionText("Q?");
        p.setExpectedAnswer(answer);
        p.setCaseSensitive(caseSensitive);
        p.setPrerequisitePuzzleIds(new ArrayList<>(prereqs));
        p.setRewardItemId(rewardItemId);
        p.setDescription("desc");
        p.setRoomId("test_room");
        return p;
    }

    private AttemptPuzzleRequest req(String puzzleId, Map<String, String> inputs) {
        AttemptPuzzleRequest r = new AttemptPuzzleRequest();
        r.setPuzzleId(puzzleId);
        r.setInputs(inputs);
        return r;
    }

    private GameSession makeSession(UUID id, List<String> solvedPuzzleIds) {
        PlayerInventory inv = new PlayerInventory();
        inv.setId(UUID.randomUUID());

        GameSession session = new GameSession();
        session.setId(id);
        session.setCurrentRoomId("test_room");
        session.setStatus(GameStatus.IN_PROGRESS);
        session.setCreatedAt(LocalDateTime.now());
        session.setLastUpdatedAt(LocalDateTime.now());
        session.setInventory(inv);
        session.setSolvedPuzzleIds(solvedPuzzleIds);
        return session;
    }
}
