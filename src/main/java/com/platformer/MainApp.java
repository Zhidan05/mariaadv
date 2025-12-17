package com.platformer;

import com.platformer.input.Input;
// TAMBAHAN: Import dari package skills
import com.platformer.skills.Skill;
import com.platformer.skills.SkillManager;
// Rarity tidak perlu diimport eksplisit jika diakses lewat Skill.getRarity() 
// tapi jika dipakai langsung, tambahkan import com.platformer.skills.Rarity;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
// ... (sisa import lainnya tetap sama) ...
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
import javafx.scene.text.TextAlignment; 
import javafx.stage.Stage;
import javafx.scene.input.KeyCombination;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Interpolator;
import javafx.util.Duration;

import java.io.InputStream;
import java.io.PrintWriter; 
import java.io.StringWriter; 
import java.net.URL;
import java.util.List; 
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.media.AudioClip; 

import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

public class MainApp extends Application {

    // ... (Isi MainApp tetap SAMA PERSIS dengan sebelumnya, 
    //      perubahannya hanya pada baris import di atas) ...
    // ... (Saya sertakan full code agar aman) ...

    private static final Interpolator ELASTIC_OUT = new Interpolator() {
        @Override
        protected double curve(double t) {
            if (t == 0) return 0;
            if (t == 1) return 1;
            double p = 0.3;
            return Math.pow(2, -10 * t) * Math.sin((t - p / 4) * (2 * Math.PI) / p) + 1;
        }
    };
    private static final AudioClip WINNER_SFX = SfxManager.load("winner.mp3");

    private MediaPlayer bgmPlayer;
    private GameCanvas gameCanvas;
    private Pane homepagePane;
    private StackPane root; 
    
    private VBox pauseMenuBox; 
    private boolean isPauseMenuVisible = false;

    @Override
    public void start(Stage stage) {
        initBgm(); 
        Input input = new Input();
        gameCanvas = new GameCanvas(input);
        root = new StackPane();
        Scene scene = new Scene(root); 

        homepagePane = createHomepage(stage, root, scene);
        homepagePane.setOpacity(0.0); 

        Pane splashPane = createSplashPane(root, scene);

        if (splashPane != null) {
            root.getChildren().add(splashPane);
        } else {
            root.getChildren().add(homepagePane);
            homepagePane.setOpacity(1.0); 
            if (bgmPlayer != null) bgmPlayer.play(); 
        }

        gameCanvas.setOnGameWon(() -> showTrophyPopup());
        gameCanvas.setOnTroll(() -> showTrollPopup());
        gameCanvas.setOnPauseToggle(() -> togglePauseMenu());
        gameCanvas.setOnLevelClear(() -> showSkillSelectionPopup());

        gameCanvas.attachSceneHandlers(scene);
        stage.setTitle("Platformer 2D JavaFX (TMX + TSX)");
        stage.setScene(scene);
        stage.setFullScreen(true);
        stage.setFullScreenExitHint(""); 
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH); 
        stage.show();
    }

    private void initBgm() {
        try {
            String bgmPath = "/bgm/backgroundmusic.mp3"; 
            URL bgmUrl = getClass().getResource(bgmPath); 
            if (bgmUrl != null) {
                Media bgm = new Media(bgmUrl.toExternalForm());
                bgmPlayer = new MediaPlayer(bgm);
                bgmPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                bgmPlayer.setVolume(0.4); 
            } else {
                System.err.println("File BGM tidak ditemukan: " + bgmPath);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private Pane createSplashPane(StackPane root, Scene scene) {
        try {
            String videoPath = "/video/splash.mp4";
            URL videoUrl = getClass().getResource(videoPath); 
            if (videoUrl == null) videoUrl = getClass().getResource("/assets" + videoPath); 
            if (videoUrl == null) return null; 
            Media media = new Media(videoUrl.toExternalForm());
            MediaPlayer splashPlayer = new MediaPlayer(media);
            MediaView mediaView = new MediaView(splashPlayer);
            mediaView.fitWidthProperty().bind(scene.widthProperty());
            mediaView.fitHeightProperty().bind(scene.heightProperty());
            mediaView.setPreserveRatio(false);
            StackPane splashRoot = new StackPane(mediaView);
            splashRoot.setStyle("-fx-background-color: black;");
            Runnable onFinish = () -> {
                FadeTransition fadeOut = new FadeTransition(Duration.seconds(1), splashRoot);
                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.0);
                fadeOut.setOnFinished(event -> {
                    splashPlayer.stop();
                    root.getChildren().remove(splashRoot); 
                    root.getChildren().add(homepagePane); 
                    if (bgmPlayer != null) bgmPlayer.play(); 
                    FadeTransition fadeIn = new FadeTransition(Duration.seconds(1.5), homepagePane);
                    fadeIn.setFromValue(0.0);
                    fadeIn.setToValue(1.0);
                    fadeIn.play();
                });
                fadeOut.play();
            };
            splashPlayer.setOnEndOfMedia(onFinish); 
            splashRoot.setOnMouseClicked(e -> {
                splashRoot.setOnMouseClicked(null);
                splashPlayer.setOnEndOfMedia(null);
                onFinish.run(); 
            });
            splashPlayer.play();
            return splashRoot;
        } catch (Exception e) { return null; }
    }

    private Pane createHomepage(Stage stage, StackPane root, Scene scene) {
        Image bgImage = null;
        String imagePath = "/homepage/homepage.png"; 
        try (InputStream is = MainApp.class.getResourceAsStream(imagePath)) {
            if (is != null) bgImage = new Image(is);
            else { try (InputStream isAlt = MainApp.class.getResourceAsStream("/assets" + imagePath)) { if (isAlt != null) bgImage = new Image(isAlt); } }
        } catch (Exception e) {}
        StackPane homepageRoot = new StackPane();
        homepageRoot.setAlignment(Pos.CENTER);
        if (bgImage != null) {
            ImageView bgView = new ImageView(bgImage);
            bgView.setPreserveRatio(false); 
            bgView.fitWidthProperty().bind(scene.widthProperty());
            bgView.fitHeightProperty().bind(scene.heightProperty());
            homepageRoot.getChildren().add(bgView);
        } else { homepageRoot.setStyle("-fx-background-color: #476E83;"); }
        Button playButton = new Button("PLAY");
        Button creditsButton = new Button("CREDITS");
        Button exitButton = new Button("EXIT");
        String buttonStyle = "-fx-font: 32px 'Arial'; -fx-background-color: #F0B030; -fx-text-fill: #202020; -fx-font-weight: bold; -fx-pref-width: 200px; -fx-pref-height: 50px; -fx-border-color: #202020; -fx-border-width: 3px;";
        playButton.setStyle(buttonStyle);
        creditsButton.setStyle(buttonStyle);
        exitButton.setStyle(buttonStyle);
        VBox buttonBox = new VBox(20, playButton, creditsButton, exitButton);
        buttonBox.setAlignment(Pos.CENTER);
        homepageRoot.getChildren().add(buttonBox);
        playButton.setOnAction(e -> {
            root.getChildren().remove(homepageRoot);
            if (!root.getChildren().contains(gameCanvas)) root.getChildren().add(gameCanvas);
            try { gameCanvas.resetToLevelOne(); } catch (Exception ex) {
                ex.printStackTrace();
                root.getChildren().remove(gameCanvas);
                root.getChildren().add(homepagePane);
                showErrorPopup("Error", "Gagal memuat Level 1", ex);
                return; 
            }
            gameCanvas.setPaused(false); 
            gameCanvas.widthProperty().bind(scene.widthProperty());
            gameCanvas.heightProperty().bind(scene.heightProperty());
            gameCanvas.startGameLoop();
            gameCanvas.requestFocus();
        });
        creditsButton.setOnAction(e -> showCreditsPopup());
        exitButton.setOnAction(e -> stage.close());
        return homepageRoot;
    }

    private void showCreditsPopup() {
        gameCanvas.setPaused(true);
         Label titleLabel = new Label("CREDITS");
         titleLabel.setFont(Font.font("Arial", 48));
         titleLabel.setTextFill(Color.web("#F0B030")); 
         titleLabel.setStyle("-fx-font-weight: bold; -fx-effect: dropshadow(one-pass-box, black, 8, 0.0, 2, 0);");
         VBox contentBox = new VBox(25); 
         contentBox.setAlignment(Pos.CENTER);
         String[][] creditsData = {
             {"Zhidan “LenFrögg” Saaba", "Project Lead, Lead Programmer"},
             {"Alya Kinanti", "Art Wizard"},
             {"Khumaira Azzahra Yarman", "Map Architect"},
             {"Bulan Hijarati", "Map Designer"},
             {"Rezikal Akbar", "Certified Team Burden™"}
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
             roleLabel.setTextFill(Color.LIGHTGRAY); 
             entryBox.getChildren().addAll(nameLabel, roleLabel);
             contentBox.getChildren().add(entryBox);
         }
         Button backButton = new Button("BACK");
         String btnStyle = "-fx-font: 20px 'Arial'; -fx-background-color: #F0B030; -fx-text-fill: #202020; -fx-font-weight: bold; -fx-background-radius: 10; -fx-border-color: #202020; -fx-border-width: 2px;";
         backButton.setStyle(btnStyle);
         VBox creditsLayout = new VBox(30, titleLabel, contentBox, backButton);
         creditsLayout.setAlignment(Pos.CENTER);
         creditsLayout.setStyle("-fx-background-color: rgba(0, 0, 0, 0.90); -fx-background-radius: 20px; -fx-padding: 50px; -fx-border-color: #F0B030; -fx-border-width: 3px; -fx-border-radius: 20px;");
         creditsLayout.setMaxSize(900, 700); 
         creditsLayout.setScaleX(0);
         creditsLayout.setScaleY(0);
         ScaleTransition st = new ScaleTransition(Duration.seconds(0.5), creditsLayout);
         st.setToX(1);
         st.setToY(1);
         st.setInterpolator(Interpolator.EASE_OUT);
         st.play();
         backButton.setOnAction(e -> {
            root.getChildren().remove(creditsLayout);
            if (root.getChildren().contains(gameCanvas) && isPauseMenuVisible == false) {
                 gameCanvas.setPaused(false);
                 gameCanvas.requestFocus();
            }
         });
         root.getChildren().add(creditsLayout);
    }

    private void showErrorPopup(String title, String header, Exception ex) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        String exceptionText = sw.toString();
        TextArea textArea = new TextArea(exceptionText);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);
        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(textArea, 0, 0);
        alert.getDialogPane().setExpandableContent(expContent);
        alert.getDialogPane().setExpanded(true); 
        alert.showAndWait();
    }

    private void showSkillSelectionPopup() {
        gameCanvas.setPaused(true);
        Label titleLabel = new Label("LEVEL COMPLETE!\nCHOOSE YOUR REWARD");
        titleLabel.setFont(Font.font("Arial", 36));
        titleLabel.setTextFill(Color.web("#F0B030")); 
        titleLabel.setStyle("-fx-font-weight: bold; -fx-effect: dropshadow(one-pass-box, black, 8, 0.0, 2, 0);");
        titleLabel.setTextAlignment(TextAlignment.CENTER);

        List<Skill> randomSkills = SkillManager.getRandomSkills();
        HBox cardsContainer = new HBox(20);
        cardsContainer.setAlignment(Pos.CENTER);

        for (Skill skill : randomSkills) {
            VBox card = createSkillCard(skill, () -> {
                skill.apply(gameCanvas);
                proceedToNextLevel();
            });
            cardsContainer.getChildren().add(card);
        }

        VBox popupBox = new VBox(30, titleLabel, cardsContainer);
        popupBox.setAlignment(Pos.CENTER);
        popupBox.setStyle("-fx-background-color: rgba(0, 0, 0, 0.85); -fx-background-radius: 20px; -fx-padding: 50px; -fx-border-color: #F0B030; -fx-border-width: 4px; -fx-border-radius: 20px;");
        popupBox.setMaxSize(900, 500);
        popupBox.setScaleX(0);
        popupBox.setScaleY(0);
        ScaleTransition st = new ScaleTransition(Duration.seconds(0.5), popupBox);
        st.setToX(1);
        st.setToY(1);
        st.setInterpolator(ELASTIC_OUT);
        st.play();
        root.getChildren().add(popupBox);
        popupBox.setUserData("skillPopup"); 
    }

    private VBox createSkillCard(Skill skill, Runnable action) {
        Label t = new Label(skill.getName());
        t.setFont(Font.font("Arial", 20));
        t.setTextFill(skill.getRarity().getColor()); 
        t.setStyle("-fx-font-weight: bold;");
        t.setWrapText(true);
        t.setTextAlignment(TextAlignment.CENTER);
        Label r = new Label(skill.getRarity().getLabel());
        r.setFont(Font.font("Arial", 14));
        r.setTextFill(skill.getRarity().getColor());
        r.setStyle("-fx-font-style: italic;");
        Label d = new Label(skill.getDescription());
        d.setFont(Font.font("Arial", 14));
        d.setTextFill(Color.LIGHTGRAY);
        d.setWrapText(true);
        d.setTextAlignment(TextAlignment.CENTER);
        Button btn = new Button("PILIH");
        String btnStyle = "-fx-font: 16px 'Arial'; -fx-background-color: " + toHex(skill.getRarity().getColor()) + "; -fx-text-fill: #202020; -fx-font-weight: bold;";
        btn.setStyle(btnStyle);
        btn.setOnAction(e -> action.run());
        VBox card = new VBox(10, t, r, d, btn);
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-background-color: rgba(50, 50, 50, 0.9); -fx-border-color: " + toHex(skill.getRarity().getColor()) + "; -fx-border-width: 2; -fx-background-radius: 10; -fx-border-radius: 10; -fx-padding: 15;");
        card.setPrefSize(220, 280);
        return card;
    }

    private String toHex(Color c) {
        return String.format("#%02X%02X%02X", (int) (c.getRed() * 255), (int) (c.getGreen() * 255), (int) (c.getBlue() * 255));
    }

    private void proceedToNextLevel() {
        root.getChildren().removeIf(node -> "skillPopup".equals(node.getUserData()));
        try {
            gameCanvas.startNextLevel(); 
            gameCanvas.setPaused(false); 
            gameCanvas.requestFocus();
        } catch (Exception ex) {
            ex.printStackTrace();
            gameCanvas.setPaused(false); 
            root.getChildren().remove(gameCanvas); 
            root.getChildren().add(homepagePane); 
            showErrorPopup("Error", "Gagal memuat level", ex);
        }
    }

    // ... (showTrophyPopup, showTrollPopup, togglePauseMenu, dll tetap SAMA) ...
    private void showTrophyPopup() {
        gameCanvas.setPaused(true);
        if (WINNER_SFX != null) WINNER_SFX.play();
        Image trophyImage = null;
        String imagePath = "/trophy/trophy.png"; 
        try (InputStream is = MainApp.class.getResourceAsStream(imagePath)) { if (is != null) trophyImage = new Image(is); } catch (Exception e) {}
        ImageView trophyView = new ImageView();
        if (trophyImage != null) { trophyView.setImage(trophyImage); trophyView.setFitWidth(250); trophyView.setPreserveRatio(true); }
        Label title = new Label("Congratulations!"); title.setFont(Font.font("Arial", 48)); title.setTextFill(Color.YELLOW);
        Label subtitle = new Label("You have completed the game!"); subtitle.setFont(Font.font("Arial", 24)); subtitle.setTextFill(Color.WHITE);
        Button restartButton = new Button("Restart Game"); Button mainMenuButton = new Button("Main Menu");
        String buttonStyle = "-fx-font: 24px 'Arial'; -fx-background-color: #F0B030; -fx-text-fill: #202020; -fx-font-weight: bold; -fx-pref-width: 220px; -fx-pref-height: 50px; -fx-border-color: #202020; -fx-border-width: 3px;";
        restartButton.setStyle(buttonStyle); mainMenuButton.setStyle(buttonStyle);
        HBox buttonBox = new HBox(20, restartButton, mainMenuButton); buttonBox.setAlignment(Pos.CENTER);
        VBox trophyPopupBox = new VBox(30, trophyView, title, subtitle, buttonBox);
        trophyPopupBox.setAlignment(Pos.CENTER); trophyPopupBox.setStyle("-fx-background-color: rgba(0, 0, 0, 0.8); -fx-background-radius: 20px; -fx-padding: 40px;");
        trophyPopupBox.setMaxSize(600, 600); 
        restartButton.setOnAction(e -> { if (WINNER_SFX != null) WINNER_SFX.stop(); root.getChildren().remove(trophyPopupBox); gameCanvas.resetToLevelOne(); gameCanvas.requestFocus(); });
        mainMenuButton.setOnAction(e -> { if (WINNER_SFX != null) WINNER_SFX.stop(); gameCanvas.setPaused(false); root.getChildren().remove(trophyPopupBox); root.getChildren().remove(gameCanvas); root.getChildren().add(homepagePane); });
        root.getChildren().add(trophyPopupBox);
    }
    private void showTrollPopup() {
        gameCanvas.setPaused(true);
        Label trollLabel = new Label("Skill issue kah maniezz?"); trollLabel.setFont(Font.font("Comic Sans MS", 42)); trollLabel.setTextFill(Color.web("#FF3B3B")); trollLabel.setStyle("-fx-font-weight: bold; -fx-effect: dropshadow(one-pass-box, black, 8, 0.0, 2, 0);");
        Label subLabel = new Label("(Mati 3x beruntun nih yee...)"); subLabel.setFont(Font.font("Arial", 18)); subLabel.setTextFill(Color.WHITE); 
        Button closeButton = new Button("Maaf Bang Jago 😭"); String btnStyle = "-fx-font: 20px 'Arial'; -fx-background-color: #F0B030; -fx-text-fill: #202020; -fx-font-weight: bold; -fx-background-radius: 10; -fx-border-color: #202020; -fx-border-width: 2px; -fx-border-radius: 10;"; closeButton.setStyle(btnStyle);
        VBox trollBox = new VBox(20, trollLabel, subLabel, closeButton); trollBox.setAlignment(Pos.CENTER); trollBox.setStyle("-fx-background-color: rgba(0, 0, 0, 0.85); -fx-background-radius: 20px; -fx-padding: 40px; -fx-border-color: #FF3B3B; -fx-border-width: 4px; -fx-border-radius: 20px;");
        trollBox.setMaxSize(800, 400); trollBox.setScaleX(0); trollBox.setScaleY(0); ScaleTransition st = new ScaleTransition(Duration.seconds(0.8), trollBox); st.setToX(1); st.setToY(1); st.setInterpolator(ELASTIC_OUT); st.play();
        closeButton.setOnAction(e -> { gameCanvas.stopLoseSfx(); root.getChildren().remove(trollBox); gameCanvas.setPaused(false); gameCanvas.requestFocus(); });
        root.getChildren().add(trollBox);
    }
    private void togglePauseMenu() { if (isPauseMenuVisible) hidePauseMenu(); else showPauseMenu(); }
    private void showPauseMenu() { if (pauseMenuBox == null) createPauseMenuLayout(); root.getChildren().add(pauseMenuBox); isPauseMenuVisible = true; gameCanvas.setPaused(true); pauseMenuBox.setScaleX(0); pauseMenuBox.setScaleY(0); ScaleTransition st = new ScaleTransition(Duration.seconds(0.3), pauseMenuBox); st.setToX(1); st.setToY(1); st.setInterpolator(Interpolator.EASE_OUT); st.play(); }
    private void hidePauseMenu() { if (pauseMenuBox != null && root.getChildren().contains(pauseMenuBox)) { root.getChildren().remove(pauseMenuBox); isPauseMenuVisible = false; gameCanvas.setPaused(false); gameCanvas.requestFocus(); } }
    private void createPauseMenuLayout() {
        Label titleLabel = new Label("PAUSED"); titleLabel.setFont(Font.font("Arial", 48)); titleLabel.setTextFill(Color.WHITE); titleLabel.setStyle("-fx-font-weight: bold; -fx-effect: dropshadow(one-pass-box, black, 8, 0.0, 2, 0);");
        Button resumeButton = new Button("RESUME"); Button restartButton = new Button("RESTART LEVEL"); Button mainMenuButton = new Button("MAIN MENU");
        String btnStyle = "-fx-font: 20px 'Arial'; -fx-background-color: #F0B030; -fx-text-fill: #202020; -fx-font-weight: bold; -fx-pref-width: 250px; -fx-pref-height: 45px; -fx-border-color: #202020; -fx-border-width: 2px; -fx-border-radius: 5px; -fx-background-radius: 5px;";
        resumeButton.setStyle(btnStyle); restartButton.setStyle(btnStyle); mainMenuButton.setStyle(btnStyle);
        resumeButton.setOnAction(e -> hidePauseMenu()); restartButton.setOnAction(e -> { hidePauseMenu(); gameCanvas.resetLevel(); }); mainMenuButton.setOnAction(e -> { gameCanvas.stopLoseSfx(); hidePauseMenu(); root.getChildren().remove(gameCanvas); root.getChildren().add(homepagePane); });
        VBox layout = new VBox(20, titleLabel, resumeButton, restartButton, mainMenuButton); layout.setAlignment(Pos.CENTER); layout.setStyle("-fx-background-color: rgba(0, 0, 0, 0.85); -fx-background-radius: 20px; -fx-padding: 50px; -fx-border-color: #F0B030; -fx-border-width: 4px; -fx-border-radius: 20px;"); layout.setMaxSize(400, 400);
        pauseMenuBox = layout;
    }

    public static void main(String[] args) { launch(args); }
}