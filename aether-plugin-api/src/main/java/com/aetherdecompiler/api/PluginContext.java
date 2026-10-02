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
 * 在激活时交给每个插件的、以读为主的宿主视图。
 *
 * <p>这是插件用来访问宿主服务的唯一入口。它携带配置快照、共享选项注册表、
 * 事件总线，以及一个用于按需读取更多类字节的解析器。通过把一切路由到一个
 * 狭窄接口，宿主可以在不破坏插件的前提下演进其内部实现。</p>
 *
 * <p>故事类比：工厂门口的访客证与对讲机。访客证说明访客可以看什么；
 * 对讲机让他们向前台询问。访客从不无陪同地游荡厂区。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public interface PluginContext {

    /**
     * @return 本次运行不可变的配置快照
     */
    Options options();

    /**
     * @return 共享的选项注册表，以便插件发布自己的选项
     */
    OptionRegistry optionRegistry();

    /**
     * @return 事件总线，以便插件发布或订阅事件
     */
    EventBus eventBus();

    /**
     * 使用宿主当前正在读取的 {@link ClassSource}，按内部名解析类字节。
     *
     * @param internalName 内部二进制名，例如 {@code com/foo/Bar}
     * @return 类字节；若无法解析则为 {@code null}
     */
    byte[] resolveClassBytes(String internalName);
}
