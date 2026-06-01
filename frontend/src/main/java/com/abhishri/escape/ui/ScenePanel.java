package com.abhishri.escape.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;

public class ScenePanel extends JPanel {

    private static final int PREFERRED_WIDTH = 800;
    private static final int PREFERRED_HEIGHT = 500;

    private final AssetManager assetManager;
    private List<Hotspot> hotspots = new ArrayList<>();
    private String currentRoomId = "foyer";

    public ScenePanel(AssetManager assetManager) {
        this.assetManager = assetManager;
        setPreferredSize(new Dimension(PREFERRED_WIDTH, PREFERRED_HEIGHT));
    }

    public void setHotspots(List<Hotspot> hotspots) {
        this.hotspots = new ArrayList<>(hotspots);
        repaint();
    }

    public void setCurrentRoomId(String roomId) {
        this.currentRoomId = roomId;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        g2.drawImage(assetManager.getBackground(currentRoomId), 0, 0, getWidth(), getHeight(), this);

        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        for (Hotspot hotspot : hotspots) {
            g2.setColor(new Color(255, 255, 0, 150));
            g2.draw(hotspot.getBounds());
            g2.setColor(Color.YELLOW);
            g2.drawString(hotspot.getLabel(),
                    hotspot.getBounds().x + 4,
                    hotspot.getBounds().y + 14);
        }
    }
}
