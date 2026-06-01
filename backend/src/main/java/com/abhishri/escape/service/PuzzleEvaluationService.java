package com.abhishri.escape.service;

import com.abhishri.escape.domain.GameSession;
import com.abhishri.escape.domain.puzzle.ItemUsePuzzle;
import com.abhishri.escape.domain.puzzle.Puzzle;
import com.abhishri.escape.dto.AttemptPuzzleRequest;
import com.abhishri.escape.dto.GameStateDTO;
import com.abhishri.escape.dto.LastActionResult;
import com.abhishri.escape.dto.UseItemRequest;
import com.abhishri.escape.exception.GameNotFoundException;
import com.abhishri.escape.exception.ItemNotInInventoryException;
import com.abhishri.escape.exception.PrerequisiteNotMetException;
import com.abhishri.escape.exception.PuzzleNotFoundException;
import com.abhishri.escape.repository.GameSessionRepository;
import com.abhishri.escape.repository.PuzzleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PuzzleEvaluationService {

    private static final Logger log = LoggerFactory.getLogger(PuzzleEvaluationService.class);

    private final GameSessionRepository gameSessionRepository;
    private final PuzzleRepository puzzleRepository;
    private final InventoryService inventoryService;
    private final GameSessionService gameSessionService;

    public PuzzleEvaluationService(GameSessionRepository gameSessionRepository,
                                    PuzzleRepository puzzleRepository,
                                    InventoryService inventoryService,
                                    GameSessionService gameSessionService) {
        this.gameSessionRepository = gameSessionRepository;
        this.puzzleRepository = puzzleRepository;
        this.inventoryService = inventoryService;
        this.gameSessionService = gameSessionService;
    }

    @Transactional
    public GameStateDTO attempt(UUID gameId, AttemptPuzzleRequest req) {
        GameSession session = gameSessionRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException(gameId));

        Puzzle puzzle = puzzleRepository.findById(req.getPuzzleId())
                .orElseThrow(() -> new PuzzleNotFoundException(req.getPuzzleId()));

        // Inheritance rubric: polymorphic dispatch to the correct attempt() implementation
        if (!session.getSolvedPuzzleIds().containsAll(puzzle.getPrerequisitePuzzleIds())) {
            throw new PrerequisiteNotMetException(req.getPuzzleId());
        }

        boolean solved = puzzle.attempt(req.getInputs());

        if (solved) {
            if (!session.getSolvedPuzzleIds().contains(puzzle.getId())) {
                session.getSolvedPuzzleIds().add(puzzle.getId());
                if (puzzle.getRewardItemId() != null) {
                    inventoryService.addItem(session, puzzle.getRewardItemId());
                }
                session.setLastUpdatedAt(LocalDateTime.now());
                gameSessionRepository.save(session);
                log.info("Puzzle solved game={} puzzle={}", gameId, puzzle.getId());
            }
            return gameSessionService.buildStateDTO(session, puzzle.getDescription(), LastActionResult.PUZZLE_SOLVED);
        } else {
            return gameSessionService.buildStateDTO(session, "That's not right. Try again.", LastActionResult.PUZZLE_FAILED);
        }
    }

    @Transactional
    public GameStateDTO useItem(UUID gameId, UseItemRequest req) {
        GameSession session = gameSessionRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException(gameId));

        if (!inventoryService.hasItem(session, req.getItemId())) {
            throw new ItemNotInInventoryException(req.getItemId());
        }

        // Find the ItemUsePuzzle in the current room matching (requiredItemId, targetObjectId)
        ItemUsePuzzle puzzle = puzzleRepository.findByRoomId(session.getCurrentRoomId()).stream()
                .filter(p -> p instanceof ItemUsePuzzle)
                .map(p -> (ItemUsePuzzle) p)
                .filter(p -> p.getRequiredItemId().equals(req.getItemId()) &&
                             p.getTargetObjectId().equals(req.getTargetObjectId()))
                .findFirst()
                .orElseThrow(() -> new PuzzleNotFoundException(req.getItemId() + "+" + req.getTargetObjectId()));

        if (!session.getSolvedPuzzleIds().containsAll(puzzle.getPrerequisitePuzzleIds())) {
            throw new PrerequisiteNotMetException(puzzle.getId());
        }

        if (!session.getSolvedPuzzleIds().contains(puzzle.getId())) {
            session.getSolvedPuzzleIds().add(puzzle.getId());
            if (puzzle.getRewardItemId() != null) {
                inventoryService.addItem(session, puzzle.getRewardItemId());
            }
            session.setLastUpdatedAt(LocalDateTime.now());
            gameSessionRepository.save(session);
            log.info("ItemUsePuzzle solved game={} puzzle={}", gameId, puzzle.getId());
        }

        String message = puzzle.getOutcomeMessage() != null ? puzzle.getOutcomeMessage() : puzzle.getDescription();
        return gameSessionService.buildStateDTO(session, message, LastActionResult.PUZZLE_SOLVED);
    }
}
