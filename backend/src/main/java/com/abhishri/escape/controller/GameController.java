package com.abhishri.escape.controller;

import com.abhishri.escape.dto.AttemptPuzzleRequest;
import com.abhishri.escape.dto.ExamineRequest;
import com.abhishri.escape.dto.GameStateDTO;
import com.abhishri.escape.dto.MoveRequest;
import com.abhishri.escape.dto.PickupRequest;
import com.abhishri.escape.dto.UseItemRequest;
import com.abhishri.escape.service.GameSessionService;
import com.abhishri.escape.service.PuzzleEvaluationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/game")
public class GameController {

    private final GameSessionService gameSessionService;
    private final PuzzleEvaluationService puzzleEvaluationService;

    public GameController(GameSessionService gameSessionService,
                          PuzzleEvaluationService puzzleEvaluationService) {
        this.gameSessionService = gameSessionService;
        this.puzzleEvaluationService = puzzleEvaluationService;
    }

    @PostMapping("/new")
    public ResponseEntity<GameStateDTO> newGame() {
        GameStateDTO dto = gameSessionService.createNewGame();
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/{gameId}")
    public ResponseEntity<GameStateDTO> getState(@PathVariable("gameId") UUID gameId) {
        return ResponseEntity.ok(gameSessionService.getState(gameId));
    }

    @PostMapping("/{gameId}/move")
    public ResponseEntity<GameStateDTO> move(
            @PathVariable("gameId") UUID gameId,
            @Valid @RequestBody MoveRequest req) {
        return ResponseEntity.ok(gameSessionService.move(gameId, req));
    }

    @PostMapping("/{gameId}/examine")
    public ResponseEntity<GameStateDTO> examine(
            @PathVariable("gameId") UUID gameId,
            @Valid @RequestBody ExamineRequest req) {
        return ResponseEntity.ok(gameSessionService.examine(gameId, req));
    }

    @PostMapping("/{gameId}/pickup")
    public ResponseEntity<GameStateDTO> pickup(
            @PathVariable("gameId") UUID gameId,
            @Valid @RequestBody PickupRequest req) {
        return ResponseEntity.ok(gameSessionService.pickup(gameId, req));
    }

    @PostMapping("/{gameId}/attempt-puzzle")
    public ResponseEntity<GameStateDTO> attemptPuzzle(
            @PathVariable("gameId") UUID gameId,
            @Valid @RequestBody AttemptPuzzleRequest req) {
        return ResponseEntity.ok(puzzleEvaluationService.attempt(gameId, req));
    }

    @PostMapping("/{gameId}/use-item")
    public ResponseEntity<GameStateDTO> useItem(
            @PathVariable("gameId") UUID gameId,
            @Valid @RequestBody UseItemRequest req) {
        return ResponseEntity.ok(puzzleEvaluationService.useItem(gameId, req));
    }
}
