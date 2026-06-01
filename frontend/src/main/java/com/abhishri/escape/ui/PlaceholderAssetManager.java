package com.abhishri.escape.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;

public class PlaceholderAssetManager implements AssetManager {

    private static final int BG_WIDTH = 800;
    private static final int BG_HEIGHT = 500;
    private static final int ICON_SIZE = 32;

    @Override
    public Image getBackground(String roomId) {
        BufferedImage img = new BufferedImage(BG_WIDTH, BG_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(30, 30, 60));
        g.fillRect(0, 0, BG_WIDTH, BG_HEIGHT);
        g.setColor(new Color(100, 80, 50));
        g.drawRect(20, 20, BG_WIDTH - 40, BG_HEIGHT - 40);
        g.setColor(new Color(80, 60, 40));
        g.drawString("[" + roomId + "]", 30, 40);
        g.dispose();
        return img;
    }

    @Override
    public Image getItemIcon(String assetKey) {
        BufferedImage img = new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(180, 140, 60));
        g.fillOval(2, 2, ICON_SIZE - 4, ICON_SIZE - 4);
        g.dispose();
        return img;
    }
}
