package com.rogueclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Holds every user-customizable appearance value for the launcher (panel colors,
 * button colors, text colors, fonts, headline size) and persists them to
 * ~/.rogueclient/theme.json, following the same plain-fields + load()/save()
 * pattern as SettingsManager so it slots into the existing config convention
 * instead of inventing a new one.
 *
 * This is a singleton (ThemeManager.get()) rather than something threaded through
 * every constructor, because dozens of unrelated windows across the launcher need
 * to read the current theme and a handful of long-lived panels (Main, LeftPanel,
 * CenterPanel, NewsPanel) need to react live when it changes. Listeners registered
 * via addListener() are notified after every applyDraft()/resetToDefaults() call.
 *
 * The DEFAULT_* constants below are exactly Rogue Client's original hardcoded
 * colors/fonts - the "default theme" required by the spec is just these values,
 * always available via resetToDefaults().
 */
public class ThemeManager {

    // ---- Defaults (the launcher's original, untouched appearance) ----
    public static final String DEFAULT_MAIN_BACKGROUND        = "#080404";
    public static final String DEFAULT_LEFT_PANEL_BACKGROUND  = "#0f0f0f";
    public static final String DEFAULT_CENTER_PANEL_BACKGROUND = "#080404";
    public static final String DEFAULT_NEWS_PANEL_BACKGROUND  = "#080404";
    public static final String DEFAULT_PANEL_BORDER_COLOR     = "#1a1a1a";

    public static final String DEFAULT_BUTTON_BACKGROUND         = "#0f0f0f";
    public static final String DEFAULT_BUTTON_HOVER_BACKGROUND   = "#161616";
    public static final String DEFAULT_BUTTON_PRESSED_BACKGROUND = "#1f1f1f";
    public static final String DEFAULT_BUTTON_TEXT_COLOR          = "#ffffff";
    public static final String DEFAULT_BUTTON_DISABLED_BACKGROUND = "#1a1a1a";
    public static final String DEFAULT_BUTTON_DISABLED_TEXT_COLOR = "#666666";

    public static final String DEFAULT_TEXT_COLOR           = "#ffffff";
    public static final String DEFAULT_SECONDARY_TEXT_COLOR = "#888888";
    public static final String DEFAULT_HEADLINE_TEXT_COLOR  = "#ffffff";

    /** null = use the bundled "Gondens DEMO" font that ships with the launcher. */
    public static final String DEFAULT_HEADLINE_FONT_FILE = null;
    public static final double DEFAULT_HEADLINE_FONT_SIZE = 64;

    /** null = use the bundled "JetBrains Mono" font that ships with the launcher. */
    public static final String DEFAULT_TEXT_FONT_FILE   = null;
    public static final String DEFAULT_TEXT_FONT_FAMILY = "JetBrains Mono";

    // ---- Live values ----
    public String mainBackground        = DEFAULT_MAIN_BACKGROUND;
    public String leftPanelBackground   = DEFAULT_LEFT_PANEL_BACKGROUND;
    public String centerPanelBackground = DEFAULT_CENTER_PANEL_BACKGROUND;
    public String newsPanelBackground   = DEFAULT_NEWS_PANEL_BACKGROUND;
    public String panelBorderColor      = DEFAULT_PANEL_BORDER_COLOR;

    public String buttonBackground         = DEFAULT_BUTTON_BACKGROUND;
    public String buttonHoverBackground    = DEFAULT_BUTTON_HOVER_BACKGROUND;
    public String buttonPressedBackground  = DEFAULT_BUTTON_PRESSED_BACKGROUND;
    public String buttonTextColor          = DEFAULT_BUTTON_TEXT_COLOR;
    public String buttonDisabledBackground = DEFAULT_BUTTON_DISABLED_BACKGROUND;
    public String buttonDisabledTextColor  = DEFAULT_BUTTON_DISABLED_TEXT_COLOR;

    public String textColor          = DEFAULT_TEXT_COLOR;
    public String secondaryTextColor = DEFAULT_SECONDARY_TEXT_COLOR;
    public String headlineTextColor  = DEFAULT_HEADLINE_TEXT_COLOR;

    /** Absolute path to a font file under ~/.rogueclient/fonts/headline, or null for the bundled default. */
    public String headlineFontFile = DEFAULT_HEADLINE_FONT_FILE;
    public double headlineFontSize = DEFAULT_HEADLINE_FONT_SIZE;

    /** Absolute path to a font file under ~/.rogueclient/fonts/text, or null for the bundled default. */
    public String textFontFile   = DEFAULT_TEXT_FONT_FILE;
    public String textFontFamily = DEFAULT_TEXT_FONT_FAMILY;

    // ---- Persistence ----
    private static final Path DATA_DIR   = Paths.get(System.getProperty("user.home"), ".rogueclient");
    private static final Path THEME_FILE = DATA_DIR.resolve("theme.json");
    private static final Gson GSON       = new GsonBuilder().setPrettyPrinting().create();

    // ---- Singleton + listeners ----
    private static ThemeManager instance;
    private static final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    public static synchronized ThemeManager get() {
        if (instance == null) {
            instance = new ThemeManager();
            instance.load();
        }
        return instance;
    }

    private ThemeManager() {}

    /** Registers a callback to run (on whatever thread applyDraft/resetToDefaults was called from) whenever the theme changes. */
    public static void addListener(Runnable r) {
        listeners.add(r);
    }

    public static void removeListener(Runnable r) {
        listeners.remove(r);
    }

    private static void notifyListeners() {
        for (Runnable r : listeners) {
            try {
                r.run();
            } catch (Exception e) {
                System.out.println("Theme listener failed: " + e.getMessage());
            }
        }
    }

    /** Copies every appearance field from another ThemeManager into this one, in place. Does not save or notify listeners. */
    public void copyFieldsFrom(ThemeManager src) {
        this.mainBackground        = src.mainBackground;
        this.leftPanelBackground   = src.leftPanelBackground;
        this.centerPanelBackground = src.centerPanelBackground;
        this.newsPanelBackground   = src.newsPanelBackground;
        this.panelBorderColor      = src.panelBorderColor;
        this.buttonBackground         = src.buttonBackground;
        this.buttonHoverBackground    = src.buttonHoverBackground;
        this.buttonPressedBackground  = src.buttonPressedBackground;
        this.buttonTextColor          = src.buttonTextColor;
        this.buttonDisabledBackground = src.buttonDisabledBackground;
        this.buttonDisabledTextColor  = src.buttonDisabledTextColor;
        this.textColor          = src.textColor;
        this.secondaryTextColor = src.secondaryTextColor;
        this.headlineTextColor  = src.headlineTextColor;
        this.headlineFontFile = src.headlineFontFile;
        this.headlineFontSize = src.headlineFontSize;
        this.textFontFile   = src.textFontFile;
        this.textFontFamily = src.textFontFamily;
    }

    /** Returns a free-standing copy of the current live theme, safe to edit without affecting the app until applyDraft() is called. */
    public ThemeManager copy() {
        ThemeManager d = new ThemeManager();
        d.copyFieldsFrom(this);
        return d;
    }

    /** Commits every field from a draft (usually produced by copy() and edited in ThemeEditorWindow) into the live singleton, saves it, and notifies listeners. */
    public void applyDraft(ThemeManager draft) {
        copyFieldsFrom(draft);
        ThemedStyles.clearFontCache();
        save();
        notifyListeners();
    }

    /** Restores every value to Rogue Client's original, hardcoded appearance. */
    public void resetToDefaults() {
        mainBackground        = DEFAULT_MAIN_BACKGROUND;
        leftPanelBackground   = DEFAULT_LEFT_PANEL_BACKGROUND;
        centerPanelBackground = DEFAULT_CENTER_PANEL_BACKGROUND;
        newsPanelBackground   = DEFAULT_NEWS_PANEL_BACKGROUND;
        panelBorderColor      = DEFAULT_PANEL_BORDER_COLOR;
        buttonBackground         = DEFAULT_BUTTON_BACKGROUND;
        buttonHoverBackground    = DEFAULT_BUTTON_HOVER_BACKGROUND;
        buttonPressedBackground  = DEFAULT_BUTTON_PRESSED_BACKGROUND;
        buttonTextColor          = DEFAULT_BUTTON_TEXT_COLOR;
        buttonDisabledBackground = DEFAULT_BUTTON_DISABLED_BACKGROUND;
        buttonDisabledTextColor  = DEFAULT_BUTTON_DISABLED_TEXT_COLOR;
        textColor          = DEFAULT_TEXT_COLOR;
        secondaryTextColor = DEFAULT_SECONDARY_TEXT_COLOR;
        headlineTextColor  = DEFAULT_HEADLINE_TEXT_COLOR;
        headlineFontFile = DEFAULT_HEADLINE_FONT_FILE;
        headlineFontSize = DEFAULT_HEADLINE_FONT_SIZE;
        textFontFile   = DEFAULT_TEXT_FONT_FILE;
        textFontFamily = DEFAULT_TEXT_FONT_FAMILY;
        ThemedStyles.clearFontCache();
        save();
        notifyListeners();
    }

    /** True if every value is still at the default theme (used to grey out "Reset to Default" when there's nothing to reset). */
    public boolean isDefault() {
        ThemeManager d = get();
        return eq(d.mainBackground, DEFAULT_MAIN_BACKGROUND)
            && eq(d.leftPanelBackground, DEFAULT_LEFT_PANEL_BACKGROUND)
            && eq(d.centerPanelBackground, DEFAULT_CENTER_PANEL_BACKGROUND)
            && eq(d.newsPanelBackground, DEFAULT_NEWS_PANEL_BACKGROUND)
            && eq(d.panelBorderColor, DEFAULT_PANEL_BORDER_COLOR)
            && eq(d.buttonBackground, DEFAULT_BUTTON_BACKGROUND)
            && eq(d.buttonHoverBackground, DEFAULT_BUTTON_HOVER_BACKGROUND)
            && eq(d.buttonPressedBackground, DEFAULT_BUTTON_PRESSED_BACKGROUND)
            && eq(d.buttonTextColor, DEFAULT_BUTTON_TEXT_COLOR)
            && eq(d.buttonDisabledBackground, DEFAULT_BUTTON_DISABLED_BACKGROUND)
            && eq(d.buttonDisabledTextColor, DEFAULT_BUTTON_DISABLED_TEXT_COLOR)
            && eq(d.textColor, DEFAULT_TEXT_COLOR)
            && eq(d.secondaryTextColor, DEFAULT_SECONDARY_TEXT_COLOR)
            && eq(d.headlineTextColor, DEFAULT_HEADLINE_TEXT_COLOR)
            && eq(d.headlineFontFile, DEFAULT_HEADLINE_FONT_FILE)
            && d.headlineFontSize == DEFAULT_HEADLINE_FONT_SIZE
            && eq(d.textFontFile, DEFAULT_TEXT_FONT_FILE)
            && eq(d.textFontFamily, DEFAULT_TEXT_FONT_FAMILY);
    }

    private static boolean eq(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    public void load() {
        try {
            if (Files.exists(THEME_FILE)) {
                String json = new String(Files.readAllBytes(THEME_FILE));
                JsonObject obj = GSON.fromJson(json, JsonObject.class);
                if (obj == null) return;
                mainBackground        = str(obj, "mainBackground", mainBackground);
                leftPanelBackground   = str(obj, "leftPanelBackground", leftPanelBackground);
                centerPanelBackground = str(obj, "centerPanelBackground", centerPanelBackground);
                newsPanelBackground   = str(obj, "newsPanelBackground", newsPanelBackground);
                panelBorderColor      = str(obj, "panelBorderColor", panelBorderColor);
                buttonBackground         = str(obj, "buttonBackground", buttonBackground);
                buttonHoverBackground    = str(obj, "buttonHoverBackground", buttonHoverBackground);
                buttonPressedBackground  = str(obj, "buttonPressedBackground", buttonPressedBackground);
                buttonTextColor          = str(obj, "buttonTextColor", buttonTextColor);
                buttonDisabledBackground = str(obj, "buttonDisabledBackground", buttonDisabledBackground);
                buttonDisabledTextColor  = str(obj, "buttonDisabledTextColor", buttonDisabledTextColor);
                textColor          = str(obj, "textColor", textColor);
                secondaryTextColor = str(obj, "secondaryTextColor", secondaryTextColor);
                headlineTextColor  = str(obj, "headlineTextColor", headlineTextColor);
                headlineFontFile = str(obj, "headlineFontFile", headlineFontFile);
                if (obj.has("headlineFontSize")) headlineFontSize = obj.get("headlineFontSize").getAsDouble();
                textFontFile   = str(obj, "textFontFile", textFontFile);
                textFontFamily = str(obj, "textFontFamily", textFontFamily);
            }
        } catch (Exception e) {
            System.out.println("Could not load theme: " + e.getMessage());
        }
    }

    private static String str(JsonObject obj, String key, String fallback) {
        return (obj.has(key) && !obj.get(key).isJsonNull()) ? obj.get(key).getAsString() : fallback;
    }

    public void save() {
        try {
            Files.createDirectories(DATA_DIR);
            JsonObject obj = new JsonObject();
            obj.addProperty("mainBackground", mainBackground);
            obj.addProperty("leftPanelBackground", leftPanelBackground);
            obj.addProperty("centerPanelBackground", centerPanelBackground);
            obj.addProperty("newsPanelBackground", newsPanelBackground);
            obj.addProperty("panelBorderColor", panelBorderColor);
            obj.addProperty("buttonBackground", buttonBackground);
            obj.addProperty("buttonHoverBackground", buttonHoverBackground);
            obj.addProperty("buttonPressedBackground", buttonPressedBackground);
            obj.addProperty("buttonTextColor", buttonTextColor);
            obj.addProperty("buttonDisabledBackground", buttonDisabledBackground);
            obj.addProperty("buttonDisabledTextColor", buttonDisabledTextColor);
            obj.addProperty("textColor", textColor);
            obj.addProperty("secondaryTextColor", secondaryTextColor);
            obj.addProperty("headlineTextColor", headlineTextColor);
            if (headlineFontFile != null) obj.addProperty("headlineFontFile", headlineFontFile);
            obj.addProperty("headlineFontSize", headlineFontSize);
            if (textFontFile != null) obj.addProperty("textFontFile", textFontFile);
            obj.addProperty("textFontFamily", textFontFamily);
            Files.write(THEME_FILE, GSON.toJson(obj).getBytes());
        } catch (Exception e) {
            System.out.println("Could not save theme: " + e.getMessage());
        }
    }
}
