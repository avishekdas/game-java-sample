package com.abhishri.escape.ui;

import com.abhishri.escape.ui.dto.GameStateDTO;
import com.abhishri.escape.ui.dto.GameStatus;
import com.abhishri.escape.ui.dto.RoomDTO;
import com.abhishri.escape.ui.dto.RoomObjectDTO;
import com.abhishri.escape.ui.dto.SaveMetadataDTO;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class MainFrame extends JFrame {

    private final StatusBar statusBar;
    private final ScenePanel scenePanel;
    private final InventoryPanel inventoryPanel;
    private final DialoguePanel dialoguePanel;

    private final GameApiClient client;
    private final Map<String, BufferedImage> hintCardImages;
    private final Set<String> shownHintCards = new HashSet<>();
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
        inventoryPanel = new InventoryPanel(assetManager);
        dialoguePanel = new DialoguePanel();

        setLayout(new BorderLayout());
        getContentPane().add(statusBar, BorderLayout.NORTH);
        getContentPane().add(scenePanel, BorderLayout.CENTER);
        getContentPane().add(inventoryPanel, BorderLayout.EAST);
        getContentPane().add(dialoguePanel, BorderLayout.SOUTH);

        Map<String, BufferedImage> cards = new HashMap<>();
        for (String oid : List.of("wall_clock", "reception_desk", "reading_lamp", "filing_cabinets")) {
            java.awt.Image img = assetManager.getHintCard(oid);
            if (img instanceof BufferedImage bi) cards.put(oid, bi);
        }
        this.hintCardImages = Collections.unmodifiableMap(cards);

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
        Set<String> solvedIds = state.getSolvedPuzzleIds() != null
                ? new HashSet<>(state.getSolvedPuzzleIds())
                : Collections.emptySet();

        if (state.getCurrentRoom() != null) {
            Map<String, RoomObjectDTO> map = new HashMap<>();
            for (RoomObjectDTO obj : state.getCurrentRoom().getObjects()) {
                map.put(obj.getId(), obj);
            }
            roomObjectsByHotspotId = map;
            statusBar.setRoomName(state.getCurrentRoom().getName());
            scenePanel.setCurrentRoomId(state.getCurrentRoom().getId());
            scenePanel.setHotspots(buildHotspots(state.getCurrentRoom(), solvedIds));
        }
        if (state.getInventory() != null) {
            inventoryPanel.setItems(state.getInventory());
        }
        if (!solvedIds.isEmpty()) {
            int total = state.getTotalPuzzles() > 0 ? state.getTotalPuzzles() : 6;
            statusBar.setSolvedCount(solvedIds.size(), total);
        }
        if (state.getDialogueMessage() != null && !state.getDialogueMessage().isBlank()) {
            dialoguePanel.append(state.getDialogueMessage());
        }
        if (state.getGameStatus() == GameStatus.COMPLETE) {
            dialoguePanel.append("*** YOU SOLVED THE MYSTERY! The Vanishing Librarian case is closed. ***");
            statusBar.setSaveEnabled(false);
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

    protected boolean confirmNewGame() {
        int choice = JOptionPane.showConfirmDialog(this,
                "Start a new game? Unsaved progress will be lost.",
                "New Game", JOptionPane.YES_NO_OPTION);
        return choice == JOptionPane.YES_OPTION;
    }

    protected String selectSaveFile(List<String> saves) {
        return (String) JOptionPane.showInputDialog(this,
                "Select save file:", "Load Game",
                JOptionPane.PLAIN_MESSAGE, null,
                saves.toArray(), saves.get(0));
    }

    public StatusBar getStatusBar() { return statusBar; }
    public ScenePanel getScenePanel() { return scenePanel; }
    public InventoryPanel getInventoryPanel() { return inventoryPanel; }
    public DialoguePanel getDialoguePanel() { return dialoguePanel; }

    Map<String, BufferedImage> getHintCardImages() { return hintCardImages; }
    Set<String> getShownHintCards() { return shownHintCards; }

    private void handleNewGame() {
        if (!confirmNewGame()) return;
        try {
            GameStateDTO state = client.newGame();
            gameId = state.getGameId();
            winShown = false;
            shownHintCards.clear();
            statusBar.setSaveEnabled(true);
            dialoguePanel.setText("");
            applyState(state);
        } catch (RuntimeException e) {
            dialoguePanel.append("Error: " + toFriendlyMessage(e));
        }
    }

    private void handleSave() {
        if (gameId == null) {
            dialoguePanel.append("No game in progress.");
            return;
        }
        try {
            String filename = client.saveGame(gameId);
            dialoguePanel.append("Saved as: " + filename);
        } catch (RuntimeException e) {
            dialoguePanel.append("Save failed: " + toFriendlyMessage(e));
        }
    }

    private void handleLoad() {
        if (gameId == null) {
            dialoguePanel.append("No game in progress.");
            return;
        }
        try {
            List<SaveMetadataDTO> saves = client.listSaves(gameId);
            if (saves.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No saves yet.", "Load Game", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            List<String> filenames = new ArrayList<>();
            for (SaveMetadataDTO save : saves) {
                filenames.add(save.getFilename());
            }
            String selected = selectSaveFile(filenames);
            if (selected == null) return;
            GameStateDTO state = client.loadGame(gameId, selected);
            shownHintCards.clear();
            applyState(state);
        } catch (RuntimeException e) {
            dialoguePanel.append("Load failed: " + toFriendlyMessage(e));
        }
    }

    private void handleHotspotClick(Hotspot hotspot) {
        if (gameId == null) {
            dialoguePanel.append("Start a new game first.");
            return;
        }
        try {
            if ("EXIT".equals(hotspot.getType())) {
                applyState(client.move(gameId, hotspot.getObjectId()));
                return;
            }
            // ITEM hotspots always trigger pickup — never route through use-item
            if ("ITEM".equals(hotspot.getType())) {
                applyState(client.pickup(gameId, hotspot.getObjectId()));
                return;
            }
            String selectedItemId = inventoryPanel.getSelectedItemId();
            if (selectedItemId != null) {
                try {
                    applyState(client.useItem(gameId, selectedItemId, hotspot.getObjectId()));
                    inventoryPanel.clearSelection();
                    return;
                } catch (RuntimeException e) {
                    // 404 means no puzzle exists for this item+object combo — clear selection
                    // and fall through to normal examine/puzzle dispatch
                    if (e.getMessage() != null && e.getMessage().contains("404")) {
                        inventoryPanel.clearSelection();
                    } else {
                        throw e;
                    }
                }
            }
            if ("PUZZLE".equals(hotspot.getType())) {
                String oid = hotspot.getObjectId();
                if (!shownHintCards.contains(oid) && hintCardImages.containsKey(oid)) {
                    shownHintCards.add(oid);
                    scenePanel.showHintCard(hintCardImages.get(oid));
                    RoomObjectDTO capturedObj = roomObjectsByHotspotId.get(oid);
                    Timer delay = new Timer(1500, e -> {
                        ((Timer) e.getSource()).stop();
                        if (capturedObj != null) handlePuzzleClick(capturedObj);
                    });
                    delay.setRepeats(false);
                    delay.start();
                    return;
                }
                RoomObjectDTO obj = roomObjectsByHotspotId.get(oid);
                if (obj != null && obj.getPuzzleType() != null) {
                    handlePuzzleClick(obj);
                    return;
                }
            }
            applyState(client.examine(gameId, hotspot.getObjectId()));
            if (hintCardImages.containsKey(hotspot.getObjectId())) {
                scenePanel.showHintCard(hintCardImages.get(hotspot.getObjectId()));
            }
        } catch (RuntimeException e) {
            dialoguePanel.append("Error: " + toFriendlyMessage(e));
        }
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

    private String toFriendlyMessage(RuntimeException e) {
        String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        if (msg.contains("ConnectException") || msg.contains("HTTP POST failed")
                || msg.contains("HTTP GET failed")) {
            return "Cannot reach game server — is the backend running on port 8080?";
        }
        if (msg.contains("Server error 4") || msg.contains("Server error 5")) {
            return msg;
        }
        return msg;
    }

    private List<Hotspot> buildHotspots(RoomDTO room, Set<String> solvedIds) {
        List<Hotspot> hotspots = new ArrayList<>();
        int panelW = scenePanel.getWidth();
        int panelH = scenePanel.getHeight();
        if (panelW == 0) panelW = 800;
        if (panelH == 0) panelH = 500;

        List<RoomObjectDTO> objects = room.getObjects() != null ? room.getObjects() : new ArrayList<>();
        int n = objects.size();
        if (n > 0) {
            int spacing = panelW / (n + 1);
            int baseY = panelH * 2 / 3;
            for (int i = 0; i < n; i++) {
                RoomObjectDTO obj = objects.get(i);
                int x = spacing * (i + 1) - 50;
                Rectangle bounds = new Rectangle(x, baseY - 30, 100, 60);
                String type = obj.getObjectType() != null ? obj.getObjectType().name() : "SCENERY";
                // ⚠ Solved lookup uses obj.getPuzzleId() (e.g. "puzzle_clock"), NOT obj.getId()
                boolean solved = "PUZZLE".equals(type) && solvedIds.contains(obj.getPuzzleId());
                hotspots.add(new Hotspot(obj.getId(), type, obj.getLabel(), bounds, obj.getId(), solved));
            }
        }

        List<String> exits = room.getExits() != null ? room.getExits() : new ArrayList<>();
        int m = exits.size();
        if (m > 0) {
            int spacing = panelW / (m + 1);
            int exitY = panelH - 55;
            for (int i = 0; i < m; i++) {
                String roomId = exits.get(i);
                String label = "→ " + toRoomName(roomId);
                int x = spacing * (i + 1) - 60;
                Rectangle bounds = new Rectangle(x, exitY, 120, 40);
                hotspots.add(new Hotspot(roomId, "EXIT", label, bounds, roomId));
            }
        }

        return hotspots;
    }

    private String toRoomName(String roomId) {
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
