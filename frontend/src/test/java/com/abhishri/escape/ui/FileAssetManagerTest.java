package com.abhishri.escape.ui;

import org.junit.jupiter.api.Test;

import java.awt.Image;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FileAssetManagerTest {

    @Test
    void getBackground_noArtFilePresent_returnsFallbackImage() {
        AssetManager manager = new FileAssetManager();
        Image image = manager.getBackground("room_foyer");
        assertNotNull(image, "Must never return null — fallback placeholder must kick in");
    }

    @Test
    void getItemIcon_noArtFilePresent_returnsFallbackImage() {
        AssetManager manager = new FileAssetManager();
        Image image = manager.getItemIcon("desk_key");
        assertNotNull(image, "Must never return null — fallback placeholder must kick in");
    }

    @Test
    void fileAssetManager_isAnAssetManager() {
        assertInstanceOf(AssetManager.class, new FileAssetManager(),
                "FileAssetManager must implement AssetManager (Inheritance rubric)");
    }
}
