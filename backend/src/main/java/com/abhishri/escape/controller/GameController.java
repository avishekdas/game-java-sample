package com.abhishri.escape.controller;

import com.abhishri.escape.dto.GameStateDTO;
import com.abhishri.escape.service.GameSessionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/game")
public class GameController {

    private final GameSessionService gameSessionService;

    public GameController(GameSessionService gameSessionService) {
        this.gameSessionService = gameSessionService;
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
}
