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

import com.aetherdecompiler.api.IRKind;
import com.aetherdecompiler.api.IRObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 单个方法<strong>操作数栈提升（Stack Lifting）</strong>的结果。
 *
 * <p>字节码把表达式求值结果放在一个隐式的操作数栈上，栈本身没有名字。真实源码里
 * 这些中间结果是“无名临时量”。本对象记录：把栈上第 {@code d} 个单元在某个程序点
 * 上视为一个<strong>符号化栈单元</strong>（{@link StackCell}）后，每个单元由哪条
 * 指令定义、在控制流汇合处又如何经 {@link StackPhi} 合并。</p>
 *
 * <p>这就把“隐式栈”翻译成“显式的、只赋值一次的临时候选量”，与局部变量 SSA
 * （{@link SsaForm}）合流：后续若要生成 {@code int v1 = a + b;} 这类显式临时变量、
 * 或做表达式重组与栈到变量的消解，都以此为依据。</p>
 *
 * <p>本对象不可变，可安全共享。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class StackLiftResult implements IRObject {

    private final String ownerClass;
    private final String methodId;
    private final int maxDepth;
    private final int cellCount;
    private final List<StackPhi> phis;
    private final Map<Integer, Integer> depthBefore;
    private final Map<Integer, List<Integer>> blockEntryCells;
    private final Map<Integer, Integer> insnProducedCell;
    private final boolean approximate;
    private final String approximateReason;

    /**
     * @param ownerClass        所属类内部名
     * @param methodId          方法标识符
     * @param maxDepth          全程最大操作数栈深度（以单元计）
     * @param cellCount         提升出的栈单元总数
     * @param phis              汇合点上的栈 phi 节点
     * @param depthBefore       指令索引 → 执行前栈深
     * @param blockEntryCells   块 id → 进入该块时按栈序排列的栈单元 id
     * @param insnProducedCell  指令索引 → 它在栈顶产出的栈单元 id（未产出为 -1）
     * @param approximate       是否存在无法精确判定的栈形态
     * @param approximateReason 无法精确判定的原因（无则为空串）
     */
    public StackLiftResult(String ownerClass, String methodId, int maxDepth, int cellCount,
                           List<StackPhi> phis, Map<Integer, Integer> depthBefore,
                           Map<Integer, List<Integer>> blockEntryCells,
                           Map<Integer, Integer> insnProducedCell,
                           boolean approximate, String approximateReason) {
        this.ownerClass = ownerClass;
        this.methodId = methodId;
        this.maxDepth = maxDepth;
        this.cellCount = cellCount;
        this.phis = List.copyOf(phis);
        this.depthBefore = Collections.unmodifiableMap(new HashMap<>(depthBefore));
        Map<Integer, List<Integer>> be = new HashMap<>();
        for (Map.Entry<Integer, List<Integer>> e : blockEntryCells.entrySet()) {
            be.put(e.getKey(), List.copyOf(e.getValue()));
        }
        this.blockEntryCells = Collections.unmodifiableMap(be);
        this.insnProducedCell = Collections.unmodifiableMap(new HashMap<>(insnProducedCell));
        this.approximate = approximate;
        this.approximateReason = approximateReason == null ? "" : approximateReason;
    }

    /** 构造一个空的提升结果（方法无指令或分析被跳过时使用）。 */
    public static StackLiftResult empty(String ownerClass, String methodId) {
        return new StackLiftResult(ownerClass, methodId, 0, 0, List.of(), Map.of(), Map.of(),
                Map.of(), false, "");
    }

    /** @return 所属类内部名 */
    public String ownerClass() {
        return ownerClass;
    }

    /** @return 方法标识符 */
    public String methodId() {
        return methodId;
    }

    /** @return 全程最大操作数栈深度（单元计） */
    public int maxDepth() {
        return maxDepth;
    }

    /** @return 提升出的栈单元总数 */
    public int cellCount() {
        return cellCount;
    }

    /** @return 汇合点上的栈 phi 节点 */
    public List<StackPhi> phis() {
        return phis;
    }

    /** @return 栈 phi 节点数量 */
    public int phiCount() {
        return phis.size();
    }

    /** @return 指令索引 → 执行前栈深的只读映射 */
    public Map<Integer, Integer> depthBefore() {
        return depthBefore;
    }

    /**
     * @param insnIndex 指令索引
     * @return 执行该指令前的栈深；未知为 -1
     */
    public int depthBefore(int insnIndex) {
        return depthBefore.getOrDefault(insnIndex, -1);
    }

    /** @return 块 id → 进入该块时按栈序排列的栈单元 id */
    public Map<Integer, List<Integer>> blockEntryCells() {
        return blockEntryCells;
    }

    /**
     * @param blockId 块 id
     * @return 进入该块时按栈序排列的栈单元 id；无则为空列表
     */
    public List<Integer> entryCells(int blockId) {
        return blockEntryCells.getOrDefault(blockId, List.of());
    }

    /** @return 指令索引 → 其在栈顶产出的栈单元 id */
    public Map<Integer, Integer> insnProducedCell() {
        return insnProducedCell;
    }

    /**
     * @param insnIndex 指令索引
     * @return 该指令在栈顶产出的栈单元 id；未产出为 -1
     */
    public int producedCell(int insnIndex) {
        return insnProducedCell.getOrDefault(insnIndex, -1);
    }

    /** @return 是否存在无法精确判定的栈形态 */
    public boolean isApproximate() {
        return approximate;
    }

    /** @return 无法精确判定的原因（无则为空串） */
    public String approximateReason() {
        return approximateReason;
    }

    @Override
    public IRKind kind() {
        return IRKind.SSA;
    }

    @Override
    public String toString() {
        return "StackLiftResult{" + ownerClass + "." + methodId
                + ", maxDepth=" + maxDepth
                + ", cells=" + cellCount
                + ", phis=" + phis.size()
                + (approximate ? ", approximate" : "")
                + "}";
    }
}
