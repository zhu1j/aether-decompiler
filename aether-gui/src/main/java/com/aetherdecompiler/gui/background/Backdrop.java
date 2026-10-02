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

import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;

/**
 * An immutable description of one backdrop the studio can paint behind the work
 * area: its title, its medium (still image or video), the media file, and an
 * optional preview thumbnail.
 *
 * <p>A backdrop may come from a bare image/video file or from a
 * Wallpaper&nbsp;Engine project folder (see {@link WallpaperProject}); this type
 * is the common shape both are normalised into, so the studio never has to care
 * which kind of source it was.</p>
 *
 * <p>Story analogy: a single "film" in the projector's library — regardless of
 * whether it was hand-drawn on a slide or shot on video.</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class Backdrop {

    private static final Set<String> IMAGE_EXT = Set.of(
            ".png", ".jpg", ".jpeg", ".gif", ".bmp", ".webp");
    private static final Set<String> VIDEO_EXT = Set.of(
            ".mp4", ".m4v", ".mov", ".webm", ".flv", ".mkv");

    private final String title;
    private final BackdropKind kind;
    private final Path media;
    private final Path preview;

    private Backdrop(String title, BackdropKind kind, Path media, Path preview) {
        this.title = title;
        this.kind = kind;
        this.media = media;
        this.preview = preview;
    }

    /**
     * @param title   display title
     * @param media   the media file
     * @param preview an optional thumbnail (may be {@code null})
     * @return a backdrop whose kind is inferred from the media extension
     */
    public static Backdrop of(String title, Path media, Path preview) {
        return new Backdrop(title, kindOf(media), media, preview);
    }

    /**
     * @param path a file whose extension identifies the media kind
     * @return the kind implied by the extension, or {@link BackdropKind#NONE}
     */
    public static BackdropKind kindOf(Path path) {
        if (path == null) {
            return BackdropKind.NONE;
        }
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        int dot = name.lastIndexOf('.');
        String ext = dot < 0 ? "" : name.substring(dot);
        if (IMAGE_EXT.contains(ext)) {
            return BackdropKind.IMAGE;
        }
        if (VIDEO_EXT.contains(ext)) {
            return BackdropKind.VIDEO;
        }
        return BackdropKind.NONE;
    }

    /** @return whether an extension is a supported backdrop image */
    public static boolean isImage(String name) {
        return IMAGE_EXT.contains(suffix(name));
    }

    /** @return whether an extension is a supported backdrop video */
    public static boolean isVideo(String name) {
        return VIDEO_EXT.contains(suffix(name));
    }

    /** @return whether an extension is any supported backdrop media */
    public static boolean isMedia(String name) {
        return isImage(name) || isVideo(name);
    }

    private static String suffix(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        int dot = lower.lastIndexOf('.');
        return dot < 0 ? "" : lower.substring(dot);
    }

    /** @return the display title */
    public String title() {
        return title;
    }

    /** @return the media kind */
    public BackdropKind kind() {
        return kind;
    }

    /** @return the media file */
    public Path media() {
        return media;
    }

    /** @return the thumbnail, or {@code null} when none is available */
    public Path preview() {
        return preview;
    }

    /** @return whether this backdrop is a video */
    public boolean isVideo() {
        return kind == BackdropKind.VIDEO;
    }

    @Override
    public String toString() {
        return title + "  [" + kind.name().toLowerCase(Locale.ROOT) + "]";
    }
}
