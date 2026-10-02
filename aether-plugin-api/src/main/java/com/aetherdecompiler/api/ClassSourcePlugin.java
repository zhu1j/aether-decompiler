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

/**
 * 扩展点 #1 —— 类字节的提供者。
 *
 * <p>实现位于插件中（例如 {@code plugin-source-jar}），并通过
 * {@link java.util.ServiceLoader} 被发现。把类的来源藏在插件之后，意味着内核
 * 永不硬编码“jar”或“目录”：新增一种来源（网络、内存、加密归档）纯粹是
 * 插件工作。</p>
 *
 * <p>故事类比：扩展点是一个固定形状的电源插座。任何电器 —— jar、文件夹、
 * 内存缓冲区 —— 都可以插上去，而无需给工厂重新布线。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public interface ClassSourcePlugin {

    /**
     * @return 唯一、稳定的插件 id，例如 {@code "source.jar"}
     */
    String id();

    /**
     * @return 供 UI 显示的人类可读名称
     */
    String displayName();

    /**
     * 为给定的、插件特有的定位符打开一个类来源。
     *
     * @param locator 插件定义的定位符字符串（路径、URL、键……）
     * @param options 引擎配置快照
     * @return 一个已打开的 {@link ClassSource}；由调用方负责关闭
     */
    ClassSource open(String locator, Options options);
}
