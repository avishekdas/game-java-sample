package com.abhishri.escape.ui;

import com.abhishri.escape.ui.dto.GameStateDTO;
import com.abhishri.escape.ui.dto.GameStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

class StatusBarSaveButtonTest {

    @Test
    void saveButton_click_callsSaveGameAndShowsFilenameInDialogue() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Requires display");

        String savedFilename = "abc123-2026.json";
        boolean[] saveGameCalled = {false};

        GameApiClient stubClient = new GameApiClient("http://unused", new ObjectMapper()) {
            @Override
            public GameStateDTO newGame() {
                GameStateDTO s = new GameStateDTO();
                s.setGameId(UUID.randomUUID());
                s.setGameStatus(GameStatus.IN_PROGRESS);
                return s;
            }

            @Override
            public String saveGame(UUID gameId) {
                saveGameCalled[0] = true;
                return savedFilename;
            }
        };

        MainFrame[] frameRef = {null};
        SwingUtilities.invokeAndWait(() ->
            frameRef[0] = new MainFrame(new PlaceholderAssetManager(), stubClient) {
                @Override protected boolean confirmNewGame() { return true; }
            }
        );
        MainFrame frame = frameRef[0];

        SwingUtilities.invokeAndWait(() -> frame.getStatusBar().getNewButton().doClick());
        SwingUtilities.invokeAndWait(() -> frame.getStatusBar().getSaveButton().doClick());

        assertTrue(saveGameCalled[0], "client.saveGame() should have been called");
        assertTrue(frame.getDialoguePanel().getText().contains(savedFilename),
                "Dialogue should mention the saved filename");

        SwingUtilities.invokeAndWait(frame::dispose);
    }
}
