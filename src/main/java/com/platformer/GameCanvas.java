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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GameCanvas extends Canvas {

    private enum State {
        PLAYING, WON, DEAD
    }

    private State state = State.PLAYING;

    private final Input input;
    private final Player player;
    private final TiledMap map;
    private final Rectangle2D door;

    private final List<Enemy> enemies = new ArrayList<>();

    private long lastNs = 0;

    public GameCanvas(Input input) {
        this.input = input;

        map = new TiledMap("/maps/level1.tmx");
        setWidth(map.getCols() * map.getTile());
        setHeight(map.getRows() * map.getTile());

        player = new Player(map.getSpawnX(), map.getSpawnY(), 28);
        door = map.getDoor();

        for (double[] p : map.getEnemySpawns()) {
            enemies.add(new Enemy(p[0], p[1]));
        }

        startLoop();
    }

    public void attachSceneHandlers(javafx.scene.Scene scene) {
        input.hook(scene);
        scene.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.R)
                reset();
            if (e.getCode() == KeyCode.F3)
                map.showColliderOverlay = !map.showColliderOverlay;
        });
    }

    private void startLoop() {
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

        stompAndDamageLogic();

        if (door != null && player.bounds().intersects(door)) {
            state = State.WON;
        }

        if (player.hp <= 0) {
            state = State.DEAD;
        }
    }

    private void stompAndDamageLogic() {
        Rectangle2D pb = player.bounds();
        double playerTop = pb.getMinY();
        double playerBottom = pb.getMaxY();
        double playerCenterX = pb.getMinX() + pb.getWidth() / 2.0;

        Iterator<Enemy> it = enemies.iterator();
        while (it.hasNext()) {
            Enemy e = it.next();
            if (!e.alive)
                continue;

            boolean overlap = !(pb.getMaxX() < e.left() || pb.getMinX() > e.right()
                    || pb.getMaxY() < e.top() || pb.getMinY() > e.bottom());

            if (!overlap)
                continue;

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
                    player.invTime = 0.3;

                    double dir = Math.signum(playerCenterX - e.centerX());
                    if (dir == 0)
                        dir = 1;

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
        g.setImageSmoothing(false); // penting untuk pixel art biar tidak glitch/blur

        map.render(g);

        for (Enemy e : enemies)
            e.render(g);

        player.render(g);

        g.setFill(Color.WHITE);
        g.setFont(Font.font(18));
        g.fillText("Move: A/D or ←/→ | Jump: W/↑/Space | R: Restart | F3: Toggle Collision", 18, 26);

        drawHpDots(g, player.hp);

        if (state == State.WON) {
            g.setFont(Font.font(48));
            g.setFill(Color.WHITE);
            g.fillText("YOU WIN! 🎉  (R untuk ulang)", getWidth() * 0.35, 180);
        } else if (state == State.DEAD) {
            g.setFont(Font.font(48));
            g.setFill(Color.WHITE);
            g.fillText("YOU DIED 💀  (R untuk ulang)", getWidth() * 0.35, 180);
        }
    }

    private void drawHpDots(GraphicsContext g, int hp) {
        double x0 = 20, y0 = 40, r = 8, gap = 16;
        for (int i = 0; i < 3; i++) {
            g.setFill(i < hp ? Color.web("#FF3B3B") : Color.gray(0.4));
            g.fillOval(x0 + i * (r * 2 + gap), y0, r * 2, r * 2);
        }
    }

    private void reset() {
        player.x = map.getSpawnX();
        player.y = map.getSpawnY();
        player.vx = 0;
        player.vy = 0;
        player.onGround = false;
        player.hp = 3;
        player.invTime = 0;

        enemies.clear();
        for (double[] p : map.getEnemySpawns()) {
            enemies.add(new Enemy(p[0], p[1]));
        }

        state = State.PLAYING;
    }
}
