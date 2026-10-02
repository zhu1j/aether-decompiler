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
package com.aetherdecompiler.core.event;

import com.aetherdecompiler.api.AetherEvent;
import com.aetherdecompiler.api.AetherListener;
import com.aetherdecompiler.api.EventBus;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 最小、无依赖的 {@link EventBus} 实现。
 *
 * <p>采用写时复制列表，使得流水线可能正在发布时订阅仍是安全的；遍历永不阻塞，
 * 也永不抛出 {@code ConcurrentModificationException}。抛出异常的监听器会被
 * 隔离，因此一个坏观察者无法拖垮引擎。</p>
 *
 * <p>故事类比：一块公告板，规则是“把你的启事钉上就走开；被撕坏的启事会被
 * 忽略，绝不允许它把整面墙弄塌。”</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class SimpleEventBus implements EventBus {

    private final List<AetherListener<?>> listeners = new CopyOnWriteArrayList<>();

    @Override
    public <E extends AetherEvent> void subscribe(AetherListener<E> listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    @Override
    public void publish(AetherEvent event) {
        for (AetherListener<?> listener : listeners) {
            try {
                publishTo(listener, event);
            } catch (RuntimeException ex) {
                // 观察绝不能破坏流水线。
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends AetherEvent> void publishTo(AetherListener<E> listener, AetherEvent event) {
        listener.onEvent((E) event);
    }

    /** @return 已订阅监听器的数量 */
    public int listenerCount() {
        return listeners.size();
    }
}
