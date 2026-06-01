package com.abhishri.escape.ui;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import java.awt.BorderLayout;
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
        setLayout(new BorderLayout(8, 8));
        add(inputPanel, BorderLayout.CENTER);

        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");
        okButton.addActionListener(e -> { confirmed = true; dispose(); });
        cancelButton.addActionListener(e -> { confirmed = false; dispose(); });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(okButton);
        buttons.add(cancelButton);
        add(buttons, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(getOwner());
    }

    public abstract Map<String, String> getInputs();

    public boolean isConfirmed() { return confirmed; }
}
