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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainFrameBuildHotspotsSolvedTest {

    private RoomObjectDTO clockObj() {
        RoomObjectDTO obj = new RoomObjectDTO();
        obj.setId("wall_clock");
        obj.setLabel("Wall Clock");
        obj.setObjectType(ObjectType.PUZZLE);
        obj.setPuzzleId("puzzle_clock");
        return obj;
    }

    private RoomDTO foyer(RoomObjectDTO obj) {
        RoomDTO room = new RoomDTO();
        room.setId("room_foyer");
        room.setName("Entry Foyer");
        room.setObjects(List.of(obj));
        return room;
    }

    private MainFrame buildFrame() throws Exception {
        MainFrame[] ref = {null};
        SwingUtilities.invokeAndWait(() ->
            ref[0] = new MainFrame(new PlaceholderAssetManager(),
                    new GameApiClient("http://unused", new ObjectMapper()))
        );
        return ref[0];
    }

    @Test
    void applyState_puzzleSolved_hotspotIsSolvedTrue() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Requires display");
        MainFrame frame = buildFrame();
        try {
            GameStateDTO dto = new GameStateDTO();
            dto.setGameStatus(GameStatus.IN_PROGRESS);
            dto.setCurrentRoom(foyer(clockObj()));
            dto.setSolvedPuzzleIds(List.of("puzzle_clock"));

            SwingUtilities.invokeAndWait(() -> frame.applyState(dto));

            Hotspot h = frame.getScenePanel().getHotspots().stream()
                    .filter(hs -> "wall_clock".equals(hs.getId()))
                    .findFirst().orElse(null);
            assertNotNull(h, "hotspot for wall_clock must exist");
            assertTrue(h.isSolved(), "wall_clock hotspot must be solved when puzzle_clock is in solvedIds");
        } finally {
            SwingUtilities.invokeAndWait(frame::dispose);
        }
    }

    @Test
    void applyState_puzzleNotSolved_hotspotIsSolvedFalse() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Requires display");
        MainFrame frame = buildFrame();
        try {
            GameStateDTO dto = new GameStateDTO();
            dto.setGameStatus(GameStatus.IN_PROGRESS);
            dto.setCurrentRoom(foyer(clockObj()));
            dto.setSolvedPuzzleIds(List.of());   // empty — not solved

            SwingUtilities.invokeAndWait(() -> frame.applyState(dto));

            Hotspot h = frame.getScenePanel().getHotspots().stream()
                    .filter(hs -> "wall_clock".equals(hs.getId()))
                    .findFirst().orElse(null);
            assertNotNull(h);
            assertFalse(h.isSolved(), "wall_clock hotspot must not be solved when solvedIds is empty");
        } finally {
            SwingUtilities.invokeAndWait(frame::dispose);
        }
    }

    @Test
    void applyState_nullSolvedPuzzleIds_hotspotIsSolvedFalse() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Requires display");
        MainFrame frame = buildFrame();
        try {
            GameStateDTO dto = new GameStateDTO();
            dto.setGameStatus(GameStatus.IN_PROGRESS);
            dto.setCurrentRoom(foyer(clockObj()));
            dto.setSolvedPuzzleIds(null);   // null — treated as empty, no NPE

            SwingUtilities.invokeAndWait(() -> frame.applyState(dto));

            Hotspot h = frame.getScenePanel().getHotspots().stream()
                    .filter(hs -> "wall_clock".equals(hs.getId()))
                    .findFirst().orElse(null);
            assertNotNull(h);
            assertFalse(h.isSolved(), "wall_clock hotspot must not be solved when solvedIds is null");
        } finally {
            SwingUtilities.invokeAndWait(frame::dispose);
        }
    }
}
