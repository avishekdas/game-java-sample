package com.abhishri.escape.ui;

import com.abhishri.escape.ui.dto.GameStateDTO;
import com.abhishri.escape.ui.dto.GameStatus;
import com.abhishri.escape.ui.dto.SaveMetadataDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatusBarLoadFlowTest {

    @Test
    void loadButton_withSaves_listsSavesAndLoadsSelectedFile() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Requires display");

        UUID testGameId = UUID.randomUUID();
        boolean[] loadGameCalled = {false};
        String[] loadedFilename = {null};

        GameApiClient stubClient = new GameApiClient("http://unused", new ObjectMapper()) {
            @Override
            public GameStateDTO newGame() {
                GameStateDTO s = new GameStateDTO();
                s.setGameId(testGameId);
                s.setGameStatus(GameStatus.IN_PROGRESS);
                return s;
            }

            @Override
            public List<SaveMetadataDTO> listSaves(UUID gameId) {
                SaveMetadataDTO meta = new SaveMetadataDTO();
                meta.setFilename("save-slot1.json");
                return List.of(meta);
            }

            @Override
            public GameStateDTO loadGame(UUID gameId, String filename) {
                loadGameCalled[0] = true;
                loadedFilename[0] = filename;
                GameStateDTO s = new GameStateDTO();
                s.setGameId(gameId);
                s.setGameStatus(GameStatus.IN_PROGRESS);
                return s;
            }
        };

        MainFrame[] frameRef = {null};
        SwingUtilities.invokeAndWait(() ->
            frameRef[0] = new MainFrame(new PlaceholderAssetManager(), stubClient) {
                @Override protected boolean confirmNewGame() { return true; }
                @Override protected String selectSaveFile(List<String> saves) { return saves.get(0); }
            }
        );
        MainFrame frame = frameRef[0];

        SwingUtilities.invokeAndWait(() -> frame.getStatusBar().getNewButton().doClick());
        SwingUtilities.invokeAndWait(() -> frame.getStatusBar().getLoadButton().doClick());

        assertTrue(loadGameCalled[0], "client.loadGame() should have been called");
        assertEquals("save-slot1.json", loadedFilename[0]);

        SwingUtilities.invokeAndWait(frame::dispose);
    }
}
