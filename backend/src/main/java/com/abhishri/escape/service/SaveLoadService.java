package com.abhishri.escape.service;

import com.abhishri.escape.domain.GameSession;
import com.abhishri.escape.dto.GameSnapshotDTO;
import com.abhishri.escape.dto.GameStateDTO;
import com.abhishri.escape.dto.LastActionResult;
import com.abhishri.escape.dto.SaveConfirmationDTO;
import com.abhishri.escape.dto.SaveMetadataDTO;
import com.abhishri.escape.exception.ApiErrorCode;
import com.abhishri.escape.exception.GameNotFoundException;
import com.abhishri.escape.exception.SaveLoadException;
import com.abhishri.escape.repository.GameSessionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class SaveLoadService {

    private static final Logger log = LoggerFactory.getLogger(SaveLoadService.class);
    private static final int SCHEMA_VERSION = 1;
    private static final DateTimeFormatter TIMESTAMP_FMT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

    private final Path savesDir;
    private final ObjectMapper objectMapper;
    private final GameSessionRepository gameSessionRepository;
    private final GameSessionService gameSessionService;

    public SaveLoadService(
            @Value("${escape.saves.directory}") String savesDirPath,
            ObjectMapper objectMapper,
            GameSessionRepository gameSessionRepository,
            GameSessionService gameSessionService) {
        this.savesDir = Paths.get(savesDirPath);
        this.objectMapper = objectMapper;
        this.gameSessionRepository = gameSessionRepository;
        this.gameSessionService = gameSessionService;
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(savesDir);
            log.debug("Saves directory ready: {}", savesDir.toAbsolutePath());
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create saves directory: " + savesDir, e);
        }
    }

    @Transactional(readOnly = true)
    public SaveConfirmationDTO save(UUID gameId) {
        GameSession session = gameSessionRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException(gameId));

        GameSnapshotDTO snapshot = new GameSnapshotDTO();
        snapshot.setSchemaVersion(SCHEMA_VERSION);
        snapshot.setGameId(session.getId());
        snapshot.setCurrentRoomId(session.getCurrentRoomId());
        snapshot.setStatus(session.getStatus());
        snapshot.setCreatedAt(session.getCreatedAt());
        snapshot.setLastUpdatedAt(session.getLastUpdatedAt());
        snapshot.setSolvedPuzzleIds(new ArrayList<>(session.getSolvedPuzzleIds()));
        snapshot.setHeldItemIds(new ArrayList<>(session.getInventory().getHeldItemIds()));

        String timestamp = LocalDateTime.now().format(TIMESTAMP_FMT);
        String filename = gameId + "-" + timestamp + ".json";
        File file = savesDir.resolve(filename).toFile();

        try {
            objectMapper.writeValue(file, snapshot);
        } catch (IOException e) {
            throw new SaveLoadException("Failed to write save file: " + filename, ApiErrorCode.SAVE_FAILED, e);
        }

        log.info("Saved game={} to file={} bytes={}", gameId, filename, file.length());
        return new SaveConfirmationDTO(filename, Instant.now().toString(), file.length());
    }

    @Transactional
    public GameStateDTO load(UUID gameId, String filename) {
        // Find game first so unknown gameId returns 404, not 500
        GameSession session = gameSessionRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException(gameId));

        File file = savesDir.resolve(filename).toFile();
        if (!file.exists()) {
            throw new SaveLoadException("Save file not found: " + filename, ApiErrorCode.LOAD_FAILED);
        }

        GameSnapshotDTO snapshot;
        try {
            snapshot = objectMapper.readValue(file, GameSnapshotDTO.class);
        } catch (IOException e) {
            throw new SaveLoadException("Failed to read save file: " + filename, ApiErrorCode.LOAD_FAILED, e);
        }

        if (snapshot.getSchemaVersion() != SCHEMA_VERSION) {
            throw new SaveLoadException(
                    "Incompatible save version: " + snapshot.getSchemaVersion(), ApiErrorCode.LOAD_FAILED);
        }

        session.setCurrentRoomId(snapshot.getCurrentRoomId());
        session.setStatus(snapshot.getStatus());
        session.setCreatedAt(snapshot.getCreatedAt());
        session.setLastUpdatedAt(snapshot.getLastUpdatedAt());
        session.getSolvedPuzzleIds().clear();
        session.getSolvedPuzzleIds().addAll(snapshot.getSolvedPuzzleIds());
        session.getInventory().getHeldItemIds().clear();
        session.getInventory().getHeldItemIds().addAll(snapshot.getHeldItemIds());
        gameSessionRepository.save(session);

        log.info("Loaded game={} from file={}", gameId, filename);
        return gameSessionService.buildStateDTO(session, "Game loaded.", LastActionResult.LOADED);
    }

    public List<SaveMetadataDTO> listSaves(UUID gameId) {
        String prefix = gameId.toString() + "-";
        try {
            if (!Files.exists(savesDir)) return List.of();
            try (Stream<Path> paths = Files.list(savesDir)) {
                return paths
                        .filter(p -> p.getFileName().toString().startsWith(prefix))
                        .filter(p -> p.getFileName().toString().endsWith(".json"))
                        .map(p -> {
                            File f = p.toFile();
                            return new SaveMetadataDTO(
                                    f.getName(),
                                    Instant.ofEpochMilli(f.lastModified()).toString(),
                                    f.length());
                        })
                        .collect(Collectors.toList());
            }
        } catch (IOException e) {
            throw new SaveLoadException("Failed to list saves for game: " + gameId, ApiErrorCode.LOAD_FAILED, e);
        }
    }
}
