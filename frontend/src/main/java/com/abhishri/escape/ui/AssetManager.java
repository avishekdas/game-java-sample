package com.abhishri.escape.ui;

import java.awt.Image;
import java.awt.image.BufferedImage;

public interface AssetManager {
    Image getBackground(String roomId);
    Image getItemIcon(String assetKey);

    default Image getHintCard(String objectId) {
        return new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
    }
}
