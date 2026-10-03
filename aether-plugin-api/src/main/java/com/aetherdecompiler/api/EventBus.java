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
 * 把引擎事件发布给任意数量监听器的总线。
 *
 * <p>内核向总线发布；它从不直接引用某个监听器。这反转了依赖，并使可观测性
 * 成为一种纯粹的附加能力。</p>
 *
 * <p>故事类比：广播话筒。工位对着话筒讲话，并不知道也不关心
 * 有多少喇叭接在上面。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public interface EventBus {

    /**
     * 订阅一个监听器以接收所有事件。
     *
     * @param listener 要添加的监听器
     * @param <E>      该监听器接受的事件类型
     */
    <E extends AetherEvent> void subscribe(AetherListener<E> listener);

    /**
     * 把一个事件发布给每个订阅者。
     *
     * @param event 要发布的事件
     */
    void publish(AetherEvent event);
}
