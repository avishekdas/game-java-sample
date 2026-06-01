package com.abhishri.escape.ui;

import com.abhishri.escape.ui.dto.InventoryItemDTO;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InventoryPanel extends JPanel {

    private final AssetManager assetManager;
    private final DefaultListModel<InventoryItemDTO> model = new DefaultListModel<>();
    private final JList<InventoryItemDTO> itemList = new JList<>(model);
    private final Map<String, Icon> iconCache = new HashMap<>();
    private final TitledBorder titledBorder;

    private boolean updatingItems = false;
    private String panelTitle = "INVENTORY";

    public InventoryPanel(AssetManager assetManager) {
        this.assetManager = assetManager;
        setLayout(new BorderLayout());
        setBackground(ThemeConstants.DARK_WOOD);
        setPreferredSize(new Dimension(200, 0));

        titledBorder = new TitledBorder(
                BorderFactory.createLineBorder(ThemeConstants.AGED_BRASS, 1),
                "INVENTORY",
                TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION,
                ThemeConstants.FONT_LABEL,
                ThemeConstants.CANDLE_TEXT);
        setBorder(titledBorder);

        itemList.setBackground(ThemeConstants.DARK_WOOD);
        itemList.setForeground(ThemeConstants.CANDLE_TEXT);
        itemList.setFont(ThemeConstants.FONT_INVENTORY);
        itemList.setCellRenderer(new ItemCellRenderer());

        itemList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting() || updatingItems) return;
            InventoryItemDTO selected = itemList.getSelectedValue();
            if (selected != null) {
                updatePanelTitle("USING: " + selected.getName());
            } else {
                updatePanelTitle("INVENTORY");
            }
        });

        JScrollPane scrollPane = new JScrollPane(itemList);
        scrollPane.setBackground(ThemeConstants.DARK_WOOD);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void setItems(List<InventoryItemDTO> items) {
        updatingItems = true;
        try {
            model.clear();
            items.forEach(model::addElement);
        } finally {
            updatingItems = false;
        }
    }

    public String getSelectedItemId() {
        InventoryItemDTO selected = itemList.getSelectedValue();
        return selected != null ? selected.getId() : null;
    }

    public void clearSelection() {
        itemList.clearSelection();
        updatePanelTitle("INVENTORY");
    }

    public JList<InventoryItemDTO> getItemList() {
        return itemList;
    }

    // Package-private for InventoryUseModeTest
    String getPanelTitle() { return panelTitle; }

    private void updatePanelTitle(String title) {
        panelTitle = title;
        titledBorder.setTitle(title);
        repaint();
    }

    private Icon loadIcon(String assetKey) {
        return iconCache.computeIfAbsent(assetKey, k -> {
            Image raw = assetManager.getItemIcon(k);
            return new ImageIcon(raw.getScaledInstance(40, 40, Image.SCALE_SMOOTH));
        });
    }

    private final class ItemCellRenderer extends JPanel implements ListCellRenderer<InventoryItemDTO> {

        private final JLabel iconLabel = new JLabel();
        private final JLabel nameLabel = new JLabel();

        ItemCellRenderer() {
            setLayout(new BorderLayout(6, 0));
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
            iconLabel.setPreferredSize(new Dimension(40, 40));
            nameLabel.setFont(ThemeConstants.FONT_INVENTORY);
            nameLabel.setForeground(ThemeConstants.CANDLE_TEXT);
            add(iconLabel, BorderLayout.WEST);
            add(nameLabel, BorderLayout.CENTER);
            setPreferredSize(new Dimension(0, 52));
        }

        @Override
        public Component getListCellRendererComponent(
                JList<? extends InventoryItemDTO> list,
                InventoryItemDTO value,
                int index,
                boolean isSelected,
                boolean cellHasFocus) {

            String key = (value != null && value.getAssetKey() != null)
                    ? value.getAssetKey() : "";
            iconLabel.setIcon(key.isEmpty() ? null : loadIcon(key));
            nameLabel.setText(value != null ? value.getName() : "");

            if (isSelected) {
                setBackground(ThemeConstants.DARK_WOOD);
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(ThemeConstants.BRASS_GLOW, 2),
                        BorderFactory.createEmptyBorder(2, 4, 2, 4)));
            } else {
                setBackground(ThemeConstants.DARK_WOOD);
                setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
            }
            return this;
        }
    }
}
