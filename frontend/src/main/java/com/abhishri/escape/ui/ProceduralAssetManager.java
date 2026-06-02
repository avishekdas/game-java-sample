package com.abhishri.escape.ui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

public class ProceduralAssetManager implements AssetManager {

    @Override
    public Image getBackground(String roomId) {
        BufferedImage img = new BufferedImage(800, 500, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        paintBaseAndGradients(g2);
        paintSilhouettes(g2, roomId);
        paintVignette(g2);
        paintRoomLabel(g2, roomId);

        g2.dispose();
        return img;
    }

    @Override
    public Image getItemIcon(String assetKey) {
        BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        paintItemBase(g2);
        paintItemSymbol(g2, assetKey);

        g2.dispose();
        return img;
    }

    // -------------------------------------------------------------------------
    // Background layers
    // -------------------------------------------------------------------------

    private void paintBaseAndGradients(Graphics2D g2) {
        g2.setColor(ThemeConstants.NIGHT_BLACK);
        g2.fillRect(0, 0, 800, 500);

        // Primary: warm floor lamp glow
        float[] fractions1 = {0.0f, 0.5f, 1.0f};
        Color[] colors1 = {new Color(0x5A, 0x3A, 0x10), new Color(0x2A, 0x1A, 0x08), ThemeConstants.NIGHT_BLACK};
        g2.setPaint(new RadialGradientPaint(400, 380, 280, fractions1, colors1));
        g2.fillRect(0, 0, 800, 500);

        // Secondary: ceiling scatter
        float[] fractions2 = {0.0f, 1.0f};
        Color[] colors2 = {new Color(0x3A, 0x28, 0x08), ThemeConstants.NIGHT_BLACK};
        g2.setPaint(new RadialGradientPaint(160, 160, 160, fractions2, colors2));
        g2.fillRect(0, 0, 800, 500);
    }

    private void paintSilhouettes(Graphics2D g2, String roomId) {
        switch (roomId) {
            case "room_foyer" -> paintFoyerSilhouettes(g2);
            case "room_reading_hall" -> paintReadingHallSilhouettes(g2);
            case "room_archives" -> paintArchivesSilhouettes(g2);
            // unknown rooms: no silhouettes (graceful fallback)
        }
    }

    private void paintFoyerSilhouettes(Graphics2D g2) {
        Color silhouette = new Color(0x1A, 0x0F, 0x04);
        Color doorInset  = new Color(0x22, 0x14, 0x06);
        g2.setColor(silhouette);

        // Left pillar
        g2.fillRect(60, 80, 80, 420);
        // Right pillar
        g2.fillRect(660, 80, 80, 420);

        // Arched doorframe top (arc connecting pillars)
        g2.fill(new Arc2D.Float(140, 60, 520, 200, 0, 180, Arc2D.PIE));

        // Door panel inset (slightly lighter)
        g2.setColor(doorInset);
        g2.fillRect(180, 150, 440, 350);
        g2.setColor(silhouette);

        // Wall clock silhouette: oval body + two rect hands
        g2.fillOval(210, 80, 48, 48);
        g2.fillRect(233, 88, 3, 16);  // minute hand
        g2.fillRect(228, 96, 14, 3);  // hour hand

        // Reception desk rect
        g2.fillRect(80, 340, 200, 80);

        // Brass nail heads near arched doorframe
        g2.setColor(ThemeConstants.AGED_BRASS);
        int[] nailX = {192, 207, 222};
        int[] nailY = {145, 140, 148};
        for (int i = 0; i < 3; i++) {
            g2.fillOval(nailX[i] - 2, nailY[i] - 2, 5, 5);
        }
    }

    private void paintReadingHallSilhouettes(Graphics2D g2) {
        Color silhouette = new Color(0x1A, 0x0F, 0x04);
        g2.setColor(silhouette);

        // Eight bookshelf columns evenly spaced
        int shelfW = 72;
        int gap = (800 - 8 * shelfW) / 9;
        for (int i = 0; i < 8; i++) {
            int sx = gap + i * (shelfW + gap);
            g2.fillRect(sx, 50, shelfW, 360);
        }

        // Fireplace opening (arch rect)
        g2.fillRect(300, 280, 180, 200);
        g2.fill(new Arc2D.Float(300, 240, 180, 90, 0, 180, Arc2D.PIE));

        // Reading lamp pole + cone of light
        g2.fillRect(215, 150, 8, 180);
        // cone: translucent amber gradient
        g2.setPaint(new GradientPaint(219, 150, new Color(0xC8, 0x90, 0x20, 80), 219, 330, new Color(0xC8, 0x90, 0x20, 0)));
        int[] coneX = {219, 160, 280};
        int[] coneY = {150, 330, 330};
        g2.fillPolygon(coneX, coneY, 3);
    }

    private void paintArchivesSilhouettes(Graphics2D g2) {
        Color silhouette = new Color(0x1A, 0x0F, 0x04);
        g2.setColor(silhouette);

        // Four filing cabinets
        int[] cabinetX = {30, 140, 250, 360};
        for (int x : cabinetX) {
            g2.fillRect(x, 120, 90, 260);
            // Drawer face lines
            g2.setColor(new Color(0x28, 0x18, 0x08));
            for (int y = 155; y < 380; y += 65) {
                g2.fillRect(x + 3, y, 84, 25);
                // Handle notch
                g2.setColor(new Color(0x8B, 0x69, 0x14, 60));
                g2.fillRect(x + 36, y + 9, 18, 7);
                g2.setColor(new Color(0x28, 0x18, 0x08));
            }
            g2.setColor(silhouette);
        }

        // Cipher wheel on right wall (stroke only, in AGED_BRASS to stand out)
        g2.setColor(ThemeConstants.AGED_BRASS);
        g2.setStroke(new BasicStroke(3f));
        g2.draw(new Ellipse2D.Float(660, 180, 120, 120));
        g2.draw(new Ellipse2D.Float(690, 210, 60, 60));
        g2.setStroke(new BasicStroke(1f));
        g2.setColor(silhouette);

        // Iron chest
        g2.fillRect(470, 320, 150, 100);
        g2.fill(new Arc2D.Float(470, 290, 150, 70, 0, 180, Arc2D.PIE));
    }

    private void paintVignette(Graphics2D g2) {
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));

        // Top vignette
        g2.setPaint(new GradientPaint(0, 0, new Color(0x0D, 0x0B, 0x07, 200), 0, 120, new Color(0x0D, 0x0B, 0x07, 0)));
        g2.fillRect(0, 0, 800, 120);

        // Bottom vignette
        g2.setPaint(new GradientPaint(0, 420, new Color(0x0D, 0x0B, 0x07, 0), 0, 500, new Color(0x0D, 0x0B, 0x07, 160)));
        g2.fillRect(0, 420, 800, 80);
    }

    private void paintRoomLabel(Graphics2D g2, String roomId) {
        String label = toDisplayName(roomId);
        g2.setFont(ThemeConstants.FONT_SMALL);
        g2.setColor(ThemeConstants.DIM_TEXT);
        g2.drawString(label, 12, 488);
    }

    // -------------------------------------------------------------------------
    // Item icon layers
    // -------------------------------------------------------------------------

    private void paintItemBase(Graphics2D g2) {
        RoundRectangle2D.Float chip = new RoundRectangle2D.Float(3, 3, 58, 58, 8, 8);
        g2.setColor(ThemeConstants.DARK_WOOD);
        g2.fill(chip);
        g2.setColor(ThemeConstants.AGED_BRASS);
        g2.setStroke(new BasicStroke(1f));
        g2.draw(chip);
    }

    private void paintItemSymbol(Graphics2D g2, String assetKey) {
        switch (assetKey) {
            case "item_key"        -> paintKey(g2);
            case "item_lens"       -> paintLens(g2);
            case "item_scrap"      -> paintScrap(g2);
            case "item_manuscript" -> paintManuscript(g2);
            case "item_token"      -> paintToken(g2);
            // unknown: no symbol drawn — base chip only
        }
    }

    private void paintKey(Graphics2D g2) {
        g2.setColor(ThemeConstants.AGED_BRASS);
        // Oval head
        g2.fillOval(8, 20, 20, 20);
        // Hole in head
        g2.setColor(ThemeConstants.DARK_WOOD);
        g2.fillOval(13, 25, 10, 10);
        g2.setColor(ThemeConstants.AGED_BRASS);
        // Shaft
        g2.fillRect(26, 28, 26, 8);
        // Teeth
        g2.fillRect(44, 36, 6, 6);
        g2.fillRect(38, 36, 5, 5);
    }

    private void paintLens(Graphics2D g2) {
        // Interior fill first (so handle doesn't disappear behind it)
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f));
        g2.setColor(ThemeConstants.DARK_WOOD);
        g2.fillOval(9, 9, 30, 30);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));

        // Circle stroke
        g2.setColor(ThemeConstants.AGED_BRASS);
        g2.setStroke(new BasicStroke(3f));
        g2.drawOval(8, 8, 32, 32);
        g2.setStroke(new BasicStroke(1f));

        // Diagonal handle
        g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(38, 38, 56, 56);
        g2.setStroke(new BasicStroke(1f));
    }

    private void paintScrap(Graphics2D g2) {
        // Parchment rect body
        g2.setColor(ThemeConstants.PARCHMENT);
        g2.fillRect(12, 10, 38, 44);
        // Torn top edge overlay
        g2.setColor(ThemeConstants.DARK_WOOD);
        int[] tornX = {12, 16, 20, 25, 30, 35, 40, 45, 50};
        int[] tornY = {10, 14, 11, 15, 12, 16, 13, 11, 10};
        g2.fillPolygon(tornX, tornY, 9);
        // Ruled lines
        g2.setColor(ThemeConstants.PARCHMENT_TEXT);
        g2.setStroke(new BasicStroke(1f));
        g2.drawLine(16, 26, 46, 26);
        g2.drawLine(16, 33, 46, 33);
        g2.drawLine(16, 40, 46, 40);
        g2.setStroke(new BasicStroke(1f));
    }

    private void paintManuscript(Graphics2D g2) {
        // Parchment rect body
        g2.setColor(ThemeConstants.PARCHMENT);
        g2.fillRect(10, 14, 42, 40);
        // Torn top edge
        g2.setColor(ThemeConstants.DARK_WOOD);
        int[] tornX = {10, 14, 19, 24, 29, 34, 39, 44, 52};
        int[] tornY = {14, 18, 15, 19, 14, 18, 15, 20, 14};
        g2.fillPolygon(tornX, tornY, 9);
        // Ruled lines
        g2.setColor(ThemeConstants.PARCHMENT_TEXT);
        g2.setStroke(new BasicStroke(1f));
        g2.drawLine(14, 27, 48, 27);
        g2.drawLine(14, 33, 48, 33);
        g2.drawLine(14, 39, 48, 39);
        g2.drawLine(14, 45, 48, 45);
        // Wax seal circle
        g2.setColor(new Color(0x8B, 0x1A, 0x1A));
        g2.fillOval(34, 42, 14, 14);
        g2.setStroke(new BasicStroke(1f));
    }

    private void paintToken(Graphics2D g2) {
        // Outer brass circle
        g2.setColor(ThemeConstants.AGED_BRASS);
        g2.fillOval(10, 10, 44, 44);
        // Inner dark circle
        g2.setColor(ThemeConstants.DARK_WOOD);
        g2.fillOval(18, 18, 28, 28);
        // Concentric ring in BRASS_GLOW
        g2.setColor(ThemeConstants.BRASS_GLOW);
        g2.setStroke(new BasicStroke(2f));
        g2.drawOval(22, 22, 20, 20);
        g2.setStroke(new BasicStroke(1f));
    }

    // -------------------------------------------------------------------------
    // Hint cards
    // -------------------------------------------------------------------------

    @Override
    public Image getHintCard(String objectId) {
        return switch (objectId) {
            case "wall_clock"      -> paintWallClockCard();
            case "reception_desk"  -> paintReceptionDeskCard();
            case "reading_lamp"    -> paintReadingLampCard();
            case "filing_cabinets" -> paintFilingCabinetsCard();
            default -> new BufferedImage(320, 200, BufferedImage.TYPE_INT_ARGB);
        };
    }

    private BufferedImage paintWallClockCard() {
        BufferedImage img = new BufferedImage(320, 200, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(ThemeConstants.DARK_WOOD);
        g2.fillRect(0, 0, 320, 200);
        g2.setColor(ThemeConstants.AGED_BRASS);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRect(1, 1, 318, 198);

        // Clock face circle (center 160,95 radius 70)
        g2.setStroke(new BasicStroke(2f));
        g2.drawOval(90, 25, 140, 140);

        // 12 tick marks (clockwise from 12 o'clock)
        g2.setColor(ThemeConstants.CANDLE_TEXT);
        g2.setStroke(new BasicStroke(1.5f));
        for (int i = 0; i < 12; i++) {
            double a = i * 30.0 * Math.PI / 180.0;
            double dx = Math.sin(a);
            double dy = -Math.cos(a);
            g2.drawLine((int)(160 + 70 * dx), (int)(95 + 70 * dy),
                        (int)(160 + 62 * dx), (int)(95 + 62 * dy));
        }

        // Clock hands at 11:47
        // Hour hand: 11h + 47/60 * 30° = 353.5° (almost at 12, slightly left), shorter+thicker
        g2.setColor(ThemeConstants.CANDLE_TEXT);
        g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        double aHour = 353.5 * Math.PI / 180.0;
        g2.drawLine(160, 95, (int)(160 + 40 * Math.sin(aHour)), (int)(95 - 40 * Math.cos(aHour)));
        // Minute hand: 47 * 6° = 282° (between 9 and 10, pointing left), longer+thinner
        g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        double aMin = 282.0 * Math.PI / 180.0;
        g2.drawLine(160, 95, (int)(160 + 55 * Math.sin(aMin)), (int)(95 - 55 * Math.cos(aMin)));
        g2.setStroke(new BasicStroke(1f));

        // Format label
        g2.setFont(ThemeConstants.FONT_SMALL);
        g2.setColor(ThemeConstants.BRASS_GLOW);
        FontMetrics fm = g2.getFontMetrics();
        String label = "H H : M M";
        g2.drawString(label, (320 - fm.stringWidth(label)) / 2, 180);

        g2.dispose();
        return img;
    }

    private BufferedImage paintReceptionDeskCard() {
        BufferedImage img = new BufferedImage(320, 200, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(ThemeConstants.DARK_WOOD);
        g2.fillRect(0, 0, 320, 200);
        g2.setColor(ThemeConstants.AGED_BRASS);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRect(1, 1, 318, 198);

        // Doorframe pillars and arch
        Color frameColor = new Color(0x1A, 0x0F, 0x04);
        g2.setColor(frameColor);
        g2.fillRect(96, 30, 10, 150);
        g2.fillRect(214, 30, 10, 150);
        g2.setStroke(new BasicStroke(10f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
        g2.draw(new Arc2D.Float(85, -10, 150, 150, 37, 106, Arc2D.OPEN));
        g2.setStroke(new BasicStroke(1f));

        // 3 brass nail circles
        g2.setColor(ThemeConstants.AGED_BRASS);
        g2.fillOval(126, 66, 8, 8);
        g2.fillOval(154, 56, 8, 8);
        g2.fillOval(184, 64, 8, 8);

        g2.dispose();
        return img;
    }

    private BufferedImage paintReadingLampCard() {
        BufferedImage img = new BufferedImage(320, 200, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(ThemeConstants.DARK_WOOD);
        g2.fillRect(0, 0, 320, 200);
        g2.setColor(ThemeConstants.AGED_BRASS);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRect(1, 1, 318, 198);

        // 8 book spines, bottom-aligned, varying heights
        Color spineColor = new Color(0x1A, 0x0F, 0x04);
        int[] heights = {78, 82, 76, 80, 77, 83, 79, 81};
        int spineW = 22, spineGap = 3, startX = 28, baseY = 145;
        for (int i = 0; i < 8; i++) {
            int x = startX + i * (spineW + spineGap);
            int h = heights[i];
            int y = baseY - h;
            g2.setColor(spineColor);
            g2.fillRect(x, y, spineW, h);
            g2.setColor(ThemeConstants.DIM_TEXT);
            g2.drawLine(x, y, x + spineW - 1, y);
        }

        g2.dispose();
        return img;
    }

    private BufferedImage paintFilingCabinetsCard() {
        BufferedImage img = new BufferedImage(320, 200, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(ThemeConstants.DARK_WOOD);
        g2.fillRect(0, 0, 320, 200);
        g2.setColor(ThemeConstants.AGED_BRASS);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRect(1, 1, 318, 198);

        // 4 cabinet faces with drawer dividers and handles
        Color cabinetColor = new Color(0x1A, 0x0F, 0x04);
        Color drawerColor  = new Color(0x28, 0x18, 0x08);
        int cabW = 56, cabH = 100, cabGap = 6, startX = 18, startY = 50;
        for (int i = 0; i < 4; i++) {
            int x = startX + i * (cabW + cabGap);
            g2.setColor(cabinetColor);
            g2.fillRect(x, startY, cabW, cabH);
            // 3 dividers → 4 drawer sections
            g2.setColor(drawerColor);
            for (int d = 1; d <= 3; d++) {
                g2.fillRect(x, startY + d * 25, cabW, 2);
            }
            // Drawer handles
            g2.setColor(ThemeConstants.AGED_BRASS);
            for (int d = 0; d < 4; d++) {
                int hx = x + (cabW - 18) / 2;
                int hy = startY + d * 25 + 10;
                g2.fillRect(hx, hy, 18, 4);
            }
        }

        g2.dispose();
        return img;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String toDisplayName(String roomId) {
        String stripped = roomId.startsWith("room_") ? roomId.substring(5) : roomId;
        String[] parts = stripped.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (sb.length() > 0) sb.append(" ");
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)));
                sb.append(part.substring(1));
            }
        }
        return sb.toString();
    }
}
