package com.abhishri.escape.ui;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SequencePuzzleDialog extends PuzzleDialog {

    private final DefaultListModel<String> listModel;

    public SequencePuzzleDialog(Window owner, String puzzleId, List<String> availableItems,
                                String description) {
        super(owner, puzzleId, "Arrange the Sequence");

        listModel = new DefaultListModel<>();
        availableItems.forEach(listModel::addElement);
        JList<String> list = new JList<>(listModel);

        JButton moveUp = new JButton("▲ Up");
        JButton moveDown = new JButton("▼ Down");
        moveUp.addActionListener(e -> {
            int idx = list.getSelectedIndex();
            if (idx > 0) {
                String item = listModel.remove(idx);
                listModel.add(idx - 1, item);
                list.setSelectedIndex(idx - 1);
            }
        });
        moveDown.addActionListener(e -> {
            int idx = list.getSelectedIndex();
            if (idx >= 0 && idx < listModel.size() - 1) {
                String item = listModel.remove(idx);
                listModel.add(idx + 1, item);
                list.setSelectedIndex(idx + 1);
            }
        });

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonRow.add(moveUp);
        buttonRow.add(moveDown);

        JPanel inputPanel = new JPanel(new BorderLayout(8, 4));
        inputPanel.add(new JLabel(description), BorderLayout.NORTH);
        inputPanel.add(new JScrollPane(list), BorderLayout.CENTER);
        inputPanel.add(buttonRow, BorderLayout.SOUTH);
        initLayout(inputPanel);
    }

    @Override
    public Map<String, String> getInputs() {
        List<String> items = new ArrayList<>();
        for (int i = 0; i < listModel.size(); i++) {
            items.add(listModel.getElementAt(i));
        }
        return Map.of("sequence", String.join(",", items));
    }

    DefaultListModel<String> getListModel() { return listModel; }
}
