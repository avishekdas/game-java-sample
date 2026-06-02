package com.abhishri.escape.ui;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import java.awt.FlowLayout;
import java.awt.Window;
import java.util.Map;

public class CombinationPuzzleDialog extends PuzzleDialog {

    private final JSpinner[] spinners;

    public CombinationPuzzleDialog(Window owner, String puzzleId, int digitCount, String description) {
        super(owner, puzzleId, "Enter Combination");

        spinners = new JSpinner[digitCount];
        JPanel inputPanel = new JPanel(new FlowLayout());
        inputPanel.add(new JLabel(description + ":"));

        for (int i = 0; i < digitCount; i++) {
            spinners[i] = new JSpinner(new SpinnerNumberModel(0, 0, 9, 1));
            ((JSpinner.DefaultEditor) spinners[i].getEditor()).getTextField().setColumns(2);

            // Style parchment inputs BEFORE initLayout — applyThemeRecursively skips JSpinner
            spinners[i].setBackground(ThemeConstants.PARCHMENT);
            spinners[i].setForeground(ThemeConstants.PARCHMENT_TEXT);
            ((JSpinner.DefaultEditor) spinners[i].getEditor()).getTextField()
                    .setBackground(ThemeConstants.PARCHMENT);
            ((JSpinner.DefaultEditor) spinners[i].getEditor()).getTextField()
                    .setForeground(ThemeConstants.PARCHMENT_TEXT);

            inputPanel.add(spinners[i]);
        }
        initLayout(inputPanel);
    }

    @Override
    public Map<String, String> getInputs() {
        StringBuilder code = new StringBuilder();
        for (JSpinner spinner : spinners) {
            code.append(spinner.getValue());
        }
        return Map.of("code", code.toString());
    }

    JSpinner[] getSpinners() { return spinners; }
}
