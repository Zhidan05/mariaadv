package com.platformer;

import com.platformer.input.Input;
import com.platformer.objects.Enemy;
import com.platformer.objects.Player;
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
import javafx.scene.media.AudioClip; 

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GameCanvas extends Canvas {

    public static boolean gameBeaten = false;
    private static final AudioClip STOMP_SFX = SfxManager.load("stomp.mp3");
    private static final AudioClip HURT_SFX = SfxManager.load("hurt.mp3");
    private static final AudioClip LOSE_SFX = SfxManager.load("loser.mp3");

    private enum State { PLAYING, LEVEL_CLEAR, WON, DEAD }
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
    private boolean gameLoopStarted = false; 
    private double camX = 0, camY = 0;
    private double zoomFactor = 2.0; 
    private double lerpFactor = 0.08; 

    private Runnable onGameWonHandler;
    private Runnable onTrollHandler;
    private Runnable onPauseToggleHandler;
    private Runnable onLevelClearHandler; 
    
    private int consecutiveDeaths = 0;
    private boolean isPaused = false;

    // ==== DATA STATS PERSISTEN ====
    private double pSpeedMult = 1.0;
    private double pJumpMult = 1.0;
    private int pMaxHp = 3;

    public GameCanvas(Input input) {
        this.input = input; 
        setFocusTraversable(true); 
        loadLevel(0); 
    }

    public void setOnGameWon(Runnable handler) { this.onGameWonHandler = handler; }
    public void setOnTroll(Runnable handler) { this.onTrollHandler = handler; }
    public void setOnPauseToggle(Runnable handler) { this.onPauseToggleHandler = handler; }
    public void setOnLevelClear(Runnable handler) { this.onLevelClearHandler = handler; }

    // === METHOD UPGRADE SKILL (DINAMIS) ===
    public void addSpeed(double amount) { pSpeedMult += amount; }
    public void addJump(double amount) { pJumpMult += amount; }
    public void addMaxHp(int amount) { pMaxHp += amount; }
    public void fullHeal() { if (player != null) player.hp = pMaxHp; }

    public void setPaused(boolean paused) {
        this.isPaused = paused;
        if (isPaused) {
            if (input != null) input.clear();
        } else {
            lastNs = System.nanoTime(); 
        }
    }

    public void stopLoseSfx() { if (LOSE_SFX != null) LOSE_SFX.stop(); }
    public void resetLevel() { stopLoseSfx(); loadLevel(currentLevelIndex); }
    public void resetToLevelOne() {
        stopLoseSfx(); 
        consecutiveDeaths = 0;
        pSpeedMult = 1.0;
        pJumpMult = 1.0;
        pMaxHp = 3;
        loadLevel(0); 
    }
    public void startNextLevel() { currentLevelIndex++; loadLevel(currentLevelIndex); }
    
    public String getCurrentLevelPath() {
        if (currentLevelIndex >= 0 && currentLevelIndex < LEVELS.length) return LEVELS[currentLevelIndex];
        return "INDEX INVALID";
    }
    public String getNextLevelPath() {
        int nextIndex = currentLevelIndex + 1;
        if (nextIndex >= 0 && nextIndex < LEVELS.length) return LEVELS[nextIndex];
        return "INDEX INVALID";
    }

    private void loadLevel(int index) {
        if (index < 0 || index >= LEVELS.length) index = 0;
        currentLevelIndex = index;
        map = new TiledMap(LEVELS[currentLevelIndex]);
        player = new Player(map.getSpawnX(), map.getSpawnY(), 28);
        player.setStats(pSpeedMult, pJumpMult, pMaxHp); // Terapkan stats yang sudah diakumulasi
        door = map.getDoor();
        enemies.clear();
        for (double[] p : map.getEnemySpawns()) enemies.add(new Enemy(p[0], p[1]));
        if (getWidth() > 0) {
             camX = getTargetCamX();
             camY = getTargetCamY();
             clampCamera(); 
        } else {
            camX = player.x;
            camY = player.y;
        }
        state = State.PLAYING;
        isPaused = false; 
    }

    // ... (attachSceneHandlers, startGameLoop, update, dll TETAP SAMA) ...
    public void attachSceneHandlers(javafx.scene.Scene scene) {
        input.hook(scene);
        scene.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                if (state == State.PLAYING && onPauseToggleHandler != null) onPauseToggleHandler.run();
                return; 
            }
            if (isPaused) return;
            if (e.getCode() == KeyCode.R) resetLevel(); 
            if (e.getCode() == KeyCode.F3) map.showColliderOverlay = !map.showColliderOverlay;
        });
    }
    public void startGameLoop() {
        if (gameLoopStarted) return; 
        gameLoopStarted = true;
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (lastNs == 0) { lastNs = now; return; }
                if (isPaused) { lastNs = now; render(); return; }
                double dt = (now - lastNs) / 1_000_000_000.0;
                lastNs = now;
                update(dt);
                render();
            }
        }.start();
    }
    private void update(double dt) {
        if (state != State.PLAYING) return;
        player.update(dt, input.left, input.right, input.up, map);
        double targetX = player.x + 14;
        double targetY = player.y + 14;
        for (Enemy e : enemies) e.update(dt, map, targetX, targetY);
        updateCamera(dt);
        stompAndDamageLogic();
        if (door != null && player.bounds().intersects(door)) {
            consecutiveDeaths = 0; 
            if (currentLevelIndex < LEVELS.length - 1) {
                if (state != State.LEVEL_CLEAR) { 
                    state = State.LEVEL_CLEAR;
                    if (onLevelClearHandler != null) onLevelClearHandler.run();
                }
            } else {
                if (state != State.WON) { 
                    state = State.WON;
                    gameBeaten = true;
                    if (onGameWonHandler != null) onGameWonHandler.run();
                }
            }
        } else if (player.hp <= 0) {
            if (state != State.DEAD) { 
                state = State.DEAD;
                consecutiveDeaths++; 
                if (LOSE_SFX != null) LOSE_SFX.play();
                if (consecutiveDeaths > 0 && consecutiveDeaths % 3 == 0) {
                    if (onTrollHandler != null) onTrollHandler.run();
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
        if (maxCamX < minCamX) camX = (maxCamX + minCamX) / 2; 
        if (maxCamY < minCamY) camY = (maxCamY + minCamY) / 2; 
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
        if (player == null) return; 
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
                player.vy = -player.baseJumpVel * 0.6; 
                player.onGround = false;
                if (STOMP_SFX != null) STOMP_SFX.play();
            } else {
                if (player.invTime <= 0) {
                    player.hp -= 1;
                    if (HURT_SFX != null) HURT_SFX.play();
                    player.invTime = 0.5; 
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
        if (map != null && player != null) {
            g.scale(zoomFactor, zoomFactor);
            g.translate(-camX, -camY);
            map.render(g);
            for (Enemy e : enemies) e.render(g);
            player.render(g);
        } else {
             g.setFill(Color.BLACK);
             g.fillRect(0, 0, getWidth(), getHeight());
        }
        g.restore();
        g.setFill(Color.WHITE);
        g.setFont(Font.font(18));
        g.fillText(
                "Level " + (currentLevelIndex + 1) + "/" + LEVELS.length +
                "   |   Move: A/D or ←/→   |   Jump: W/↑/Space   |   R: Restart level   |   F3: Toggle Collision",
                18, 26);
        if (isPaused) g.fillText(" [ PAUSED ]", 200, 26); 
        drawHpDots(g, player.hp);
        if (state == State.DEAD) {
            g.setFont(Font.font(48));
            g.setFill(Color.WHITE);
            g.setTextAlign(TextAlignment.CENTER); 
            g.fillText("YOU DIED 💀", getWidth() / 2, getHeight() * 0.3); 
            g.setFont(Font.font(24));
            g.fillText("(R untuk ulang level)", getWidth() / 2, getHeight() * 0.3 + 40);
            g.setTextAlign(TextAlignment.LEFT); 
        }
    }
    private void drawHpDots(GraphicsContext g, int hp) {
        double x0 = 20, y0 = 40, r = 8, gap = 16;
        int max = (player != null) ? player.maxHp : 3;
        for (int i = 0; i < max; i++) {
            g.setFill(i < hp ? Color.web("#FF3B3B") : Color.gray(0.4));
            g.fillOval(x0 + i * (r * 2 + gap), y0, r * 2, r * 2);
        }
    }
}