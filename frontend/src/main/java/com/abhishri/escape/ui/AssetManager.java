package com.abhishri.escape.ui;

import java.awt.Image;

public interface AssetManager {
    Image getBackground(String roomId);
    Image getItemIcon(String assetKey);
}
