package com.abhishri.escape;

import com.abhishri.escape.domain.GameSession;
import com.abhishri.escape.domain.GameStatus;
import com.abhishri.escape.domain.PlayerInventory;
import com.abhishri.escape.dto.GameSnapshotDTO;
import com.abhishri.escape.dto.GameStateDTO;
import com.abhishri.escape.dto.SaveConfirmationDTO;
import com.abhishri.escape.exception.SaveLoadException;
import com.abhishri.escape.repository.GameSessionRepository;
import com.abhishri.escape.service.GameSessionService;
import com.abhishri.escape.service.SaveLoadService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaveLoadServiceTest {

    @TempDir
    java.nio.file.Path tempDir;

    @Mock private GameSessionRepository gameSessionRepository;
    @Mock private GameSessionService gameSessionService;

    private SaveLoadService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
        service = new SaveLoadService(
                tempDir.toString(), objectMapper, gameSessionRepository, gameSessionService);
        service.init();
    }

    @Test
    void save_createsFile_inSavesDir() {
        UUID gameId = UUID.randomUUID();
        when(gameSessionRepository.findById(gameId)).thenReturn(Optional.of(buildSession(gameId)));

        SaveConfirmationDTO confirmation = service.save(gameId);

        File file = tempDir.resolve(confirmation.getFilename()).toFile();
        assertThat(file).exists();
    }

    @Test
    void save_fileContainsSchemaVersion1AndGameId() throws Exception {
        UUID gameId = UUID.randomUUID();
        when(gameSessionRepository.findById(gameId)).thenReturn(Optional.of(buildSession(gameId)));

        SaveConfirmationDTO confirmation = service.save(gameId);
        File file = tempDir.resolve(confirmation.getFilename()).toFile();
        GameSnapshotDTO snapshot = objectMapper.readValue(file, GameSnapshotDTO.class);

        assertThat(snapshot.getSchemaVersion()).isEqualTo(1);
        assertThat(snapshot.getGameId()).isEqualTo(gameId);
    }

    @Test
    void save_filenameStartsWithGameIdAndEndsWithJson() {
        UUID gameId = UUID.randomUUID();
        when(gameSessionRepository.findById(gameId)).thenReturn(Optional.of(buildSession(gameId)));

        SaveConfirmationDTO confirmation = service.save(gameId);

        assertThat(confirmation.getFilename()).startsWith(gameId.toString());
        assertThat(confirmation.getFilename()).endsWith(".json");
        assertThat(confirmation.getSizeBytes()).isGreaterThan(0);
    }

    @Test
    void load_happyPath_updatesSessionFromSnapshot() throws Exception {
        UUID gameId = UUID.randomUUID();
        GameSession session = buildSession(gameId);
        GameSnapshotDTO snapshot = buildSnapshot(gameId, "test_room_2",
                List.of("test_puzzle"), List.of("test_item"));
        String filename = gameId + "-20260531T120000.json";
        objectMapper.writeValue(tempDir.resolve(filename).toFile(), snapshot);

        when(gameSessionRepository.findById(gameId)).thenReturn(Optional.of(session));
        when(gameSessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(gameSessionService.buildStateDTO(any(), any(), any())).thenReturn(new GameStateDTO());

        service.load(gameId, filename);

        ArgumentCaptor<GameSession> captor = ArgumentCaptor.forClass(GameSession.class);
        verify(gameSessionRepository).save(captor.capture());
        GameSession saved = captor.getValue();
        assertThat(saved.getCurrentRoomId()).isEqualTo("test_room_2");
        assertThat(saved.getSolvedPuzzleIds()).containsExactly("test_puzzle");
        assertThat(saved.getInventory().getHeldItemIds()).containsExactly("test_item");
    }

    @Test
    void load_schemaVersionMismatch_throwsSaveLoadException() throws Exception {
        UUID gameId = UUID.randomUUID();
        GameSession session = buildSession(gameId);
        GameSnapshotDTO snapshot = buildSnapshot(gameId, "test_room", List.of(), List.of());
        snapshot.setSchemaVersion(99);
        String filename = gameId + "-20260531T120000.json";
        objectMapper.writeValue(tempDir.resolve(filename).toFile(), snapshot);

        when(gameSessionRepository.findById(gameId)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service.load(gameId, filename))
                .isInstanceOf(SaveLoadException.class)
                .hasMessageContaining("Incompatible save version");
    }

    @Test
    void load_fileNotFound_throwsSaveLoadException() {
        UUID gameId = UUID.randomUUID();
        String filename = gameId + "-20260531T120000.json";

        when(gameSessionRepository.findById(gameId)).thenReturn(Optional.of(buildSession(gameId)));

        assertThatThrownBy(() -> service.load(gameId, filename))
                .isInstanceOf(SaveLoadException.class);
    }

    // --- helpers ---

    private GameSession buildSession(UUID gameId) {
        PlayerInventory inv = new PlayerInventory();
        inv.setId(UUID.randomUUID());

        GameSession session = new GameSession();
        session.setId(gameId);
        session.setCurrentRoomId("test_room");
        session.setStatus(GameStatus.IN_PROGRESS);
        session.setCreatedAt(LocalDateTime.now());
        session.setLastUpdatedAt(LocalDateTime.now());
        session.setInventory(inv);
        return session;
    }

    private GameSnapshotDTO buildSnapshot(UUID gameId, String roomId,
                                          List<String> solved, List<String> held) {
        GameSnapshotDTO snapshot = new GameSnapshotDTO();
        snapshot.setSchemaVersion(1);
        snapshot.setGameId(gameId);
        snapshot.setCurrentRoomId(roomId);
        snapshot.setStatus(GameStatus.IN_PROGRESS);
        snapshot.setCreatedAt(LocalDateTime.now());
        snapshot.setLastUpdatedAt(LocalDateTime.now());
        snapshot.setSolvedPuzzleIds(new ArrayList<>(solved));
        snapshot.setHeldItemIds(new ArrayList<>(held));
        return snapshot;
    }
}
