package com.platformer.skills; // UBAH PACKAGE

import com.platformer.GameCanvas; // IMPORT GameCanvas
import java.util.function.Consumer;

public class Skill {
    private final String name;
    private final String description;
    private final Rarity rarity;
    // Logic efek skill disimpan dalam Consumer
    private final Consumer<GameCanvas> effect; 

    public Skill(String name, String description, Rarity rarity, Consumer<GameCanvas> effect) {
        this.name = name;
        this.description = description;
        this.rarity = rarity;
        this.effect = effect;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public Rarity getRarity() { return rarity; }
    
    // Terapkan skill ke game canvas
    public void apply(GameCanvas game) {
        effect.accept(game);
    }
}