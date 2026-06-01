package com.abhishri.escape.ui.dto;

import java.util.List;
import java.util.UUID;

public class GameStateDTO {

    private UUID gameId;
    private GameStatus gameStatus;
    private RoomDTO currentRoom;
    private List<InventoryItemDTO> inventory;
    private List<String> solvedPuzzleIds;
    private String dialogueMessage;
    private LastActionResult lastActionResult;
    private int totalPuzzles;

    public GameStateDTO() {}

    public UUID getGameId() { return gameId; }
    public void setGameId(UUID gameId) { this.gameId = gameId; }

    public GameStatus getGameStatus() { return gameStatus; }
    public void setGameStatus(GameStatus gameStatus) { this.gameStatus = gameStatus; }

    public RoomDTO getCurrentRoom() { return currentRoom; }
    public void setCurrentRoom(RoomDTO currentRoom) { this.currentRoom = currentRoom; }

    public List<InventoryItemDTO> getInventory() { return inventory; }
    public void setInventory(List<InventoryItemDTO> inventory) { this.inventory = inventory; }

    public List<String> getSolvedPuzzleIds() { return solvedPuzzleIds; }
    public void setSolvedPuzzleIds(List<String> solvedPuzzleIds) { this.solvedPuzzleIds = solvedPuzzleIds; }

    public String getDialogueMessage() { return dialogueMessage; }
    public void setDialogueMessage(String dialogueMessage) { this.dialogueMessage = dialogueMessage; }

    public LastActionResult getLastActionResult() { return lastActionResult; }
    public void setLastActionResult(LastActionResult lastActionResult) { this.lastActionResult = lastActionResult; }

    public int getTotalPuzzles() { return totalPuzzles; }
    public void setTotalPuzzles(int totalPuzzles) { this.totalPuzzles = totalPuzzles; }
}
