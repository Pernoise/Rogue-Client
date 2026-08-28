package com.rogueclient;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The "Color Selection Preview Window" required by the customization spec.
 *
 * Opens as a modal popup showing a live, scaled-down replica of the Rogue Client launcher
 * (left panel / center panel / news panel / buttons / headline) that updates immediately as
 * colors, fonts, or headline size are changed. Nothing is written to disk or applied to the
 * real running launcher until "Save / Apply" is pressed - Cancel discards the whole session,
 * Reset to Default snaps every field (and the preview) back to the baked-in defaults.
 */
public class ThemeCustomizationWindow {

    public static void open() {
        ThemeManager live  = ThemeManager.getInstance();
        ThemeManager draft = live.copy(); // nothing touches `live` until Save is pressed

        FontManager.ensureFoldersAndLoad();

        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #0f0f0f;");

        HBox body = new HBox(20);
        body.setPadding(new Insets(20));

        VBox controls = buildControls(draft);
        ScrollPane controlsScroll = new ScrollPane(controls);
        controlsScroll.getStyleClass().add("rogue-scroll");
        controlsScroll.setFitToWidth(true);
        controlsScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent;");
        controlsScroll.setPrefWidth(300);
        controlsScroll.setPrefHeight(440);

        VBox previewBox = buildPreview(draft);
        previewBox.setPrefWidth(420);

        HBox.setHgrow(controlsScroll, Priority.NEVER);
        HBox.setHgrow(previewBox, Priority.ALWAYS);
        body.getChildren().addAll(controlsScroll, previewBox);

        // Re-render the preview any time a draft field changes (simplest reliable approach:
        // rebuild the whole preview node rather than trying to patch individual styles).
        Runnable rebuildPreview = () -> {
            VBox fresh = buildPreview(draft);
            fresh.setPrefWidth(420);
            HBox.setHgrow(fresh, Priority.ALWAYS);
            body.getChildren().set(1, fresh);
        };
        controls.getProperties().put("onDraftChanged", rebuildPreview);

        // Buttons
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(0, 20, 20, 20));

        Button resetBtn = new Button("Reset to Default");
        resetBtn.setStyle(secondaryBtnStyle());
        resetBtn.setOnAction(e -> {
            draft.resetToDefaults();
            popup.close();
            open(); // reopen fresh so every control re-reads the reset draft
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle(secondaryBtnStyle());
        cancelBtn.setOnAction(e -> popup.close());

        Button saveBtn = new Button("Save / Apply");
        saveBtn.setStyle(primaryBtnStyle());
        saveBtn.setOnAction(e -> {
            live.copyFrom(draft);
            live.applyAndSave();
            popup.close();
        });

        footer.getChildren().addAll(resetBtn, cancelBtn, saveBtn);

        Label title = new Label("CUSTOMIZE APPEARANCE");
        title.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono'; -fx-padding: 20 20 0 20;");

        root.setTop(title);
        root.setCenter(body);
        root.setBottom(footer);

        RogueWindowChrome.apply(popup, "STYLE", root, 760, 560, null);
        popup.centerOnScreen();
        popup.show();
    }

    // ---------------------------------------------------------------------
    // Controls (left column)
    // ---------------------------------------------------------------------

    private static VBox buildControls(ThemeManager draft) {
        VBox box = new VBox(14);
        box.setPadding(new Insets(0, 12, 0, 0));

        box.getChildren().add(sectionLabel("Colors"));
        box.getChildren().add(colorRow(box, "Title Bar", draft.titleBarColor, c -> draft.titleBarColor = c));
        box.getChildren().add(colorRow(box, "Left Panel", draft.leftPanelColor, c -> draft.leftPanelColor = c));
        box.getChildren().add(colorRow(box, "Center Panel", draft.centerPanelColor, c -> draft.centerPanelColor = c));
        box.getChildren().add(colorRow(box, "News Panel", draft.newsPanelColor, c -> draft.newsPanelColor = c));
        box.getChildren().add(colorRow(box, "Buttons", draft.buttonColor, c -> draft.buttonColor = c));
        box.getChildren().add(colorRow(box, "Button Hover", draft.buttonHoverColor, c -> draft.buttonHoverColor = c));
        box.getChildren().add(colorRow(box, "Button Pressed", draft.buttonPressedColor, c -> draft.buttonPressedColor = c));
        box.getChildren().add(colorRow(box, "Button Text", draft.buttonTextColor, c -> draft.buttonTextColor = c));
        box.getChildren().add(colorRow(box, "Text", draft.textColor, c -> draft.textColor = c));
        box.getChildren().add(colorRow(box, "Secondary Text", draft.secondaryTextColor, c -> draft.secondaryTextColor = c));
        box.getChildren().add(colorRow(box, "Headline Text", draft.headlineColor, c -> draft.headlineColor = c));

        box.getChildren().add(sectionLabel("Fonts"));

        List<String> headlineOptions = new ArrayList<>();
        headlineOptions.add("(Default)");
        headlineOptions.addAll(FontManager.getHeadlineFontFamilies());
        box.getChildren().addAll(smallLabel("Headline Font (drop files in fonts/headline)"),
            fontPicker(box, headlineOptions, draft.headlineFontFamily, val -> draft.headlineFontFamily = val));

        List<String> textOptions = new ArrayList<>();
        textOptions.add("(Default)");
        textOptions.addAll(FontManager.getTextFontFamilies());
        box.getChildren().addAll(smallLabel("Text Font (drop files in fonts/text)"),
            fontPicker(box, textOptions, draft.textFontFamily, val -> draft.textFontFamily = val));

        Label sizeLabel = new Label("Headline Size: " + (int) draft.headlineFontSize);
        sizeLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 10; -fx-font-family: 'JetBrains Mono';");
        Slider sizeSlider = new Slider(18, 96, draft.headlineFontSize);
        sizeSlider.getStyleClass().add("rogue-slider");
        sizeSlider.setMaxWidth(Double.MAX_VALUE);
        sizeSlider.valueProperty().addListener((o, old, val) -> {
            draft.headlineFontSize = val.doubleValue();
            sizeLabel.setText("Headline Size: " + (int) draft.headlineFontSize);
            fireChange(box);
        });
        box.getChildren().addAll(sizeLabel, sizeSlider);

        return box;
    }

    /**
     * Font picker styled to match the launcher's own version-select dropdown (a button that
     * reveals a scrollable list of rows) instead of a stock JavaFX ComboBox.
     */
    private static VBox fontPicker(VBox parentBox, List<String> options, String currentValue, Consumer<String> onChange) {
        String initial = (currentValue == null || currentValue.isEmpty()) ? "(Default)" : currentValue;

        VBox wrap = new VBox(4);
        Button toggle = new Button(initial);
        toggle.setMaxWidth(Double.MAX_VALUE);
        toggle.setAlignment(Pos.CENTER_LEFT);
        toggle.setStyle(fontPickerBtnStyle());

        VBox listContent = new VBox(2);
        listContent.setPadding(new Insets(4));

        ScrollPane scroll = new ScrollPane(listContent);
        scroll.getStyleClass().add("rogue-scroll");
        scroll.setMaxHeight(160);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #0d0d0d; -fx-background-color: #0d0d0d; -fx-border-color: #1a1a1a; -fx-border-radius: 7; -fx-background-radius: 7;");
        scroll.setVisible(false);
        scroll.setManaged(false);

        for (String option : options) {
            Label row = new Label(option);
            row.setMaxWidth(Double.MAX_VALUE);
            row.setStyle(dropdownRowStyle());
            row.setOnMouseEntered(e -> row.setStyle(dropdownRowHoverStyle()));
            row.setOnMouseExited(e -> row.setStyle(dropdownRowStyle()));
            row.setOnMouseClicked(e -> {
                toggle.setText(option);
                onChange.accept("(Default)".equals(option) ? "" : option);
                scroll.setVisible(false);
                scroll.setManaged(false);
                fireChange(parentBox);
            });
            listContent.getChildren().add(row);
        }

        toggle.setOnAction(e -> {
            boolean opening = !scroll.isVisible();
            scroll.setVisible(opening);
            scroll.setManaged(opening);
        });

        wrap.getChildren().addAll(toggle, scroll);
        return wrap;
    }

    private static HBox colorRow(VBox parent, String label, String initialHex, Consumer<String> onChange) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);

        Label l = new Label(label);
        l.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono';");
        l.setPrefWidth(120);

        ColorPicker picker = new ColorPicker(safeColor(initialHex));
        picker.setStyle("-fx-background-color: #141414;");

        TextField hex = new TextField(initialHex);
        hex.setStyle(fieldStyle());
        hex.setPrefWidth(80);

        picker.setOnAction(e -> {
            String h = toHex(picker.getValue());
            hex.setText(h);
            onChange.accept(h);
            fireChange(parent);
        });

        hex.textProperty().addListener((o, old, val) -> {
            if (val == null || !val.matches("#?[0-9a-fA-F]{6}")) return;
            String h = val.startsWith("#") ? val : "#" + val;
            picker.setValue(safeColor(h));
            onChange.accept(h);
            fireChange(parent);
        });

        row.getChildren().addAll(l, picker, hex);
        return row;
    }

    private static void fireChange(VBox controlsBox) {
        Object handler = controlsBox.getProperties().get("onDraftChanged");
        if (handler instanceof Runnable) ((Runnable) handler).run();
    }

    // ---------------------------------------------------------------------
    // Preview (right column) - a scaled mock-up of the real launcher layout
    // ---------------------------------------------------------------------

    private static VBox buildPreview(ThemeManager t) {
        VBox wrapper = new VBox(6);

        Label caption = new Label("LIVE PREVIEW");
        caption.setStyle("-fx-text-fill: #666666; -fx-font-size: 9; -fx-font-family: 'JetBrains Mono';");

        VBox launcher = new VBox();
        launcher.setPrefSize(420, 260);
        launcher.setMaxSize(420, 260);
        launcher.setStyle("-fx-background-color: #080404; -fx-background-radius: 10; -fx-border-color: #1a1a1a; -fx-border-radius: 10; -fx-border-width: 1;");

        HBox titleBarMock = new HBox();
        titleBarMock.setPrefHeight(20);
        titleBarMock.setAlignment(Pos.CENTER_LEFT);
        titleBarMock.setPadding(new Insets(0, 8, 0, 10));
        titleBarMock.setStyle("-fx-background-color: " + t.titleBarColor + "; -fx-background-radius: 10 10 0 0;");
        Label titleBarLabel = new Label("Rogue Client");
        titleBarLabel.setStyle("-fx-text-fill: " + t.textColor + "; -fx-font-size: 8; -fx-font-family: 'JetBrains Mono';");
        titleBarMock.getChildren().add(titleBarLabel);

        HBox body = new HBox();
        VBox.setVgrow(body, Priority.ALWAYS);

        // Left panel mock
        VBox left = new VBox(6);
        left.setPrefWidth(46);
        left.setAlignment(Pos.TOP_CENTER);
        left.setPadding(new Insets(10, 0, 10, 0));
        left.setStyle("-fx-background-color: " + t.leftPanelColor + "; -fx-background-radius: 10 0 0 10;");
        for (int i = 0; i < 3; i++) {
            javafx.scene.layout.Region icon = new javafx.scene.layout.Region();
            icon.setPrefSize(24, 24);
            icon.setStyle("-fx-background-color: " + t.buttonColor + "; -fx-background-radius: 6;");
            left.getChildren().add(icon);
        }

        // Center panel mock
        VBox center = new VBox(8);
        center.setAlignment(Pos.CENTER);
        center.setStyle("-fx-background-color: " + t.centerPanelColor + ";");
        HBox.setHgrow(center, Priority.ALWAYS);

        Label headline = new Label("ROGUE CLIENT");
        Font hf = fontOrNull(t.headlineFontFamily, t.headlineFontSize / 3.2);
        if (hf != null) {
            headline.setFont(hf);
            headline.setStyle("-fx-text-fill: " + t.headlineColor + ";");
        } else {
            headline.setStyle("-fx-text-fill: " + t.headlineColor + "; -fx-font-size: " + (t.headlineFontSize / 3.2) + "; -fx-font-family: 'JetBrains Mono'; -fx-font-weight: bold;");
        }

        Label bodyText = new Label("Sample launcher text");
        bodyText.setStyle("-fx-text-fill: " + t.textColor + "; -fx-font-size: 10; -fx-font-family: '" + t.textFontFamilyOrDefault() + "';");

        Button playBtn = new Button("PLAY");
        playBtn.setStyle("-fx-background-color: " + t.buttonColor + "; -fx-text-fill: " + t.buttonTextColor + "; -fx-font-family: 'JetBrains Mono'; -fx-font-size: 11; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 8 22;");
        playBtn.setOnMouseEntered(e -> playBtn.setStyle("-fx-background-color: " + t.buttonHoverColor + "; -fx-text-fill: " + t.buttonTextColor + "; -fx-font-family: 'JetBrains Mono'; -fx-font-size: 11; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 8 22;"));
        playBtn.setOnMousePressed(e -> playBtn.setStyle("-fx-background-color: " + t.buttonPressedColor + "; -fx-text-fill: " + t.buttonTextColor + "; -fx-font-family: 'JetBrains Mono'; -fx-font-size: 11; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 8 22;"));
        playBtn.setOnMouseExited(e -> playBtn.setStyle("-fx-background-color: " + t.buttonColor + "; -fx-text-fill: " + t.buttonTextColor + "; -fx-font-family: 'JetBrains Mono'; -fx-font-size: 11; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 8 22;"));

        center.getChildren().addAll(headline, bodyText, playBtn);

        // News panel mock
        VBox news = new VBox(6);
        news.setPrefWidth(110);
        news.setPadding(new Insets(10));
        news.setStyle("-fx-background-color: " + t.newsPanelColor + "; -fx-background-radius: 0 10 10 0;");
        Label newsTitle = new Label("NEWS");
        newsTitle.setStyle("-fx-text-fill: " + t.textColor + "; -fx-font-size: 8; -fx-font-family: 'JetBrains Mono';");
        Label newsItem = new Label("Sample update entry goes here.");
        newsItem.setWrapText(true);
        newsItem.setStyle("-fx-text-fill: " + t.secondaryTextColor + "; -fx-font-size: 8; -fx-font-family: 'JetBrains Mono';");
        news.getChildren().addAll(newsTitle, newsItem);

        body.getChildren().addAll(left, center, news);
        launcher.getChildren().addAll(titleBarMock, body);
        wrapper.getChildren().addAll(caption, launcher);
        return wrapper;
    }

    private static Font fontOrNull(String family, double size) {
        if (family == null || family.isEmpty()) return null;
        try {
            return Font.font(family, size);
        } catch (Exception e) {
            return null;
        }
    }

    private static Color safeColor(String hex) {
        try {
            return Color.web(hex);
        } catch (Exception e) {
            return Color.WHITE;
        }
    }

    private static String toHex(Color c) {
        return String.format("#%02X%02X%02X",
            (int) Math.round(c.getRed() * 255),
            (int) Math.round(c.getGreen() * 255),
            (int) Math.round(c.getBlue() * 255));
    }

    private static Label sectionLabel(String text) {
        Label l = new Label(text.toUpperCase());
        l.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 9; -fx-font-family: 'JetBrains Mono'; -fx-padding: 8 0 0 0;");
        return l;
    }

    private static Label smallLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #888888; -fx-font-size: 9; -fx-font-family: 'JetBrains Mono';");
        l.setWrapText(true);
        return l;
    }

    private static String fieldStyle() {
        return "-fx-background-color: #141414; -fx-text-fill: #ffffff; " +
               "-fx-font-family: 'JetBrains Mono'; -fx-font-size: 11; " +
               "-fx-border-color: #222222; -fx-border-radius: 6; -fx-background-radius: 6; " +
               "-fx-padding: 6 8;";
    }

    private static String fontPickerBtnStyle() {
        return "-fx-background-color: #141414; -fx-text-fill: #ffffff; " +
               "-fx-font-family: 'JetBrains Mono'; -fx-font-size: 11; " +
               "-fx-border-color: #222222; -fx-border-radius: 6; -fx-background-radius: 6; " +
               "-fx-cursor: hand; -fx-padding: 6 10;";
    }

    private static String dropdownRowStyle() {
        return "-fx-text-fill: #ffffff; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono'; -fx-padding: 6 10;";
    }

    private static String dropdownRowHoverStyle() {
        return dropdownRowStyle() + " -fx-background-color: #161616; -fx-background-radius: 5;";
    }

    private static String secondaryBtnStyle() {
        return "-fx-background-color: #141414; -fx-text-fill: #ffffff; " +
               "-fx-font-family: 'JetBrains Mono'; -fx-font-size: 12; " +
               "-fx-border-color: #222222; -fx-border-radius: 6; -fx-background-radius: 6; " +
               "-fx-cursor: hand; -fx-padding: 8 16;";
    }

    private static String primaryBtnStyle() {
        return "-fx-background-color: #ffffff; -fx-text-fill: #000000; " +
               "-fx-font-family: 'JetBrains Mono'; -fx-font-size: 12; -fx-font-weight: bold; " +
               "-fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 8 16;";
    }
}
