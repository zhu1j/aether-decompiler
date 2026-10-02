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
package com.aetherdecompiler.core.source;

import com.aetherdecompiler.api.ClassSource;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 基于“名称到字节”映射的内存内 {@link ClassSource}。
 *
 * <p>供单元测试以及已持有类字节的调用方使用（例如网络插件或 IDE 缓冲区）。</p>
 *
 * <p>故事类比：一只只装着你已经取来的零件的工具箱 —— 没有柜子，没有翻找，
 * 只有手上的这些。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class InMemoryClassSource implements ClassSource {

    private final String label;
    private final Map<String, byte[]> classes;

    /**
     * @param label   用于日志的描述
     * @param classes 内部名到字节的映射
     */
    public InMemoryClassSource(String label, Map<String, byte[]> classes) {
        this.label = label == null ? "memory" : label;
        this.classes = Map.copyOf(classes);
    }

    @Override
    public String describe() {
        return "memory:" + label;
    }

    @Override
    public List<String> classNames() {
        return List.copyOf(classes.keySet());
    }

    @Override
    public Optional<byte[]> readClass(String internalName) {
        byte[] bytes = classes.get(internalName);
        return bytes == null ? Optional.empty() : Optional.of(bytes.clone());
    }

    @Override
    public void close() {
        // 内存内来源无需释放任何东西。
    }
}
