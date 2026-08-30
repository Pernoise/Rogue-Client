package com.rogueclient;

import javafx.scene.text.Font;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Thin static accessors over ThemeManager.get(), used throughout the UI so style-string
 * builder methods read like "-fx-text-fill: " + ThemedStyles.text() + ";" instead of a
 * hardcoded hex literal. Centralizing it here means every file reads the *current* theme
 * value at the moment a style string is built, which is what makes re-invoking those style
 * methods after a theme change ("live refresh") actually pick up the new colors.
 */
public class ThemedStyles {

    private ThemedStyles() {}

    // ---- Panels ----
    public static String mainBg()   { return ThemeManager.get().mainBackground; }
    public static String panelBg()  { return ThemeManager.get().leftPanelBackground; }
    public static String centerBg() { return ThemeManager.get().centerPanelBackground; }
    public static String newsBg()   { return ThemeManager.get().newsPanelBackground; }
    public static String border()   { return ThemeManager.get().panelBorderColor; }

    /**
     * The launcher's fixed base background - always the original default dark color,
     * never affected by the "Main Background" theme customization. That customization
     * only recolors the title bar strip (see mainBg()); everything else that isn't a
     * Left/Center/News panel (window wrapper edges, popup bodies, etc.) uses this instead,
     * so changing the title bar color doesn't unexpectedly recolor the rest of the launcher.
     */
    public static String fixedBaseBg() { return ThemeManager.DEFAULT_MAIN_BACKGROUND; }

    public static String splashBg() { return ThemeManager.get().splashBackground; }

    // ---- Buttons ----
    public static String btnBg()            { return ThemeManager.get().buttonBackground; }
    public static String btnHoverBg()       { return ThemeManager.get().buttonHoverBackground; }
    public static String btnPressedBg()     { return ThemeManager.get().buttonPressedBackground; }
    public static String btnText()          { return ThemeManager.get().buttonTextColor; }
    public static String btnDisabledBg()    { return ThemeManager.get().buttonDisabledBackground; }
    public static String btnDisabledText()  { return ThemeManager.get().buttonDisabledTextColor; }

    // ---- Text ----
    public static String text()          { return ThemeManager.get().textColor; }
    public static String textSecondary() { return ThemeManager.get().secondaryTextColor; }
    public static String headlineColor() { return ThemeManager.get().headlineTextColor; }

    // ---- Fonts ----
    private static final Map<String, String> registeredFamilyCache = new HashMap<>();

    /** CSS font-family name to use for normal UI text: the user's chosen custom font if one is set and loadable, else the bundled JetBrains Mono. */
    public static String font() {
        ThemeManager t = ThemeManager.get();
        if (t.textFontFile != null) {
            String family = registerFont(t.textFontFile);
            if (family != null) return family;
        }
        return t.textFontFamily != null ? t.textFontFamily : ThemeManager.DEFAULT_TEXT_FONT_FAMILY;
    }

    public static double headlineSize() {
        return ThemeManager.get().headlineFontSize;
    }

    /**
     * Loads the headline font (ROGUE CLIENT title) at the given size: the user's custom
     * headline font if set, else the bundled "Gondens DEMO" font, else a bold fallback
     * using the current text font. Mirrors CenterPanel's original loading approach so the
     * title continues to be set via Label.setFont(...) rather than CSS.
     */
    public static Font headlineFont(double size) {
        ThemeManager t = ThemeManager.get();
        if (t.headlineFontFile != null) {
            Font f = FontManager.loadFontRaw(Paths.get(t.headlineFontFile), size);
            if (f != null) return f;
        }
        try {
            Font f = Font.loadFont(
                ThemedStyles.class.getResourceAsStream("/fonts/gondens-demo/Gondens DEMO.otf"), size);
            if (f != null) return f;
        } catch (Exception ignored) {}
        return Font.font(font(), javafx.scene.text.FontWeight.BOLD, size);
    }

    private static String registerFont(String path) {
        return registeredFamilyCache.computeIfAbsent(path, p -> {
            Font f = FontManager.loadFontRaw(Paths.get(p), 12);
            return f != null ? f.getFamily() : null;
        });
    }

    /** Clears the font registration cache - call after the theme changes in case a font file was replaced on disk. */
    public static void clearFontCache() {
        registeredFamilyCache.clear();
    }
}
