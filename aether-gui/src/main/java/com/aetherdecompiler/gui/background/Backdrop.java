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

import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;

/**
 * 对工作台可在工作区之后绘制的一张背景的不可变描述：它的标题、介质
 * （静止图像或视频）、媒体文件，以及可选的预览缩略图。
 *
 * <p>一张背景既可以来自裸的图片/视频文件，也可以来自 Wallpaper&nbsp;Engine
 * 工程文件夹（见 {@link WallpaperProject}）；本类型是二者被归一化成的共同
 * 形态，因此工作台永远不必关心它的来源是哪一种。</p>
 *
 * <p>故事类比：放映机片库里的一卷“胶片” —— 无论它是手绘在幻灯片上，
 * 还是用摄影机拍摄的。</p>
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
     * @param title   显示标题
     * @param media   媒体文件
     * @param preview 可选缩略图（可为 {@code null}）
     * @return 一种类型由媒体扩展名推断得出的背景
     */
    public static Backdrop of(String title, Path media, Path preview) {
        return new Backdrop(title, kindOf(media), media, preview);
    }

    /**
     * @param path 扩展名可标识媒体类型的文件
     * @return 由扩展名推断出的类型，或 {@link BackdropKind#NONE}
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

    /** @return 某个扩展名是否为受支持的背景图片 */
    public static boolean isImage(String name) {
        return IMAGE_EXT.contains(suffix(name));
    }

    /** @return 某个扩展名是否为受支持的背景视频 */
    public static boolean isVideo(String name) {
        return VIDEO_EXT.contains(suffix(name));
    }

    /** @return 某个扩展名是否为任意受支持的背景媒体 */
    public static boolean isMedia(String name) {
        return isImage(name) || isVideo(name);
    }

    private static String suffix(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        int dot = lower.lastIndexOf('.');
        return dot < 0 ? "" : lower.substring(dot);
    }

    /** @return 显示标题 */
    public String title() {
        return title;
    }

    /** @return 媒体类型 */
    public BackdropKind kind() {
        return kind;
    }

    /** @return 媒体文件 */
    public Path media() {
        return media;
    }

    /** @return 缩略图；若无则返回 {@code null} */
    public Path preview() {
        return preview;
    }

    /** @return 该背景是否为视频 */
    public boolean isVideo() {
        return kind == BackdropKind.VIDEO;
    }

    @Override
    public String toString() {
        return title + "  [" + kind.name().toLowerCase(Locale.ROOT) + "]";
    }
}
