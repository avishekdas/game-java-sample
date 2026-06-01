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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MainFrame extends JFrame {

    private final StatusBar statusBar;
    private final ScenePanel scenePanel;
    private final InventoryPanel inventoryPanel;
    private final DialoguePanel dialoguePanel;

    private final GameApiClient client;
    private UUID gameId;
    private boolean winShown = false;
    private Map<String, RoomObjectDTO> roomObjectsByHotspotId = new HashMap<>();

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
            Map<String, RoomObjectDTO> map = new HashMap<>();
            for (RoomObjectDTO obj : state.getCurrentRoom().getObjects()) {
                map.put(obj.getId(), obj);
            }
            roomObjectsByHotspotId = map;
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
            if (!winShown) {
                winShown = true;
                String msg = state.getDialogueMessage() != null ? state.getDialogueMessage()
                        : "You solved the mystery! The Vanishing Librarian case is closed.";
                showWinDialog(msg);
            }
        }
    }

    protected void showWinDialog(String message) {
        JOptionPane.showMessageDialog(this, message, "You're free!", JOptionPane.INFORMATION_MESSAGE);
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
        if ("EXIT".equals(hotspot.getType())) {
            applyState(client.move(gameId, hotspot.getObjectId()));
            return;
        }
        String selectedItemId = inventoryPanel.getSelectedItemId();
        if (selectedItemId != null) {
            applyState(client.useItem(gameId, selectedItemId, hotspot.getObjectId()));
            inventoryPanel.clearSelection();
            return;
        }
        if ("PUZZLE".equals(hotspot.getType())) {
            RoomObjectDTO obj = roomObjectsByHotspotId.get(hotspot.getObjectId());
            if (obj != null && obj.getPuzzleType() != null) {
                handlePuzzleClick(obj);
                return;
            }
        }
        applyState(client.examine(gameId, hotspot.getObjectId()));
    }

    private void handlePuzzleClick(RoomObjectDTO obj) {
        if ("ITEM_USE".equals(obj.getPuzzleType())) {
            String selectedItemId = inventoryPanel.getSelectedItemId();
            if (selectedItemId == null) {
                dialoguePanel.append("Select an item from your inventory to use here.");
                return;
            }
            applyState(client.useItem(gameId, selectedItemId, obj.getId()));
            inventoryPanel.clearSelection();
            return;
        }
        PuzzleDialog dialog = createPuzzleDialog(obj);
        if (dialog == null) return;
        dialog.setVisible(true);
        if (dialog.isConfirmed()) {
            applyState(client.attemptPuzzle(gameId, obj.getPuzzleId(), dialog.getInputs()));
        }
    }

    private PuzzleDialog createPuzzleDialog(RoomObjectDTO obj) {
        return switch (obj.getPuzzleType()) {
            case "COMBINATION" -> new CombinationPuzzleDialog(this, obj.getPuzzleId(),
                    obj.getDigitCount() > 0 ? obj.getDigitCount() : 3, obj.getLabel());
            case "RIDDLE" -> new RiddlePuzzleDialog(this, obj.getPuzzleId(),
                    obj.getQuestionText() != null ? obj.getQuestionText() : obj.getLabel());
            case "SEQUENCE" -> new SequencePuzzleDialog(this, obj.getPuzzleId(),
                    obj.getAvailableItems() != null ? obj.getAvailableItems() : List.of(),
                    obj.getLabel());
            default -> null;
        };
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
