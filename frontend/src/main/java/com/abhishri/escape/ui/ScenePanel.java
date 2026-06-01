package com.abhishri.escape.ui;

import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class ScenePanel extends JPanel {

    private static final int PREFERRED_WIDTH  = 800;
    private static final int PREFERRED_HEIGHT = 500;

    private final AssetManager assetManager;
    private List<Hotspot> hotspots = new ArrayList<>();
    private String currentRoomId = null;
    private String hoveredHotspotId = null;
    private float fadeAlpha = 1.0f;
    private Consumer<Hotspot> hotspotClickListener;

    // Package-private so ScenePanelFadeGuardTest can access it directly
    final Timer fadeTimer;

    public ScenePanel(AssetManager assetManager) {
        this.assetManager = assetManager;
        setPreferredSize(new Dimension(PREFERRED_WIDTH, PREFERRED_HEIGHT));
        setBackground(ThemeConstants.NIGHT_BLACK);

        fadeTimer = new Timer(16, e -> {
            fadeAlpha = Math.min(1.0f, fadeAlpha + 0.08f);
            if (fadeAlpha >= 1.0f) ((Timer) e.getSource()).stop();
            repaint();
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Hotspot h = findHotspotAt(e.getX(), e.getY());
                if (h != null && hotspotClickListener != null) {
                    hotspotClickListener.accept(h);
                }
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                updateHover(e.getX(), e.getY());
            }
        });
    }

    public void setHotspots(List<Hotspot> hotspots) {
        this.hotspots = new ArrayList<>(hotspots);
        repaint();
    }

    public void setCurrentRoomId(String newId) {
        if (Objects.equals(newId, this.currentRoomId)) return;
        String previousId = this.currentRoomId;
        this.currentRoomId = newId;
        if (previousId != null) {   // skip fade on first load (null → first room)
            if (fadeTimer.isRunning()) fadeTimer.stop();
            fadeAlpha = 0.0f;
            fadeTimer.start();
        }
        repaint();
    }

    public void setHotspotClickListener(Consumer<Hotspot> listener) {
        this.hotspotClickListener = listener;
    }

    // --- Package-private accessors for tests ---

    List<Hotspot> getHotspots() { return hotspots; }

    String getHoveredHotspotId() { return hoveredHotspotId; }

    boolean isFadeRunning() { return fadeTimer.isRunning(); }

    // --- Package-private methods ---

    void updateHover(int x, int y) {
        Hotspot found = findHotspotAt(x, y);
        String newId = (found != null) ? found.getId() : null;
        if (!Objects.equals(newId, hoveredHotspotId)) {
            hoveredHotspotId = newId;
            setCursor(Cursor.getPredefinedCursor(
                    newId != null ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            repaint();
        }
    }

    Hotspot findHotspotAt(int x, int y) {
        for (Hotspot h : hotspots) {
            if (h.getBounds().contains(x, y)) {
                return h;
            }
        }
        return null;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            // Layer 1 — background image (faded in on room change)
            if (currentRoomId != null) {
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fadeAlpha));
                g2.drawImage(assetManager.getBackground(currentRoomId),
                        0, 0, getWidth(), getHeight(), this);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
            }

            // Layer 2 — hotspot overlays (always full opacity)
            for (Hotspot hotspot : hotspots) {
                paintHotspot(g2, hotspot);
            }
        } finally {
            g2.dispose();
        }
    }

    private void paintHotspot(Graphics2D g2, Hotspot h) {
        java.awt.Rectangle b = h.getBounds();
        RoundRectangle2D.Float rr = new RoundRectangle2D.Float(b.x, b.y, b.width, b.height, 8, 8);

        // Fill color: type + solved state
        Color fill;
        if ("EXIT".equals(h.getType())) {
            fill = new Color(ThemeConstants.EXIT_OVERLAY.getRed(),
                    ThemeConstants.EXIT_OVERLAY.getGreen(),
                    ThemeConstants.EXIT_OVERLAY.getBlue(), 102);   // 40% alpha
        } else if (h.isSolved()) {
            fill = new Color(ThemeConstants.SOLVED_OVERLAY.getRed(),
                    ThemeConstants.SOLVED_OVERLAY.getGreen(),
                    ThemeConstants.SOLVED_OVERLAY.getBlue(), 128); // 50% alpha
        } else {
            fill = new Color(ThemeConstants.AGED_BRASS.getRed(),
                    ThemeConstants.AGED_BRASS.getGreen(),
                    ThemeConstants.AGED_BRASS.getBlue(), 80);       // ~31% alpha
        }

        g2.setColor(fill);
        g2.fill(rr);

        // Stroke: hovered = BRASS_GLOW 2.5px, default = AGED_BRASS 1.5px
        boolean hovered = h.getId().equals(hoveredHotspotId);
        g2.setColor(hovered ? ThemeConstants.BRASS_GLOW : ThemeConstants.AGED_BRASS);
        g2.setStroke(new BasicStroke(hovered ? 2.5f : 1.5f));
        g2.draw(rr);
        g2.setStroke(new BasicStroke(1f));

        // Label: centered, FONT_LABEL, CANDLE_TEXT; append ✓ if solved
        String label = h.isSolved() ? h.getLabel() + " ✓" : h.getLabel();
        g2.setFont(ThemeConstants.FONT_LABEL);
        g2.setColor(ThemeConstants.CANDLE_TEXT);
        FontMetrics fm = g2.getFontMetrics();
        int tx = b.x + (b.width  - fm.stringWidth(label)) / 2;
        int ty = b.y + (b.height - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(label, tx, ty);
    }
}
