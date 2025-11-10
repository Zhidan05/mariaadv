package com.platformer.input;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public class Input {
    public boolean left, right, up;

    public void hook(Scene scene){
        scene.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            KeyCode c = e.getCode();
            if (c == KeyCode.A || c == KeyCode.LEFT)  left  = true;
            if (c == KeyCode.D || c == KeyCode.RIGHT) right = true;
            if (c == KeyCode.W || c == KeyCode.UP || c == KeyCode.SPACE) up = true;
        });
        scene.addEventHandler(KeyEvent.KEY_RELEASED, e -> {
            KeyCode c = e.getCode();
            if (c == KeyCode.A || c == KeyCode.LEFT)  left  = false;
            if (c == KeyCode.D || c == KeyCode.RIGHT) right = false;
            if (c == KeyCode.W || c == KeyCode.UP || c == KeyCode.SPACE) up = false;
        });
    }
}
