package com.abhishri.escape.ui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class StatusBarDotsTest {

    private StatusBar statusBar;

    @BeforeEach
    void setUp() {
        statusBar = new StatusBar();
    }

    @Test
    void setSolvedCount_storesValuesInDotsPanel() {
        statusBar.setSolvedCount(3, 6);
        StatusBar.PuzzleDotsPanel dots = statusBar.getDotsPanel();
        assertNotNull(dots, "getDotsPanel() must return a non-null PuzzleDotsPanel");
        assertEquals(3, dots.getSolved(), "getSolved() must return the value passed to setSolvedCount");
        assertEquals(6, dots.getTotal(),  "getTotal() must return the value passed to setSolvedCount");
    }

    @Test
    void setSolvedCount_zero_storesZero() {
        statusBar.setSolvedCount(0, 6);
        assertEquals(0, statusBar.getDotsPanel().getSolved());
        assertEquals(6, statusBar.getDotsPanel().getTotal());
    }

    @Test
    void setSolvedCount_full_storesFull() {
        statusBar.setSolvedCount(6, 6);
        assertEquals(6, statusBar.getDotsPanel().getSolved());
        assertEquals(6, statusBar.getDotsPanel().getTotal());
    }

    // Regression gate: existing button getters must still return non-null JButton instances

    @Test
    void buttonGetters_returnNonNull() {
        assertNotNull(statusBar.getNewButton(),  "getNewButton()");
        assertNotNull(statusBar.getSaveButton(), "getSaveButton()");
        assertNotNull(statusBar.getLoadButton(), "getLoadButton()");
    }
}
