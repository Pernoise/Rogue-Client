package com.rogueclient;

import javafx.scene.text.Font;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Manages user-installable fonts for the customization system. Users drop font
 * files into ~/.rogueclient/fonts/headline (for the big ROGUE CLIENT title) or
 * ~/.rogueclient/fonts/text (for normal launcher text), and they're picked up
 * automatically the next time the Style settings are opened - no manual config
 * editing required.
 *
 * JavaFX's Font.loadFont() only actually supports TrueType (.ttf) and OpenType
 * (.otf) files; .woff/.woff2 aren't understood by the JavaFX font loader. Those
 * files are still shown in the folder listing (so it's clear they were found)
 * but are marked unsupported and excluded from the pickable list rather than
 * silently failing.
 */
public class FontManager {

    private static final Path DATA_DIR    = Paths.get(System.getProperty("user.home"), ".rogueclient");
    private static final Path FONTS_ROOT  = DATA_DIR.resolve("fonts");
    public static final Path HEADLINE_DIR = FONTS_ROOT.resolve("headline");
    public static final Path TEXT_DIR     = FONTS_ROOT.resolve("text");

    private static final Set<String> LOADABLE_EXTENSIONS   = Set.of("ttf", "otf");
    private static final Set<String> RECOGNIZED_EXTENSIONS = Set.of("ttf", "otf", "woff", "woff2");

    /** Creates the font folders on disk if they don't already exist. Safe to call repeatedly. */
    public static void ensureDirectories() {
        try {
            Files.createDirectories(HEADLINE_DIR);
            Files.createDirectories(TEXT_DIR);
        } catch (Exception e) {
            System.out.println("Could not create font directories: " + e.getMessage());
        }
    }

    public static class FontEntry {
        public final String displayName; // resolved font family name, or the filename if it couldn't be read
        public final Path file;
        public final boolean loadable;   // false for formats JavaFX can't load (woff/woff2)

        FontEntry(String displayName, Path file, boolean loadable) {
            this.displayName = displayName;
            this.file = file;
            this.loadable = loadable;
        }
    }

    public static List<FontEntry> listHeadlineFonts() {
        return scan(HEADLINE_DIR);
    }

    public static List<FontEntry> listTextFonts() {
        return scan(TEXT_DIR);
    }

    private static List<FontEntry> scan(Path dir) {
        List<FontEntry> out = new ArrayList<>();
        ensureDirectories();
        try (var files = Files.list(dir)) {
            files.filter(Files::isRegularFile)
                 .sorted()
                 .forEach(p -> {
                     String ext = extensionOf(p);
                     if (!RECOGNIZED_EXTENSIONS.contains(ext)) return;

                     boolean loadable = LOADABLE_EXTENSIONS.contains(ext);
                     String name = p.getFileName().toString();
                     if (loadable) {
                         Font f = loadFontRaw(p, 12);
                         if (f != null) name = f.getFamily();
                     }
                     out.add(new FontEntry(name, p, loadable));
                 });
        } catch (Exception e) {
            System.out.println("Could not scan font directory " + dir + ": " + e.getMessage());
        }
        return out;
    }

    /** Loads a font file at the given size, or returns null if it can't be read/isn't a supported format. */
    public static Font loadFontRaw(Path path, double size) {
        try (InputStream is = Files.newInputStream(path)) {
            return Font.loadFont(is, size);
        } catch (Exception e) {
            return null;
        }
    }

    private static String extensionOf(Path p) {
        String name = p.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase();
    }
}
