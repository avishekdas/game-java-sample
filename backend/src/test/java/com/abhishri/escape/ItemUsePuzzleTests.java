package com.abhishri.escape;

import com.abhishri.escape.domain.puzzle.ItemUsePuzzle;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ItemUsePuzzleTests {

    @Test
    void itemUsePuzzle_correctItemAndTarget_returnsTrue() {
        ItemUsePuzzle p = iup("key_item", "locked_box");
        assertThat(p.attempt(Map.of("itemId", "key_item", "targetObjectId", "locked_box"))).isTrue();
    }

    @Test
    void itemUsePuzzle_wrongItem_returnsFalse() {
        ItemUsePuzzle p = iup("key_item", "locked_box");
        assertThat(p.attempt(Map.of("itemId", "wrong_item", "targetObjectId", "locked_box"))).isFalse();
    }

    @Test
    void itemUsePuzzle_wrongTarget_returnsFalse() {
        ItemUsePuzzle p = iup("key_item", "locked_box");
        assertThat(p.attempt(Map.of("itemId", "key_item", "targetObjectId", "wrong_target"))).isFalse();
    }

    @Test
    void itemUsePuzzle_missingInputs_returnsFalse() {
        ItemUsePuzzle p = iup("key_item", "locked_box");
        assertThat(p.attempt(Map.of())).isFalse();
    }

    @Test
    void itemUsePuzzle_missingTarget_returnsFalse() {
        ItemUsePuzzle p = iup("key_item", "locked_box");
        assertThat(p.attempt(Map.of("itemId", "key_item"))).isFalse();
    }

    private ItemUsePuzzle iup(String requiredItemId, String targetObjectId) {
        ItemUsePuzzle p = new ItemUsePuzzle();
        p.setId("iup");
        p.setRoomId("room");
        p.setDescription("item use puzzle");
        p.setRequiredItemId(requiredItemId);
        p.setTargetObjectId(targetObjectId);
        return p;
    }
}
