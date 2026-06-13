package com.abhishri.escape.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class StatusBar extends JPanel {

    // Custom paintComponent with branching dot logic
    static class PuzzleDotsPanel extends JPanel {

        private static final int DOT_SIZE    = 10;
        private static final int DOT_SPACING = 16;

        int solved = 0;
        int total  = 0;

        PuzzleDotsPanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(130, 24));
        }

        void setSolvedCount(int s, int t) {
            this.solved = s;
            this.total  = t;
            repaint();
        }

        int getSolved() { return solved; }
        int getTotal()  { return total; }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int startX = (getWidth() - total * DOT_SPACING) / 2;
                int y = (getHeight() - DOT_SIZE) / 2;
                for (int i = 0; i < total; i++) {
                    int x = startX + i * DOT_SPACING;
                    if (i < solved) {
                        g2.setColor(ThemeConstants.BRASS_GLOW);
                        g2.fillOval(x, y, DOT_SIZE, DOT_SIZE);
                    } else {
                        g2.setColor(ThemeConstants.DIM_TEXT);
                        g2.drawOval(x, y, DOT_SIZE, DOT_SIZE);
                    }
                }
            } finally {
                g2.dispose();
            }
        }
    }

    private final JLabel roomLabel;
    private final PuzzleDotsPanel dotsPanel;
    final JButton saveButton;
    final JButton loadButton;
    final JButton newButton;

    public StatusBar() {
        setLayout(new BorderLayout());
        setBackground(ThemeConstants.NIGHT_BLACK);
        setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        roomLabel = new JLabel("◀ Entry Foyer");
        roomLabel.setFont(ThemeConstants.FONT_TITLE);
        roomLabel.setForeground(ThemeConstants.CANDLE_TEXT);

        dotsPanel = new PuzzleDotsPanel();

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        buttonPanel.setOpaque(false);
        saveButton = new JButton("Save");
        loadButton = new JButton("Load");
        newButton  = new JButton("New Game");
        ThemeConstants.applyDarkButton(saveButton);
        ThemeConstants.applyDarkButton(loadButton);
        ThemeConstants.applyDarkButton(newButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(loadButton);
        buttonPanel.add(newButton);

        add(roomLabel,   BorderLayout.WEST);
        add(dotsPanel,   BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.EAST);
    }

    public void setRoomName(String name) {
        roomLabel.setText("◀ " + name);
    }

    public void setSolvedCount(int solved, int total) {
        dotsPanel.setSolvedCount(solved, total);
    }

    public void setSaveEnabled(boolean enabled) {
        saveButton.setEnabled(enabled);
    }

    public JButton getSaveButton() { return saveButton; }
    public JButton getLoadButton() { return loadButton; }
    public JButton getNewButton()  { return newButton;  }

    PuzzleDotsPanel getDotsPanel() { return dotsPanel; }
}
