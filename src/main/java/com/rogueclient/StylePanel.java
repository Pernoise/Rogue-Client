package com.rogueclient;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;

/**
 * "Style" tab inside Settings: one row per customizable category (Background & Panels,
 * Buttons, Text Colors, Fonts & Headline). Each row shows a small live swatch of the
 * current theme and a "Customize" button that opens the shared live-preview editor
 * (ThemeEditorWindow) scoped to that category.
 */
public class StylePanel extends VBox {

    public StylePanel() {
        setSpacing(14);
        setPadding(new Insets(16, 0, 0, 0));

        getChildren().add(sectionLabel("Appearance"));
        Label info = new Label("Customize colors and fonts across the launcher. Nothing is applied until you press Save inside each editor.");
        info.setWrapText(true);
        info.setStyle("-fx-text-fill: " + ThemedStyles.textSecondary() + "; -fx-font-size: 11; -fx-font-family: '" + ThemedStyles.font() + "';");
        getChildren().add(info);

        getChildren().add(categoryRow(
            "Background & Panels",
            "Main background, Left Panel, Center Panel, News Panel, borders",
            () -> new String[]{ThemeManager.get().mainBackground, ThemeManager.get().leftPanelBackground,
                                ThemeManager.get().centerPanelBackground, ThemeManager.get().newsPanelBackground},
            this::openBackgroundEditor
        ));

        getChildren().add(categoryRow(
            "Buttons",
            "Background, hover, pressed/active, text, disabled",
            () -> new String[]{ThemeManager.get().buttonBackground, ThemeManager.get().buttonHoverBackground,
                                ThemeManager.get().buttonPressedBackground},
            this::openButtonsEditor
        ));

        getChildren().add(categoryRow(
            "Text Colors",
            "Normal text, secondary text, headline/title text",
            () -> new String[]{ThemeManager.get().textColor, ThemeManager.get().secondaryTextColor,
                                ThemeManager.get().headlineTextColor},
            this::openTextColorsEditor
        ));

        getChildren().add(categoryRow(
            "Fonts & Headline",
            "Headline font + size, normal UI text font",
            () -> new String[0],
            this::openFontsEditor
        ));

        getChildren().add(sectionLabel("Custom Fonts"));
        Label fontInfo = new Label(
            "Drop .ttf or .otf files into the folders below to make them available above. " +
            ".woff/.woff2 files are detected but can't be loaded by the launcher's UI toolkit."
        );
        fontInfo.setWrapText(true);
        fontInfo.setStyle("-fx-text-fill: " + ThemedStyles.textSecondary() + "; -fx-font-size: 11; -fx-font-family: '" + ThemedStyles.font() + "';");
        getChildren().add(fontInfo);

        Button openHeadlineFolderBtn = new Button("Open Headline Fonts Folder");
        openHeadlineFolderBtn.setStyle(secondaryBtnStyle());
        openHeadlineFolderBtn.setOnAction(e -> openFolder(FontManager.HEADLINE_DIR));

        Button openTextFolderBtn = new Button("Open Text Fonts Folder");
        openTextFolderBtn.setStyle(secondaryBtnStyle());
        openTextFolderBtn.setOnAction(e -> openFolder(FontManager.TEXT_DIR));

        HBox folderRow = new HBox(8, openHeadlineFolderBtn, openTextFolderBtn);
        getChildren().add(folderRow);
    }

    private interface SwatchSource { String[] get(); }
    private interface EditorOpener { void open(); }

    private HBox categoryRow(String title, String subtitle, SwatchSource swatches, EditorOpener opener) {
        VBox textCol = new VBox(2);
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: " + ThemedStyles.text() + "; -fx-font-size: 12; -fx-font-family: '" + ThemedStyles.font() + "'; -fx-font-weight: bold;");
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.setWrapText(true);
        subtitleLabel.setStyle("-fx-text-fill: " + ThemedStyles.textSecondary() + "; -fx-font-size: 10; -fx-font-family: '" + ThemedStyles.font() + "';");
        textCol.getChildren().addAll(titleLabel, subtitleLabel);
        HBox.setHgrow(textCol, Priority.ALWAYS);
        textCol.setMaxWidth(240);

        HBox swatchRow = new HBox(4);
        swatchRow.setAlignment(Pos.CENTER_LEFT);
        for (String hex : swatches.get()) {
            Region dot = new Region();
            dot.setPrefSize(16, 16);
            dot.setMaxSize(16, 16);
            String safe = (hex != null && hex.matches("^#[0-9a-fA-F]{6}$")) ? hex : "#000000";
            dot.setStyle("-fx-background-color: " + safe + "; -fx-background-radius: 4; -fx-border-color: " + ThemedStyles.border() + "; -fx-border-radius: 4;");
            swatchRow.getChildren().add(dot);
        }

        Button customizeBtn = new Button("Customize");
        customizeBtn.setStyle(secondaryBtnStyle());
        customizeBtn.setOnAction(e -> opener.open());

        HBox row = new HBox(14, textCol, swatchRow, customizeBtn);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10));
        row.setStyle(
            "-fx-background-color: " + ThemedStyles.panelBg() + "; -fx-border-color: " + ThemedStyles.border() + "; " +
            "-fx-border-radius: 8; -fx-background-radius: 8;"
        );
        return row;
    }

    private void openBackgroundEditor() {
        ThemeEditorWindow.open("Background & Panels", (draft, onChange) -> {
            VBox col = new VBox(14);
            col.getChildren().addAll(
                ThemeEditorWindow.colorRow("Main Background", draft, d -> d.mainBackground, (d, v) -> d.mainBackground = v, onChange),
                ThemeEditorWindow.colorRow("Left Panel", draft, d -> d.leftPanelBackground, (d, v) -> d.leftPanelBackground = v, onChange),
                ThemeEditorWindow.colorRow("Center Panel", draft, d -> d.centerPanelBackground, (d, v) -> d.centerPanelBackground = v, onChange),
                ThemeEditorWindow.colorRow("News Panel", draft, d -> d.newsPanelBackground, (d, v) -> d.newsPanelBackground = v, onChange),
                ThemeEditorWindow.colorRow("Panel Borders", draft, d -> d.panelBorderColor, (d, v) -> d.panelBorderColor = v, onChange)
            );
            return col;
        });
    }

    private void openButtonsEditor() {
        ThemeEditorWindow.open("Buttons", (draft, onChange) -> {
            VBox col = new VBox(14);
            col.getChildren().addAll(
                ThemeEditorWindow.colorRow("Background", draft, d -> d.buttonBackground, (d, v) -> d.buttonBackground = v, onChange),
                ThemeEditorWindow.colorRow("Hover", draft, d -> d.buttonHoverBackground, (d, v) -> d.buttonHoverBackground = v, onChange),
                ThemeEditorWindow.colorRow("Pressed / Active", draft, d -> d.buttonPressedBackground, (d, v) -> d.buttonPressedBackground = v, onChange),
                ThemeEditorWindow.colorRow("Text", draft, d -> d.buttonTextColor, (d, v) -> d.buttonTextColor = v, onChange),
                ThemeEditorWindow.colorRow("Disabled Background", draft, d -> d.buttonDisabledBackground, (d, v) -> d.buttonDisabledBackground = v, onChange),
                ThemeEditorWindow.colorRow("Disabled Text", draft, d -> d.buttonDisabledTextColor, (d, v) -> d.buttonDisabledTextColor = v, onChange)
            );
            return col;
        });
    }

    private void openTextColorsEditor() {
        ThemeEditorWindow.open("Text Colors", (draft, onChange) -> {
            VBox col = new VBox(14);
            col.getChildren().addAll(
                ThemeEditorWindow.colorRow("Normal Text", draft, d -> d.textColor, (d, v) -> d.textColor = v, onChange),
                ThemeEditorWindow.colorRow("Secondary Text", draft, d -> d.secondaryTextColor, (d, v) -> d.secondaryTextColor = v, onChange),
                ThemeEditorWindow.colorRow("Headline / Title Text", draft, d -> d.headlineTextColor, (d, v) -> d.headlineTextColor = v, onChange)
            );
            return col;
        });
    }

    private void openFontsEditor() {
        ThemeEditorWindow.open("Fonts & Headline", (draft, onChange) -> {
            VBox col = new VBox(16);

            // ---- Headline font ----
            Label headlineFontLabel = fieldLabel("Headline Font");
            ComboBox<FontChoice> headlineFontBox = new ComboBox<>();
            headlineFontBox.getItems().add(FontChoice.bundled("Bundled (Gondens DEMO)"));
            for (FontManager.FontEntry fe : FontManager.listHeadlineFonts()) {
                if (fe.loadable) headlineFontBox.getItems().add(FontChoice.custom(fe.displayName, fe.file.toString()));
            }
            headlineFontBox.setStyle(comboStyle());
            FontChoice currentHeadline = draft.headlineFontFile == null
                ? headlineFontBox.getItems().get(0)
                : findByPath(headlineFontBox.getItems(), draft.headlineFontFile);
            headlineFontBox.setValue(currentHeadline != null ? currentHeadline : headlineFontBox.getItems().get(0));
            headlineFontBox.valueProperty().addListener((obs, o, n) -> {
                if (n == null) return;
                draft.headlineFontFile = n.filePath;
                onChange.run();
            });

            // ---- Headline size ----
            Label sizeLabel = fieldLabel("Headline Size (" + Math.round(draft.headlineFontSize) + "px)");
            Slider sizeSlider = new Slider(12, 160, draft.headlineFontSize);
            sizeSlider.getStyleClass().add("rocket-slider");
            sizeSlider.setMaxWidth(Double.MAX_VALUE);
            sizeSlider.valueProperty().addListener((obs, o, n) -> {
                draft.headlineFontSize = n.doubleValue();
                sizeLabel.setText("Headline Size (" + Math.round(draft.headlineFontSize) + "px)");
                onChange.run();
            });

            // ---- Text font ----
            Label textFontLabel = fieldLabel("Normal Text Font");
            ComboBox<FontChoice> textFontBox = new ComboBox<>();
            textFontBox.getItems().add(FontChoice.bundled("Bundled (JetBrains Mono)"));
            for (FontManager.FontEntry fe : FontManager.listTextFonts()) {
                if (fe.loadable) textFontBox.getItems().add(FontChoice.custom(fe.displayName, fe.file.toString()));
            }
            textFontBox.setStyle(comboStyle());
            FontChoice currentText = draft.textFontFile == null
                ? textFontBox.getItems().get(0)
                : findByPath(textFontBox.getItems(), draft.textFontFile);
            textFontBox.setValue(currentText != null ? currentText : textFontBox.getItems().get(0));
            textFontBox.valueProperty().addListener((obs, o, n) -> {
                if (n == null) return;
                draft.textFontFile = n.filePath;
                onChange.run();
            });

            col.getChildren().addAll(
                headlineFontLabel, headlineFontBox,
                sizeLabel, sizeSlider,
                textFontLabel, textFontBox
            );
            return col;
        });
    }

    private static FontChoice findByPath(List<FontChoice> items, String path) {
        for (FontChoice fc : items) {
            if (path.equals(fc.filePath)) return fc;
        }
        return null;
    }

    /** Wraps a font option for the ComboBox: either the bundled default (filePath == null) or a user-installed font file. */
    private static class FontChoice {
        final String label;
        final String filePath;

        private FontChoice(String label, String filePath) {
            this.label = label;
            this.filePath = filePath;
        }

        static FontChoice bundled(String label) { return new FontChoice(label, null); }
        static FontChoice custom(String label, String path) { return new FontChoice(label, path); }

        @Override public String toString() { return label; }
    }

    private static Label fieldLabel(String text) {
        Label l = new Label(text.toUpperCase());
        l.setStyle("-fx-text-fill: " + ThemedStyles.text() + "; -fx-font-size: 9; -fx-font-family: '" + ThemedStyles.font() + "';");
        return l;
    }

    private static String comboStyle() {
        return "-fx-background-color: #141414; -fx-text-fill: #ffffff; -fx-font-family: '" + ThemedStyles.font() + "'; " +
               "-fx-font-size: 12; -fx-border-color: #222222; -fx-border-radius: 6; -fx-background-radius: 6; -fx-mark-color: #ffffff;";
    }

    private static Label sectionLabel(String text) {
        Label l = new Label(text.toUpperCase());
        l.setStyle("-fx-text-fill: " + ThemedStyles.text() + "; -fx-font-size: 9; -fx-font-family: '" + ThemedStyles.font() + "';");
        return l;
    }

    private static String secondaryBtnStyle() {
        return "-fx-background-color: " + ThemedStyles.btnBg() + "; -fx-text-fill: " + ThemedStyles.btnText() + "; " +
               "-fx-font-family: '" + ThemedStyles.font() + "'; -fx-font-size: 11; " +
               "-fx-border-color: " + ThemedStyles.border() + "; -fx-border-radius: 6; -fx-background-radius: 6; " +
               "-fx-cursor: hand; -fx-padding: 8 14;";
    }

    private static void openFolder(java.nio.file.Path path) {
        try {
            FontManager.ensureDirectories();
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
            System.out.println("Could not open fonts folder: " + e.getMessage());
        }
    }
}
