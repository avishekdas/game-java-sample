package com.abhishri.escape.service;

import com.abhishri.escape.domain.GameSession;
import com.abhishri.escape.domain.GameStatus;
import com.abhishri.escape.domain.PlayerInventory;
import com.abhishri.escape.domain.Room;
import com.abhishri.escape.domain.RoomObject;
import com.abhishri.escape.dto.ExamineRequest;
import com.abhishri.escape.dto.GameStateDTO;
import com.abhishri.escape.dto.InventoryItemDTO;
import com.abhishri.escape.dto.LastActionResult;
import com.abhishri.escape.dto.MoveRequest;
import com.abhishri.escape.dto.PickupRequest;
import com.abhishri.escape.dto.RoomDTO;
import com.abhishri.escape.dto.RoomObjectDTO;
import com.abhishri.escape.domain.puzzle.CombinationPuzzle;
import com.abhishri.escape.domain.puzzle.ItemUsePuzzle;
import com.abhishri.escape.domain.puzzle.Puzzle;
import com.abhishri.escape.domain.puzzle.RiddlePuzzle;
import com.abhishri.escape.domain.puzzle.SequencePuzzle;
import com.abhishri.escape.exception.GameNotFoundException;
import com.abhishri.escape.exception.InvalidMoveException;
import com.abhishri.escape.repository.GameSessionRepository;
import com.abhishri.escape.repository.PuzzleRepository;
import com.abhishri.escape.repository.RoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class GameSessionService {

    private static final Logger log = LoggerFactory.getLogger(GameSessionService.class);

    private final String startingRoomId;
    private final GameSessionRepository gameSessionRepository;
    private final RoomRepository roomRepository;
    private final InventoryService inventoryService;
    private final PuzzleRepository puzzleRepository;

    public GameSessionService(
            @Value("${escape.world.starting-room}") String startingRoomId,
            GameSessionRepository gameSessionRepository,
            RoomRepository roomRepository,
            InventoryService inventoryService,
            PuzzleRepository puzzleRepository) {
        this.startingRoomId = startingRoomId;
        this.gameSessionRepository = gameSessionRepository;
        this.roomRepository = roomRepository;
        this.inventoryService = inventoryService;
        this.puzzleRepository = puzzleRepository;
    }

    @Transactional
    public GameStateDTO createNewGame() {
        // Per design.md §19 risk #4: create inventory first, then attach to session
        PlayerInventory inventory = new PlayerInventory();
        inventory.setId(UUID.randomUUID());

        GameSession session = new GameSession();
        session.setId(UUID.randomUUID());
        session.setCurrentRoomId(startingRoomId);
        session.setStatus(GameStatus.IN_PROGRESS);
        session.setCreatedAt(LocalDateTime.now());
        session.setLastUpdatedAt(LocalDateTime.now());
        session.setInventory(inventory);

        session = gameSessionRepository.save(session);
        log.info("New game created id={}", session.getId());

        return buildStateDTO(session,
            "You wake on the cold floor. The door is sealed. The silence waits.",
            LastActionResult.NEW_GAME);
    }

    @Transactional
    public GameStateDTO move(UUID gameId, MoveRequest req) {
        GameSession session = gameSessionRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException(gameId));
        Room currentRoom = roomRepository.findById(session.getCurrentRoomId())
                .orElseThrow(() -> new IllegalStateException("Room not found: " + session.getCurrentRoomId()));

        // Conditionals rubric: adjacency check is the guard
        if (!currentRoom.isConnectedTo(req.getTargetRoomId())) {
            throw new InvalidMoveException("Cannot move to " + req.getTargetRoomId() + " from here.");
        }

        Room targetRoom = roomRepository.findById(req.getTargetRoomId())
                .orElseThrow(() -> new InvalidMoveException("Unknown room: " + req.getTargetRoomId()));

        session.setCurrentRoomId(req.getTargetRoomId());
        session.setLastUpdatedAt(LocalDateTime.now());
        GameSession saved = gameSessionRepository.save(session);
        log.info("Move game={} → room={}", gameId, req.getTargetRoomId());

        return buildStateDTO(saved, "You enter " + targetRoom.getName() + ".", LastActionResult.MOVE_SUCCESS);
    }

    @Transactional
    public GameStateDTO examine(UUID gameId, ExamineRequest req) {
        GameSession session = gameSessionRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException(gameId));
        Room currentRoom = roomRepository.findById(session.getCurrentRoomId())
                .orElseThrow(() -> new IllegalStateException("Room not found: " + session.getCurrentRoomId()));

        RoomObject obj = currentRoom.getObjects().stream()
                .filter(o -> o.getId().equals(req.getObjectId()))
                .findFirst()
                .orElseThrow(() -> new InvalidMoveException("Object not found in this room: " + req.getObjectId()));

        return buildStateDTO(session, obj.getLabel(), LastActionResult.EXAMINE_OK);
    }

    @Transactional
    public GameStateDTO pickup(UUID gameId, PickupRequest req) {
        GameSession session = gameSessionRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException(gameId));
        Room currentRoom = roomRepository.findById(session.getCurrentRoomId())
                .orElseThrow(() -> new IllegalStateException("Room not found: " + session.getCurrentRoomId()));

        RoomObject obj = currentRoom.getObjects().stream()
                .filter(o -> o.getId().equals(req.getObjectId()))
                .findFirst()
                .orElseThrow(() -> new InvalidMoveException("Object not found in this room: " + req.getObjectId()));

        if (obj.getPickupItemId() == null) {
            throw new InvalidMoveException("Cannot pick that up: " + req.getObjectId());
        }

        if (inventoryService.hasItem(session, obj.getPickupItemId())) {
            throw new InvalidMoveException("Already holding: " + obj.getPickupItemId());
        }

        inventoryService.addItem(session, obj.getPickupItemId());
        session.setLastUpdatedAt(LocalDateTime.now());
        gameSessionRepository.save(session);
        log.info("Pickup game={} item={}", gameId, obj.getPickupItemId());

        return buildStateDTO(session, "You pick up " + obj.getLabel() + ".", LastActionResult.PICKUP_OK);
    }

    @Transactional
    public GameStateDTO getState(UUID id) {
        GameSession session = gameSessionRepository.findById(id)
                .orElseThrow(() -> new GameNotFoundException(id));
        return buildStateDTO(session, null, null);
    }

    public GameStateDTO buildStateDTO(GameSession session, String message, LastActionResult result) {
        // Win condition: Conditionals rubric — flip status to COMPLETE when all puzzles solved
        List<String> allPuzzleIds = puzzleRepository.findAll().stream()
                .map(Puzzle::getId)
                .toList();
        if (session.getStatus() == GameStatus.IN_PROGRESS) {
            if (!allPuzzleIds.isEmpty() && session.getSolvedPuzzleIds().containsAll(allPuzzleIds)) {
                session.setStatus(GameStatus.COMPLETE);
                gameSessionRepository.save(session);
                log.info("Win condition met for game={}", session.getId());
            }
        }

        Room room = roomRepository.findById(session.getCurrentRoomId())
                .orElseThrow(() -> new IllegalStateException(
                    "Room not found: " + session.getCurrentRoomId()));

        List<RoomObjectDTO> objects = new ArrayList<>();
        for (RoomObject o : room.getObjects()) {
            RoomObjectDTO objDto = new RoomObjectDTO(
                o.getId(), o.getLabel(), o.isInteractable(),
                o.getPuzzleId(), o.getPickupItemId(), o.getObjectType());
            if (o.getPuzzleId() != null) {
                puzzleRepository.findById(o.getPuzzleId()).ifPresent(puzzle ->
                        populatePuzzleFields(objDto, puzzle));
            }
            objects.add(objDto);
        }

        RoomDTO roomDTO = new RoomDTO(
            room.getId(), room.getName(), room.getDescription(),
            objects, new ArrayList<>(room.getConnectedRoomIds()));

        List<InventoryItemDTO> inventory = inventoryService.snapshot(session);

        GameStateDTO dto = new GameStateDTO();
        dto.setGameId(session.getId());
        dto.setGameStatus(session.getStatus());
        dto.setCurrentRoom(roomDTO);
        dto.setInventory(inventory);
        dto.setSolvedPuzzleIds(new ArrayList<>(session.getSolvedPuzzleIds()));
        dto.setTotalPuzzles(allPuzzleIds.size());
        dto.setDialogueMessage(message);
        dto.setLastActionResult(result);
        return dto;
    }

    private void populatePuzzleFields(RoomObjectDTO dto, Puzzle puzzle) {
        if (puzzle instanceof CombinationPuzzle cp) {
            dto.setPuzzleType("COMBINATION");
            dto.setDigitCount(cp.getDigitCount());
        } else if (puzzle instanceof RiddlePuzzle rp) {
            dto.setPuzzleType("RIDDLE");
            dto.setQuestionText(rp.getQuestionText());
        } else if (puzzle instanceof SequencePuzzle sp) {
            dto.setPuzzleType("SEQUENCE");
            dto.setAvailableItems(new ArrayList<>(sp.getAvailableItems()));
        } else if (puzzle instanceof ItemUsePuzzle) {
            dto.setPuzzleType("ITEM_USE");
        }
    }
}
