package com.rogueclient;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class SplashScreen {

    private final Stage stage;
    private final Runnable onFinished;

    public SplashScreen(Runnable onFinished) {
        this.onFinished = onFinished;
        this.stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setResizable(false);

        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(60, 80, 32, 80));
        root.setStyle(
            "-fx-background-color: " + ThemedStyles.splashBg() + ";" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: " + ThemedStyles.border() + ";" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 12;"
        );

        ImageView logo = new ImageView();
        try {
            Image img = new Image(getClass().getClassLoader().getResourceAsStream("icons/rogue-launch.png"));
            logo.setImage(img);
            logo.setFitWidth(48);
            logo.setFitHeight(48);
            logo.setPreserveRatio(true);
        } catch (Exception e) {
            System.out.println("Could not load splash logo");
        }

        Label title = new Label("Rogue Client");
        title.setStyle("-fx-text-fill: " + ThemedStyles.text() + "; -fx-font-size: 22; -fx-font-family: '" + ThemedStyles.font() + "'; -fx-font-weight: bold; -fx-opacity: 0.88;");

        Label version = new Label("v1.0");
        version.setStyle("-fx-text-fill: " + ThemedStyles.text() + "; -fx-font-size: 10; -fx-font-family: '" + ThemedStyles.font() + "';");

        Label status = new Label("Initializing...");
        status.setStyle("-fx-text-fill: " + ThemedStyles.text() + "; -fx-font-size: 10; -fx-font-family: '" + ThemedStyles.font() + "';");

        Label website = new Label("rogue.pernoise.workers.dev");
        website.setStyle("-fx-text-fill: " + ThemedStyles.text() + "; -fx-font-size: 9; -fx-font-family: '" + ThemedStyles.font() + "';");

        root.getChildren().addAll(logo, title, version, status, website);

        Scene scene = new Scene(root, 480, 220);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);

        try {
            Image icon = new Image(getClass().getClassLoader().getResourceAsStream("icons/rogue-launch.png"));
            stage.getIcons().add(icon);
        } catch (Exception ignored) {}

        stage.centerOnScreen();

        String[][] steps = {
            {"0.10", "Initializing..."},
            {"0.30", "Loading assets..."},
            {"0.55", "Checking for updates..."},
            {"0.75", "Preparing launcher..."},
            {"0.95", "Almost ready..."},
            {"1.00", "Welcome back."}
        };

        Timeline timeline = new Timeline();
        for (int i = 0; i < steps.length; i++) {
            final String text  = steps[i][1];
            final boolean last = (i == steps.length - 1);

            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(600 * (i + 1)), e -> {
                status.setText(text);
                if (last) {
                    new Timeline(new KeyFrame(Duration.millis(400), ev -> {
                        stage.close();
                        onFinished.run();
                    })).play();
                }
            }));
        }

        stage.setOnShown(e -> timeline.play());
    }

    public void show() {
        stage.show();
    }
}
