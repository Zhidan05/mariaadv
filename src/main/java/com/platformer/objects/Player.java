package com.platformer.objects;

import com.platformer.GameCanvas;
import com.platformer.SfxManager;
import com.platformer.tiled.TiledMap;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.media.AudioClip;

public class Player {

    // ============ FISIKA ============
    public double x, y;
    public double vx = 0, vy = 0;
    public boolean onGround = false;

    private final double w;
    private final double h;

    private final double moveAccel = 1600;
    private final double baseMaxSpeed = 260; // Ganti nama jadi base
    private final double gravity = 1900;
    public final double baseJumpVel = 600; // Ganti nama jadi base

    public int maxHp = 3; // Bisa di-upgrade
    public int hp = 3;
    public double invTime = 0;

    // STATS MULTIPLIER (Skill)
    private double speedMultiplier = 1.0;
    private double jumpMultiplier = 1.0;

    // ============ SPRITE ============
    private static final Image IMG_STAND;
    private static final Image IMG_RUN1;
    private static final Image IMG_RUN2;
    private static final Image IMG_JUMP;
    
    private static final AudioClip JUMP_SFX;

    static {
        IMG_STAND = new Image(Player.class.getResourceAsStream("/player/Stand.png"));
        IMG_JUMP = new Image(Player.class.getResourceAsStream("/player/Jump.png"));
        IMG_RUN1 = new Image(Player.class.getResourceAsStream("/player/Run1.png"));
        IMG_RUN2 = new Image(Player.class.getResourceAsStream("/player/Run2.png"));
        
        JUMP_SFX = SfxManager.load("jump.mp3");
    }
    
    private enum AnimState { STAND, RUN, JUMP }
    private AnimState animState = AnimState.STAND;
    private boolean facingRight = true;
    private int runFrame = 0;
    private double runTimer = 0;
    private static final double RUN_FRAME_DURATION = 0.1; 
    private final double spriteScale = 2.0;

    public Player(double spawnX, double spawnY, double size) {
        this.w = size;
        this.h = size;
        this.x = spawnX;
        this.y = spawnY - h;
    }

    // METHOD UNTUK APPLY SKILL
    public void setStats(double speedMult, double jumpMult, int maxHp) {
        this.speedMultiplier = speedMult;
        this.jumpMultiplier = jumpMult;
        this.maxHp = maxHp;
        this.hp = maxHp; // Heal saat spawn/upgrade
    }

    // ============ UPDATE ============
    public void update(double dt, boolean left, boolean right, boolean jump, TiledMap map) {
        int tile = map.getTile();
        if (invTime > 0) {
            invTime -= dt;
            if (invTime < 0) invTime = 0;
        }
        double dir = 0;
        if (left && !right) dir = -1;
        else if (right && !left) dir = 1;

        double oldX = x; 
        
        // --- APPLY SKILL KECEPATAN ---
        double currentMaxSpeed = baseMaxSpeed * speedMultiplier;

        if (dir == 0) {
            vx = 0; 
        } else {
            vx += dir * moveAccel * dt; 
            if (vx > currentMaxSpeed) vx = currentMaxSpeed;
            if (vx < -currentMaxSpeed) vx = -currentMaxSpeed;
        }

        if (vx > 10) facingRight = true;
        if (vx < -10) facingRight = false;

        vy += gravity * dt;
        double newX = x + vx * dt;
        x = collideX(x, newX, y, w, h, map, tile); 
        double newY = y + vy * dt;
        YRes yr = collideY(y, newY, x, w, h, map, tile);
        y = yr.y;
        onGround = yr.grounded;
        if (yr.hit) vy = 0;
        
        // --- APPLY SKILL LOMPAT ---
        double currentJumpVel = baseJumpVel * jumpMultiplier;
        
        if (jump && onGround) {
            vy = -currentJumpVel; 
            onGround = false;
            if (JUMP_SFX != null) JUMP_SFX.play();
        }
        
        boolean actuallyMoving = (Math.abs(x - oldX) > 0.001);

        if (!onGround) {
            animState = AnimState.JUMP;
        } else if (actuallyMoving) { 
            animState = AnimState.RUN;
        } else {
            animState = AnimState.STAND;
        }

        if (animState == AnimState.RUN) {
            runTimer += dt;
            while (runTimer >= RUN_FRAME_DURATION) {
                runTimer -= RUN_FRAME_DURATION;
                runFrame = 1 - runFrame; 
            }
        } else {
            runTimer = 0;
            runFrame = 0;
        }
    }

    // ... (Render, Collision, dll TETAP SAMA) ...
    public void render(GraphicsContext g) {
        Image img;
        switch (animState) {
            case JUMP: img = IMG_JUMP; break;
            case RUN: img = (runFrame == 0) ? IMG_RUN1 : IMG_RUN2; break;
            case STAND: default: img = IMG_STAND; break;
        }
        double drawW = w * spriteScale;
        double drawH = h * spriteScale;
        double drawX = x - (drawW - w) / 2.0;
        double drawY = y - (drawH - h);
        double alpha = (invTime > 0) ? 0.5 : 1.0;
        g.save();
        g.setGlobalAlpha(alpha);
        if (GameCanvas.gameBeaten) {
            ColorAdjust goldEffect = new ColorAdjust();
            goldEffect.setHue(-0.1); 
            goldEffect.setSaturation(0.5); 
            goldEffect.setBrightness(0.2); 
            g.setEffect(goldEffect);
        }
        if (facingRight) {
            g.drawImage(img, drawX, drawY, drawW, drawH);
        } else {
            g.translate(drawX + drawW, drawY);
            g.scale(-1, 1);
            g.drawImage(img, 0, 0, drawW, drawH);
        }
        g.restore();
    }

    public Rectangle2D bounds() { return new Rectangle2D(x, y, w, h); }

    private double collideX(double oldX, double newX, double y, double w, double h, TiledMap map, int tile) {
        if (newX > oldX) { 
            double right = newX + w - 1;
            int topT = (int) Math.floor(y / tile);
            int botT = (int) Math.floor((y + h - 1) / tile);
            int tx = (int) Math.floor(right / tile);
            for (int ty = topT; ty <= botT; ty++) {
                if (map.isSolid(tx, ty) && map.slopeAt(tx, ty) == TiledMap.Slope.NONE) {
                    vx = 0; 
                    return tx * tile - w;
                }
            }
        } else if (newX < oldX) { 
            double left = newX;
            int topT = (int) Math.floor(y / tile);
            int botT = (int)Math.floor((y + h - 1) / tile);
            int tx = (int) Math.floor(left / tile);
            for (int ty = topT; ty <= botT; ty++) {
                if (map.isSolid(tx, ty) && map.slopeAt(tx, ty) == TiledMap.Slope.NONE) {
                    vx = 0; 
                    return (tx + 1) * tile;
                }
            }
        }
        return newX;
    }

    private static class YRes { double y; boolean grounded; boolean hit; }
    private YRes collideY(double oldY, double newY, double x, double w, double h, TiledMap map, int tile) {
        YRes r = new YRes();
        r.y = newY;
        r.grounded = false;
        r.hit = false;
        if (newY > oldY) { 
            double bottom = newY + h - 1;
            int leftT = (int) Math.floor(x / tile);
            int rightT = (int) Math.floor((x + w - 1) / tile);
            int ty = (int) Math.floor(bottom / tile);
            for (int tx = leftT; tx <= rightT; tx++) {
                TiledMap.Slope slope = map.slopeAt(tx, ty);
                if (slope == TiledMap.Slope.NONE) continue;
                double centerX = x + w * 0.5;
                double localX = centerX - tx * tile;
                if (localX < 0) localX = 0;
                if (localX > tile) localX = tile;
                double surfaceY = (slope == TiledMap.Slope.UP_RIGHT) ? ty * tile + (tile - localX) : ty * tile + localX;
                double desiredY = surfaceY - h;
                if (newY >= desiredY - 0.5) {
                    r.y = desiredY;
                    r.grounded = true;
                    r.hit = true;
                    return r;
                }
            }
            for (int tx = leftT; tx <= rightT; tx++) {
                if (map.isSolid(tx, ty) && map.slopeAt(tx, ty) == TiledMap.Slope.NONE) {
                    r.y = ty * tile - h;
                    r.grounded = true;
                    r.hit = true;
                    return r;
                }
            }
        } else if (newY < oldY) { 
            double top = newY;
            int leftT = (int) Math.floor(x / tile);
            int rightT = (int) Math.floor((x + w - 1) / tile);
            int ty = (int) Math.floor(top / tile);
            for (int tx = leftT; tx <= rightT; tx++) {
                if (map.isSolid(tx, ty)) {
                    r.y = (ty + 1) * tile;
                    r.hit = true;
                    return r;
                }
            }
        }
        return r;
    }
}