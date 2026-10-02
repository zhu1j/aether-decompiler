/*
 * aether-decompiler — an independent, reusable JVM decompilation engine.
 * Copyright 2026 Jerry Zhu (Zeek) <zhujiejava1@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author Jerry Zhu (Zeek)
 * "Run the Code, Run the World!"
 */
package com.aetherdecompiler.gui.skin;

import javafx.scene.Scene;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.stream.Stream;

/**
 * Discovers, holds, imports, and applies skins and backdrop images.
 *
 * <p>Two kinds of skin are supported with the same mechanism:</p>
 * <ul>
 *   <li><strong>Built-in skins</strong> shipped as {@code /skins/*.css} on the
 *       classpath and registered in {@link #BUILTIN}.</li>
 *   <li><strong>Custom skins</strong> dropped by the user into
 *       {@code ~/.aether/skins/}: any {@code *.css} file, optionally accompanied
 *       by a {@code <name>.skin.properties} for metadata. No recompile, no
 *       registry edit — the directory is the extension point.</li>
 * </ul>
 *
 * <p>Beyond dropping files by hand, the studio can also <em>import</em> them at
 * runtime: {@link #importSkin(Path)} copies a chosen {@code *.css} into the user
 * skin directory and reloads the catalogue, and {@link #importBackground(Path)}
 * copies a chosen image into the backdrop directory. Both are thin, honest file
 * operations — the directory remains the single source of truth.</p>
 *
 * <p>Story analogy: a wardrobe. The shop-bought outfits hang on one rail; the
 * tailor-made ones the owner drops in are hung on the other. Getting dressed
 * uses the same mirror either way.</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class SkinManager {

    /** The shared layout stylesheet always applied beneath any skin. */
    public static final String BASE_CSS = "styles/base.css";

    /** Image extensions accepted for imported backdrops. */
    private static final List<String> IMAGE_EXT =
            List.of(".png", ".jpg", ".jpeg", ".gif", ".bmp", ".webp");

    /** The built-in skin tuples: id, name, author, accent, bg, css resource. */
    private static final String[][] BUILTIN = {
            {"midnight-aether", "Midnight Aether", "Jerry Zhu (Zeek)", "#34e0c8", "#0b0f14", "skins/midnight-aether.css"},
            {"obsidian-amber", "Obsidian Amber", "Jerry Zhu (Zeek)", "#f5b544", "#14110c", "skins/obsidian-amber.css"},
            {"matrix-green", "Matrix Terminal", "Jerry Zhu (Zeek)", "#39ff14", "#030705", "skins/matrix-green.css"},
            {"nebula-violet", "Nebula Violet", "Jerry Zhu (Zeek)", "#b18cff", "#0d0a17", "skins/nebula-violet.css"},
            {"solar-light", "Solar Light", "Jerry Zhu (Zeek)", "#007a6e", "#f4f6f8", "skins/solar-light.css"},
    };

    private final Path userSkinDir;
    private final List<Skin> skins = new ArrayList<>();

    /**
     * Build a manager, loading built-in skins and any user skins.
     *
     * @param userSkinDir the directory to scan for custom skins (may not exist)
     */
    public SkinManager(Path userSkinDir) {
        this.userSkinDir = userSkinDir;
        reload();
    }

    /** Rebuild the skin catalogue from built-ins plus the user skin directory. */
    public void reload() {
        skins.clear();
        loadBuiltin();
        loadUser();
    }

    private void loadBuiltin() {
        ClassLoader cl = SkinManager.class.getClassLoader();
        for (String[] row : BUILTIN) {
            URL url = cl.getResource(row[5]);
            if (url != null) {
                skins.add(Skin.of(row[0], row[1], row[2], row[3], row[4], url, true));
            }
        }
    }

    private void loadUser() {
        if (userSkinDir == null || !Files.isDirectory(userSkinDir)) {
            return;
        }
        try (Stream<Path> files = Files.list(userSkinDir)) {
            files.filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".css"))
                    .sorted()
                    .forEach(this::addUserSkin);
        } catch (IOException ex) {
            // A missing or unreadable user directory is not an error.
        }
    }

    private void addUserSkin(Path cssFile) {
        String fileName = cssFile.getFileName().toString();
        String baseName = stripExtension(fileName);
        Properties meta = readMeta(cssFile.resolveSibling(baseName + ".skin.properties"));
        String name = meta.getProperty("name", baseName);
        String author = meta.getProperty("author", "user");
        String accent = meta.getProperty("accent", "#34e0c8");
        String bg = meta.getProperty("bg", "#0b0f14");
        try {
            Skin skin = Skin.of("user-" + baseName, name, author, accent, bg,
                    cssFile.toUri().toURL(), false);
            // Optional per-skin default backdrop declared in the metadata sidecar.
            String background = meta.getProperty("background");
            if (background != null && !background.isBlank()) {
                Path bgPath = cssFile.getParent().resolve(background.trim());
                if (Files.isRegularFile(bgPath)) {
                    double opacity = parseDouble(meta.getProperty("backgroundOpacity"),
                            Skin.DEFAULT_BACKGROUND_OPACITY);
                    skin = skin.withBackground(bgPath.toUri().toURL(), opacity);
                }
            }
            skins.add(skin);
        } catch (IOException ex) {
            // Skip an unreadable skin file.
        }
    }

    private Properties readMeta(Path metaFile) {
        Properties props = new Properties();
        if (Files.isRegularFile(metaFile)) {
            try (InputStream in = Files.newInputStream(metaFile)) {
                props.load(in);
            } catch (IOException ex) {
                // Fall back to defaults.
            }
        }
        return props;
    }

    // -------------------------------------------------------------------- import

    /**
     * Import a {@code *.css} stylesheet as a custom skin.
     *
     * <p>The file (and an optional {@code <name>.skin.properties} sidecar) is
     * copied into the user skin directory, the catalogue is reloaded, and the
     * resulting skin is returned so the caller can select it immediately. The
     * user's original file is never modified.</p>
     *
     * @param source the chosen {@code *.css} file
     * @return the registered custom skin
     * @throws IOException if the file cannot be copied
     */
    public Skin importSkin(Path source) throws IOException {
        if (source == null || !Files.isRegularFile(source)) {
            throw new IOException("not a readable file: " + source);
        }
        String fileName = source.getFileName().toString();
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".css")) {
            throw new IOException("not a css file: " + fileName);
        }
        Files.createDirectories(userSkinDir);
        Path dest = userSkinDir.resolve(fileName);
        Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);

        String baseName = stripExtension(fileName);
        Path metaSrc = source.resolveSibling(baseName + ".skin.properties");
        if (Files.isRegularFile(metaSrc)) {
            Files.copy(metaSrc, userSkinDir.resolve(baseName + ".skin.properties"),
                    StandardCopyOption.REPLACE_EXISTING);
        }

        reload();
        Skin imported = byId("user-" + baseName);
        return imported != null ? imported : all().get(all().size() - 1);
    }

    /**
     * Import an image as a reusable backdrop.
     *
     * @param source the chosen image file
     * @return the stored backdrop path inside the backdrop directory
     * @throws IOException if the file cannot be copied
     */
    public Path importBackground(Path source) throws IOException {
        if (source == null || !Files.isRegularFile(source)) {
            throw new IOException("not a readable file: " + source);
        }
        String fileName = source.getFileName().toString();
        if (!isImage(fileName)) {
            throw new IOException("not an image file: " + fileName);
        }
        Path dir = ensureBackgroundDir();
        Path dest = dir.resolve(fileName);
        Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
        return dest;
    }

    /** @return whether a file name has a supported image extension */
    public static boolean isImage(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        for (String ext : IMAGE_EXT) {
            if (lower.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    /** @return all backdrop images already stored in the backdrop directory */
    public List<Path> backgrounds() {
        Path dir = defaultBackgroundDir();
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(dir)) {
            return files.filter(p -> isImage(p.getFileName().toString()))
                    .sorted()
                    .toList();
        } catch (IOException ex) {
            return List.of();
        }
    }

    // -------------------------------------------------------------------- access

    /** @return all discovered skins, built-ins first */
    public List<Skin> all() {
        return Collections.unmodifiableList(skins);
    }

    /**
     * @param id a skin id
     * @return the matching skin, or {@code null}
     */
    public Skin byId(String id) {
        for (Skin s : skins) {
            if (s.id().equals(id)) {
                return s;
            }
        }
        return null;
    }

    /** @return the first built-in skin, used as the startup default */
    public Skin defaultSkin() {
        return skins.isEmpty() ? null : skins.get(0);
    }

    /**
     * Apply a skin to a scene: the shared base layout plus the skin stylesheet.
     *
     * @param scene the scene to re-dress
     * @param skin  the skin to apply
     */
    public void apply(Scene scene, Skin skin) {
        scene.getStylesheets().clear();
        URL base = SkinManager.class.getClassLoader().getResource(BASE_CSS);
        if (base != null) {
            scene.getStylesheets().add(base.toExternalForm());
        }
        if (skin != null) {
            scene.getStylesheets().add(skin.externalForm());
        }
    }

    // --------------------------------------------------------------------- paths

    /** @return the conventional skin root, {@code ~/.aether} */
    public static Path aetherHome() {
        return Path.of(System.getProperty("user.home", "."), ".aether");
    }

    /** @return the conventional user skin directory, {@code ~/.aether/skins} */
    public static Path defaultUserSkinDir() {
        return aetherHome().resolve("skins");
    }

    /** @return the conventional backdrop directory, {@code ~/.aether/backgrounds} */
    public static Path defaultBackgroundDir() {
        return aetherHome().resolve("backgrounds");
    }

    /** @return the studio preferences file, {@code ~/.aether/studio.properties} */
    public static Path studioPrefsFile() {
        return aetherHome().resolve("studio.properties");
    }

    /**
     * Ensure the user skin directory exists so the "open skins folder" affordance
     * always has somewhere to point.
     *
     * @return the user skin directory
     */
    public static Path ensureUserSkinDir() {
        return ensureDir(defaultUserSkinDir());
    }

    /**
     * Ensure the backdrop directory exists.
     *
     * @return the backdrop directory
     */
    public static Path ensureBackgroundDir() {
        return ensureDir(defaultBackgroundDir());
    }

    private static Path ensureDir(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException ex) {
            // Non-fatal: the picker simply lists built-in skins only.
        }
        return dir;
    }

    // ------------------------------------------------------------------- helpers

    private static String stripExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? fileName : fileName.substring(0, dot);
    }

    private static double parseDouble(String value, double fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}
