package com.abhishri.escape.ui;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Window;
import java.util.Map;

public class RiddlePuzzleDialog extends PuzzleDialog {

    private final JTextField answerField;

    public RiddlePuzzleDialog(Window owner, String puzzleId, String questionText) {
        super(owner, puzzleId, "Solve the Riddle");

        answerField = new JTextField(25);
        // Style parchment inputs BEFORE initLayout — applyThemeRecursively skips JTextField
        answerField.setBackground(ThemeConstants.PARCHMENT);
        answerField.setForeground(ThemeConstants.PARCHMENT_TEXT);
        answerField.setCaretColor(ThemeConstants.PARCHMENT_TEXT);
        answerField.setBorder(BorderFactory.createLineBorder(ThemeConstants.AGED_BRASS, 1));

        JPanel inputPanel = new JPanel(new BorderLayout(8, 4));
        inputPanel.add(
                new JLabel("<html><body style='width:280px'>" + questionText + "</body></html>"),
                BorderLayout.NORTH);
        inputPanel.add(answerField, BorderLayout.CENTER);
        initLayout(inputPanel);
    }

    @Override
    public Map<String, String> getInputs() {
        return Map.of("answer", answerField.getText());
    }

    JTextField getAnswerField() { return answerField; }
}
