package com.abhishri.escape.ui;

import com.abhishri.escape.ui.dto.InventoryItemDTO;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;

public class InventoryPanel extends JPanel {

    private final DefaultListModel<InventoryItemDTO> model = new DefaultListModel<>();
    private final JList<InventoryItemDTO> itemList = new JList<>(model);

    public InventoryPanel() {
        setLayout(new BorderLayout());
        setBorder(new TitledBorder("Inventory"));
        setPreferredSize(new Dimension(200, 0));
        add(new JScrollPane(itemList), BorderLayout.CENTER);
    }

    public void setItems(List<InventoryItemDTO> items) {
        model.clear();
        items.forEach(model::addElement);
    }

    public JList<InventoryItemDTO> getItemList() {
        return itemList;
    }
}
