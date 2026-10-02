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
 * 流水线事件的订阅者。
 *
 * <p>硬性约束 #8 要求每一步中间结果（CFG、SSA、AST）都可观测。本接口是唯一的
 * 订阅界面：内核发出带类型的 {@link AetherEvent}，而观察类插件（或 GUI 的
 * 进度条，或未来的 AI 辅助逆向工程工具）消费它们，内核并不知道谁在监听。</p>
 *
 * <p>故事类比：工厂车间的公共广播系统。各工位播报“阶段完成”；任意数量的
 * 监听者 —— 主管、记录员、仪表盘 —— 都可以收听。工位从不指名道姓地
 * 呼叫某个监听者。</p>
 *
 * <p>实现必须快，且绝不可抛出：一个缓慢或失败的监听器会拖慢甚至破坏
 * 流水线。</p>
 *
 * @author Jerry Zhu (Zeek)
 * @param <E> 该监听器接受的事件类型
 */
@FunctionalInterface
public interface AetherListener<E extends AetherEvent> {

    /**
     * 接收一个事件。
     *
     * @param event 事件；永不为 {@code null}
     */
    void onEvent(E event);
}
