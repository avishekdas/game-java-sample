package com.abhishri.escape.ui;

import javax.swing.JFrame;
import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class MainFrame extends JFrame {

    private final StatusBar statusBar;
    private final ScenePanel scenePanel;
    private final InventoryPanel inventoryPanel;
    private final DialoguePanel dialoguePanel;

    public MainFrame(AssetManager assetManager) {
        super("Mystery Escape Room — The Vanishing Librarian");

        statusBar = new StatusBar();
        scenePanel = new ScenePanel(assetManager);
        inventoryPanel = new InventoryPanel();
        dialoguePanel = new DialoguePanel();

        setLayout(new BorderLayout());
        getContentPane().add(statusBar, BorderLayout.NORTH);
        getContentPane().add(scenePanel, BorderLayout.CENTER);
        getContentPane().add(inventoryPanel, BorderLayout.EAST);
        getContentPane().add(dialoguePanel, BorderLayout.SOUTH);

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

    public StatusBar getStatusBar() { return statusBar; }
    public ScenePanel getScenePanel() { return scenePanel; }
    public InventoryPanel getInventoryPanel() { return inventoryPanel; }
    public DialoguePanel getDialoguePanel() { return dialoguePanel; }
}
