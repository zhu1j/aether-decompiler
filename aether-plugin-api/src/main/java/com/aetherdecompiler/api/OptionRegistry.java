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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * core 与插件贡献的 {@link OptionSpec} 注册表。
 *
 * <p>内核在激活期间向每个插件暴露一个 {@code OptionRegistry}，以便插件发布
 * 自己的选项。应用层随后查询同一个注册表来构建它的 CLI/GUI。正是这套机制
 * 让“存在哪些选项”脱离了内核的源代码。</p>
 *
 * <p>故事类比：注册表是工厂的总配电盘索引 —— 每个部门把它的开关拧到同一块
 * 面板上，操作员从一个地方读取整块面板。</p>
 *
 * <p>本类刻意狭窄且非线程安全；插件在单线程的激活阶段进行注册。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class OptionRegistry {

    private final Map<String, OptionSpec> specs = new LinkedHashMap<>();

    /**
     * 注册一个 spec。对同一个键重复注册会替换先前的 spec；
     * 这使插件重载具备幂等性。
     *
     * @param spec 要注册的 spec
     * @return 本注册表，便于链式调用
     */
    public OptionRegistry register(OptionSpec spec) {
        specs.put(spec.key(), spec);
        return this;
    }

    /**
     * @param key 选项键
     * @return 某个键的 spec（若已定义）
     */
    public Optional<OptionSpec> find(String key) {
        return Optional.ofNullable(specs.get(key));
    }

    /**
     * @return 全部已注册的 spec，按注册顺序
     */
    public Collection<OptionSpec> all() {
        return Collections.unmodifiableCollection(specs.values());
    }

    /**
     * @return “键到 spec”映射的不可变副本
     */
    public Map<String, OptionSpec> asMap() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(specs));
    }
}
