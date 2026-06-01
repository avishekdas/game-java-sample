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

class StatusBarNewGameTest {

    @Test
    void newGameButton_click_callsClientNewGameAndRendersState() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Requires display");

        boolean[] newGameCalled = {false};
        GameApiClient stubClient = new GameApiClient("http://unused", new ObjectMapper()) {
            @Override
            public GameStateDTO newGame() {
                newGameCalled[0] = true;
                GameStateDTO s = new GameStateDTO();
                s.setGameId(UUID.randomUUID());
                s.setGameStatus(GameStatus.IN_PROGRESS);
                return s;
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

        assertTrue(newGameCalled[0], "client.newGame() should have been called");

        SwingUtilities.invokeAndWait(frame::dispose);
    }

    @Test
    void newGameButton_cancelConfirm_doesNotCallClient() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Requires display");

        boolean[] newGameCalled = {false};
        GameApiClient stubClient = new GameApiClient("http://unused", new ObjectMapper()) {
            @Override
            public GameStateDTO newGame() {
                newGameCalled[0] = true;
                GameStateDTO s = new GameStateDTO();
                s.setGameId(UUID.randomUUID());
                s.setGameStatus(GameStatus.IN_PROGRESS);
                return s;
            }
        };

        MainFrame[] frameRef = {null};
        SwingUtilities.invokeAndWait(() ->
            frameRef[0] = new MainFrame(new PlaceholderAssetManager(), stubClient) {
                @Override protected boolean confirmNewGame() { return false; }
            }
        );
        MainFrame frame = frameRef[0];

        SwingUtilities.invokeAndWait(() -> frame.getStatusBar().getNewButton().doClick());

        assertTrue(!newGameCalled[0], "client.newGame() should NOT have been called when confirmation is cancelled");

        SwingUtilities.invokeAndWait(frame::dispose);
    }
}
