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
package com.aetherdecompiler.gui.skin;

import java.net.URL;
import java.util.Objects;

/**
 * 对一款视觉皮肤的不可变描述。
 *
 * <p>皮肤是纯应用层概念：它是一张样式表加上元数据，并可选择性地附带一张显示
 * 在工作区之后的默认背景图。它绝不触碰内核、插件或引擎，因此为第三方添加一套
 * 外观是零风险、零重编译的操作。</p>
 *
 * <p>背景图刻意以描述符（URL + 透明度）形式保存，而不是在此渲染：皮肤说明
 * 外观<em>是什么</em>，而应用决定<em>如何</em>绘制它（作为一层半透明图层）。</p>
 *
 * <p>故事类比：同一辆车可互换的一套座椅套。引擎、车轮与线路原封不动；
 * 只有外观改变。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class Skin {

    /** 导入背景时若未指定透明度，则使用该默认值。 */
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
     * @param id      稳定的皮肤 id
     * @param name    显示名称
     * @param author  作者字符串
     * @param accent  用于选择器色板的强调色
     * @param bg      用于选择器色板的背景色
     * @param cssUrl  样式表 URL
     * @param builtin 是否为随应用内置
     * @return 一个没有默认背景的新型皮肤描述符
     */
    public static Skin of(String id, String name, String author,
                          String accent, String bg, URL cssUrl, boolean builtin) {
        return new Skin(id, name, author, accent, bg, cssUrl, builtin, null, DEFAULT_BACKGROUND_OPACITY);
    }

    /**
     * 返回携带默认背景图的本皮肤副本。
     *
     * @param image   背景图 URL（可为 {@code null} 以清除）
     * @param opacity 背景透明度，取值于 {@code (0, 1]}
     * @return 新的皮肤描述符
     */
    public Skin withBackground(URL image, double opacity) {
        return new Skin(id, name, author, swatchAccent, swatchBg, cssUrl, builtin, image, opacity);
    }

    /** @return 稳定的皮肤 id */
    public String id() {
        return id;
    }

    /** @return 显示名称 */
    public String name() {
        return name;
    }

    /** @return 作者字符串 */
    public String author() {
        return author;
    }

    /** @return 选择器色板的强调色 */
    public String swatchAccent() {
        return swatchAccent;
    }

    /** @return 选择器色板的背景色 */
    public String swatchBg() {
        return swatchBg;
    }

    /** @return 样式表 URL */
    public URL cssUrl() {
        return cssUrl;
    }

    /** @return 该皮肤是否随应用内置 */
    public boolean isBuiltin() {
        return builtin;
    }

    /** @return 默认背景图 URL；若该皮肤没有则返回 {@code null} */
    public URL backgroundImage() {
        return backgroundImage;
    }

    /** @return 默认背景透明度，取值于 {@code (0, 1]} */
    public double backgroundOpacity() {
        return backgroundOpacity;
    }

    /** @return 该皮肤是否声明了默认背景图 */
    public boolean hasBackground() {
        return backgroundImage != null;
    }

    /** @return {@code Scene.getStylesheets()} 所需的外部字符串形式 */
    public String externalForm() {
        return cssUrl.toExternalForm();
    }

    @Override
    public String toString() {
        return name + (builtin ? "" : "  \u2022 user");
    }
}
