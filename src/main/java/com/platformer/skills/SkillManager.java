package com.platformer.skills; // UBAH PACKAGE

import com.platformer.GameCanvas; // IMPORT GameCanvas
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class SkillManager {

    private static final List<Skill> allSkills = new ArrayList<>();
    private static final Random random = new Random();

    static {
        // === COMMON (8 Skill) ===
        allSkills.add(new Skill("Lompatan Katak", "Tinggi Lompat +25%", Rarity.COMMON, g -> g.addJump(0.25)));
        allSkills.add(new Skill("Sepatu Ringan", "Kecepatan +15%", Rarity.COMMON, g -> g.addSpeed(0.15)));
        allSkills.add(new Skill("Vitamin C", "Max HP +1", Rarity.COMMON, g -> g.addMaxHp(1)));
        allSkills.add(new Skill("Bekal Makanan", "Heal Penuh + Max HP +1", Rarity.COMMON, g -> { g.addMaxHp(1); g.fullHeal(); }));
        allSkills.add(new Skill("Lutut Pelari", "Kecepatan +10% & Lompat +10%", Rarity.COMMON, g -> { g.addSpeed(0.1); g.addJump(0.1); }));
        allSkills.add(new Skill("Pemanasan", "Kecepatan +12%", Rarity.COMMON, g -> g.addSpeed(0.12)));
        allSkills.add(new Skill("Per pegas", "Tinggi Lompat +30%", Rarity.COMMON, g -> g.addJump(0.30)));
        allSkills.add(new Skill("Helm Proyek", "Max HP +1", Rarity.COMMON, g -> g.addMaxHp(1)));

        // === RARE (6 Skill) ===
        allSkills.add(new Skill("Lari Maraton", "Kecepatan +25%", Rarity.RARE, g -> g.addSpeed(0.25)));
        allSkills.add(new Skill("Lompatan Kijang", "Tinggi Lompat +40%", Rarity.RARE, g -> g.addJump(0.40)));
        allSkills.add(new Skill("Rompi Tebal", "Max HP +2", Rarity.RARE, g -> g.addMaxHp(2)));
        allSkills.add(new Skill("Parkour", "Speed +20% & Jump +20%", Rarity.RARE, g -> { g.addSpeed(0.2); g.addJump(0.2); }));
        allSkills.add(new Skill("Double Jump?", "Tinggi Lompat +50%", Rarity.RARE, g -> g.addJump(0.50)));
        allSkills.add(new Skill("Adrenalin", "Speed +30% tapi -1 HP (Min 1)", Rarity.RARE, g -> g.addSpeed(0.30))); 

        // === EPIC (4 Skill) ===
        allSkills.add(new Skill("Sonic Boom", "Kecepatan +50%", Rarity.EPIC, g -> g.addSpeed(0.50)));
        allSkills.add(new Skill("Gravitasi Bulan", "Tinggi Lompat +75%", Rarity.EPIC, g -> g.addJump(0.75)));
        allSkills.add(new Skill("Jantung Besi", "Max HP +3 & Full Heal", Rarity.EPIC, g -> { g.addMaxHp(3); g.fullHeal(); }));
        allSkills.add(new Skill("Ninja Master", "Speed +35% & Jump +35%", Rarity.EPIC, g -> { g.addSpeed(0.35); g.addJump(0.35); }));

        // === LEGENDARY (2 Skill) ===
        allSkills.add(new Skill("GOD SPEED", "Kecepatan +100% (Hati-hati!)", Rarity.LEGENDARY, g -> g.addSpeed(1.0)));
        allSkills.add(new Skill("SKY HIGH", "Lompatan +100% & Max HP +5", Rarity.LEGENDARY, g -> { g.addJump(1.0); g.addMaxHp(5); g.fullHeal(); }));
    }

    public static List<Skill> getRandomSkills() {
        List<Skill> selected = new ArrayList<>();
        List<Skill> pool = new ArrayList<>(allSkills);
        
        while (selected.size() < 3 && !pool.isEmpty()) {
            Skill candidate = pool.get(random.nextInt(pool.size()));
            
            boolean keep = false;
            double roll = random.nextDouble(); 
            
            switch (candidate.getRarity()) {
                case COMMON: keep = true; break;
                case RARE: keep = roll < 0.6; break;    
                case EPIC: keep = roll < 0.3; break;     
                case LEGENDARY: keep = roll < 0.1; break; 
            }

            if (keep) {
                selected.add(candidate);
                pool.remove(candidate); 
            }
        }
        
        while (selected.size() < 3) {
            Skill s = allSkills.get(random.nextInt(allSkills.size()));
            if (!selected.contains(s)) selected.add(s);
        }

        return selected;
    }
}