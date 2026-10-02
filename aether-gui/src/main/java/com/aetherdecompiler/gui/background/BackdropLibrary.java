/*
 * aether-decompiler —— 一个独立、可复用的 JVM 反编译引擎。
 * Copyright 2026 Jerry Zhu (Zeek) <zhujiejava1@gmail.com>
 *
 * 依据 Apache License, Version 2.0（下称“本许可证”）授权；
 * 除非遵守本许可证，否则你不得使用本文件。
 * 你可以在以下地址获取本许可证副本：
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * 除非适用法律要求或书面同意，依据本许可证分发的软件
 * 均按“原样（AS IS）”提供，不附带任何明示或默示的担保，
 * 包括但不限于对适销性、特定用途适用性的担保。
 * 关于本许可证下具体权限与限制的表述，请参见本许可证。
 *
 * @author Jerry Zhu (Zeek)
 * “Run the Code, Run the World!”
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
 * 用户的背景库，持久化于 {@code ~/.aether/backgrounds} 之下。
 *
 * <p>该库接受三种导入，最终都归约为一个 {@link Backdrop}：</p>
 * <ul>
 *   <li>一个裸的图片或视频文件；</li>
 *   <li>一个 Wallpaper&nbsp;Engine 工程文件夹（{@code project.json} + 媒体 +
 *       预览图），整体复制，使其相对引用继续有效；</li>
 *   <li>任何此前存储的条目都会在下次启动时被重新列出。</li>
 * </ul>
 *
 * <p>把每一次导入都落到磁盘上（而非只在内存中），意味着选定的壁纸无需额外
 * 代价就能在重启后保留，且该文件夹始终是一个普通、可浏览、用户也能手动管理
 * 的地方。</p>
 *
 * <p>故事类比：放映机旁的胶片档案 —— 你今天装入的内容，明天仍在架上。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class BackdropLibrary {

    private final Path root;

    /**
     * @param root 库目录（按需创建）
     */
    public BackdropLibrary(Path root) {
        this.root = root;
    }

    /** @return 约定的库目录 {@code ~/.aether/backgrounds} */
    public static Path defaultRoot() {
        return SkinManager.defaultBackgroundDir();
    }

    /**
     * 确保库目录存在。
     *
     * @return 库目录
     */
    public Path ensureRoot() {
        try {
            Files.createDirectories(root);
        } catch (IOException ex) {
            // 非致命：导入会各自暴露自己的错误。
        }
        return root;
    }

    /** @return 库根目录 */
    public Path root() {
        return root;
    }

    /**
     * 列出库中的每张背景：先是 Wallpaper Engine 工程文件夹，
     * 再是零散的媒体文件，各自按名称排序。
     *
     * @return 发现的背景（永不为 {@code null}）
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
                            // 跳过无法读取的工程。
                        }
                    }
                } else if (Backdrop.isMedia(p.getFileName().toString())
                        && !p.getFileName().toString().toLowerCase(java.util.Locale.ROOT)
                        .startsWith("preview")) {
                    loose.add(Backdrop.of(p.getFileName().toString(), p, null));
                }
            });
        } catch (IOException ex) {
            // 返回已有的内容。
        }
        projects.addAll(loose);
        return projects;
    }

    /**
     * 把一个裸的图片或视频文件导入库中。
     *
     * @param source 选定的媒体文件
     * @return 已存储的背景
     * @throws IOException 若该文件不是媒体或无法复制
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
     * 把一个 Wallpaper Engine 工程文件夹导入库中。
     *
     * <p>整个文件夹会被复制到库下，从而使描述符中的相对媒体与预览引用
     * 保持有效。</p>
     *
     * @param projectDir 包含 {@code project.json} 的文件夹
     * @return 已存储的背景
     * @throws IOException 若该文件夹不是工程或无法复制
     */
    public Backdrop importProject(Path projectDir) throws IOException {
        if (!WallpaperProject.isProject(projectDir)) {
            throw new IOException("not a Wallpaper Engine project: " + projectDir);
        }
        WallpaperProject project = WallpaperProject.load(projectDir);
        ensureRoot();
        Path dest = root.resolve(projectDir.getFileName().toString());
        copyTree(projectDir, dest);
        // 从已存储的副本重新解析，使路径指向库。
        WallpaperProject stored = WallpaperProject.load(dest);
        return stored.toBackdrop();
    }

    /**
     * 导入某个路径所对应的任一来源：一个 Wallpaper Engine 工程
     * 文件夹，或一个零散的媒体文件。
     *
     * @param source 工程文件夹或媒体文件
     * @return 已存储的背景
     * @throws IOException 若该来源两者皆非，或复制失败
     */
    public Backdrop importAny(Path source) throws IOException {
        if (WallpaperProject.isProject(source)) {
            return importProject(source);
        }
        return importMedia(source);
    }

    /**
     * 移除一条已存储条目（一个零散文件或整个工程文件夹）。
     *
     * @param media 已存储背景的媒体路径
     * @throws IOException 若删除失败
     */
    public void remove(Path media) throws IOException {
        if (media == null) {
            return;
        }
        // 零散文件直接删除；工程的媒体文件在其所在文件夹位于库根之下时，
        // 连同其所在文件夹一并删除。
        Path parent = media.getParent();
        if (parent != null && parent.startsWith(root) && !parent.equals(root)
                && WallpaperProject.isProject(parent)) {
            deleteTree(parent);
        } else {
            Files.deleteIfExists(media);
            // 同时删除它旁边的孤立预览图。
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
