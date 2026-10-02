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
import java.util.Locale;
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
 * <p>该解析器刻意是一个极小、无依赖、宽容的读取器：它用一个<strong>深度感知</strong>
 * 的扫描器读取少数几个具名字符串键，而不是把 JSON 库拉进 GUI。之所以必须是
 * 深度感知的，是因为 Wallpaper Engine 的真实文件会在嵌套对象里复用同名的键
 * —— 例如 {@code schemecolor} 对象内部也含有 {@code "type" : "color"}，而它
 * 往往排在顶层 {@code "type" : "video"} 之前。若用朴素的“首个匹配”正则，就会
 * 把工程类型误读为 {@code color}，进而让视频壁纸被当成无效背景。本读取器只在
 * 根对象的深度上匹配这些键，因此同名的嵌套键不会再造成干扰。</p>
 *
 * <p>故事类比：一位双语助理，只需在标签上认出几个已知的词，
 * 而不必通读整本手册 —— 而且他知道要看最外层那张标签，
 * 而不是抽屉深处的一张。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class WallpaperProject {

    private static final Pattern SCHEME_COLOR_KEY =
            Pattern.compile("\"schemecolor\"\\s*:");
    private static final Pattern VALUE_KEY =
            Pattern.compile("\"value\"\\s*:\\s*\"([^\"]*)\"");

    private final Path directory;
    private final Path projectJson;
    private final String title;
    private final String type;
    private final String description;
    private final Path media;
    private final Path preview;
    private final String schemeColor;

    private WallpaperProject(Path directory, Path projectJson, String title, String type,
                             String description, Path media, Path preview, String schemeColor) {
        this.directory = directory;
        this.projectJson = projectJson;
        this.title = title;
        this.type = type;
        this.description = description;
        this.media = media;
        this.preview = preview;
        this.schemeColor = schemeColor;
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

        // 只读取根对象深度上的键，避开 schemecolor 等嵌套对象里的同名键。
        String file = topLevelString(text, "file");
        String preview = topLevelString(text, "preview");
        String title = topLevelString(text, "title");
        String type = topLevelString(text, "type");
        String description = topLevelString(text, "description");
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
                description == null ? "" : description, media, thumb, schemeColor);
    }

    /** @return {@code type} 字段（video / scene / web / application），已转为小写 */
    public String type() {
        return type == null ? "" : type.toLowerCase(Locale.ROOT);
    }

    /** @return {@code description} 字段，或空字符串 */
    public String description() {
        return description == null ? "" : description;
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

    /**
     * 读取某个字符串键的值，但仅当该键出现在根对象（JSON 的最外层）中时才返回。
     *
     * <p>实现是一个极小的字符级扫描器：它跟踪花括号/方括号的嵌套深度，并在
     * 深度为 1（即根对象的成员位置）处匹配 {@code "key"} 后紧跟的字符串值。
     * 与键同名的嵌套键（例如 {@code schemecolor} 内部的 {@code "type"}）位于
     * 更深的层级，因而被自然忽略。</p>
     *
     * @param text 完整的 {@code project.json} 文本
     * @param key  顶层键名（不含引号）
     * @return 该键的字符串值；若不存在或值不是字符串则返回 {@code null}
     */
    private static String topLevelString(String text, String key) {
        String needle = "\"" + key + "\"";
        int depth = 0;
        boolean inStr = false;
        boolean esc = false;
        int n = text.length();
        for (int i = 0; i < n; i++) {
            char c = text.charAt(i);
            if (inStr) {
                if (esc) {
                    esc = false;
                } else if (c == '\\') {
                    esc = true;
                } else if (c == '"') {
                    inStr = false;
                }
                continue;
            }
            if (c == '"') {
                if (depth == 1 && text.startsWith(needle, i)) {
                    int j = i + needle.length();
                    while (j < n && Character.isWhitespace(text.charAt(j))) {
                        j++;
                    }
                    if (j < n && text.charAt(j) == ':') {
                        j++;
                        while (j < n && Character.isWhitespace(text.charAt(j))) {
                            j++;
                        }
                        if (j < n && text.charAt(j) == '"') {
                            StringBuilder sb = new StringBuilder();
                            int k = j + 1;
                            while (k < n && text.charAt(k) != '"') {
                                if (text.charAt(k) == '\\' && k + 1 < n) {
                                    k++;
                                }
                                sb.append(text.charAt(k));
                                k++;
                            }
                            return sb.toString();
                        }
                    }
                }
                inStr = true;
                continue;
            }
            if (c == '{' || c == '[') {
                depth++;
            } else if (c == '}' || c == ']') {
                depth--;
            }
        }
        return null;
    }

    private static Path resolve(Path dir, String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        // Wallpaper Engine 有时用反斜杠书写相对路径；先规范化再解析。
        String normalized = name.replace('\\', '/');
        Path p = dir.resolve(normalized);
        return Files.isRegularFile(p) ? p : null;
    }

    /**
     * 当 {@code file} 缺失或不可解析时的回退：扫描文件夹，返回首个受支持的媒体，
     * 并优先选择视频（视频壁纸工程最常见）。
     */
    private static Path firstMedia(Path dir) {
        try (var stream = Files.list(dir)) {
            var candidates = stream.filter(Files::isRegularFile)
                    .filter(p -> Backdrop.isMedia(p.getFileName().toString()))
                    .filter(p -> !p.getFileName().toString().toLowerCase(Locale.ROOT)
                            .startsWith("preview"))
                    .sorted()
                    .toList();
            for (Path p : candidates) {
                String name = p.getFileName().toString().toLowerCase(Locale.ROOT);
                if (name.endsWith(".mp4") || name.endsWith(".m4v") || name.endsWith(".mov")
                        || name.endsWith(".webm") || name.endsWith(".flv")) {
                    return p;
                }
            }
            return candidates.isEmpty() ? null : candidates.get(0);
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
        return Backdrop.of(title, media, preview, description);
    }
}
