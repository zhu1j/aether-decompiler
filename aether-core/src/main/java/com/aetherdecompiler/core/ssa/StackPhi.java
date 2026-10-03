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

import java.util.List;

/**
 * 控制流汇合点上的<strong>栈 phi 节点</strong>。
 *
 * <p>当多条前驱路径到达同一块时，若该点操作数栈的第 {@code posFromBottom} 个单元
 * 分别由不同前驱定义，就需要在这条汇合边“按来源择一”——这正是栈层面的 phi。
 * {@code operands} 按前驱顺序记录各路来源的栈单元 id。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class StackPhi implements IRObject {

    private final int id;
    private final int blockId;
    private final int posFromBottom;
    private final List<Integer> operands;

    /**
     * @param id            该栈 phi 产出的栈单元 id
     * @param blockId       所在块 id
     * @param posFromBottom 栈内自底向上的位置下标
     * @param operands      各前驱来源的栈单元 id，按前驱顺序排列
     */
    public StackPhi(int id, int blockId, int posFromBottom, List<Integer> operands) {
        this.id = id;
        this.blockId = blockId;
        this.posFromBottom = posFromBottom;
        this.operands = List.copyOf(operands);
    }

    /** @return 该栈 phi 产出的栈单元 id */
    public int id() {
        return id;
    }

    /** @return 所在块 id */
    public int blockId() {
        return blockId;
    }

    /** @return 栈内自底向上的位置下标 */
    public int posFromBottom() {
        return posFromBottom;
    }

    /** @return 各前驱来源的栈单元 id，按前驱顺序排列 */
    public List<Integer> operands() {
        return operands;
    }

    /** @return 便于展示的文本形式 */
    public String display() {
        StringBuilder sb = new StringBuilder();
        sb.append("s").append(id).append(" = phi(");
        for (int i = 0; i < operands.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append("s").append(operands.get(i));
        }
        sb.append(")  @栈位 ").append(posFromBottom).append(" @B").append(blockId);
        return sb.toString();
    }

    @Override
    public IRKind kind() {
        return IRKind.SSA;
    }

    @Override
    public String toString() {
        return "StackPhi{" + display() + "}";
    }
}
