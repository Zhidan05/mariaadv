package com.platformer.skills;

import javafx.scene.paint.Color;

public enum Rarity {
    COMMON(Color.WHITE, "Common"),
    RARE(Color.web("#3498db"), "Rare"),        // Biru
    EPIC(Color.web("#9b59b6"), "Epic"),        // Ungu
    LEGENDARY(Color.web("#f1c40f"), "Legendary"); // Emas

    private final Color color;
    private final String label;

    Rarity(Color color, String label) {
        this.color = color;
        this.label = label;
    }

    public Color getColor() { return color; }
    public String getLabel() { return label; }
}