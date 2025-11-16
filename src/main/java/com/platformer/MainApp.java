package com.platformer;

import com.platformer.input.Input;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label; 
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox; 
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color; 
import javafx.scene.text.Font; 
import javafx.scene.text.TextAlignment; // TAMBAHAN: Import untuk perataan teks
import javafx.stage.Stage;

import javafx.animation.ScaleTransition;
import javafx.animation.Interpolator;
import javafx.util.Duration;

import java.io.InputStream;
import java.net.URL;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

public class MainApp extends Application {

    // Interpolator custom untuk efek membal
    private static final Interpolator ELASTIC_OUT = new Interpolator() {
        @Override
        protected double curve(double t) {
            if (t == 0) return 0;
            if (t == 1) return 1;
            double p = 0.3;
            return Math.pow(2, -10 * t) * Math.sin((t - p / 4) * (2 * Math.PI) / p) + 1;
        }
    };

    private MediaPlayer bgmPlayer;
    private GameCanvas gameCanvas;
    private Pane homepagePane;
    private StackPane root; 

    @Override
    public void start(Stage stage) {
        
        initBgm();
        Input input = new Input();
        
        gameCanvas = new GameCanvas(input);
        root = new StackPane();

        Scene scene = new Scene(root); 

        homepagePane = createHomepage(stage, root, scene);
        root.getChildren().add(homepagePane);

        gameCanvas.setOnGameWon(() -> {
            showTrophyPopup(); 
        });

        gameCanvas.setOnTroll(() -> {
            showTrollPopup();
        });

        gameCanvas.attachSceneHandlers(scene);
        stage.setTitle("Platformer 2D JavaFX (TMX + TSX)");
        stage.setScene(scene);
        stage.setFullScreen(true);
        stage.setFullScreenExitHint(""); 
        stage.show();
    }

    private void initBgm() {
        try {
            String bgmPath = "/bgm/backgroundmusic.mp3"; 
            URL bgmUrl = getClass().getResource(bgmPath);
            if (bgmUrl == null) {
                bgmUrl = getClass().getResource("/assets" + bgmPath);
                if (bgmUrl == null) {
                    System.err.println("File BGM tidak ditemukan di: " + bgmPath + " atau /assets" + bgmPath);
                    return; 
                }
            }
            Media bgm = new Media(bgmUrl.toExternalForm());
            bgmPlayer = new MediaPlayer(bgm);
            bgmPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            bgmPlayer.setVolume(0.4); 
            bgmPlayer.play();
        } catch (Exception e) {
            System.err.println("Error saat memuat BGM:");
            e.printStackTrace();
        }
    }

    private Pane createHomepage(Stage stage, StackPane root, Scene scene) {
        Image bgImage = null;
        String imagePath = "/homepage/homepage.png"; 
        try (InputStream is = MainApp.class.getResourceAsStream(imagePath)) {
            if (is == null) {
                try (InputStream isAlt = MainApp.class.getResourceAsStream("/assets" + imagePath)) {
                    if (isAlt == null) {
                        throw new RuntimeException("Resource not found: " + imagePath + " or /assets" + imagePath);
                    }
                    bgImage = new Image(isAlt);
                }
            } else {
                 bgImage = new Image(is);
            }
        } catch (Exception e) {
            System.err.println("Gagal memuat image: " + imagePath);
            e.printStackTrace();
            bgImage = new Image(GameConfig.SKY, 1920, 1080, false, true);
        }

        ImageView bgView = new ImageView(bgImage);
        bgView.setPreserveRatio(false); 
        bgView.fitWidthProperty().bind(scene.widthProperty());
        bgView.fitHeightProperty().bind(scene.heightProperty());

        // TOMBOL-TOMBOL HOMEPAGE
        Button playButton = new Button("PLAY");
        Button creditsButton = new Button("CREDITS"); // TAMBAHAN: Tombol Credits
        Button exitButton = new Button("EXIT");
        
        String buttonStyle = "-fx-font: 32px 'Arial'; " +
                             "-fx-background-color: #F0B030; " +
                             "-fx-text-fill: #202020; " +
                             "-fx-font-weight: bold; " +
                             "-fx-pref-width: 200px; " +
                             "-fx-pref-height: 50px; " +
                             "-fx-border-color: #202020; " +
                             "-fx-border-width: 3px;";
        
        playButton.setStyle(buttonStyle);
        creditsButton.setStyle(buttonStyle); // Style sama
        exitButton.setStyle(buttonStyle);

        // Layout tombol di VBox (tambah creditsButton di tengah)
        VBox buttonBox = new VBox(20, playButton, creditsButton, exitButton);
        buttonBox.setAlignment(Pos.CENTER);
        
        StackPane homepageRoot = new StackPane(bgView, buttonBox);
        homepageRoot.setAlignment(Pos.CENTER);

        // AKSI TOMBOL
        playButton.setOnAction(e -> {
            root.getChildren().remove(homepageRoot); 
            root.getChildren().add(gameCanvas); 

            gameCanvas.widthProperty().bind(scene.widthProperty());
            gameCanvas.heightProperty().bind(scene.heightProperty());

            gameCanvas.startGameLoop(); 
            gameCanvas.requestFocus(); 
        });

        creditsButton.setOnAction(e -> {
            showCreditsPopup(); // Panggil popup credits
        });

        exitButton.setOnAction(e -> {
            stage.close(); 
        });

        return homepageRoot;
    }

    // ============== POP-UP CREDITS (TAMBAHAN BARU) ==============
    private void showCreditsPopup() {
        // 1. Judul
        Label titleLabel = new Label("CREDITS");
        titleLabel.setFont(Font.font("Arial", 48));
        titleLabel.setTextFill(Color.web("#F0B030")); // Warna Emas
        titleLabel.setStyle("-fx-font-weight: bold; -fx-effect: dropshadow(one-pass-box, black, 8, 0.0, 2, 0);");

        // 2. Kontainer Isi Credits
        VBox contentBox = new VBox(25); // Spacing antar entry
        contentBox.setAlignment(Pos.CENTER);

        // Data Credits
        String[][] creditsData = {
            {"Zhidan “LenFrögg” Saaba", "The Supreme Leader of Bugs and Fixes — Project Lead, Lead Programmer, Lead Developer"},
            {"Alya Kinanti", "Art Wizard — Responsible for making everything actually look good"},
            {"Khumaira Azzahra Yarman", "Map Architect & Keeper of Reports — Ensures the world makes sense and the paperwork exists"},
            {"Bulan Hijarati", "Map Designer — Creates places where players get lost on purpose"},
            {"Rezikal Akbar", "Certified Team Burden™ — Provides moral chaos and emotional damage"}
        };

        for (String[] entry : creditsData) {
            VBox entryBox = new VBox(5);
            entryBox.setAlignment(Pos.CENTER);
            
            Label nameLabel = new Label(entry[0]);
            nameLabel.setFont(Font.font("Arial", 22));
            nameLabel.setTextFill(Color.WHITE);
            nameLabel.setStyle("-fx-font-weight: bold;");

            Label roleLabel = new Label(entry[1]);
            roleLabel.setFont(Font.font("Arial", 16));
            roleLabel.setTextFill(Color.LIGHTGRAY); // Warna agak redup untuk role
            roleLabel.setWrapText(true);
            roleLabel.setTextAlignment(TextAlignment.CENTER);

            entryBox.getChildren().addAll(nameLabel, roleLabel);
            contentBox.getChildren().add(entryBox);
        }

        // 3. Tombol Back
        Button backButton = new Button("BACK");
        String btnStyle = "-fx-font: 20px 'Arial'; " +
                          "-fx-background-color: #F0B030; " +
                          "-fx-text-fill: #202020; " +
                          "-fx-font-weight: bold; " +
                          "-fx-background-radius: 10; " +
                          "-fx-border-color: #202020; " +
                          "-fx-border-width: 2px;";
        backButton.setStyle(btnStyle);

        // 4. Layout Utama Popup
        VBox creditsLayout = new VBox(30, titleLabel, contentBox, backButton);
        creditsLayout.setAlignment(Pos.CENTER);
        creditsLayout.setStyle("-fx-background-color: rgba(0, 0, 0, 0.90); " + // Latar belakang sangat gelap
                               "-fx-background-radius: 20px; " +
                               "-fx-padding: 50px; " +
                               "-fx-border-color: #F0B030; " + // Border Emas
                               "-fx-border-width: 3px; " + 
                               "-fx-border-radius: 20px;");
        
        creditsLayout.setMaxSize(900, 700); // Ukuran cukup besar untuk memuat semua nama

        // Animasi Muncul
        creditsLayout.setScaleX(0);
        creditsLayout.setScaleY(0);
        ScaleTransition st = new ScaleTransition(Duration.seconds(0.5), creditsLayout);
        st.setToX(1);
        st.setToY(1);
        st.setInterpolator(Interpolator.EASE_OUT);
        st.play();

        // 5. Aksi Tombol Back
        backButton.setOnAction(e -> {
            root.getChildren().remove(creditsLayout);
        });

        root.getChildren().add(creditsLayout);
    }


    // ============== POP-UP TROFI (WIN) ==============
    private void showTrophyPopup() {
        Image trophyImage = null;
        String imagePath = "/trophy/trophy.png"; 
        try (InputStream is = MainApp.class.getResourceAsStream(imagePath)) {
             if (is == null) {
                try (InputStream isAlt = MainApp.class.getResourceAsStream("/assets" + imagePath)) {
                    if (isAlt == null) throw new RuntimeException("Trophy not found: " + imagePath);
                    trophyImage = new Image(isAlt);
                }
             } else {
                trophyImage = new Image(is);
             }
        } catch (Exception e) {
            System.err.println("Gagal memuat trophy.png: " + e.getMessage());
        }

        ImageView trophyView = new ImageView(trophyImage);
        trophyView.setFitWidth(250); 
        trophyView.setPreserveRatio(true);

        Label title = new Label("Congratulations!");
        title.setFont(Font.font("Arial", 48));
        title.setTextFill(Color.YELLOW);

        Label subtitle = new Label("You have completed the game!");
        subtitle.setFont(Font.font("Arial", 24));
        subtitle.setTextFill(Color.WHITE);

        Button restartButton = new Button("Restart Game");
        Button mainMenuButton = new Button("Main Menu");
        
        String buttonStyle = "-fx-font: 24px 'Arial'; " +
                             "-fx-background-color: #F0B030; " +
                             "-fx-text-fill: #202020; " +
                             "-fx-font-weight: bold; " +
                             "-fx-pref-width: 220px; " +
                             "-fx-pref-height: 50px; " +
                             "-fx-border-color: #202020; " +
                             "-fx-border-width: 3px;";
        restartButton.setStyle(buttonStyle);
        mainMenuButton.setStyle(buttonStyle);

        HBox buttonBox = new HBox(20, restartButton, mainMenuButton);
        buttonBox.setAlignment(Pos.CENTER);

        VBox trophyPopupBox = new VBox(30, trophyView, title, subtitle, buttonBox);
        trophyPopupBox.setAlignment(Pos.CENTER);
        trophyPopupBox.setStyle("-fx-background-color: rgba(0, 0, 0, 0.8); " +
                                "-fx-background-radius: 20px; " +
                                "-fx-padding: 40px;");
        trophyPopupBox.setMaxSize(600, 600); 

        restartButton.setOnAction(e -> {
            root.getChildren().remove(trophyPopupBox); 
            gameCanvas.resetToLevelOne(); 
            gameCanvas.requestFocus(); 
        });

        mainMenuButton.setOnAction(e -> {
            root.getChildren().remove(trophyPopupBox); 
            root.getChildren().remove(gameCanvas); 
            root.getChildren().add(homepagePane); 
        });

        root.getChildren().add(trophyPopupBox);
    }

    // ============== POP-UP TROLL (FAIL 3x) ==============
    private void showTrollPopup() {
        Label trollLabel = new Label("Skill issue kah maniezz?");
        trollLabel.setFont(Font.font("Comic Sans MS", 42)); 
        trollLabel.setTextFill(Color.web("#FF3B3B")); 
        trollLabel.setStyle("-fx-font-weight: bold; -fx-effect: dropshadow(one-pass-box, black, 8, 0.0, 2, 0);");

        Label subLabel = new Label("(Mati 3x beruntun nih yee...)");
        subLabel.setFont(Font.font("Arial", 18));
        subLabel.setTextFill(Color.WHITE); 

        Button closeButton = new Button("Maaf Bang Jago 😭");
        String btnStyle = "-fx-font: 20px 'Arial'; " +
                          "-fx-background-color: #F0B030; " + 
                          "-fx-text-fill: #202020; " + 
                          "-fx-font-weight: bold; " +
                          "-fx-background-radius: 10; " +
                          "-fx-border-color: #202020; " +
                          "-fx-border-width: 2px; " + 
                          "-fx-border-radius: 10;";
        closeButton.setStyle(btnStyle);

        VBox trollBox = new VBox(20, trollLabel, subLabel, closeButton);
        trollBox.setAlignment(Pos.CENTER);
        trollBox.setStyle("-fx-background-color: rgba(0, 0, 0, 0.85); " + 
                          "-fx-background-radius: 20px; " +
                          "-fx-padding: 40px; " +
                          "-fx-border-color: #FF3B3B; " + 
                          "-fx-border-width: 4px; " +
                          "-fx-border-radius: 20px;");
        
        trollBox.setMaxSize(800, 400); 

        trollBox.setScaleX(0);
        trollBox.setScaleY(0);
        ScaleTransition st = new ScaleTransition(Duration.seconds(0.8), trollBox); 
        st.setToX(1);
        st.setToY(1);
        st.setInterpolator(ELASTIC_OUT); 
        st.play();

        closeButton.setOnAction(e -> {
            root.getChildren().remove(trollBox);
            gameCanvas.requestFocus(); 
        });

        root.getChildren().add(trollBox);
    }

    public static void main(String[] args) { launch(args); }
}