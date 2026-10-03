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

import com.aetherdecompiler.api.AetherException;
import com.aetherdecompiler.api.ErrorCode;
import com.aetherdecompiler.core.model.BasicBlock;
import com.aetherdecompiler.core.model.ClassModel;
import com.aetherdecompiler.core.model.Insn;
import com.aetherdecompiler.core.model.MethodModel;
import com.aetherdecompiler.core.model.TryCatchEntry;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 从指令列表构建 {@link ControlFlowGraph}。
 *
 * <p>算法：基本块前导点优先切分。新的基本块始于方法入口、每个分支目标，
 * 以及任何分支之后的指令（包括 switch 之后的指令以及每个异常处理器入口之后的
 * 指令）。这个经典的 O(n) 遍历可在不借助任何库的情况下，得到一个最大的
 * 单入口基本块划分。</p>
 *
 * <p>故事类比：把一条长绸带裁成若干段。你在起点处剪，在每个可以进入的
 * 交汇处剪，并在每个可以离开的交汇处之后立刻剪 —— 结果是一段段没人能
 * 从中途进入的片段。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class CfgBuilder {

    /**
     * 为单个方法构建 CFG。
     *
     * @param owner  所属类（用于诊断）
     * @param method 方法模型
     * @return 控制流图
     * @throws AetherException 若某个跳转指向不存在的指令
     */
    public ControlFlowGraph build(ClassModel owner, MethodModel method) {
        List<Insn> insns = method.instructions();
        int n = insns.size();
        if (n == 0) {
            return new ControlFlowGraph(owner.name(), method.id(), insns, List.of());
        }

        // 1. 找出前导点：各基本块的入口。
        boolean[] isLeader = new boolean[n];
        isLeader[0] = true;
        for (int i = 0; i < n; i++) {
            Insn insn = insns.get(i);
            if (insn.isBranch()) {
                for (int target : insn.branchTargets()) {
                    if (target < 0 || target > n) {
                        throw new AetherException(ErrorCode.CFG_DANGLING_JUMP_TARGET,
                                "Jump at insn " + i + " targets invalid index " + target
                                        + " in " + owner.name() + "." + method.id());
                    }
                    // 目标等于 n 表示“越过末尾”：这是合法的；它
                    // 只是结束方法，并不产生新的块。
                    if (target < n) {
                        isLeader[target] = true;
                    }
                }
                if (i + 1 < n) {
                    isLeader[i + 1] = true;
                }
            }
        }
        // 异常处理器入口同样是前导点。
        for (TryCatchEntry tce : method.tryCatchEntries()) {
            if (tce.handlerIndex() >= 0 && tce.handlerIndex() < n) {
                isLeader[tce.handlerIndex()] = true;
            }
            // 受保护区间边界也必须成为前导点：否则 try 体可能与后续指令共享一个
            // 基本块，下游无法在块粒度上切出干净的 try 范围（曾导致 try/catch
            // 结构被整段丢弃）。起始含首、结束不含尾，因此两者都切。
            if (tce.startIndex() >= 0 && tce.startIndex() < n) {
                isLeader[tce.startIndex()] = true;
            }
            if (tce.endIndex() >= 0 && tce.endIndex() < n) {
                isLeader[tce.endIndex()] = true;
            }
        }
        // 紧跟异常处理器入口之后、以及紧跟受保护区间结束之后无需额外处理：
        // 前导点已保证这些位置自成块首。

        // 2. 在前导点处把指令流切分为基本块。
        List<int[]> ranges = new ArrayList<>(); // [起始指令, 结束指令]
        int start = 0;
        for (int i = 1; i < n; i++) {
            if (isLeader[i]) {
                ranges.add(new int[]{start, i - 1});
                start = i;
            }
        }
        ranges.add(new int[]{start, n - 1});

        // 3. 建立“指令索引 -> 块 id”的映射。
        int[] blockOfInsn = new int[n];
        for (int b = 0; b < ranges.size(); b++) {
            int[] r = ranges.get(b);
            for (int i = r[0]; i <= r[1]; i++) {
                blockOfInsn[i] = b;
            }
        }

        // 4. 依据每个块最后一条指令计算其后继。
        List<BasicBlock> blocks = new ArrayList<>(ranges.size());
        for (int b = 0; b < ranges.size(); b++) {
            int[] r = ranges.get(b);
            int last = r[1];
            Insn tail = insns.get(last);

            Set<Integer> succ = new LinkedHashSet<>();
            if (tail.isBranch()) {
                for (int target : tail.branchTargets()) {
                    if (target >= 0 && target < n) {
                        succ.add(blockOfInsn[target]);
                    }
                }
                // 条件分支会“贯穿直落”；无条件分支不会。
                if (isConditional(tail.opcode()) && last + 1 < n) {
                    succ.add(blockOfInsn[last + 1]);
                }
            } else if (!isTerminal(tail.opcode()) && last + 1 < n) {
                succ.add(blockOfInsn[last + 1]);
            }

            blocks.add(new BasicBlock(b, r[0], r[1],
                    new ArrayList<>(succ), List.of(), b == 0));
        }

        // 5. 附加异常边：对每个受保护区间，其基本块
        //    获得一条指向处理器块的边。
        List<List<Integer>> exceptionSucc = new ArrayList<>();
        for (int b = 0; b < blocks.size(); b++) {
            exceptionSucc.add(new ArrayList<>());
        }
        for (TryCatchEntry tce : method.tryCatchEntries()) {
            int handler = tce.handlerIndex();
            if (handler < 0 || handler >= n) {
                continue;
            }
            int handlerBlock = blockOfInsn[handler];
            for (int b = 0; b < blocks.size(); b++) {
                BasicBlock block = blocks.get(b);
                if (block.lastInsn() >= tce.startIndex() && block.firstInsn() < tce.endIndex()) {
                    if (b != handlerBlock && !exceptionSucc.get(b).contains(handlerBlock)) {
                        exceptionSucc.get(b).add(handlerBlock);
                    }
                }
            }
        }

        List<BasicBlock> finalBlocks = new ArrayList<>(blocks.size());
        for (int b = 0; b < blocks.size(); b++) {
            BasicBlock block = blocks.get(b);
            finalBlocks.add(new BasicBlock(block.id(), block.firstInsn(), block.lastInsn(),
                    block.successors(), exceptionSucc.get(b), block.isEntry()));
        }

        return new ControlFlowGraph(owner.name(), method.id(), insns, finalBlocks);
    }

    private boolean isConditional(int opcode) {
        // 0x99..0xA6 是双操作数的整数/引用条件跳转，
        // 0xC6/0xC7 是 ifnull/ifnonnull。goto/jsr 是无条件跳转。
        return (opcode >= 0x99 && opcode <= 0xA6) || opcode == 0xC6 || opcode == 0xC7;
    }

    private boolean isTerminal(int opcode) {
        // return 家族（0xAC..0xB1）、athrow（0xBF）、goto/jsr 以及 switch 由
        // 分支路径处理；ret（0xA9）同样会结束一个块。
        return (opcode >= 0xAC && opcode <= 0xB1) || opcode == 0xBF || opcode == 0xA9;
    }
}
