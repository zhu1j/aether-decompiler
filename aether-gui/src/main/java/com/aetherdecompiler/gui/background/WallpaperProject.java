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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Wallpaper&nbsp;Engine 工程文件夹的读取器。
 *
 * <p>一个 Wallpaper Engine 工程是一个包含 {@code project.json} 描述符以及
 * 壁纸媒体文件与 {@code preview} 缩略图的目录。该描述符携带工作台关心的
 * 字段：</p>
 *
 * <pre>{@code
 * {
 *   "file":      "IMG_0505.mp4",     // 壁纸实际播放的媒体
 *   "preview":   "preview.jpg",      // 浏览器/创作器中显示的缩略图
 *   "title":     "吾王美如画",          // 显示标题
 *   "type":      "video",            // 类型：video | scene | web | application
 *   "general": { "properties": { "schemecolor": { "value": "0.15 0.43 0.90" } } }
 * }
 * }</pre>
 *
 * <p>该解析器刻意是一个极小、无依赖、宽容的读取器：它用正则表达式查找少数
 * 几个具名字符串键，而不是把 JSON 库拉进 GUI。Wallpaper Engine 文件简单而
 * 稳定，缺失的键会被当作“不存在”而非致命错误，因此一个不寻常的工程仍能通过
 * 回退到文件夹中的首个媒体文件而产出可用的背景。</p>
 *
 * <p>故事类比：一位双语助理，只需在标签上认出几个已知的词，
 * 而不必通读整本手册。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class WallpaperProject {

    private static final Pattern FILE_KEY = stringKey("file");
    private static final Pattern PREVIEW_KEY = stringKey("preview");
    private static final Pattern TITLE_KEY = stringKey("title");
    private static final Pattern TYPE_KEY = stringKey("type");
    // “schemecolor” 映射到一个对象，所以只匹配该键；其 “value” 字符串
    // 在此键之后单独定位。
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
     * @param directory 一个 Wallpaper Engine 工程文件夹
     * @return 该文件夹是否看起来像一个 Wallpaper Engine 工程
     */
    public static boolean isProject(Path directory) {
        return directory != null
                && Files.isDirectory(directory)
                && Files.isRegularFile(directory.resolve("project.json"));
    }

    /**
     * 解析一个 Wallpaper Engine 工程文件夹。
     *
     * @param directory 包含 {@code project.json} 的文件夹
     * @return 解析后的工程
     * @throws IOException 若描述符缺失或不可读
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

    /** @return {@code type} 字段（video / scene / web / application），已转为小写 */
    public String type() {
        return type == null ? "" : type.toLowerCase(java.util.Locale.ROOT);
    }

    /**
     * @return 由 Wallpaper Engine 的 {@code "r g b"} 浮点三元组解析得的方案色
     *         {@code #rrggbb} 字符串；或 {@code null}
     */
    public String schemeColor() {
        return schemeColor;
    }

    private static String parseSchemeColor(String text) {
        Matcher sc = SCHEME_COLOR_KEY.matcher(text);
        if (!sc.find()) {
            return null;
        }
        // 在 schemecolor 键之后查找第一个 “value” 字符串。
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

    /** @return 工程文件夹 */
    public Path directory() {
        return directory;
    }

    /** @return {@code project.json} 路径 */
    public Path projectJson() {
        return projectJson;
    }

    /** @return 显示标题 */
    public String title() {
        return title;
    }

    /** @return 壁纸的媒体文件 */
    public Path media() {
        return media;
    }

    /** @return 预览缩略图，或 {@code null} */
    public Path preview() {
        return preview;
    }

    /** @return 归一化为 {@link Backdrop} 的工程 */
    public Backdrop toBackdrop() {
        return Backdrop.of(title, media, preview);
    }
}
