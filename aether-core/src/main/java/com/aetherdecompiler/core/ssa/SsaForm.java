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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 单个方法的<strong>静态单赋值（SSA）形式</strong>——内核在 CFG 之上产出的第二层方法视图。
 *
 * <p>它把“一个会被反复覆盖的局部变量槽”，重写为一组“只被赋值一次的值”（{@link SsaVariable}），
 * 并在控制流汇合点插入 {@link PhiNode} 以合并多路定值。产出的核心事实有三类：</p>
 * <ul>
 *   <li><b>phi 集合</b>——每个汇合点上传入什么、产出什么；</li>
 *   <li><b>定值映射</b>——每条指令定义了哪个 SSA 值；</li>
 *   <li><b>使用映射</b>——每条指令读取了哪些 SSA 值。</li>
 * </ul>
 *
 * <p>得到这三样东西之后，原本需要反复迭代的数据流分析（可达定值、常量传播、活跃性、
 * 死代码消除）都会退化为一次对“定值-使用链”的简单遍历——这正是内核选择在 CFG 之后、
 * AST 之前建立 SSA 层的原因。</p>
 *
 * <p>本对象不可变，可安全共享、缓存与并行读取。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class SsaForm implements IRObject {

    private final String ownerClass;
    private final String methodId;
    private final int slotCount;
    private final List<PhiNode> phis;
    private final Map<Integer, SsaVariable> definitions;
    private final Map<Integer, List<SsaVariable>> uses;

    /**
     * @param ownerClass  所属类的内部名
     * @param methodId    方法标识符 {@code name + descriptor}
     * @param slotCount   局部变量槽位数
     * @param phis        全部 phi 节点
     * @param definitions 指令索引 → 它定义的 SSA 值
     * @param uses        指令索引 → 它读取的 SSA 值列表
     */
    public SsaForm(String ownerClass, String methodId, int slotCount,
                   List<PhiNode> phis,
                   Map<Integer, SsaVariable> definitions,
                   Map<Integer, List<SsaVariable>> uses) {
        this.ownerClass = ownerClass;
        this.methodId = methodId;
        this.slotCount = slotCount;
        this.phis = List.copyOf(phis);
        this.definitions = Collections.unmodifiableMap(new HashMap<>(definitions));
        Map<Integer, List<SsaVariable>> copy = new HashMap<>();
        for (Map.Entry<Integer, List<SsaVariable>> e : uses.entrySet()) {
            copy.put(e.getKey(), List.copyOf(e.getValue()));
        }
        this.uses = Collections.unmodifiableMap(copy);
    }

    /** @return 所属类的内部名 */
    public String ownerClass() {
        return ownerClass;
    }

    /** @return 方法标识符 */
    public String methodId() {
        return methodId;
    }

    /** @return 局部变量槽位数 */
    public int slotCount() {
        return slotCount;
    }

    /** @return 全部 phi 节点，按块 id、槽位顺序稳定排列 */
    public List<PhiNode> phis() {
        return phis;
    }

    /** @return 指令索引 → 定值 的只读映射 */
    public Map<Integer, SsaVariable> definitions() {
        return definitions;
    }

    /** @return 指令索引 → 使用 的只读映射 */
    public Map<Integer, List<SsaVariable>> uses() {
        return uses;
    }

    /**
     * @param insnIndex 指令索引
     * @return 该指令定义的 SSA 值；若它不定义任何变量则为 {@code null}
     */
    public SsaVariable definitionAt(int insnIndex) {
        return definitions.get(insnIndex);
    }

    /**
     * @param insnIndex 指令索引
     * @return 该指令读取的 SSA 值；若无则为空列表
     */
    public List<SsaVariable> usesAt(int insnIndex) {
        return uses.getOrDefault(insnIndex, List.of());
    }

    /**
     * @param blockId 块 id
     * @return 承载于该块的 phi 节点
     */
    public List<PhiNode> phisInBlock(int blockId) {
        List<PhiNode> out = new ArrayList<>();
        for (PhiNode phi : phis) {
            if (phi.blockId() == blockId) {
                out.add(phi);
            }
        }
        return out;
    }

    /** @return phi 节点的数量 */
    public int phiCount() {
        return phis.size();
    }

    @Override
    public IRKind kind() {
        return IRKind.SSA;
    }

    @Override
    public String toString() {
        return "SsaForm{" + ownerClass + "." + methodId
                + ", slots=" + slotCount
                + ", phis=" + phis.size()
                + ", defs=" + definitions.size()
                + ", uses=" + uses.size() + "}";
    }
}
