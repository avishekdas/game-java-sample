package com.abhishri.escape.ui;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;

public class FileAssetManager implements AssetManager {

    private final AssetManager fallback = new ProceduralAssetManager();

    @Override
    public Image getBackground(String roomId) {
        Image loaded = loadFromClasspath("/art/" + roomId + ".png");
        return loaded != null ? loaded : fallback.getBackground(roomId);
    }

    @Override
    public Image getItemIcon(String assetKey) {
        Image loaded = loadFromClasspath("/art/" + assetKey + ".png");
        return loaded != null ? loaded : fallback.getItemIcon(assetKey);
    }

    @Override
    public java.awt.Image getHintCard(String objectId) {
        return fallback.getHintCard(objectId);
    }

    private Image loadFromClasspath(String path) {
        try (InputStream stream = getClass().getResourceAsStream(path)) {
            if (stream == null) return null;
            return ImageIO.read(stream);
        } catch (IOException e) {
            return null;
        }
    }
}
