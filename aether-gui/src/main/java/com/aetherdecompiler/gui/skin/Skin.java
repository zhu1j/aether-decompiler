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

import java.net.URL;
import java.util.Objects;

/**
 * An immutable description of a visual skin.
 *
 * <p>A skin is a pure application-layer concept: it is a stylesheet plus
 * metadata, and optionally a default backdrop image shown behind the workspace.
 * It never touches the kernel, the plugins, or the engine, so adding a
 * look-and-feel is a zero-risk, zero-recompile operation for third parties.</p>
 *
 * <p>The backdrop image is deliberately stored as a descriptor (URL + opacity)
 * rather than being rendered here: the skin says <em>what</em> the look is, and
 * the application decides <em>how</em> to paint it (as a translucent layer).</p>
 *
 * <p>Story analogy: a set of interchangeable seat covers for the same car. The
 * engine, the wheels, and the wiring are untouched; only the look changes.</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class Skin {

    /** The opacity used for an imported backdrop when none is specified. */
    public static final double DEFAULT_BACKGROUND_OPACITY = 0.35;

    private final String id;
    private final String name;
    private final String author;
    private final String swatchAccent;
    private final String swatchBg;
    private final URL cssUrl;
    private final boolean builtin;
    private final URL backgroundImage;
    private final double backgroundOpacity;

    private Skin(String id, String name, String author,
                 String swatchAccent, String swatchBg, URL cssUrl, boolean builtin,
                 URL backgroundImage, double backgroundOpacity) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.author = author == null ? "" : author;
        this.swatchAccent = swatchAccent == null ? "#34e0c8" : swatchAccent;
        this.swatchBg = swatchBg == null ? "#0b0f14" : swatchBg;
        this.cssUrl = Objects.requireNonNull(cssUrl, "cssUrl");
        this.builtin = builtin;
        this.backgroundImage = backgroundImage;
        this.backgroundOpacity = clamp(backgroundOpacity);
    }

    private static double clamp(double v) {
        if (v <= 0) {
            return DEFAULT_BACKGROUND_OPACITY;
        }
        return Math.min(1.0, v);
    }

    /**
     * @param id      the stable skin id
     * @param name    the display name
     * @param author  the author string
     * @param accent  the accent colour used for the picker swatch
     * @param bg      the background colour used for the picker swatch
     * @param cssUrl  the stylesheet URL
     * @param builtin whether this ships with the application
     * @return a new skin descriptor with no default backdrop
     */
    public static Skin of(String id, String name, String author,
                          String accent, String bg, URL cssUrl, boolean builtin) {
        return new Skin(id, name, author, accent, bg, cssUrl, builtin, null, DEFAULT_BACKGROUND_OPACITY);
    }

    /**
     * Return a copy of this skin carrying a default backdrop image.
     *
     * @param image   the backdrop image URL (may be {@code null} to clear)
     * @param opacity the backdrop opacity in {@code (0, 1]}
     * @return a new skin descriptor
     */
    public Skin withBackground(URL image, double opacity) {
        return new Skin(id, name, author, swatchAccent, swatchBg, cssUrl, builtin, image, opacity);
    }

    /** @return the stable skin id */
    public String id() {
        return id;
    }

    /** @return the display name */
    public String name() {
        return name;
    }

    /** @return the author string */
    public String author() {
        return author;
    }

    /** @return the accent colour of the picker swatch */
    public String swatchAccent() {
        return swatchAccent;
    }

    /** @return the background colour of the picker swatch */
    public String swatchBg() {
        return swatchBg;
    }

    /** @return the stylesheet URL */
    public URL cssUrl() {
        return cssUrl;
    }

    /** @return whether this skin ships with the application */
    public boolean isBuiltin() {
        return builtin;
    }

    /** @return the default backdrop image URL, or {@code null} when the skin has none */
    public URL backgroundImage() {
        return backgroundImage;
    }

    /** @return the default backdrop opacity in {@code (0, 1]} */
    public double backgroundOpacity() {
        return backgroundOpacity;
    }

    /** @return whether this skin declares a default backdrop image */
    public boolean hasBackground() {
        return backgroundImage != null;
    }

    /** @return the external string form required by {@code Scene.getStylesheets()} */
    public String externalForm() {
        return cssUrl.toExternalForm();
    }

    @Override
    public String toString() {
        return name + (builtin ? "" : "  \u2022 user");
    }
}
