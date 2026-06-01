package com.abhishri.escape.ui;

import javax.swing.SwingUtilities;

public class EscapeRoomApp {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame(new PlaceholderAssetManager());
            frame.setVisible(true);
        });
    }
}
