package com.platformer;

import com.platformer.input.Input;
import com.platformer.objects.Player;
import com.platformer.objects.Enemy;
import com.platformer.tiled.TiledMap;

import javafx.animation.AnimationTimer;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GameCanvas extends Canvas {

    public static boolean gameBeaten = false;

    private enum State {
        PLAYING, 
        LEVEL_CLEAR, 
        WON, 
        DEAD
    }
    private State state = State.PLAYING;

    private static final String[] LEVELS = {
            "/maps/level1.tmx",
            "/maps/level2.tmx",
            "/maps/level3.tmx",
            "/maps/level4.tmx"
    };

    private int currentLevelIndex = 0;

    private final Input input;
    private Player player;
    private TiledMap map;
    private Rectangle2D door;

    private final List<Enemy> enemies = new ArrayList<>();

    private long lastNs = 0;
    private double camX = 0;
    private double camY = 0;
    private double zoomFactor = 2.0; 
    private double lerpFactor = 0.08; 

    // Handler untuk memberi tahu MainApp
    private Runnable onGameWonHandler;
    
    // TAMBAHAN: Handler untuk troll
    private Runnable onTrollHandler;
    // TAMBAHAN: Counter kematian beruntun
    private int consecutiveDeaths = 0;

    public GameCanvas(Input input) {
        this.input = input;
        loadLevel(0); 
        setFocusTraversable(true); 
    }

    public void setOnGameWon(Runnable handler) {
        this.onGameWonHandler = handler;
    }

    // TAMBAHAN: Method setter untuk troll handler
    public void setOnTroll(Runnable handler) {
        this.onTrollHandler = handler;
    }

    private void loadLevel(int index) {
        if (index < 0 || index >= LEVELS.length) {
            index = 0;
        }
        currentLevelIndex = index;
        map = new TiledMap(LEVELS[currentLevelIndex]);
        player = new Player(map.getSpawnX(), map.getSpawnY(), 28);
        player.setLevel(currentLevelIndex); 
        door = map.getDoor();
        enemies.clear();
        for (double[] p : map.getEnemySpawns()) {
            enemies.add(new Enemy(p[0], p[1]));
        }
        if (getWidth() > 0) {
             camX = getTargetCamX();
             camY = getTargetCamY();
             clampCamera(); 
        } else {
            camX = player.x;
            camY = player.y;
        }
        state = State.PLAYING;
    }

    public void attachSceneHandlers(javafx.scene.Scene scene) {
        input.hook(scene);
        scene.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            if (state == State.LEVEL_CLEAR) {
                currentLevelIndex++; 
                loadLevel(currentLevelIndex); 
                return; 
            }
            if (e.getCode() == KeyCode.R)
                reset();
            if (e.getCode() == KeyCode.F3)
                map.showColliderOverlay = !map.showColliderOverlay;
        });
    }

    public void startGameLoop() {
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (lastNs == 0) {
                    lastNs = now;
                    return;
                }
                double dt = (now - lastNs) / 1_000_000_000.0;
                lastNs = now;
                update(dt);
                render();
            }
        }.start();
    }

    private void update(double dt) {
        if (state != State.PLAYING)
            return;

        player.update(dt, input.left, input.right, input.up, map);

        double targetX = player.x + 14;
        double targetY = player.y + 14;
        for (Enemy e : enemies) {
            e.update(dt, map, targetX, targetY);
        }
        
        updateCamera(dt);
        
        stompAndDamageLogic();

        if (door != null && player.bounds().intersects(door)) {
            // TAMBAHAN: Reset streak kematian jika berhasil menyelesaikan level
            consecutiveDeaths = 0; 

            if (currentLevelIndex < LEVELS.length - 1) {
                state = State.LEVEL_CLEAR;
            } else {
                if (state != State.WON) { 
                    state = State.WON;
                    gameBeaten = true;
                    if (onGameWonHandler != null) {
                        onGameWonHandler.run();
                    }
                }
            }
        } else if (player.hp <= 0) {
            // UBAH: Cek transisi ke DEAD untuk menghitung kematian
            if (state != State.DEAD) {
                state = State.DEAD;
                consecutiveDeaths++; // Tambah counter mati
                
                // Jika mati kelipatan 3x, panggil troll
                if (consecutiveDeaths > 0 && consecutiveDeaths % 3 == 0) {
                    if (onTrollHandler != null) {
                        onTrollHandler.run();
                    }
                }
            }
        }
    }

    private double getTargetCamX() {
        if (player == null) return 0;
        double playerCenterX = player.x + (player.bounds().getWidth() / 2);
        return playerCenterX - (getWidth() / 2 / zoomFactor);
    }
    private double getTargetCamY() {
        if (player == null) return 0;
        double playerCenterY = player.y + (player.bounds().getHeight() / 2);
        return playerCenterY - (getHeight() / 2 / zoomFactor);
    }
    private void clampCamera() {
        if (map == null) return;
        double viewWidth = getWidth() / zoomFactor;
        double viewHeight = getHeight() / zoomFactor;
        double minCamX = 0;
        double maxCamX = (map.getCols() * map.getTile()) - viewWidth;
        double minCamY = 0;
        double maxCamY = (map.getRows() * map.getTile()) - viewHeight;
        if (camX < minCamX) camX = minCamX;
        if (camX > maxCamX) camX = maxCamX;
        if (camY < minCamY) camY = minCamY;
        if (camY > maxCamY) camY = maxCamY;
        if (maxCamX < minCamX) {
            camX = (maxCamX + minCamX) / 2; 
        }
        if (maxCamY < minCamY) {
            camY = (maxCamY + minCamY) / 2; 
        }
    }
    private void updateCamera(double dt) {
        if (player == null || map == null || dt == 0) return;
        double targetCamX = getTargetCamX();
        double targetCamY = getTargetCamY();
        double rate = 1.0 - Math.pow(1.0 - lerpFactor, dt * 60.0);
        if (Double.isNaN(rate)) rate = lerpFactor;
        camX += (targetCamX - camX) * rate;
        camY += (targetCamY - camY) * rate;
        if (Math.abs(camX - targetCamX) < 0.1) camX = targetCamX;
        if (Math.abs(camY - targetCamY) < 0.1) camY = targetCamY;
        clampCamera();
    }
    
    private void stompAndDamageLogic() {
        Rectangle2D pb = player.bounds();
        double playerTop = pb.getMinY();
        double playerBottom = pb.getMaxY();
        double playerCenterX = pb.getMinX() + pb.getWidth() / 2.0;
        Iterator<Enemy> it = enemies.iterator();
        while (it.hasNext()) {
            Enemy e = it.next();
            if (!e.alive) continue;
            boolean overlap = !(pb.getMaxX() < e.left() || pb.getMinX() > e.right()
                    || pb.getMaxY() < e.top() || pb.getMinY() > e.bottom());
            if (!overlap) continue;
            double enemyTop = e.top();
            double enemyMidY = e.centerY();
            boolean stomping = player.vy > 0 &&
                    playerTop < enemyTop &&
                    playerBottom <= enemyMidY + 4;
            if (stomping) {
                e.alive = false;
                it.remove();
                player.vy = -player.jumpVel * 0.6; 
                player.onGround = false;
            } else {
                if (player.invTime <= 0) {
                    player.hp -= 1;
                    if (currentLevelIndex >= 3) {
                        player.invTime = 0.5; 
                    } else {
                        player.invTime = 0.3; 
                    }
                    double dir = Math.signum(playerCenterX - e.centerX());
                    if (dir == 0) dir = 1;
                    double playerKbH = 260;
                    double enemyKbH = 220;
                    double kbUpP = 260;
                    double kbUpE = 200;
                    player.vx = dir * playerKbH;
                    player.vy = -kbUpP;
                    player.onGround = false;
                    e.vx = -dir * enemyKbH;
                    e.vy = -kbUpE;
                    e.onGround = false;
                }
            }
        }
    }


    private void render() {
        GraphicsContext g = getGraphicsContext2D();
        g.setImageSmoothing(false); 

        g.save();
        g.scale(zoomFactor, zoomFactor);
        g.translate(-camX, -camY);

        map.render(g);
        for (Enemy e : enemies)
            e.render(g);
        player.render(g);

        g.restore();

        g.setFill(Color.WHITE);
        g.setFont(Font.font(18));
        g.fillText(
                "Level " + (currentLevelIndex + 1) + "/" + LEVELS.length +
                "   |   Move: A/D or ←/→   |   Jump: W/↑/Space   |   R: Restart level   |   F3: Toggle Collision",
                18, 26);

        drawHpDots(g, player.hp);

        if (state == State.DEAD) {
            g.setFont(Font.font(48));
            g.setFill(Color.WHITE);
            g.setTextAlign(TextAlignment.CENTER); 
            g.fillText("YOU DIED 💀", getWidth() / 2, getHeight() * 0.3); 
            g.setFont(Font.font(24));
            g.fillText("(R untuk ulang level)", getWidth() / 2, getHeight() * 0.3 + 40);
            g.setTextAlign(TextAlignment.LEFT); 
        
        } else if (state == State.LEVEL_CLEAR) {
            g.setFill(Color.web("#000000", 0.7)); 
            g.fillRect(0, 0, getWidth(), getHeight()); 
            g.setTextAlign(TextAlignment.CENTER); 
            g.setFont(Font.font(48));
            g.setFill(Color.WHITE);
            g.fillText("LEVEL " + (currentLevelIndex + 1) + " SELESAI!", getWidth() / 2, getHeight() * 0.3); 
            g.setFont(Font.font(24));
            g.setFill(Color.YELLOW); 
            String skillMessage = getSkillMessage(currentLevelIndex + 1);
            g.fillText(skillMessage, getWidth() / 2, getHeight() * 0.3 + 60);
            g.setFont(Font.font(20));
            g.setFill(Color.WHITE);
            g.fillText("Tekan tombol apa saja untuk Lanjut...", getWidth() / 2, getHeight() * 0.3 + 120);
            g.setTextAlign(TextAlignment.LEFT); 
        }
    }


    private void drawHpDots(GraphicsContext g, int hp) {
        double x0 = 20, y0 = 40, r = 8, gap = 16;
        for (int i = 0; i < 3; i++) {
            g.setFill(i < hp ? Color.web("#FF3B3B") : Color.gray(0.4));
            g.fillOval(x0 + i * (r * 2 + gap), y0, r * 2, r * 2);
        }
    }
    
    
    private String getSkillMessage(int nextLevelIndex) {
        switch (nextLevelIndex) {
            case 1: 
                return "SKILL BARU: Lincah (Kecepatan Gerak +15%)";
            case 2: 
                return "SKILL BARU: Lompatan Gesit (Tinggi Lompat +10%)";
            case 3: 
                return "SKILL BARU: Daya Tahan (Durasi Kebal +0.2d)";
            default:
                return "Menuju level berikutnya..."; 
        }
    }

    private void reset() {
        loadLevel(currentLevelIndex);
    }

    public void resetToLevelOne() {
        gameBeaten = true; 
        consecutiveDeaths = 0; // Reset counter kematian jika restart game
        loadLevel(0); 
    }
}