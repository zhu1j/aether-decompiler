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

import com.aetherdecompiler.gui.skin.SkinManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * The user's backdrop library, persisted under {@code ~/.aether/backgrounds}.
 *
 * <p>The library accepts three kinds of import, all reduced to a
 * {@link Backdrop}:</p>
 * <ul>
 *   <li>a bare image or video file;</li>
 *   <li>a Wallpaper&nbsp;Engine project folder ({@code project.json} + media +
 *       preview), copied wholesale so its relative references keep working;</li>
 *   <li>any previously stored entry is re-listed on the next launch.</li>
 * </ul>
 *
 * <p>Keeping every import on disk (rather than only in memory) means a chosen
 * wallpaper survives restarts for free, and the folder stays a plain, browsable
 * place the user can also manage by hand.</p>
 *
 * <p>Story analogy: the film archive next to the projector — whatever you load
 * today is still on the shelf tomorrow.</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class BackdropLibrary {

    private final Path root;

    /**
     * @param root the library directory (created on demand)
     */
    public BackdropLibrary(Path root) {
        this.root = root;
    }

    /** @return the conventional library directory, {@code ~/.aether/backgrounds} */
    public static Path defaultRoot() {
        return SkinManager.defaultBackgroundDir();
    }

    /**
     * Ensure the library directory exists.
     *
     * @return the library directory
     */
    public Path ensureRoot() {
        try {
            Files.createDirectories(root);
        } catch (IOException ex) {
            // Non-fatal: imports will surface their own error.
        }
        return root;
    }

    /** @return the library root directory */
    public Path root() {
        return root;
    }

    /**
     * List every backdrop in the library: Wallpaper Engine project folders first,
     * then loose media files, each sorted by name.
     *
     * @return the discovered backdrops (never {@code null})
     */
    public List<Backdrop> list() {
        List<Backdrop> projects = new ArrayList<>();
        List<Backdrop> loose = new ArrayList<>();
        if (!Files.isDirectory(root)) {
            return projects;
        }
        try (Stream<Path> children = Files.list(root)) {
            children.sorted().forEach(p -> {
                if (Files.isDirectory(p)) {
                    if (WallpaperProject.isProject(p)) {
                        try {
                            projects.add(WallpaperProject.load(p).toBackdrop());
                        } catch (IOException ex) {
                            // Skip an unreadable project.
                        }
                    }
                } else if (Backdrop.isMedia(p.getFileName().toString())
                        && !p.getFileName().toString().toLowerCase(java.util.Locale.ROOT)
                        .startsWith("preview")) {
                    loose.add(Backdrop.of(p.getFileName().toString(), p, null));
                }
            });
        } catch (IOException ex) {
            // Return whatever we have.
        }
        projects.addAll(loose);
        return projects;
    }

    /**
     * Import a bare image or video file into the library.
     *
     * @param source the chosen media file
     * @return the stored backdrop
     * @throws IOException if the file is not media or cannot be copied
     */
    public Backdrop importMedia(Path source) throws IOException {
        if (source == null || !Files.isRegularFile(source)) {
            throw new IOException("not a readable file: " + source);
        }
        String name = source.getFileName().toString();
        if (!Backdrop.isMedia(name)) {
            throw new IOException("not a supported image/video: " + name);
        }
        ensureRoot();
        Path dest = root.resolve(name);
        Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
        return Backdrop.of(name, dest, null);
    }

    /**
     * Import a Wallpaper Engine project folder into the library.
     *
     * <p>The whole folder is copied under the library so that the descriptor's
     * relative media and preview references remain valid.</p>
     *
     * @param projectDir the folder containing {@code project.json}
     * @return the stored backdrop
     * @throws IOException if the folder is not a project or cannot be copied
     */
    public Backdrop importProject(Path projectDir) throws IOException {
        if (!WallpaperProject.isProject(projectDir)) {
            throw new IOException("not a Wallpaper Engine project: " + projectDir);
        }
        WallpaperProject project = WallpaperProject.load(projectDir);
        ensureRoot();
        Path dest = root.resolve(projectDir.getFileName().toString());
        copyTree(projectDir, dest);
        // Re-parse from the stored copy so paths point at the library.
        WallpaperProject stored = WallpaperProject.load(dest);
        return stored.toBackdrop();
    }

    /**
     * Import whichever kind of source a path is: a Wallpaper Engine project
     * folder, or a loose media file.
     *
     * @param source a project folder or a media file
     * @return the stored backdrop
     * @throws IOException if the source is neither, or copying fails
     */
    public Backdrop importAny(Path source) throws IOException {
        if (WallpaperProject.isProject(source)) {
            return importProject(source);
        }
        return importMedia(source);
    }

    /**
     * Remove a stored entry (a loose file or a whole project folder).
     *
     * @param media the media path of a stored backdrop
     * @throws IOException if deletion fails
     */
    public void remove(Path media) throws IOException {
        if (media == null) {
            return;
        }
        // A loose file is removed directly; a project's file is removed with its
        // containing folder when that folder sits under the library root.
        Path parent = media.getParent();
        if (parent != null && parent.startsWith(root) && !parent.equals(root)
                && WallpaperProject.isProject(parent)) {
            deleteTree(parent);
        } else {
            Files.deleteIfExists(media);
            // Also drop an orphan preview next to it.
            String name = media.getFileName().toString();
            int dot = name.lastIndexOf('.');
            Path sibling = media.resolveSibling("preview.jpg");
            if (dot > 0 && Files.exists(sibling) && !parent.equals(root)) {
                Files.deleteIfExists(sibling);
            }
        }
    }

    private static void copyTree(Path from, Path to) throws IOException {
        Files.createDirectories(to);
        try (Stream<Path> walk = Files.walk(from)) {
            for (Path src : walk.toList()) {
                Path rel = from.relativize(src);
                Path dst = to.resolve(rel.toString());
                if (Files.isDirectory(src)) {
                    Files.createDirectories(dst);
                } else {
                    Files.createDirectories(dst.getParent());
                    Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static void deleteTree(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            for (Path p : walk.sorted((a, b) -> b.getNameCount() - a.getNameCount()).toList()) {
                Files.deleteIfExists(p);
            }
        }
    }
}
