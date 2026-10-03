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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 引擎配置的不可变快照。
 *
 * <p>设计契约（在 1.0 主版本冻结）：内核消费一个 {@code Options} 值，
 * 而绝不自行解析命令行字符串。选项<em>定义</em>（名称、类型、默认值、帮助
 * 文本）由插件通过 {@link OptionRegistry} 贡献；应用层（CLI/GUI）把用户输入
 * 解析为一个 {@code Options} 并向下传递。这让内核不含任何 UI 关注点，并让
 * 插件能够新增选项而无需改动内核。</p>
 *
 * <p>故事类比：{@code Options} 是一份密封、已签署的工单，从前台流转到工厂
 * 车间。车间从不接受口头请求；它只执行一份完整说明、已签署的工单。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class Options {

    private static final Options EMPTY = new Options(Collections.emptyMap());

    private final Map<String, Object> values;

    private Options(Map<String, Object> values) {
        this.values = values;
    }

    /**
     * @return 空选项集（全部使用默认值）
     */
    public static Options empty() {
        return EMPTY;
    }

    /**
     * @return 一个新的构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return 某个键的原始值，或 {@code null}
     */
    public Object raw(String key) {
        return values.get(key);
    }

    /**
     * @param key 选项键
     * @return 某个键的字符串值（若存在）
     */
    public Optional<String> getString(String key) {
        Object v = values.get(key);
        return v == null ? Optional.empty() : Optional.of(String.valueOf(v));
    }

    /**
     * @param key          选项键
     * @param defaultValue 不存在时返回的值
     * @return 布尔值，由字符串/布尔存储强制转换而来
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        Object v = values.get(key);
        if (v == null) {
            return defaultValue;
        }
        if (v instanceof Boolean b) {
            return b;
        }
        return Boolean.parseBoolean(String.valueOf(v));
    }

    /**
     * @param key          选项键
     * @param defaultValue 不存在或无法解析时返回的值
     * @return 整数值
     */
    public int getInt(String key, int defaultValue) {
        Object v = values.get(key);
        if (v == null) {
            return defaultValue;
        }
        if (v instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * @param key          选项键
     * @param defaultValue 不存在或无法解析时返回的值
     * @return 长整数值
     */
    public long getLong(String key, long defaultValue) {
        Object v = values.get(key);
        if (v == null) {
            return defaultValue;
        }
        if (v instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * @return 全部已设值的不可变视图
     */
    public Map<String, Object> asMap() {
        return values;
    }

    /**
     * 返回一个新增或替换了某个键的新 {@code Options}，保持本实例不变。
     *
     * @param key   选项键
     * @param value 新值
     * @return 新的不可变快照
     */
    public Options with(String key, Object value) {
        Objects.requireNonNull(key, "key");
        Map<String, Object> copy = new LinkedHashMap<>(values);
        copy.put(key, value);
        return new Options(Collections.unmodifiableMap(copy));
    }

    /**
     * {@code Options} 的流式构建器。保留插入顺序，使调试转储具有确定性。
     *
     * @author Jerry Zhu (Zeek)
     */
    public static final class Builder {
        private final Map<String, Object> values = new LinkedHashMap<>();

        private Builder() {
        }

        /**
         * @param key   选项键
         * @param value 值
         * @return 本构建器
         */
        public Builder set(String key, Object value) {
            Objects.requireNonNull(key, "key");
            values.put(key, value);
            return this;
        }

        /**
         * @return 不可变的 {@code Options}
         */
        public Options build() {
            if (values.isEmpty()) {
                return EMPTY;
            }
            return new Options(Collections.unmodifiableMap(new LinkedHashMap<>(values)));
        }
    }

    @Override
    public String toString() {
        return "Options" + values;
    }
}
