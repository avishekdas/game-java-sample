package com.abhishri.escape.ui;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;

public class DialoguePanel extends JPanel {

    private static final int PREFERRED_HEIGHT = 120;

    private final JTextArea textArea;

    public DialoguePanel() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(0, PREFERRED_HEIGHT));
        setBackground(ThemeConstants.DARK_WOOD);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(4, 4, 4, 4),
                BorderFactory.createLineBorder(ThemeConstants.AGED_BRASS, 1)));

        textArea = new JTextArea(4, 60);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setBackground(ThemeConstants.PARCHMENT);
        textArea.setForeground(ThemeConstants.PARCHMENT_TEXT);
        textArea.setFont(ThemeConstants.FONT_BODY);
        textArea.setCaretColor(ThemeConstants.PARCHMENT_TEXT);
        textArea.setText("Welcome to the Thornwick Municipal Library. The Vanishing Librarian awaits...");

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setBackground(ThemeConstants.DARK_WOOD);
        scrollPane.getVerticalScrollBar().setUI(new ThornwickScrollBarUI());
        scrollPane.getHorizontalScrollBar().setUI(new ThornwickScrollBarUI());

        add(scrollPane, BorderLayout.CENTER);
    }

    public void append(String text) {
        textArea.append("\n" + text);
        textArea.setCaretPosition(textArea.getDocument().getLength());
    }

    public void setText(String text) {
        textArea.setText(text);
    }

    public String getText() {
        return textArea.getText();
    }

    // Package-private for DialoguePanelThemeTest
    JTextArea getTextArea() { return textArea; }

    // Component-scoped scrollbar style — avoids UIManager.put() global pollution
    private static final class ThornwickScrollBarUI extends BasicScrollBarUI {

        @Override
        protected void paintTrack(Graphics g, javax.swing.JComponent c, Rectangle trackBounds) {
            g.setColor(ThemeConstants.DARK_WOOD);
            g.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
        }

        @Override
        protected void paintThumb(Graphics g, javax.swing.JComponent c, Rectangle thumbBounds) {
            g.setColor(ThemeConstants.AGED_BRASS);
            g.fillRect(thumbBounds.x, thumbBounds.y, thumbBounds.width, thumbBounds.height);
        }
    }
}
