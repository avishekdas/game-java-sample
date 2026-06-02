package com.abhishri.escape.ui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScenePanelFadeGuardTest {

    private ScenePanel panel;

    @BeforeEach
    void setUp() {
        panel = new ScenePanel(new PlaceholderAssetManager());
    }

    @AfterEach
    void tearDown() {
        // Stop the timer after each test so ticks don't leak into the next test
        if (panel.fadeTimer != null && panel.fadeTimer.isRunning()) {
            panel.fadeTimer.stop();
        }
    }

    @Test
    void setCurrentRoomId_firstLoad_fromNullToRoom_doesNotStartFade() {
        // currentRoomId starts null; transitioning null → first room must NOT start fade
        panel.setCurrentRoomId("room_foyer");
        assertFalse(panel.isFadeRunning(), "first-load transition must not start the crossfade");
    }

    @Test
    void setCurrentRoomId_genuineRoomChange_startsFade() {
        panel.setCurrentRoomId("room_foyer");       // first load — no fade
        panel.setCurrentRoomId("room_reading_hall"); // genuine change — fade starts
        assertTrue(panel.isFadeRunning(), "genuine room change must start the crossfade timer");
    }

    @Test
    void setCurrentRoomId_sameRoom_doesNotRestartFade() {
        panel.setCurrentRoomId("room_foyer");
        panel.setCurrentRoomId("room_reading_hall");  // starts fade
        assertTrue(panel.isFadeRunning());

        // Manually record that timer is running; calling same room must not touch the timer
        panel.setCurrentRoomId("room_reading_hall");  // same room — guard fires
        // Timer should still be running (not restarted or stopped)
        assertTrue(panel.isFadeRunning(), "same-room call must leave the timer running unchanged");
    }

    @Test
    void setCurrentRoomId_roomChangeWhileFading_restartsFadeCleanly() {
        panel.setCurrentRoomId("room_foyer");
        panel.setCurrentRoomId("room_reading_hall");  // starts fade
        assertTrue(panel.isFadeRunning());

        panel.setCurrentRoomId("room_archives");      // change while fading — stop + restart
        assertTrue(panel.isFadeRunning(), "changing room while fade is in flight must restart the timer (still running)");
    }
}
