package com.abhishri.escape.exception;

public class ItemNotInInventoryException extends RuntimeException {

    public ItemNotInInventoryException(String itemId) {
        super("Item not in inventory: " + itemId);
    }
}
