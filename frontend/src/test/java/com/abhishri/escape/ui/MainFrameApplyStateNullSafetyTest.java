package com.abhishri.escape.ui;

import com.abhishri.escape.ui.dto.GameStateDTO;
import com.abhishri.escape.ui.dto.GameStatus;
import com.abhishri.escape.ui.dto.ObjectType;
import com.abhishri.escape.ui.dto.RoomDTO;
import com.abhishri.escape.ui.dto.RoomObjectDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.util.List;

/**
 * Regression-pinning test — the existing null-guards in applyState() already
 * handle these cases. This test exists to prevent future regressions.
 */
class MainFrameApplyStateNullSafetyTest {

    private MainFrame buildFrame() throws Exception {
        MainFrame[] ref = {null};
        SwingUtilities.invokeAndWait(() ->
            ref[0] = new MainFrame(new PlaceholderAssetManager(),
                    new GameApiClient("http://unused", new ObjectMapper()))
        );
        return ref[0];
    }

    @Test
    void applyState_nullSolvedPuzzleIds_andNullCurrentRoom_doesNotThrow() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Requires display");
        MainFrame frame = buildFrame();
        try {
            GameStateDTO dto = new GameStateDTO();
            dto.setGameStatus(GameStatus.IN_PROGRESS);
            // both solvedPuzzleIds and currentRoom are null
            SwingUtilities.invokeAndWait(() -> frame.applyState(dto));
        } finally {
            SwingUtilities.invokeAndWait(frame::dispose);
        }
    }

    @Test
    void applyState_nullSolvedPuzzleIds_withValidRoom_doesNotThrow() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Requires display");
        MainFrame frame = buildFrame();
        try {
            RoomObjectDTO obj = new RoomObjectDTO();
            obj.setId("wall_clock");
            obj.setLabel("Wall Clock");
            obj.setObjectType(ObjectType.PUZZLE);
            obj.setPuzzleId("puzzle_clock");

            RoomDTO room = new RoomDTO();
            room.setId("room_foyer");
            room.setName("Entry Foyer");
            room.setObjects(List.of(obj));

            GameStateDTO dto = new GameStateDTO();
            dto.setGameStatus(GameStatus.IN_PROGRESS);
            dto.setCurrentRoom(room);
            // solvedPuzzleIds is null

            SwingUtilities.invokeAndWait(() -> frame.applyState(dto));
        } finally {
            SwingUtilities.invokeAndWait(frame::dispose);
        }
    }
}
