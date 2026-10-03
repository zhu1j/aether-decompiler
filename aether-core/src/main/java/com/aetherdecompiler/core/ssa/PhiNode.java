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
package com.aetherdecompiler.core.ssa;

import java.util.List;

/**
 * SSA 形式中的 <strong>phi 函数</strong>：一个控制流汇合点上的“按来路取值”节点。
 *
 * <p>当两条（或多条）控制流路径在某个基本块汇合，而它们对同一个槽给出了不同版本时，
 * 汇合点之后到底该用哪个版本？phi 节点回答这个问题：它为该块的<em>每一条前驱边</em>准备
 * 一个操作数，运行时“沿着实际走到这里的那条边”选取对应操作数。phi 本身定义一个全新的
 * 版本（{@link #version()}）。</p>
 *
 * <p>phi 只可能出现在拥有至少两条前驱的块中（函数入口除外），且只对“确实有多路定值”
 * 的槽插入——这正是支配边界（{@link DominanceFrontier}）告诉我们的事情：支配边界恰好
 * 是“定义了某变量、却未支配该点”的那些块的汇合处。</p>
 *
 * <p>构造期可变、发布后只读：操作数在前缀重命名（renaming）阶段被逐边填充，方法返回一个
 * 填好的 {@link SsaForm} 后即视为不可变。</p>
 *
 * <p>故事类比：汇流处的调度员。每位从不同支流漂来的船夫都预先交出各自的货箱；调度员只
 * 挑那个真正到岸的。 </p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class PhiNode {

    private final int blockId;
    private final int slot;
    private final int version;
    private final List<Integer> predecessorBlocks;
    private final SsaVariable[] operands;

    /**
     * @param blockId           承载该 phi 的块 id
     * @param slot              所合并的局部变量槽位
     * @param version           该 phi 定义的版本号
     * @param predecessorBlocks 前驱块 id，按边顺序排列；操作数与之逐项对应
     */
    public PhiNode(int blockId, int slot, int version, List<Integer> predecessorBlocks) {
        this.blockId = blockId;
        this.slot = slot;
        this.version = version;
        this.predecessorBlocks = List.copyOf(predecessorBlocks);
        this.operands = new SsaVariable[predecessorBlocks.size()];
    }

    /** @return 承载该 phi 的块 id */
    public int blockId() {
        return blockId;
    }

    /** @return 所合并的局部变量槽位 */
    public int slot() {
        return slot;
    }

    /** @return 该 phi 定义的版本号 */
    public int version() {
        return version;
    }

    /** @return 前驱块 id 的不可变列表（与操作数逐项对应） */
    public List<Integer> predecessorBlocks() {
        return predecessorBlocks;
    }

    /** @return 该 phi 定义的 SSA 值 */
    public SsaVariable definedVariable() {
        return new SsaVariable(slot, version);
    }

    /**
     * @param edgeIndex 前驱边的序号，取值 {@code [0, predecessorBlocks().size())}
     * @return 该边对应的操作数；尚未填充时为 {@code null}
     */
    public SsaVariable operand(int edgeIndex) {
        return operands[edgeIndex];
    }

    /**
     * @param predecessorBlock 前驱块 id
     * @return 来自该前驱的操作数；若该块不是前驱或尚未填充则为 {@code null}
     */
    public SsaVariable operandFrom(int predecessorBlock) {
        for (int i = 0; i < predecessorBlocks.size(); i++) {
            if (predecessorBlocks.get(i) == predecessorBlock) {
                return operands[i];
            }
        }
        return null;
    }

    /** @return 操作数的防御性副本 */
    public SsaVariable[] operands() {
        return operands.clone();
    }

    /**
     * 供前缀重命名阶段逐边填充操作数。
     *
     * @param edgeIndex 前驱边序号
     * @param operand   该边对应的 SSA 值
     */
    void setOperand(int edgeIndex, SsaVariable operand) {
        operands[edgeIndex] = operand;
    }

    /** @return 简短的显示形式，例如 {@code "v2_3 = phi(v2_1, v2_2)"} */
    public String display() {
        StringBuilder sb = new StringBuilder();
        sb.append(definedVariable().name()).append(" = phi(");
        for (int i = 0; i < operands.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(operands[i] == null ? "?" : operands[i].name());
        }
        sb.append(')');
        return sb.toString();
    }

    @Override
    public String toString() {
        return "phi@" + blockId + "{" + display() + "}";
    }
}
