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
package com.aetherdecompiler.api;

import java.util.Objects;

/**
 * 单个配置选项的声明式元数据。
 *
 * <p>插件把它们的选项声明为 {@code OptionSpec} 并注册到
 * {@link OptionRegistry}。内核与应用层随后便能以通用方式发现、校验、编写
 * 文档并解析这些选项 —— 而无需知道是哪个插件声明了它们。</p>
 *
 * <p>故事类比：这是控制面板某个开关上的标签：它的名称、它的物理类型、
 * 它的出厂默认值，以及解释它的贴纸。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class OptionSpec {

    /**
     * 选项所接受的值域。刻意保持小巧且无依赖。
     */
    public enum Kind {
        /** 是/否标志。 */
        BOOLEAN,
        /** 整数。 */
        INTEGER,
        /** 长整数。 */
        LONG,
        /** 任意文本。 */
        STRING
    }

    private final String key;
    private final Kind kind;
    private final String defaultValue;
    private final String description;
    private final String declaredBy;

    private OptionSpec(String key, Kind kind, String defaultValue,
                       String description, String declaredBy) {
        this.key = Objects.requireNonNull(key, "key");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.defaultValue = defaultValue;
        this.description = description == null ? "" : description;
        this.declaredBy = declaredBy == null ? "aether" : declaredBy;
    }

    /**
     * @param key          选项键，例如 {@code "output.dir"}
     * @param kind         值域
     * @param defaultValue 文本形式的默认值（可为 {@code null}）
     * @param description  人类帮助文本
     * @param declaredBy   声明它的插件 id（内核则为 {@code "aether"}）
     * @return 新的 spec
     */
    public static OptionSpec of(String key, Kind kind, String defaultValue,
                                String description, String declaredBy) {
        return new OptionSpec(key, kind, defaultValue, description, declaredBy);
    }

    /**
     * @param key         选项键
     * @param defaultValue 默认文本值
     * @param description 帮助文本
     * @param declaredBy  声明它的插件 id
     * @return 一个 {@code STRING} spec
     */
    public static OptionSpec string(String key, String defaultValue, String description, String declaredBy) {
        return new OptionSpec(key, Kind.STRING, defaultValue, description, declaredBy);
    }

    /**
     * @param key         选项键
     * @param defaultValue 文本形式的默认布尔值
     * @param description 帮助文本
     * @param declaredBy  声明它的插件 id
     * @return 一个 {@code BOOLEAN} spec
     */
    public static OptionSpec bool(String key, boolean defaultValue, String description, String declaredBy) {
        return new OptionSpec(key, Kind.BOOLEAN, Boolean.toString(defaultValue), description, declaredBy);
    }

    /** @return 选项键 */
    public String key() {
        return key;
    }

    /** @return 值域 */
    public Kind kind() {
        return kind;
    }

    /** @return 文本形式的默认值，或 {@code null} */
    public String defaultValue() {
        return defaultValue;
    }

    /** @return 人类帮助文本 */
    public String description() {
        return description;
    }

    /** @return 声明它的插件 id；内核则为 {@code "aether"} */
    public String declaredBy() {
        return declaredBy;
    }

    @Override
    public String toString() {
        return "OptionSpec{" + key + " : " + kind
                + (defaultValue != null ? " = " + defaultValue : "")
                + " by " + declaredBy + "}";
    }
}
