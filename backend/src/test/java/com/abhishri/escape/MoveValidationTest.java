package com.abhishri.escape;

import com.abhishri.escape.domain.GameSession;
import com.abhishri.escape.domain.GameStatus;
import com.abhishri.escape.domain.PlayerInventory;
import com.abhishri.escape.domain.Room;
import com.abhishri.escape.dto.GameStateDTO;
import com.abhishri.escape.dto.LastActionResult;
import com.abhishri.escape.dto.MoveRequest;
import com.abhishri.escape.exception.InvalidMoveException;
import com.abhishri.escape.repository.GameSessionRepository;
import com.abhishri.escape.repository.RoomRepository;
import com.abhishri.escape.service.GameSessionService;
import com.abhishri.escape.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MoveValidationTest {

    @Mock private GameSessionRepository gameSessionRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private InventoryService inventoryService;

    private GameSessionService service;
    private Room roomA;
    private Room roomB;

    @BeforeEach
    void setUp() {
        service = new GameSessionService("room_a", gameSessionRepository, roomRepository, inventoryService);

        roomA = new Room();
        roomA.setId("room_a");
        roomA.setName("Room A");
        roomA.setDescription("Desc A");
        roomA.setConnectedRoomIds(List.of("room_b"));
        roomA.setObjects(new ArrayList<>());
        roomA.setPuzzleIds(new ArrayList<>());

        roomB = new Room();
        roomB.setId("room_b");
        roomB.setName("Room B");
        roomB.setDescription("Desc B");
        roomB.setConnectedRoomIds(List.of("room_a"));
        roomB.setObjects(new ArrayList<>());
        roomB.setPuzzleIds(new ArrayList<>());

        // room_a is always loaded as the current room
        when(roomRepository.findById("room_a")).thenReturn(Optional.of(roomA));
    }

    @Test
    void move_toAdjacentRoom_updatesCurrentRoomIdAndReturnsMoveSuccess() {
        UUID gameId = UUID.randomUUID();
        GameSession session = makeSession(gameId, "room_a");
        when(gameSessionRepository.findById(gameId)).thenReturn(Optional.of(session));
        when(gameSessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(roomRepository.findById("room_b")).thenReturn(Optional.of(roomB));
        when(inventoryService.snapshot(any())).thenReturn(List.of());

        MoveRequest req = new MoveRequest();
        req.setTargetRoomId("room_b");

        GameStateDTO dto = service.move(gameId, req);

        assertThat(session.getCurrentRoomId()).isEqualTo("room_b");
        assertThat(dto.getCurrentRoom().getId()).isEqualTo("room_b");
        assertThat(dto.getLastActionResult()).isEqualTo(LastActionResult.MOVE_SUCCESS);
    }

    @Test
    void move_toNonAdjacentRoom_throwsInvalidMoveException() {
        UUID gameId = UUID.randomUUID();
        GameSession session = makeSession(gameId, "room_a");
        when(gameSessionRepository.findById(gameId)).thenReturn(Optional.of(session));

        MoveRequest req = new MoveRequest();
        req.setTargetRoomId("room_c");  // not in room_a's connected list

        assertThatThrownBy(() -> service.move(gameId, req))
                .isInstanceOf(InvalidMoveException.class);
    }

    @Test
    void move_toUnknownRoom_throwsInvalidMoveException() {
        UUID gameId = UUID.randomUUID();
        GameSession session = makeSession(gameId, "room_a");
        when(gameSessionRepository.findById(gameId)).thenReturn(Optional.of(session));

        MoveRequest req = new MoveRequest();
        req.setTargetRoomId("does_not_exist");

        assertThatThrownBy(() -> service.move(gameId, req))
                .isInstanceOf(InvalidMoveException.class);
    }

    private GameSession makeSession(UUID id, String roomId) {
        GameSession session = new GameSession();
        session.setId(id);
        session.setCurrentRoomId(roomId);
        session.setStatus(GameStatus.IN_PROGRESS);
        session.setCreatedAt(LocalDateTime.now());
        session.setLastUpdatedAt(LocalDateTime.now());
        PlayerInventory inv = new PlayerInventory();
        inv.setId(UUID.randomUUID());
        session.setInventory(inv);
        return session;
    }
}
