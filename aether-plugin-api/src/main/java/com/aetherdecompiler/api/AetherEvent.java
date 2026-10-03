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
 * 引擎流水线发出的每个事件的基类型。
 *
 * <p>一个事件总是携带：它所属的流水线阶段、该时刻对象的抽象种类、一条人类
 * 可读消息，以及当前时间。它可选地携带 {@link IRObject} 本身，以便监听器
 * 检查中间结果。</p>
 *
 * <p>故事类比：一张运单加上一个窥视孔。运单说明哪一站、哪个阶段；
 * 窥视孔可选地让监听器看到实物零件。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public class AetherEvent {

    /**
     * 一个事件所属的粗粒度流水线阶段。刻意避免比这更细的粒度，
     * 以让监听器保持廉价。
     */
    public enum Phase {
        /** 读取原始类字节。 */
        READ,
        /** 构建类/方法模型。 */
        MODEL,
        /** 构建 CFG 与支配树。 */
        CFG,
        /** 构建 SSA 并推断类型。 */
        SSA,
        /** 构建抽象语法树。 */
        AST,
        /** 某个插件特有的阶段。 */
        PLUGIN
    }

    private final Phase phase;
    private final IRKind kind;
    private final String message;
    private final long timestampNanos;
    private final IRObject payload;

    /**
     * @param phase         流水线阶段（永不为 {@code null}）
     * @param kind          中间对象的种类（永不为 {@code null}）
     * @param message       人类可读的描述
     * @param payload       中间对象；若无则为 {@code null}
     */
    public AetherEvent(Phase phase, IRKind kind, String message, IRObject payload) {
        this.phase = Objects.requireNonNull(phase, "phase");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.message = message == null ? "" : message;
        this.payload = payload;
        this.timestampNanos = System.nanoTime();
    }

    /** @return 流水线阶段 */
    public Phase phase() {
        return phase;
    }

    /** @return 中间对象的种类 */
    public IRKind kind() {
        return kind;
    }

    /** @return 人类可读的描述 */
    public String message() {
        return message;
    }

    /** @return 事件创建时捕获的单调时间戳 */
    public long timestampNanos() {
        return timestampNanos;
    }

    /** @return 中间对象；若未附加则为 {@code null} */
    public IRObject payload() {
        return payload;
    }

    @Override
    public String toString() {
        return "[" + phase + "/" + kind + "] " + message;
    }
}
