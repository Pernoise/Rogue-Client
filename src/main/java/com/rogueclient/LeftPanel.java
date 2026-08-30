package com.rogueclient;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class LeftPanel extends VBox {

    private final AccountManager accountManager;
    private final SettingsManager settingsManager;
    private VBox accountWidget = new VBox(4);

    private StackPane avatarBox;
    private Label nameLabel;

    /** Non-logo icon tiles whose idle style needs to be re-applied when the theme changes (the logo tile keeps a fixed brand accent regardless of theme). */
    private final List<VBox> themedIconBoxes = new ArrayList<>();

    public LeftPanel(AccountManager accountManager, SettingsManager settingsManager) {
        this.accountManager  = accountManager;
        this.settingsManager = settingsManager;

        setPrefWidth(68);
        applyPanelStyle();
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(14, 0, 14, 0));
        setSpacing(6);

        VBox logo     = createIcon("icons/rogue-launch.png", "Rogue Client", true,  null,  false, false);
        VBox account  = createIcon("icons/user.png",          "Account",       false, null,  true,  false);
        VBox settings = createIcon("icons/gear.png",          "Settings",      false, null,  false, true);
        VBox logsBtn  = createIcon("icons/version.png",       "View Logs",     false, null,  false, false);
        logsBtn.setOnMouseClicked(e -> LogViewerWindow.open());
        VBox launcherFolder = createIcon("icons/folder.png",  "Open Launcher Folder", false, null, false, false);
        launcherFolder.setOnMouseClicked(e -> openLauncherFolder());

        VBox spacer = new VBox();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        accountWidget.setAlignment(Pos.CENTER);
        refreshAccountWidget();

        VBox discord = createIcon("icons/discord-logo.png", "Discord", false, "https://discord.com/invite/urHfdFdsbh", false, false);
        VBox website = createIcon("icons/globe.png", "Website", false, "https://rogueclient.rogueclient.abrdns.com/#home", false, false);
        getChildren().addAll(logo, account, settings, logsBtn, launcherFolder, spacer, accountWidget, discord, website);

        ThemeManager.addListener(() -> Platform.runLater(this::applyTheme));
    }

    private void applyPanelStyle() {
        setStyle(
            "-fx-background-color: " + ThemedStyles.panelBg() + "; -fx-border-color: " + ThemedStyles.border() + "; " +
            "-fx-border-width: 0 1 0 0; -fx-background-radius: 0 0 0 12; -fx-border-radius: 0 0 0 12;"
        );
    }

    /** Re-applies every themed style already on screen, without rebuilding widgets or re-fetching the avatar image. */
    private void applyTheme() {
        applyPanelStyle();
        for (VBox box : themedIconBoxes) {
            box.setStyle(iconIdleStyle());
        }
        if (avatarBox != null) {
            avatarBox.setStyle(iconIdleStyle());
        }
        if (nameLabel != null) {
            nameLabel.setStyle("-fx-text-fill: " + ThemedStyles.text() + "; -fx-font-size: 9; -fx-font-family: '" + ThemedStyles.font() + "';");
        }
    }

    public void refreshAccountWidget() {
        accountWidget.getChildren().clear();
        AccountManager.Account acc = accountManager.getSelected();
        if (acc == null) return;

        avatarBox = new StackPane();
        avatarBox.setPrefSize(34, 34);
        avatarBox.setMaxSize(34, 34);
        avatarBox.setStyle(iconIdleStyle());
        avatarBox.setOnMouseEntered(e -> avatarBox.setStyle(iconHoverStyle()));
        avatarBox.setOnMouseExited(e -> avatarBox.setStyle(iconIdleStyle()));
        avatarBox.setOnMouseClicked(e -> openSkinPanel());
        Tooltip.install(avatarBox, new Tooltip("Skin & Cape"));

        try {
            String avatarUrl = "https://skins.manacube.com/avatars/" + acc.uuid;
            Image avatar = new Image(avatarUrl, true);
            ImageView iv = new ImageView(avatar);
            iv.setFitWidth(28);
            iv.setFitHeight(28);
            iv.setPreserveRatio(true);
            iv.setSmooth(false); // avatar renders are tiny/pixel-art - bilinear upscaling just blurs them

            javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(28, 28);
            clip.setArcWidth(10);
            clip.setArcHeight(10);
            iv.setClip(clip);

            avatar.errorProperty().addListener((obs, was, isError) -> {
                if (isError) {
                    Platform.runLater(() -> {
                        avatarBox.getChildren().clear();
                        avatarBox.getChildren().add(fallbackAvatarLabel());
                    });
                }
            });

            avatarBox.getChildren().add(iv);
        } catch (Exception e) {
            System.out.println("Could not load avatar: " + e.getMessage());
            avatarBox.getChildren().add(fallbackAvatarLabel());
        }

        nameLabel = new Label(acc.username);
        nameLabel.setStyle("-fx-text-fill: " + ThemedStyles.text() + "; -fx-font-size: 9; -fx-font-family: '" + ThemedStyles.font() + "';");

        Platform.runLater(() -> accountWidget.getChildren().addAll(avatarBox, nameLabel));
    }

    private Label fallbackAvatarLabel() {
        Label fallback = new Label("?");
        fallback.setStyle("-fx-text-fill: #555555; -fx-font-size: 14; -fx-font-family: '" + ThemedStyles.font() + "';");
        return fallback;
    }

    /** Idle background for non-logo icon tiles (and the avatar tile), reusing the Button Hover color - the two happen to share the same default shade (#161616). */
    private String iconIdleStyle() {
        return "-fx-background-color: " + ThemedStyles.btnHoverBg() + "; -fx-background-radius: 8; -fx-cursor: hand;";
    }

    /** Hover background for non-logo icon tiles, reusing the Button Pressed/Active color. */
    private String iconHoverStyle() {
        return "-fx-background-color: " + ThemedStyles.btnPressedBg() + "; -fx-background-radius: 8; -fx-cursor: hand;";
    }

    private VBox createIcon(String resourcePath, String tooltip, boolean isLogo, String url, boolean isAuth, boolean isSettings) {
        VBox box = new VBox();
        box.setAlignment(Pos.CENTER);
        box.setPrefSize(42, 42);
        box.setMaxSize(42, 42);

        // The logo tile is a fixed brand accent and intentionally isn't themed, so the
        // Rogue Client mark stays recognizable no matter what the rest of the launcher looks like.
        String baseStyle = isLogo
            ? "-fx-background-color: #1a1a1a; -fx-background-radius: 8; -fx-border-color: #2a2a2a; -fx-border-radius: 8; -fx-border-width: 0.5;"
            : iconIdleStyle();
        box.setStyle(baseStyle);
        if (!isLogo) themedIconBoxes.add(box);

        try {
            Image img = new Image(getClass().getClassLoader().getResourceAsStream(resourcePath));
            ImageView iv = new ImageView(img);
            iv.setFitWidth(isLogo ? 24 : 20);
            iv.setFitHeight(isLogo ? 24 : 20);
            iv.setPreserveRatio(true);
            box.getChildren().add(iv);
        } catch (Exception e) {
            System.out.println("Could not load icon: " + resourcePath);
        }

        Tooltip.install(box, new Tooltip(tooltip));

        if (isLogo == false) {
            box.setOnMouseEntered(e -> box.setStyle(iconHoverStyle()));
            box.setOnMouseExited(e  -> box.setStyle(iconIdleStyle()));

            if (isAuth) {
                box.setOnMouseClicked(e -> openAuthPanel());
            } else if (isSettings) {
                box.setOnMouseClicked(e -> openSettingsPanel());
            } else if (url != null) {
                box.setOnMouseClicked(e -> BrowserUtil.open(url));
            }
        }

        return box;
    }

    private void openAuthPanel() {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);

        AuthPanel authPanel = new AuthPanel(accountManager);
        authPanel.setStyle("-fx-background-color: " + ThemedStyles.panelBg() + ";");

        RogueWindowChrome.apply(popup, "LOGIN", authPanel, 400, 500, this::refreshAccountWidget);
        popup.centerOnScreen();
        popup.showAndWait();
    }

    private void openSkinPanel() {
        SkinManagerWindow.open(accountManager);
    }

    private void openSettingsPanel() {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);

        SettingsPanel settingsPanel = new SettingsPanel(settingsManager);
        settingsPanel.setStyle("-fx-background-color: " + ThemedStyles.panelBg() + ";");

        RogueWindowChrome.apply(popup, "SETTINGS", settingsPanel, 560, 620, null);
        popup.centerOnScreen();
        popup.showAndWait();
    }

    private void openLauncherFolder() {
        try {
            Path path = Paths.get(System.getProperty("user.home"), ".rogueclient");
            if (Files.exists(path) == false) {
                Files.createDirectories(path);
            }

            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb;
            if (os.contains("win")) {
                pb = new ProcessBuilder("explorer", path.toString());
            } else if (os.contains("mac")) {
                pb = new ProcessBuilder("open", path.toString());
            } else {
                pb = new ProcessBuilder("xdg-open", path.toString());
            }
            pb.start();
        } catch (Exception e) {
            System.out.println("Could not open launcher folder: " + e.getMessage());
        }
    }
}
