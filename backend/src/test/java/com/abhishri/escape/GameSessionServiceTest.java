package com.abhishri.escape;

import com.abhishri.escape.domain.GameSession;
import com.abhishri.escape.domain.GameStatus;
import com.abhishri.escape.domain.ObjectType;
import com.abhishri.escape.domain.PlayerInventory;
import com.abhishri.escape.domain.Room;
import com.abhishri.escape.domain.RoomObject;
import com.abhishri.escape.dto.GameStateDTO;
import com.abhishri.escape.dto.LastActionResult;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameSessionServiceTest {

    @Mock private GameSessionRepository gameSessionRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private InventoryService inventoryService;

    private GameSessionService service;

    @BeforeEach
    void setUp() {
        service = new GameSessionService(
            "room_foyer",
            gameSessionRepository,
            roomRepository,
            inventoryService);

        RoomObject clock = new RoomObject();
        clock.setId("wall_clock");
        clock.setLabel("Wall Clock");
        clock.setObjectType(ObjectType.PUZZLE);
        clock.setPuzzleId("puzzle_clock");
        clock.setInteractable(true);

        Room foyer = new Room();
        foyer.setId("room_foyer");
        foyer.setName("The Entry Foyer");
        foyer.setDescription("Cold marble.");
        foyer.setObjects(List.of(clock));
        foyer.setConnectedRoomIds(List.of("room_reading_hall"));
        when(roomRepository.findById("room_foyer")).thenReturn(Optional.of(foyer));
        when(inventoryService.snapshot(any())).thenReturn(List.of());
    }

    @Test
    void createNewGame_returnsInProgressSessionInStartingRoom() {
        when(gameSessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GameStateDTO dto = service.createNewGame();

        assertThat(dto.getGameStatus()).isEqualTo(GameStatus.IN_PROGRESS);
        assertThat(dto.getCurrentRoom().getId()).isEqualTo("room_foyer");
        assertThat(dto.getInventory()).isEmpty();
        assertThat(dto.getSolvedPuzzleIds()).isEmpty();
        assertThat(dto.getLastActionResult()).isEqualTo(LastActionResult.NEW_GAME);
        assertThat(dto.getGameId()).isNotNull();
    }

    @Test
    void createNewGame_savesSessionOnce() {
        when(gameSessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.createNewGame();

        verify(gameSessionRepository).save(any(GameSession.class));
    }

    @Test
    void buildStateDTO_mapsRoomObjectsCorrectly() {
        PlayerInventory inv = new PlayerInventory();
        inv.setId(UUID.randomUUID());

        GameSession session = new GameSession();
        session.setId(UUID.randomUUID());
        session.setCurrentRoomId("room_foyer");
        session.setStatus(GameStatus.IN_PROGRESS);
        session.setCreatedAt(LocalDateTime.now());
        session.setLastUpdatedAt(LocalDateTime.now());
        session.setInventory(inv);

        GameStateDTO dto = service.buildStateDTO(session, "Hello", LastActionResult.NEW_GAME);

        assertThat(dto.getCurrentRoom().getObjects()).hasSize(1);
        assertThat(dto.getCurrentRoom().getObjects().get(0).getId()).isEqualTo("wall_clock");
        assertThat(dto.getCurrentRoom().getObjects().get(0).getObjectType()).isEqualTo(ObjectType.PUZZLE);
        assertThat(dto.getCurrentRoom().getExits()).containsExactly("room_reading_hall");
        assertThat(dto.getDialogueMessage()).isEqualTo("Hello");
    }
}
