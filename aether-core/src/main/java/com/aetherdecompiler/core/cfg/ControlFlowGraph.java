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
package com.aetherdecompiler.core.cfg;

import com.aetherdecompiler.api.CfgView;
import com.aetherdecompiler.api.IRKind;
import com.aetherdecompiler.core.model.BasicBlock;
import com.aetherdecompiler.core.model.Insn;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 单个方法的不可变控制流图。
 *
 * <p>CFG 就是该方法的指令列表，加上把这些指令划分成的 {@link BasicBlock}
 * 以及它们之间的边。它是内核对一个方法的第一层结构化视图，也是支配树、
 * 循环检测（Phase 1）与结构化重建（Phase 3）的基底。</p>
 *
 * <p>故事类比：一张城市路网图。指令是地址；基本块是发生路线选择的
 * 路口；边是它们之间的道路。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class ControlFlowGraph implements CfgView {

    private final String ownerClass;
    private final String methodId;
    private final List<Insn> instructions;
    private final List<BasicBlock> blocks;

    /**
     * @param ownerClass   所属类的内部名
     * @param methodId     方法标识符 {@code name + descriptor}
     * @param instructions 完整指令列表
     * @param blocks       基本块，按其 id 索引
     */
    public ControlFlowGraph(String ownerClass, String methodId,
                            List<Insn> instructions, List<BasicBlock> blocks) {
        this.ownerClass = Objects.requireNonNull(ownerClass, "ownerClass");
        this.methodId = Objects.requireNonNull(methodId, "methodId");
        this.instructions = List.copyOf(instructions);
        this.blocks = List.copyOf(blocks);
    }

    /** @return 所属类的内部名 */
    public String ownerClass() {
        return ownerClass;
    }

    /** @return 方法标识符 */
    public String methodId() {
        return methodId;
    }

    /** @return 完整指令列表 */
    public List<Insn> instructions() {
        return instructions;
    }

    /** @return 基本块，按 id 索引 */
    public List<BasicBlock> blocks() {
        return blocks;
    }

    @Override
    public List<String> insnTexts() {
        List<String> out = new ArrayList<>(instructions.size());
        for (Insn insn : instructions) {
            out.add(insn.display());
        }
        return out;
    }

    /**
     * @param id 块 id
     * @return 具有该 id 的块
     */
    public BasicBlock block(int id) {
        return blocks.get(id);
    }

    /**
     * @param insnIndex 指令索引
     * @return 包含该指令的块，若无则返回 {@code null}
     */
    public BasicBlock blockOfInsn(int insnIndex) {
        for (BasicBlock block : blocks) {
            if (insnIndex >= block.firstInsn() && insnIndex <= block.lastInsn()) {
                return block;
            }
        }
        return null;
    }

    /** @return 块的数量 */
    public int blockCount() {
        return blocks.size();
    }

    /** @return 边的数量，含普通边与异常边 */
    public int edgeCount() {
        int edges = 0;
        for (BasicBlock block : blocks) {
            edges += block.successors().size();
            edges += block.exceptionSuccessors().size();
        }
        return edges;
    }

    @Override
    public IRKind kind() {
        return IRKind.CFG;
    }

    @Override
    public String toString() {
        return "CFG{" + ownerClass + "." + methodId
                + ", blocks=" + blocks.size()
                + ", edges=" + edgeCount() + "}";
    }
}
