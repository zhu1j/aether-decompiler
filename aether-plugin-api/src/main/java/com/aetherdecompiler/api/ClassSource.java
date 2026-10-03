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

import java.util.List;
import java.util.Optional;

/**
 * 可随机访问的类字节来源。
 *
 * <p>该契约的存在是为满足“渐进/惰性加载”的硬性约束：引擎必须能够廉价地列出
 * 可用的类名，并按需恰好读取某一个类的字节，而无需把整个 jar 载入内存。
 * 实现可以基于 jar 文件、目录、单个 {@code .class} 文件或内存中的字节数组。</p>
 *
 * <p>故事类比：图书馆的卡片目录。你可以读取索引（廉价），然后从架上取下一本
 * 书（按需）—— 你从不复印整座图书馆。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public interface ClassSource extends AutoCloseable {

    /**
     * @return 对本来源的简短人类可读描述，用于日志
     */
    String describe();

    /**
     * @return 本来源能够提供的所有类的内部二进制名
     *         （例如 {@code com/foo/Bar}），且不读取它们的字节
     */
    List<String> classNames();

    /**
     * @param internalName 内部二进制名，例如 {@code com/foo/Bar}
     * @return 原始类字节；若不存在则返回 {@link Optional#empty()}
     */
    Optional<byte[]> readClass(String internalName);

    @Override
    void close();
}
