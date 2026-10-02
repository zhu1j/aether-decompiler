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
package com.aetherdecompiler.gui.background;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A reader for a Wallpaper&nbsp;Engine project folder.
 *
 * <p>A Wallpaper Engine project is a directory containing a {@code project.json}
 * descriptor plus the wallpaper's media file and a {@code preview} thumbnail.
 * The descriptor carries the fields the studio cares about:</p>
 *
 * <pre>{@code
 * {
 *   "file":      "IMG_0505.mp4",     // the media the wallpaper actually plays
 *   "preview":   "preview.jpg",      // thumbnail shown in the browser/creator
 *   "title":     "吾王美如画",          // display title
 *   "type":      "video",            // video | scene | web | application
 *   "general": { "properties": { "schemecolor": { "value": "0.15 0.43 0.90" } } }
 * }
 * }</pre>
 *
 * <p>The parser is deliberately a tiny, dependency-free, tolerant reader: it
 * looks for the handful of named string keys with regular expressions rather
 * than pulling a JSON library into the GUI. Wallpaper Engine files are simple
 * and stable, and a missing key is treated as "absent" rather than fatal, so an
 * unusual project still yields a usable backdrop by falling back to the folder's
 * first media file.</p>
 *
 * <p>Story analogy: a bilingual assistant who only needs to spot a few known
 * words on a label, not read the whole brochure.</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class WallpaperProject {

    private static final Pattern FILE_KEY = stringKey("file");
    private static final Pattern PREVIEW_KEY = stringKey("preview");
    private static final Pattern TITLE_KEY = stringKey("title");
    private static final Pattern TYPE_KEY = stringKey("type");
    // "schemecolor" maps to an object, so match the key only; its "value" string
    // is located separately after this key.
    private static final Pattern SCHEME_COLOR_KEY =
            Pattern.compile("\"schemecolor\"\\s*:");
    private static final Pattern VALUE_KEY = stringKey("value");

    private final Path directory;
    private final Path projectJson;
    private final String title;
    private final String type;
    private final Path media;
    private final Path preview;
    private final String schemeColor;

    private WallpaperProject(Path directory, Path projectJson, String title, String type,
                             Path media, Path preview, String schemeColor) {
        this.directory = directory;
        this.projectJson = projectJson;
        this.title = title;
        this.type = type;
        this.media = media;
        this.preview = preview;
        this.schemeColor = schemeColor;
    }

    private static Pattern stringKey(String key) {
        return Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]*)\"");
    }

    /**
     * @param directory a Wallpaper Engine project folder
     * @return whether the folder looks like a Wallpaper Engine project
     */
    public static boolean isProject(Path directory) {
        return directory != null
                && Files.isDirectory(directory)
                && Files.isRegularFile(directory.resolve("project.json"));
    }

    /**
     * Parse a Wallpaper Engine project folder.
     *
     * @param directory the folder containing {@code project.json}
     * @return the parsed project
     * @throws IOException if the descriptor is missing or unreadable
     */
    public static WallpaperProject load(Path directory) throws IOException {
        Path json = directory.resolve("project.json");
        if (!Files.isRegularFile(json)) {
            throw new IOException("no project.json in " + directory);
        }
        String text = Files.readString(json, StandardCharsets.UTF_8);

        String file = firstMatch(text, FILE_KEY);
        String preview = firstMatch(text, PREVIEW_KEY);
        String title = firstMatch(text, TITLE_KEY);
        String type = firstMatch(text, TYPE_KEY);
        String schemeColor = parseSchemeColor(text);

        Path media = resolve(directory, file);
        Path thumb = resolve(directory, preview);
        if (media == null) {
            media = firstMedia(directory);
        }
        if (media == null) {
            throw new IOException("no playable media found in " + directory);
        }
        if (title == null || title.isBlank()) {
            title = directory.getFileName().toString();
        }
        return new WallpaperProject(directory, json, title, type == null ? "" : type,
                media, thumb, schemeColor);
    }

    /** @return the {@code type} field (video / scene / web / application), lower-cased */
    public String type() {
        return type == null ? "" : type.toLowerCase(java.util.Locale.ROOT);
    }

    /**
     * @return the Wallpaper Engine scheme colour parsed from its {@code "r g b"}
     *         float triple into a {@code #rrggbb} string, or {@code null}
     */
    public String schemeColor() {
        return schemeColor;
    }

    private static String parseSchemeColor(String text) {
        Matcher sc = SCHEME_COLOR_KEY.matcher(text);
        if (!sc.find()) {
            return null;
        }
        // Look for the first "value" string after the schemecolor key.
        Matcher val = VALUE_KEY.matcher(text);
        if (!val.find(sc.end())) {
            return null;
        }
        String triple = val.group(1).trim();
        String[] parts = triple.split("\\s+");
        if (parts.length < 3) {
            return null;
        }
        try {
            int r = (int) Math.round(Double.parseDouble(parts[0]) * 255);
            int g = (int) Math.round(Double.parseDouble(parts[1]) * 255);
            int b = (int) Math.round(Double.parseDouble(parts[2]) * 255);
            return String.format("#%02x%02x%02x", clamp(r), clamp(g), clamp(b));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    private static String firstMatch(String text, Pattern pattern) {
        Matcher m = pattern.matcher(text);
        return m.find() ? m.group(1) : null;
    }

    private static Path resolve(Path dir, String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        Path p = dir.resolve(name);
        return Files.isRegularFile(p) ? p : null;
    }

    private static Path firstMedia(Path dir) {
        Set<String> tried = new LinkedHashSet<>();
        try (var stream = Files.list(dir)) {
            return stream.filter(Files::isRegularFile)
                    .filter(p -> Backdrop.isMedia(p.getFileName().toString()))
                    .filter(p -> !p.getFileName().toString().toLowerCase(java.util.Locale.ROOT)
                            .startsWith("preview"))
                    .sorted()
                    .findFirst()
                    .orElse(null);
        } catch (IOException ex) {
            return null;
        }
    }

    /** @return the project folder */
    public Path directory() {
        return directory;
    }

    /** @return the {@code project.json} path */
    public Path projectJson() {
        return projectJson;
    }

    /** @return the display title */
    public String title() {
        return title;
    }

    /** @return the wallpaper's media file */
    public Path media() {
        return media;
    }

    /** @return the preview thumbnail, or {@code null} */
    public Path preview() {
        return preview;
    }

    /** @return the project normalised into a {@link Backdrop} */
    public Backdrop toBackdrop() {
        return Backdrop.of(title, media, preview);
    }
}
