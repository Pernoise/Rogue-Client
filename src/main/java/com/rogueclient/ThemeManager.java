package com.rogueclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Central store for every launcher-appearance setting (colors, fonts, headline size).
 * Holds the current "live" theme, persists it to ~/.rogueclient/theme.json, and notifies
 * registered listeners whenever the theme changes so panels can restyle themselves without
 * a full restart.
 *
 * Default values below intentionally mirror the colors/fonts that were previously hardcoded
 * across LeftPanel/CenterPanel/NewsPanel/Main/RogueWindowChrome - the "default theme" this
 * spec requires is simply "what Rogue Client already looked like".
 */
public class ThemeManager {

    private static final Path DATA_DIR    = Paths.get(System.getProperty("user.home"), ".rogueclient");
    private static final Path THEME_FILE  = DATA_DIR.resolve("theme.json");
    private static final Gson GSON        = new GsonBuilder().setPrettyPrinting().create();

    private static ThemeManager instance;

    public static synchronized ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
            instance.load();
        }
        return instance;
    }

    // ---- Colors (hex strings, e.g. "#0f0f0f") ----
    public String titleBarColor     = "#080404";
    public String leftPanelColor    = "#0f0f0f";
    public String centerPanelColor  = "#080404";
    public String newsPanelColor    = "#080404";

    public String buttonColor        = "#161616";
    public String buttonTextColor    = "#ffffff";
    public String buttonHoverColor   = "#222222";
    public String buttonPressedColor = "#2a2a2a";

    public String textColor          = "#ffffff";
    public String secondaryTextColor = "#aaaaaa";
    public String headlineColor      = "#ffffff";

    // ---- Fonts ----
    // Family names as returned by Font.loadFont(...).getFamily(). Empty = built-in default.
    public String headlineFontFamily = "";
    public String textFontFamily     = "";
    public double headlineFontSize   = 64;

    private final List<Runnable> listeners = new ArrayList<>();

    private ThemeManager() {}

    public void addListener(Runnable r) {
        listeners.add(r);
    }

    public void removeListener(Runnable r) {
        listeners.remove(r);
    }

    /** Call after mutating fields to persist + repaint every listening panel. */
    public void applyAndSave() {
        save();
        notifyListeners();
    }

    public void notifyListeners() {
        for (Runnable r : new ArrayList<>(listeners)) {
            try {
                r.run();
            } catch (Exception e) {
                System.out.println("Theme listener failed: " + e.getMessage());
            }
        }
    }

    /** Returns a copy of the current theme, useful as a scratch/draft object for live previews. */
    public ThemeManager copy() {
        ThemeManager t = new ThemeManager();
        t.titleBarColor      = this.titleBarColor;
        t.leftPanelColor     = this.leftPanelColor;
        t.centerPanelColor   = this.centerPanelColor;
        t.newsPanelColor     = this.newsPanelColor;
        t.buttonColor        = this.buttonColor;
        t.buttonTextColor    = this.buttonTextColor;
        t.buttonHoverColor   = this.buttonHoverColor;
        t.buttonPressedColor = this.buttonPressedColor;
        t.textColor          = this.textColor;
        t.secondaryTextColor = this.secondaryTextColor;
        t.headlineColor      = this.headlineColor;
        t.headlineFontFamily = this.headlineFontFamily;
        t.textFontFamily     = this.textFontFamily;
        t.headlineFontSize   = this.headlineFontSize;
        return t;
    }

    /** Copies every field from `other` into this (live) instance, without saving/notifying. */
    public void copyFrom(ThemeManager other) {
        this.titleBarColor      = other.titleBarColor;
        this.leftPanelColor     = other.leftPanelColor;
        this.centerPanelColor   = other.centerPanelColor;
        this.newsPanelColor     = other.newsPanelColor;
        this.buttonColor        = other.buttonColor;
        this.buttonTextColor    = other.buttonTextColor;
        this.buttonHoverColor   = other.buttonHoverColor;
        this.buttonPressedColor = other.buttonPressedColor;
        this.textColor          = other.textColor;
        this.secondaryTextColor = other.secondaryTextColor;
        this.headlineColor      = other.headlineColor;
        this.headlineFontFamily = other.headlineFontFamily;
        this.textFontFamily     = other.textFontFamily;
        this.headlineFontSize   = other.headlineFontSize;
    }

    /** Restores every field to the baked-in default theme (does not save/notify by itself). */
    public void resetToDefaults() {
        ThemeManager def = new ThemeManager();
        copyFrom(def);
    }

    public String textFontFamilyOrDefault() {
        return (textFontFamily == null || textFontFamily.isEmpty()) ? "JetBrains Mono" : textFontFamily;
    }

    public String headlineFontFamilyOrDefault() {
        return (headlineFontFamily == null || headlineFontFamily.isEmpty()) ? "Gondens" : headlineFontFamily;
    }

    public void load() {
        try {
            if (Files.exists(THEME_FILE)) {
                String json = new String(Files.readAllBytes(THEME_FILE));
                JsonObject obj = GSON.fromJson(json, JsonObject.class);
                if (obj == null) return;
                titleBarColor      = str(obj, "titleBarColor", titleBarColor);
                leftPanelColor     = str(obj, "leftPanelColor", leftPanelColor);
                centerPanelColor   = str(obj, "centerPanelColor", centerPanelColor);
                newsPanelColor     = str(obj, "newsPanelColor", newsPanelColor);
                buttonColor        = str(obj, "buttonColor", buttonColor);
                buttonTextColor    = str(obj, "buttonTextColor", buttonTextColor);
                buttonHoverColor   = str(obj, "buttonHoverColor", buttonHoverColor);
                buttonPressedColor = str(obj, "buttonPressedColor", buttonPressedColor);
                textColor          = str(obj, "textColor", textColor);
                secondaryTextColor = str(obj, "secondaryTextColor", secondaryTextColor);
                headlineColor      = str(obj, "headlineColor", headlineColor);
                headlineFontFamily = str(obj, "headlineFontFamily", headlineFontFamily);
                textFontFamily     = str(obj, "textFontFamily", textFontFamily);
                if (obj.has("headlineFontSize")) headlineFontSize = obj.get("headlineFontSize").getAsDouble();
            }
        } catch (Exception e) {
            System.out.println("Could not load theme: " + e.getMessage());
        }
    }

    public void save() {
        try {
            Files.createDirectories(DATA_DIR);
            JsonObject obj = new JsonObject();
            obj.addProperty("titleBarColor", titleBarColor);
            obj.addProperty("leftPanelColor", leftPanelColor);
            obj.addProperty("centerPanelColor", centerPanelColor);
            obj.addProperty("newsPanelColor", newsPanelColor);
            obj.addProperty("buttonColor", buttonColor);
            obj.addProperty("buttonTextColor", buttonTextColor);
            obj.addProperty("buttonHoverColor", buttonHoverColor);
            obj.addProperty("buttonPressedColor", buttonPressedColor);
            obj.addProperty("textColor", textColor);
            obj.addProperty("secondaryTextColor", secondaryTextColor);
            obj.addProperty("headlineColor", headlineColor);
            obj.addProperty("headlineFontFamily", headlineFontFamily);
            obj.addProperty("textFontFamily", textFontFamily);
            obj.addProperty("headlineFontSize", headlineFontSize);
            Files.write(THEME_FILE, GSON.toJson(obj).getBytes());
        } catch (Exception e) {
            System.out.println("Could not save theme: " + e.getMessage());
        }
    }

    private static String str(JsonObject obj, String key, String fallback) {
        return obj.has(key) ? obj.get(key).getAsString() : fallback;
    }
}
