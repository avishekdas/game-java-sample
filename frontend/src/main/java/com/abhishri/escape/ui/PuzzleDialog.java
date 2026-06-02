package com.abhishri.escape.ui;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Window;
import java.util.Map;

public abstract class PuzzleDialog extends JDialog {

    private boolean confirmed = false;
    protected final String puzzleId;

    protected PuzzleDialog(Window owner, String puzzleId, String title) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        this.puzzleId = puzzleId;
    }

    protected final void initLayout(JPanel inputPanel) {
        // Style the dialog background and input panel
        getContentPane().setBackground(ThemeConstants.DARK_WOOD);
        inputPanel.setBackground(ThemeConstants.DARK_WOOD);
        applyThemeRecursively(inputPanel);

        setLayout(new BorderLayout(8, 8));
        add(inputPanel, BorderLayout.CENTER);

        // Style buttons inline — they are local variables, not fields
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");
        ThemeConstants.applyDarkButton(okButton);
        ThemeConstants.applyDarkButton(cancelButton);
        okButton.addActionListener(e -> { confirmed = true; dispose(); });
        cancelButton.addActionListener(e -> { confirmed = false; dispose(); });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setBackground(ThemeConstants.DARK_WOOD);
        buttons.add(okButton);
        buttons.add(cancelButton);
        add(buttons, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(getOwner());
    }

    // Applies dark theme to containers/labels; skips input widgets styled by subclasses
    private void applyThemeRecursively(Component c) {
        if (c instanceof JTextField
                || c instanceof JFormattedTextField
                || c instanceof JSpinner
                || c instanceof JButton
                || c instanceof JList) {
            return;
        }
        c.setBackground(ThemeConstants.DARK_WOOD);
        c.setForeground(ThemeConstants.CANDLE_TEXT);
        if (c instanceof Container container) {
            for (Component child : container.getComponents()) {
                applyThemeRecursively(child);
            }
        }
    }

    public abstract Map<String, String> getInputs();

    public boolean isConfirmed() { return confirmed; }
}
