package com.abhishri.escape.ui;

import com.abhishri.escape.ui.dto.GameStateDTO;
import com.abhishri.escape.ui.dto.GameStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import java.awt.GraphicsEnvironment;
import java.util.UUID;
import javax.swing.SwingUtilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class WinScreenFiresTest {

    @Test
    void applyState_completeStatus_invokesShowWinDialog() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Requires display");

        String[] capturedMessage = {null};

        MainFrame frame = new MainFrame(new PlaceholderAssetManager()) {
            @Override
            protected void showWinDialog(String message) {
                capturedMessage[0] = message;
            }
        };

        GameStateDTO dto = new GameStateDTO();
        dto.setGameId(UUID.randomUUID());
        dto.setGameStatus(GameStatus.COMPLETE);
        dto.setDialogueMessage("The mystery is solved!");

        SwingUtilities.invokeAndWait(() -> frame.applyState(dto));

        assertNotNull(capturedMessage[0], "showWinDialog should have been called");
        assertEquals("The mystery is solved!", capturedMessage[0]);
        frame.dispose();
    }
}
