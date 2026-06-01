package com.abhishri.escape.ui;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Dimension;

public class DialoguePanel extends JPanel {

    private static final int PREFERRED_HEIGHT = 120;

    private final JTextArea textArea;

    public DialoguePanel() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(0, PREFERRED_HEIGHT));

        textArea = new JTextArea(4, 60);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setText("Welcome to the Thornwick Municipal Library. The Vanishing Librarian awaits...");

        add(new JScrollPane(textArea), BorderLayout.CENTER);
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
}
