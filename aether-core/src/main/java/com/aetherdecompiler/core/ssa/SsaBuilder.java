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

import com.aetherdecompiler.core.cfg.ControlFlowGraph;
import com.aetherdecompiler.core.cfg.DominatorTree;
import com.aetherdecompiler.core.model.BasicBlock;
import com.aetherdecompiler.core.model.ClassModel;
import com.aetherdecompiler.core.model.Insn;
import com.aetherdecompiler.core.model.MethodModel;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 从 {@link ControlFlowGraph} 与 {@link DominatorTree} 构造 SSA 形式。
 *
 * <p>实现的是 Cytron 等人（1991）的经典三阶段算法，全程零依赖、确定且可重入：</p>
 * <ol>
 *   <li><b>定值点收集</b>——对每个局部变量槽，找出所有写入它的块；</li>
 *   <li><b>phi 放置</b>——沿支配边界做迭代上溯，在“首次需要合并”的块插入 phi；</li>
 *   <li><b>前缀重命名</b>——沿支配树做一次深度优先遍历，用“每槽一个版本栈”把每条
 *       load 重写成读取其到达定值的版本，并顺带填充后继 phi 的操作数。</li>
 * </ol>
 *
 * <p>阶段 3 的“版本栈”是整段算法的点睛之笔：进入一个块时压入它定义的版本，离开时弹回，
 * 于是栈顶永远恰好是“此刻支配该点的那个版本”，无需任何数据流不动点迭代。</p>
 *
 * <p>故事类比：给同一条河的各条支流统一编号。你在源头贴 0 号标签，沿支流向下，每经过一次
 * 蓄水（定值）就发一个新编号；到汇流口，用一块“按来源择号”的牌子（phi）统一口径——
 * 整条水系因此每处水样都能追溯到唯一源头。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class SsaBuilder {

    /**
     * 为单个方法构造 SSA 形式。
     *
     * @param owner  所属类（用于诊断与命名）
     * @param method 方法模型
     * @param cfg    该方法的控制流图
     * @param dom    该 CFG 的支配树
     * @return 不可变的 SSA 形式
     */
    public SsaForm build(ClassModel owner, MethodModel method,
                         ControlFlowGraph cfg, DominatorTree dom) {
        List<Insn> insns = cfg.instructions();
        int blockCount = cfg.blockCount();
        if (blockCount == 0) {
            return new SsaForm(owner.name(), method.id(), 0, List.of(), Map.of(), Map.of(),
                    StackLiftResult.empty(owner.name(), method.id()));
        }

        // 0. 确定槽位上界：既尊重 maxLocals，又容忍指令中出现的更大槽号。
        int slotCount = Math.max(0, method.maxLocals());
        for (Insn insn : insns) {
            int s = Math.max(useSlot(insn), defSlot(insn));
            if (s >= slotCount) {
                slotCount = s + 1;
            }
        }

        // 1. 前驱（仅计可达块），用于 phi 操作数对齐。
        List<List<Integer>> preds = new ArrayList<>(blockCount);
        for (int b = 0; b < blockCount; b++) {
            preds.add(new ArrayList<>());
        }
        for (BasicBlock b : cfg.blocks()) {
            if (!dom.isReachable(b.id())) {
                continue;
            }
            Set<Integer> targets = new LinkedHashSet<>(b.successors());
            targets.addAll(b.exceptionSuccessors());
            for (int t : targets) {
                if (t < 0 || t >= blockCount || !dom.isReachable(t)) {
                    continue;
                }
                if (!preds.get(t).contains(b.id())) {
                    preds.get(t).add(b.id());
                }
            }
        }

        // 2. 每个槽的定值块集合。
        List<Set<Integer>> defBlocks = new ArrayList<>(slotCount);
        for (int s = 0; s < slotCount; s++) {
            defBlocks.add(new LinkedHashSet<>());
        }
        for (BasicBlock b : cfg.blocks()) {
            if (!dom.isReachable(b.id())) {
                continue;
            }
            for (int i = b.firstInsn(); i <= b.lastInsn(); i++) {
                int s = defSlot(insns.get(i));
                if (s >= 0 && s < slotCount) {
                    defBlocks.get(s).add(b.id());
                }
            }
        }

        // 3. 支配边界 + phi 放置（迭代上溯）。
        DominanceFrontier df = DominanceFrontier.of(cfg, dom);
        List<List<PhiNode>> blockPhis = new ArrayList<>(blockCount);
        for (int b = 0; b < blockCount; b++) {
            blockPhis.add(new ArrayList<>());
        }
        int[] versionCounter = new int[slotCount];
        for (int s = 0; s < slotCount; s++) {
            versionCounter[s] = 1; // 版本 0 保留给方法入口（参数/入口值）。
        }
        List<Set<Integer>> placed = new ArrayList<>(slotCount);
        for (int s = 0; s < slotCount; s++) {
            placed.add(new HashSet<>());
        }
        for (int s = 0; s < slotCount; s++) {
            if (defBlocks.get(s).isEmpty()) {
                continue;
            }
            Deque<Integer> work = new ArrayDeque<>(defBlocks.get(s));
            Set<Integer> inList = new HashSet<>(defBlocks.get(s));
            while (!work.isEmpty()) {
                int b = work.poll();
                inList.remove(b);
                for (int d : df.frontierOf(b)) {
                    if (d < 0 || d >= blockCount) {
                        continue;
                    }
                    if (placed.get(s).add(d)) {
                        int v = versionCounter[s]++;
                        blockPhis.get(d).add(new PhiNode(d, s, v, preds.get(d)));
                        if (!defBlocks.get(s).contains(d) && inList.add(d)) {
                            work.add(d);
                        }
                    }
                }
            }
        }

        // 4. 前缀重命名（沿支配树 DFS）+ phi 操作数填充。
        Renamer renamer = new Renamer(cfg, dom, insns, preds, blockPhis, slotCount, versionCounter);
        renamer.run();

        List<PhiNode> allPhis = new ArrayList<>();
        for (List<PhiNode> list : blockPhis) {
            allPhis.addAll(list);
        }
        // C1 操作数栈提升：把隐式操作数栈符号化为 SSA 值，并记录汇合处的栈 phi。
        StackLiftResult stackLift = new StackLifter().lift(owner, method, cfg, dom);
        return new SsaForm(owner.name(), method.id(), slotCount,
                allPhis, renamer.definitions, renamer.uses, stackLift);
    }

    /**
     * 前缀重命名阶段的可变工作台。生命周期严格限定于一次 {@link #build} 调用内部。
     *
     * @author Jerry Zhu (Zeek)
     */
    private static final class Renamer {

        private final ControlFlowGraph cfg;
        private final DominatorTree dom;
        private final List<Insn> insns;
        private final List<List<Integer>> preds;
        private final List<List<PhiNode>> blockPhis;
        private final int[] versionCounter;

        private final List<Deque<Integer>> stacks = new ArrayList<>();
        private final List<Integer> pushedLog = new ArrayList<>();

        private final Map<Integer, SsaVariable> definitions = new HashMap<>();
        private final Map<Integer, List<SsaVariable>> uses = new HashMap<>();

        Renamer(ControlFlowGraph cfg, DominatorTree dom, List<Insn> insns,
                List<List<Integer>> preds, List<List<PhiNode>> blockPhis,
                int slotCount, int[] versionCounter) {
            this.cfg = cfg;
            this.dom = dom;
            this.insns = insns;
            this.preds = preds;
            this.blockPhis = blockPhis;
            this.versionCounter = versionCounter;
            for (int s = 0; s < slotCount; s++) {
                Deque<Integer> stack = new ArrayDeque<>();
                stack.push(0); // 每个槽的入口值（版本 0）。
                stacks.add(stack);
            }
        }

        void run() {
            // 显式栈的迭代式 DFS：帧 = {块 id, 下一个子节点下标, 进入时的推送标记}。
            Deque<int[]> frames = new ArrayDeque<>();
            int mark = pushedLog.size();
            enterBlock(0);
            frames.push(new int[]{0, 0, mark});
            while (!frames.isEmpty()) {
                int[] frame = frames.peek();
                int b = frame[0];
                List<Integer> children = dom.children(b);
                if (frame[1] < children.size()) {
                    int child = children.get(frame[1]++);
                    int childMark = pushedLog.size();
                    enterBlock(child);
                    frames.push(new int[]{child, 0, childMark});
                } else {
                    // 离开该块：弹回它压入的全部版本。
                    int exitMark = frame[2];
                    while (pushedLog.size() > exitMark) {
                        int s = pushedLog.remove(pushedLog.size() - 1);
                        stacks.get(s).pop();
                    }
                    frames.pop();
                }
            }
        }

        private void enterBlock(int blockId) {
            // (a) 压入本块的 phi 定值。
            for (PhiNode phi : blockPhis.get(blockId)) {
                stacks.get(phi.slot()).push(phi.version());
                pushedLog.add(phi.slot());
            }
            // (b) 处理块内指令：先使用、后定值（iinc 两者兼具，顺序天然正确）。
            BasicBlock block = cfg.block(blockId);
            for (int i = block.firstInsn(); i <= block.lastInsn(); i++) {
                Insn insn = insns.get(i);
                int us = useSlot(insn);
                if (us >= 0 && us < stacks.size()) {
                    SsaVariable used = new SsaVariable(us, stacks.get(us).peek());
                    uses.computeIfAbsent(i, k -> new ArrayList<>()).add(used);
                }
                int ds = defSlot(insn);
                if (ds >= 0 && ds < stacks.size()) {
                    int v = versionCounter[ds]++;
                    stacks.get(ds).push(v);
                    pushedLog.add(ds);
                    definitions.put(i, new SsaVariable(ds, v));
                }
            }
            // (c) 用当前版本栈填充后继块的 phi 操作数（按来路边对齐）。
            Set<Integer> succs = new LinkedHashSet<>(block.successors());
            succs.addAll(block.exceptionSuccessors());
            for (int t : succs) {
                if (t < 0 || t >= blockPhis.size()) {
                    continue;
                }
                for (PhiNode phi : blockPhis.get(t)) {
                    SsaVariable operand = new SsaVariable(phi.slot(), stacks.get(phi.slot()).peek());
                    List<Integer> tPreds = preds.get(t);
                    for (int idx = 0; idx < tPreds.size(); idx++) {
                        if (tPreds.get(idx) == blockId) {
                            phi.setOperand(idx, operand);
                        }
                    }
                }
            }
        }
    }

    /**
     * @param insn 指令
     * @return 该指令读取的局部变量槽位；若它不读取任何局部变量则为 {@code -1}
     */
    static int useSlot(Insn insn) {
        int op = insn.opcode();
        // 通用 load：iload/lload/fload/dload/aload（0x15..0x19）。
        if (op >= 21 && op <= 25) {
            return parseVar(insn.operand());
        }
        // 紧凑 load：*load_0..3（0x1A..0x2D）。
        if (op >= 26 && op <= 45) {
            return (op - 26) % 4;
        }
        // iinc 与 ret 都读取一个槽。
        if (op == 132 || op == 169) {
            return parseVar(insn.operand());
        }
        return -1;
    }

    /**
     * @param insn 指令
     * @return 该指令写入的局部变量槽位；若它不写入任何局部变量则为 {@code -1}
     */
    static int defSlot(Insn insn) {
        int op = insn.opcode();
        // 通用 store：istore/lstore/fstore/dstore/astore（0x36..0x3A）。
        if (op >= 54 && op <= 58) {
            return parseVar(insn.operand());
        }
        // 紧凑 store：*store_0..3（0x3B..0x4E）。
        if (op >= 59 && op <= 78) {
            return (op - 59) % 4;
        }
        // iinc 同时是定值。
        if (op == 132) {
            return parseVar(insn.operand());
        }
        return -1;
    }

    /**
     * 从操作数字符串（形如 {@code "var 3"} 或 {@code "var 3 by 2"}）解析槽号。
     *
     * @param operand 操作数字符串
     * @return 槽号；无法解析时返回 {@code -1}
     */
    static int parseVar(String operand) {
        if (operand == null) {
            return -1;
        }
        int idx = operand.indexOf("var ");
        if (idx < 0) {
            return -1;
        }
        int p = idx + 4;
        int n = 0;
        boolean any = false;
        while (p < operand.length() && Character.isDigit(operand.charAt(p))) {
            n = n * 10 + (operand.charAt(p) - '0');
            p++;
            any = true;
        }
        return any ? n : -1;
    }
}
