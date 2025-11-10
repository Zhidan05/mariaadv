package com.platformer.objects;

import com.platformer.tiled.TiledMap;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Enemy titik biru dengan fisika tile (solid + slope).
 * AI:
 *  - Mengejar player secara horizontal.
 *  - Lompat kalau:
 *      a) mentok dinding saat di tanah, atau
 *      b) player berada di atas dan cukup dekat secara horizontal.
 */
public class Enemy {
    // posisi top-left & kecepatan
    public double x, y;
    public double vx = 0, vy = 0;
    public boolean onGround = false;
    public boolean alive = true;

    // ukuran & visual
    private final double w, h;
    private final double radius;

    // parameter gerak
    private final double moveAccel = 900;     // percepatan horizontal
    private final double maxSpeed  = 140;     // kecepatan maksimum
    private final double gravity   = 1400;    // gravitasi
    private final double jumpVel   = 550;     // tinggi lompatan musuh

    // cooldown supaya tidak spam lompat
    private double jumpCooldown = 0;

    public Enemy(double x, double y) {
        this.radius = 12;
        this.w = radius * 2;
        this.h = radius * 2;
        this.x = x;
        this.y = y - h; // spawn dari titik object → top-left
    }

    // pusat, dipakai untuk AI & hitbox
    public double centerX(){ return x + radius; }
    public double centerY(){ return y + radius; }

    // AABB buat cek tabrakan dgn player
    public double left(){ return x; }
    public double right(){ return x + w; }
    public double top(){ return y; }
    public double bottom(){ return y + h; }

    /** Update enemy dengan fisika dan AI sederhana. */
    public void update(double dt, TiledMap map, double targetX, double targetY) {
        if (!alive) return;

        int tile = map.getTile();

        // update cooldown lompat
        if (jumpCooldown > 0) {
            jumpCooldown -= dt;
            if (jumpCooldown < 0) jumpCooldown = 0;
        }

        // ==== AI arah horizontal ke player ====
        double dxToPlayer = targetX - centerX();
        double dir = 0;
        if (Math.abs(dxToPlayer) > 2) {
            dir = (dxToPlayer > 0) ? 1 : -1;
        }

        // atur kecepatan horizontal (ada akselerasi)
        if (dir == 0) {
            vx = 0; // berhenti cepat kalau sudah sejajar
        } else {
            vx += dir * moveAccel * dt;
            if (vx >  maxSpeed) vx =  maxSpeed;
            if (vx < -maxSpeed) vx = -maxSpeed;
        }

        // gravitasi
        vy += gravity * dt;

        // ===== Gerak X (cek mentok dinding) =====
        double oldX = x;
        double newX = x + vx * dt;
        x = collideX(x, newX, y, w, h, map, tile);
        boolean blockedHoriz = Math.abs(x - newX) > 0.0001;

        // ===== Gerak Y (dengan slope) =====
        double newY = y + vy * dt;
        YRes yr = collideY(y, newY, x, w, h, map, tile);
        y = yr.y;
        onGround = yr.grounded;
        if (yr.hit) vy = 0;

        // ===== Logika lompat otomatis =====
        // Player jauh di atas?
        double dyToPlayer = targetY - centerY();
        boolean playerAbove = dyToPlayer < -32;                // ≥ 1 tile di atas
        boolean closeHoriz  = Math.abs(dxToPlayer) < 240;      // dalam ±7-8 tile

        // Kalau di tanah dan:
        //  - mentok dinding, atau
        //  - player berada di atas & cukup dekat secara horizontal
        // dan cooldown habis → lompat
        if (onGround && jumpCooldown <= 0 &&
                (blockedHoriz || (playerAbove && closeHoriz))) {

            vy = -jumpVel;
            onGround = false;
            jumpCooldown = 0.35; // tunggu sebentar sebelum boleh lompat lagi
        }
    }

    public void render(GraphicsContext g) {
        if (!alive) return;
        g.setFill(Color.web("#2D8CFF")); // biru
        g.fillOval(x, y, w, h);
    }

    // ==== Collision helpers (mirip Player) ====

    private double collideX(double oldX, double newX, double y, double w, double h, TiledMap map, int tile) {
        if (newX > oldX) { // ke kanan
            double right = newX + w - 1;
            int topT = (int)Math.floor(y / tile);
            int botT = (int)Math.floor((y + h - 1) / tile);
            int tx   = (int)Math.floor(right / tile);
            for (int ty = topT; ty <= botT; ty++) {
                // dinding vertikal: abaikan tile slope
                if (map.isSolid(tx, ty) && map.slopeAt(tx, ty) == TiledMap.Slope.NONE) {
                    return tx * tile - w;
                }
            }
        } else if (newX < oldX) { // ke kiri
            double left = newX;
            int topT = (int)Math.floor(y / tile);
            int botT = (int)Math.floor((y + h - 1) / tile);
            int tx   = (int)Math.floor(left / tile);
            for (int ty = topT; ty <= botT; ty++) {
                if (map.isSolid(tx, ty) && map.slopeAt(tx, ty) == TiledMap.Slope.NONE) {
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

        if (newY > oldY) { // turun
            double bottom = newY + h - 1;
            int leftT  = (int)Math.floor(x / tile);
            int rightT = (int)Math.floor((x + w - 1) / tile);
            int ty     = (int)Math.floor(bottom / tile);

            // slope: kaki menempel pada garis miring
            for (int tx = leftT; tx <= rightT; tx++) {
                TiledMap.Slope slope = map.slopeAt(tx, ty);
                if (slope == TiledMap.Slope.NONE) continue;

                double centerX = x + w * 0.5;
                double localX  = centerX - tx * tile; // 0..tile
                if (localX < 0) localX = 0;
                if (localX > tile) localX = tile;

                double surfaceY = (slope == TiledMap.Slope.UP_RIGHT)
                        ? ty * tile + (tile - localX)     // kiri rendah → kanan tinggi
                        : ty * tile + localX;             // kiri tinggi → kanan rendah

                double desiredY = surfaceY - h;
                if (newY >= desiredY - 0.5) {
                    r.y = desiredY;
                    r.grounded = true;
                    r.hit = true;
                    return r;
                }
            }

            // blok padat biasa
            for (int tx = leftT; tx <= rightT; tx++) {
                if (map.isSolid(tx, ty) && map.slopeAt(tx, ty) == TiledMap.Slope.NONE) {
                    r.y = ty * tile - h;
                    r.grounded = true;
                    r.hit = true;
                    return r;
                }
            }
        } else if (newY < oldY) { // naik
            double top = newY;
            int leftT  = (int)Math.floor(x / tile);
            int rightT = (int)Math.floor((x + w - 1) / tile);
            int ty     = (int)Math.floor(top / tile);
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
