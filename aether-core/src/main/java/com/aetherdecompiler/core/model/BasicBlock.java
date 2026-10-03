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
package com.aetherdecompiler.core.model;

import com.aetherdecompiler.api.CfgView;

import java.util.List;
import java.util.Objects;

/**
 * 不可变基本块：一段具有单一入口和单一出口的极大指令序列。
 *
 * <p>一个块拥有一个闭区间的指令索引，并通过块 id 引用其后继。把后继保存为
 * id（而非对象引用）可保持不可变性，并使该图易于序列化、对缓存友好。</p>
 *
 * <p>故事类比：一段过程的一个段落 —— 从顶部进入，从底部离开，
 * 从不被跳入其中间。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class BasicBlock implements CfgView.Block {

    private final int id;
    private final int firstInsn;
    private final int lastInsn;
    private final List<Integer> successors;
    private final List<Integer> exceptionSuccessors;
    private final boolean entry;

    /**
     * @param id                   块 id（等于其在 CFG 列表中的位置）
     * @param firstInsn            含首的起始指令索引
     * @param lastInsn             含尾的结束指令索引
     * @param successors           普通控制流后继块 id
     * @param exceptionSuccessors  经异常表可达的处理器块 id
     * @param entry                是否为方法入口块
     */
    public BasicBlock(int id, int firstInsn, int lastInsn,
                      List<Integer> successors, List<Integer> exceptionSuccessors,
                      boolean entry) {
        this.id = id;
        this.firstInsn = firstInsn;
        this.lastInsn = lastInsn;
        this.successors = List.copyOf(successors);
        this.exceptionSuccessors = List.copyOf(exceptionSuccessors);
        this.entry = entry;
    }

    /** @return 块 id */
    public int id() {
        return id;
    }

    /** @return 含首的起始指令索引 */
    public int firstInsn() {
        return firstInsn;
    }

    /** @return 含尾的结束指令索引 */
    public int lastInsn() {
        return lastInsn;
    }

    /** @return 普通后继块 id */
    public List<Integer> successors() {
        return successors;
    }

    /** @return 异常处理器块 id */
    public List<Integer> exceptionSuccessors() {
        return exceptionSuccessors;
    }

    /** @return 是否为入口块 */
    public boolean isEntry() {
        return entry;
    }

    /** @return 本块跨越多少条指令 */
    public int instructionCount() {
        return lastInsn - firstInsn + 1;
    }

    /** @return 简短的显示标签，例如 {@code "B3[10..15]"} */
    public String label() {
        return "B" + id + "[" + firstInsn + ".." + lastInsn + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BasicBlock other)) {
            return false;
        }
        return id == other.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return label();
    }
}
