package com.platformer;

import com.platformer.input.Input;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) {
        Input input = new Input();
        GameCanvas game = new GameCanvas(input);

        StackPane root = new StackPane(game);
        Scene scene = new Scene(root, game.getWidth(), game.getHeight());
        game.attachSceneHandlers(scene);

        stage.setTitle("Platformer 2D JavaFX (TMX + TSX)");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) { launch(args); }
}
