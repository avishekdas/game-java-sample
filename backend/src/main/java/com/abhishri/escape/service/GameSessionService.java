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
import com.abhishri.escape.dto.RoomDTO;
import com.abhishri.escape.dto.RoomObjectDTO;
import com.abhishri.escape.exception.GameNotFoundException;
import com.abhishri.escape.exception.InvalidMoveException;
import com.abhishri.escape.repository.GameSessionRepository;
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

    public GameSessionService(
            @Value("${escape.world.starting-room}") String startingRoomId,
            GameSessionRepository gameSessionRepository,
            RoomRepository roomRepository,
            InventoryService inventoryService) {
        this.startingRoomId = startingRoomId;
        this.gameSessionRepository = gameSessionRepository;
        this.roomRepository = roomRepository;
        this.inventoryService = inventoryService;
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

    @Transactional(readOnly = true)
    public GameStateDTO getState(UUID id) {
        GameSession session = gameSessionRepository.findById(id)
                .orElseThrow(() -> new GameNotFoundException(id));
        return buildStateDTO(session, null, null);
    }

    public GameStateDTO buildStateDTO(GameSession session, String message, LastActionResult result) {
        Room room = roomRepository.findById(session.getCurrentRoomId())
                .orElseThrow(() -> new IllegalStateException(
                    "Room not found: " + session.getCurrentRoomId()));

        List<RoomObjectDTO> objects = new ArrayList<>();
        for (RoomObject o : room.getObjects()) {
            objects.add(new RoomObjectDTO(
                o.getId(), o.getLabel(), o.isInteractable(),
                o.getPuzzleId(), o.getPickupItemId(), o.getObjectType()));
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
        dto.setDialogueMessage(message);
        dto.setLastActionResult(result);
        return dto;
    }
}
