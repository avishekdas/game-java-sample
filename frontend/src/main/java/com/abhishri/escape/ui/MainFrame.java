package com.abhishri.escape.ui;

import com.abhishri.escape.ui.dto.GameStateDTO;
import com.abhishri.escape.ui.dto.GameStatus;
import com.abhishri.escape.ui.dto.RoomDTO;
import com.abhishri.escape.ui.dto.RoomObjectDTO;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainFrame extends JFrame {

    private final StatusBar statusBar;
    private final ScenePanel scenePanel;
    private final InventoryPanel inventoryPanel;
    private final DialoguePanel dialoguePanel;

    private final GameApiClient client;
    private UUID gameId;

    public MainFrame(AssetManager assetManager) {
        this(assetManager, null);
    }

    public MainFrame(AssetManager assetManager, GameApiClient client) {
        super("Mystery Escape Room — The Vanishing Librarian");
        this.client = client;

        statusBar = new StatusBar();
        scenePanel = new ScenePanel(assetManager);
        inventoryPanel = new InventoryPanel();
        dialoguePanel = new DialoguePanel();

        setLayout(new BorderLayout());
        getContentPane().add(statusBar, BorderLayout.NORTH);
        getContentPane().add(scenePanel, BorderLayout.CENTER);
        getContentPane().add(inventoryPanel, BorderLayout.EAST);
        getContentPane().add(dialoguePanel, BorderLayout.SOUTH);

        if (client != null) {
            statusBar.getNewButton().addActionListener(e -> handleNewGame());
            statusBar.getSaveButton().addActionListener(e -> handleSave());
            statusBar.getLoadButton().addActionListener(e -> handleLoad());
            scenePanel.setHotspotClickListener(this::handleHotspotClick);
        }

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
                System.exit(0);
            }
        });

        pack();
        setLocationRelativeTo(null);
    }

    public void applyState(GameStateDTO state) {
        if (state.getCurrentRoom() != null) {
            statusBar.setRoomName(state.getCurrentRoom().getName());
            scenePanel.setCurrentRoomId(state.getCurrentRoom().getId());
            scenePanel.setHotspots(buildHotspots(state.getCurrentRoom()));
        }
        if (state.getInventory() != null) {
            inventoryPanel.setItems(state.getInventory());
        }
        if (state.getDialogueMessage() != null && !state.getDialogueMessage().isBlank()) {
            dialoguePanel.append(state.getDialogueMessage());
        }
        if (state.getGameStatus() == GameStatus.COMPLETE) {
            dialoguePanel.append("*** YOU SOLVED THE MYSTERY! The Vanishing Librarian case is closed. ***");
        }
    }

    public StatusBar getStatusBar() { return statusBar; }
    public ScenePanel getScenePanel() { return scenePanel; }
    public InventoryPanel getInventoryPanel() { return inventoryPanel; }
    public DialoguePanel getDialoguePanel() { return dialoguePanel; }

    private void handleNewGame() {
        GameStateDTO state = client.newGame();
        gameId = state.getGameId();
        dialoguePanel.setText("");
        applyState(state);
    }

    private void handleSave() {
        if (gameId == null) {
            dialoguePanel.append("No game in progress.");
            return;
        }
        String filename = client.saveGame(gameId);
        dialoguePanel.append("Saved as: " + filename);
    }

    private void handleLoad() {
        if (gameId == null) {
            dialoguePanel.append("No game in progress.");
            return;
        }
        String filename = JOptionPane.showInputDialog(this,
                "Enter save filename:", "Load Game", JOptionPane.PLAIN_MESSAGE);
        if (filename == null || filename.isBlank()) return;
        GameStateDTO state = client.loadGame(gameId, filename.trim());
        applyState(state);
    }

    private void handleHotspotClick(Hotspot hotspot) {
        if (gameId == null) {
            dialoguePanel.append("Start a new game first.");
            return;
        }
        GameStateDTO state;
        if ("EXIT".equals(hotspot.getType())) {
            state = client.move(gameId, hotspot.getObjectId());
        } else {
            state = client.examine(gameId, hotspot.getObjectId());
        }
        applyState(state);
    }

    private List<Hotspot> buildHotspots(RoomDTO room) {
        if (room.getObjects() == null || room.getObjects().isEmpty()) {
            return new ArrayList<>();
        }
        List<Hotspot> hotspots = new ArrayList<>();
        int panelW = scenePanel.getWidth();
        int panelH = scenePanel.getHeight();
        if (panelW == 0) panelW = 800;
        if (panelH == 0) panelH = 500;

        int n = room.getObjects().size();
        int spacing = panelW / (n + 1);
        int baseY = panelH * 2 / 3;

        for (int i = 0; i < n; i++) {
            RoomObjectDTO obj = room.getObjects().get(i);
            int x = spacing * (i + 1) - 50;
            Rectangle bounds = new Rectangle(x, baseY - 30, 100, 60);
            String type = obj.getObjectType() != null ? obj.getObjectType().name() : "SCENERY";
            hotspots.add(new Hotspot(obj.getId(), type, obj.getLabel(), bounds, obj.getId()));
        }
        return hotspots;
    }
}
