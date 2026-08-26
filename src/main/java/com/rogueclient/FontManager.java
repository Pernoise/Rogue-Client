package com.rogueclient;

import javafx.scene.text.Font;

import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Manages user-installed fonts for launcher customization.
 *
 * Users drop font files into:
 *   ~/.rogueclient/fonts/headline/   (used for the big "ROGUE CLIENT" title)
 *   ~/.rogueclient/fonts/text/       (used for normal UI text)
 *
 * Supported formats: .ttf, .otf, .woff (JavaFX's Font.loadFont does not support .woff2).
 * Nothing needs to be edited by hand - dropping a file in the folder is enough; call
 * scanAndLoad() (done once at startup, and again whenever the Style settings panel opens)
 * to pick up new additions.
 */
public class FontManager {

    private static final Path FONTS_DIR    = Paths.get(System.getProperty("user.home"), ".rogueclient", "fonts");
    private static final Path HEADLINE_DIR = FONTS_DIR.resolve("headline");
    private static final Path TEXT_DIR     = FONTS_DIR.resolve("text");

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(".ttf", ".otf", ".woff");

    private static final List<String> headlineFamilies = new ArrayList<>();
    private static final List<String> textFamilies      = new ArrayList<>();

    /** Creates the font folders (if missing) and loads every font file found inside them. */
    public static synchronized void ensureFoldersAndLoad() {
        try {
            Files.createDirectories(HEADLINE_DIR);
            Files.createDirectories(TEXT_DIR);
        } catch (Exception e) {
            System.out.println("Could not create font folders: " + e.getMessage());
        }
        scanAndLoad();
    }

    public static synchronized void scanAndLoad() {
        headlineFamilies.clear();
        textFamilies.clear();
        loadDirInto(HEADLINE_DIR, headlineFamilies);
        loadDirInto(TEXT_DIR, textFamilies);
    }

    private static void loadDirInto(Path dir, List<String> out) {
        Set<String> seen = new LinkedHashSet<>();
        if (!Files.isDirectory(dir)) return;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path file : stream) {
                String name = file.getFileName().toString().toLowerCase();
                boolean supported = SUPPORTED_EXTENSIONS.stream().anyMatch(name::endsWith);
                if (!supported) continue;
                try (InputStream is = Files.newInputStream(file)) {
                    Font f = Font.loadFont(is, 12);
                    if (f != null) seen.add(f.getFamily());
                } catch (Exception e) {
                    System.out.println("Could not load font " + file + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.out.println("Could not scan font folder " + dir + ": " + e.getMessage());
        }
        out.addAll(seen);
    }

    public static List<String> getHeadlineFontFamilies() {
        return new ArrayList<>(headlineFamilies);
    }

    public static List<String> getTextFontFamilies() {
        return new ArrayList<>(textFamilies);
    }

    public static Path getHeadlineDir() { return HEADLINE_DIR; }
    public static Path getTextDir() { return TEXT_DIR; }
}
