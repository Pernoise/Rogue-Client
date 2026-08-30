package com.rogueclient;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Shared "customize + live preview" popup used by every Style settings row (Main Background,
 * Left Panel, Center Panel, News Panel, Buttons, Text Colors, Fonts). Edits happen on a private
 * draft copy of the theme (see ThemeManager.copy()); nothing touches the real, applied theme
 * until Save is pressed, matching the "nothing is permanent until Save/Apply" requirement.
 *
 * The caller supplies a ControlsBuilder that lays out whatever fields that category needs
 * (color pickers, font pickers, a size slider, ...); this class handles the modal chrome,
 * the live preview mockup of the launcher, and the Save/Cancel/Reset buttons.
 */
public class ThemeEditorWindow {

    public interface ControlsBuilder {
        /** Builds the editor's control column. Call onChange.run() after every edit so the preview and Save button reflect it live. */
        Node build(ThemeManager draft, Runnable onChange);
    }

    public static void open(String title, ControlsBuilder controlsBuilder) {
        ThemeManager draft = ThemeManager.get().copy();

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);

        StackPane previewHolder = new StackPane();
        previewHolder.setPadding(new Insets(4));
        previewHolder.setAlignment(Pos.CENTER);

        Runnable[] refresh = new Runnable[1];
        refresh[0] = () -> previewHolder.getChildren().setAll(buildPreview(draft));
        refresh[0].run();

        Label previewLabel = sectionLabel("Live Preview");
        VBox previewBox = new VBox(8, previewLabel, previewHolder);

        ScrollPane controlsScroll = new ScrollPane();
        controlsScroll.getStyleClass().add("rocket-scroll");
        controlsScroll.setFitToWidth(true);
        controlsScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent;");
        controlsScroll.setPrefHeight(260);
        controlsScroll.setContent(controlsBuilder.build(draft, () -> refresh[0].run()));

        Button saveBtn = new Button("Save");
        saveBtn.setStyle(primaryBtnStyle());
        saveBtn.setOnMouseEntered(e -> saveBtn.setStyle(primaryBtnHoverStyle()));
        saveBtn.setOnMouseExited(e -> saveBtn.setStyle(primaryBtnStyle()));
        saveBtn.setOnAction(e -> {
            ThemeManager.get().applyDraft(draft);
            stage.close();
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle(secondaryBtnStyle());
        cancelBtn.setOnMouseEntered(e -> cancelBtn.setStyle(secondaryBtnHoverStyle()));
        cancelBtn.setOnMouseExited(e -> cancelBtn.setStyle(secondaryBtnStyle()));
        cancelBtn.setOnAction(e -> stage.close());

        Button resetBtn = new Button("Reset to Default");
        resetBtn.setStyle(secondaryBtnStyle());
        resetBtn.setOnMouseEntered(e -> resetBtn.setStyle(secondaryBtnHoverStyle()));
        resetBtn.setOnMouseExited(e -> resetBtn.setStyle(secondaryBtnStyle()));
        resetBtn.setOnAction(e -> {
            // Reset to Default restores the launcher's entire appearance, not just this
            // category, per spec - it's applied immediately (not gated behind Save) so
            // the main window and any other open windows revert right away too.
            ThemeManager.get().resetToDefaults();
            draft.copyFieldsFrom(ThemeManager.get());
            controlsScroll.setContent(controlsBuilder.build(draft, () -> refresh[0].run()));
            refresh[0].run();
        });

        HBox btnRow = new HBox(8, resetBtn, spacer(), cancelBtn, saveBtn);
        btnRow.setAlignment(Pos.CENTER_LEFT);

        VBox root = new VBox(18, previewBox, sectionLabel("Customize"), controlsScroll, btnRow);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: " + ThemedStyles.panelBg() + ";");

        RogueWindowChrome.apply(stage, title.toUpperCase(), root, 480, 620, null);
        stage.centerOnScreen();
        stage.showAndWait();
    }

    private static Region spacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    /** Renders a compact mockup of the launcher (left rail / center / news) styled entirely off the draft theme, so every change is visible immediately. */
    private static Region buildPreview(ThemeManager draft) {
        String font = ThemedStyles.font();

        VBox leftIcons = new VBox(6);
        leftIcons.setAlignment(Pos.TOP_CENTER);
        leftIcons.setPadding(new Insets(10, 0, 0, 0));
        for (int i = 0; i < 3; i++) {
            Region dot = new Region();
            dot.setPrefSize(18, 18);
            dot.setMaxSize(18, 18);
            dot.setStyle(
                "-fx-background-color: " + draft.buttonBackground + "; -fx-background-radius: 5; " +
                "-fx-border-color: " + draft.panelBorderColor + "; -fx-border-radius: 5;"
            );
            leftIcons.getChildren().add(dot);
        }
        VBox left = new VBox(leftIcons);
        left.setPrefWidth(48);
        left.setMinWidth(48);
        left.setStyle(
            "-fx-background-color: " + draft.leftPanelBackground + "; " +
            "-fx-border-color: " + draft.panelBorderColor + "; -fx-border-width: 0 1 0 0;"
        );

        Label headline = new Label("ROGUE CLIENT");
        double previewHeadlineSize = Math.max(13, Math.min(24, draft.headlineFontSize * 0.32));
        headline.setStyle(
            "-fx-text-fill: " + draft.headlineTextColor + "; -fx-font-size: " + previewHeadlineSize + "; " +
            "-fx-font-family: '" + font + "'; -fx-font-weight: bold; -fx-opacity: 0.9;"
        );

        Label playLabel = new Label("Play");
        playLabel.setStyle("-fx-text-fill: " + draft.buttonTextColor + "; -fx-font-family: '" + font + "'; -fx-font-size: 11; -fx-font-weight: bold;");
        StackPane playBtn = new StackPane(playLabel);
        playBtn.setPadding(new Insets(8, 22, 8, 22));
        playBtn.setStyle(
            "-fx-background-color: " + draft.buttonBackground + "; -fx-background-radius: 6; " +
            "-fx-border-color: " + draft.panelBorderColor + "; -fx-border-radius: 6;"
        );

        VBox center = new VBox(12, headline, playBtn);
        center.setAlignment(Pos.CENTER);
        center.setStyle("-fx-background-color: " + draft.centerPanelBackground + ";");
        HBox.setHgrow(center, Priority.ALWAYS);

        Label newsTitle = new Label("NEWS");
        newsTitle.setStyle("-fx-text-fill: " + draft.textColor + "; -fx-font-size: 9; -fx-font-family: '" + font + "';");

        Label cardDate = new Label("today");
        cardDate.setStyle("-fx-text-fill: " + draft.secondaryTextColor + "; -fx-font-size: 8; -fx-font-family: '" + font + "';");
        Label cardText = new Label("Sample update text");
        cardText.setStyle("-fx-text-fill: " + draft.textColor + "; -fx-font-size: 9; -fx-font-family: '" + font + "';");
        VBox card = new VBox(3, cardDate, cardText);
        card.setPadding(new Insets(7));
        card.setStyle("-fx-border-color: " + draft.panelBorderColor + "; -fx-border-radius: 5; -fx-background-radius: 5;");

        VBox news = new VBox(8, newsTitle, card);
        news.setPrefWidth(112);
        news.setMinWidth(112);
        news.setPadding(new Insets(10));
        news.setStyle(
            "-fx-background-color: " + draft.newsPanelBackground + "; " +
            "-fx-border-color: " + draft.panelBorderColor + "; -fx-border-width: 0 0 0 1;"
        );

        HBox launcher = new HBox(left, center, news);
        launcher.setPrefSize(420, 210);
        launcher.setMaxSize(420, 210);
        launcher.setStyle(
            "-fx-background-color: " + draft.mainBackground + "; " +
            "-fx-border-color: " + draft.panelBorderColor + "; -fx-border-width: 1; " +
            "-fx-background-radius: 8; -fx-border-radius: 8;"
        );
        return launcher;
    }

    // ---- Reusable field builders, used by StylePanel to assemble each category's controls ----

    /** A single "Label | color swatch picker | hex field" row bound to one draft color field. */
    public static HBox colorRow(String label, ThemeManager draft,
                                 Function<ThemeManager, String> getter,
                                 BiConsumer<ThemeManager, String> setter,
                                 Runnable onChange) {
        Label l = new Label(label);
        l.setStyle("-fx-text-fill: " + ThemedStyles.text() + "; -fx-font-family: '" + ThemedStyles.font() + "'; -fx-font-size: 12;");
        l.setPrefWidth(150);
        l.setWrapText(true);

        String initial = safeHex(getter.apply(draft));

        ColorPicker picker = new ColorPicker(colorOf(initial));
        picker.setStyle("-fx-font-family: '" + ThemedStyles.font() + "';");

        TextField hex = new TextField(initial);
        hex.setPrefWidth(90);
        hex.setStyle(
            "-fx-background-color: #141414; -fx-text-fill: #ffffff; -fx-font-family: '" + ThemedStyles.font() + "'; " +
            "-fx-font-size: 12; -fx-border-color: #222222; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 6 10;"
        );

        final boolean[] syncing = {false};
        picker.valueProperty().addListener((obs, o, n) -> {
            if (syncing[0]) return;
            syncing[0] = true;
            String h = toHex(n);
            hex.setText(h);
            setter.accept(draft, h);
            onChange.run();
            syncing[0] = false;
        });
        hex.textProperty().addListener((obs, o, n) -> {
            if (syncing[0]) return;
            if (!isValidHex(n)) return;
            syncing[0] = true;
            setter.accept(draft, n);
            try { picker.setValue(Color.web(n)); } catch (Exception ignored) {}
            onChange.run();
            syncing[0] = false;
        });

        HBox row = new HBox(10, l, picker, hex);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static boolean isValidHex(String s) {
        return s != null && s.matches("^#[0-9a-fA-F]{6}$");
    }

    private static String safeHex(String s) {
        return isValidHex(s) ? s : "#ffffff";
    }

    private static Color colorOf(String hex) {
        try {
            return Color.web(hex);
        } catch (Exception e) {
            return Color.WHITE;
        }
    }

    private static String toHex(Color c) {
        return String.format("#%02x%02x%02x",
            (int) Math.round(c.getRed() * 255),
            (int) Math.round(c.getGreen() * 255),
            (int) Math.round(c.getBlue() * 255));
    }

    static Label sectionLabel(String text) {
        Label l = new Label(text.toUpperCase());
        l.setStyle("-fx-text-fill: " + ThemedStyles.text() + "; -fx-font-size: 9; -fx-font-family: '" + ThemedStyles.font() + "';");
        return l;
    }

    static String primaryBtnStyle() {
        return "-fx-background-color: #ffffff; -fx-text-fill: #000000; -fx-font-family: '" + ThemedStyles.font() + "'; " +
               "-fx-font-size: 12; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 9 20; -fx-background-radius: 6;";
    }

    static String primaryBtnHoverStyle() {
        return "-fx-background-color: #e6e6e6; -fx-text-fill: #000000; -fx-font-family: '" + ThemedStyles.font() + "'; " +
               "-fx-font-size: 12; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 9 20; -fx-background-radius: 6;";
    }

    static String secondaryBtnStyle() {
        return "-fx-background-color: #141414; -fx-text-fill: #ffffff; -fx-font-family: '" + ThemedStyles.font() + "'; " +
               "-fx-font-size: 12; -fx-border-color: #222222; -fx-border-radius: 6; -fx-background-radius: 6; " +
               "-fx-cursor: hand; -fx-padding: 9 16;";
    }

    static String secondaryBtnHoverStyle() {
        return "-fx-background-color: #1e1e1e; -fx-text-fill: #ffffff; -fx-font-family: '" + ThemedStyles.font() + "'; " +
               "-fx-font-size: 12; -fx-border-color: #2a2a2a; -fx-border-radius: 6; -fx-background-radius: 6; " +
               "-fx-cursor: hand; -fx-padding: 9 16;";
    }
}
