package com.abhishri.escape.ui;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.FlowLayout;

public class StatusBar extends JPanel {

    private final JLabel roomLabel;
    private final JLabel solvedLabel;
    final JButton saveButton;
    final JButton loadButton;
    final JButton newButton;

    public StatusBar() {
        setLayout(new FlowLayout(FlowLayout.LEFT));

        roomLabel = new JLabel("Room: Foyer");
        solvedLabel = new JLabel("Solved: 0/6");
        saveButton = new JButton("Save");
        loadButton = new JButton("Load");
        newButton = new JButton("New Game");

        add(roomLabel);
        add(solvedLabel);
        add(saveButton);
        add(loadButton);
        add(newButton);
    }

    public void setRoomName(String name) {
        roomLabel.setText("Room: " + name);
    }

    public void setSolvedCount(int solved, int total) {
        solvedLabel.setText("Solved: " + solved + "/" + total);
    }

    public void setSaveEnabled(boolean enabled) {
        saveButton.setEnabled(enabled);
    }

    public JButton getSaveButton() { return saveButton; }
    public JButton getLoadButton() { return loadButton; }
    public JButton getNewButton() { return newButton; }
}
