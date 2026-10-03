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
package com.aetherdecompiler.core.ast;

import com.aetherdecompiler.core.cfg.ControlFlowGraph;
import com.aetherdecompiler.core.cfg.DominatorTree;
import com.aetherdecompiler.core.model.BasicBlock;
import com.aetherdecompiler.core.model.Insn;

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
 * 控制结构恢复（structuring）：把 CFG 扁平的基本块与边，还原成嵌套的语句结构。
 *
 * <p>算法建立在支配树之上，分三种模式递归下降：</p>
 * <ul>
 *   <li><b>循环头</b>——若某块存在一条回边（后继支配该块），则它是一个自然循环的入口；
 *       按“循环退出条件取反”还原为 {@link Stmt.While}；</li>
 *   <li><b>条件分支</b>——若某块以条件跳转结尾，则先用可达性求两条分支的<em>再汇合点</em>，
 *       还原为 {@link Stmt.If}（含可选 else）；</li>
 *   <li><b>顺序块</b>——否则顺序拼接本块语句，并沿唯一后继继续。</li>
 * </ul>
 *
 * <p>当控制流不可规约（存在多入口环路、无条件的乱跳）而无法结构化时，本类不会崩溃，
 * 而是退回“标签 + 线性基本块”的保守模式，并置位 {@code irreducible}，交由调用方如实呈现。
 * 这一点是刻意为之：真实世界的字节码（尤其被混淆过的）并不总能还原成整齐的 if/while，
 * 诚实降级远胜于伪造一个看似漂亮却错误的树。</p>
 *
 * <p>故事类比：把一张“单行道 + 立交桥”的交通图，重新画成一份有层次的流程图——遇到没法
 * 画整齐的互通立交，就老实标上“复杂立交”的牌子，而不是硬掰成十字路口。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class ControlStructurer {

    private static final int MAX_DEPTH = 512;

    private final ControlFlowGraph cfg;
    private final DominatorTree dom;
    private final List<Insn> insns;
    private final List<List<Integer>> predecessors;
    private final Set<Integer> loopHeaders = new LinkedHashSet<>();
    private final Map<Integer, Integer> loopFollow = new HashMap<>();
    private final boolean[] emitted;
    private boolean irreducible;
    private final java.util.function.IntFunction<String> varNamer;

    /**
     * @param cfg 控制流图
     * @param dom 支配树
     */
    public ControlStructurer(ControlFlowGraph cfg, DominatorTree dom) {
        this(cfg, dom, null);
    }

    /**
     * @param cfg      控制流图
     * @param dom      支配树
     * @param varNamer 槽位 → 显示名的映射（可传 SSA 名或 {@code this}）；{@code null} 时用 {@code vN}
     */
    public ControlStructurer(ControlFlowGraph cfg, DominatorTree dom,
                             java.util.function.IntFunction<String> varNamer) {
        this.cfg = cfg;
        this.dom = dom;
        this.varNamer = varNamer;
        this.insns = cfg.instructions();
        this.emitted = new boolean[Math.max(1, cfg.blockCount())];
        this.predecessors = computePredecessors();
        detectLoops();
    }

    /** @return 结构化后的语句树 */
    public Stmt structure() {
        if (cfg.blockCount() == 0) {
            return new Stmt.Block(-1, -1, List.of());
        }
        try {
            Stmt s = region(0, -1, 0);
            return s;
        } catch (RuntimeException ex) {
            irreducible = true;
            return linearFallback();
        }
    }

    /** @return 是否退回不可规约的线性模式 */
    public boolean isIrreducible() {
        return irreducible;
    }

    // ------------------------------------------------------------------
    // 结构化主循环
    // ------------------------------------------------------------------

    private Stmt region(int entry, int stop, int depth) {
        if (depth > MAX_DEPTH) {
            irreducible = true;
            return new Stmt.Block(-1, -1, List.of());
        }
        List<Stmt> out = new ArrayList<>();
        int cur = entry;
        int guard = 0;
        int limit = Math.max(4, cfg.blockCount() * 4);
        while (cur >= 0 && cur != stop && !emitted[cur] && guard++ < limit) {
            if (loopHeaders.contains(cur) && dom.dominates(cur, backEdgeSource(cur))) {
                Stmt loop = structureLoop(cur, depth);
                if (loop != null) {
                    out.add(loop);
                    Integer follow = loopFollow.get(cur);
                    cur = follow == null ? -1 : follow;
                    continue;
                }
            }
            if (endsWithCondBranch(cur)) {
                Stmt branch = structureIf(cur, stop, depth);
                if (branch != null) {
                    out.add(branch);
                    cur = branchJoin.get(cur);
                    continue;
                }
            }
            // 顺序块。
            emitted[cur] = true;
            ExpressionBuilder.Result r = reconstruct(cur);
            out.addAll(r.statements());
            if ("return".equals(r.terminatorKind()) || "throw".equals(r.terminatorKind())) {
                cur = -1;
                continue;
            }
            cur = skipToControlSuccessor(cur, stop);
        }
        if (cur >= 0 && cur != stop && emitted[cur] && !out.isEmpty()) {
            // 回边命中已发射块：结构上已由 while 覆盖，正常情形。
        }
        return new Stmt.Block(firstInsnOf(entry), lastInsnOf(entry, stop), out);
    }

    private final Map<Integer, Integer> branchJoin = new HashMap<>();

    private Stmt structureIf(int block, int stop, int depth) {
        ExpressionBuilder.Result r = reconstruct(block);
        Expr cond = r.terminatorCond();
        if (cond == null) {
            return null;
        }
        List<Integer> succs = normalSuccessors(block);
        if (succs.size() != 2) {
            return null;
        }
        int thenB = succs.get(0);
        int elseB = succs.get(1);
        int join = joinOf(thenB, elseB);
        emitted[block] = true;
        branchJoin.put(block, join);

        Stmt thenStmt = region(thenB, join, depth + 1);
        Stmt elseStmt = (elseB == join) ? null : region(elseB, join, depth + 1);
        List<Stmt> pre = new ArrayList<>(r.statements());
        Stmt ifStmt = new Stmt.If(firstInsnOf(block), lastInsnOf(block, join),
                cond, thenStmt, elseStmt);
        if (pre.isEmpty()) {
            return ifStmt;
        }
        pre.add(ifStmt);
        return new Stmt.Block(firstInsnOf(block), lastInsnOf(block, join), pre);
    }

    private Stmt structureLoop(int header, int depth) {
        int back = backEdgeSource(header);
        Set<Integer> loop = naturalLoop(header, back);
        int follow = loopExit(loop, header);
        loopFollow.put(header, follow);
        ExpressionBuilder.Result r = reconstruct(header);
        emitted[header] = true;

        Expr cond;
        int bodyEntry;
        if (endsWithCondBranch(header)) {
            List<Integer> succs = normalSuccessors(header);
            int inside = -1;
            int outside = -1;
            for (int s : succs) {
                if (loop.contains(s)) {
                    inside = s;
                } else {
                    outside = s;
                }
            }
            if (inside < 0) {
                return null;
            }
            bodyEntry = inside;
            // 若分支目标在循环外，则“跳出条件”为 cond，继续条件取其反。
            cond = (outside >= 0 && succs.get(0) == outside)
                    ? negate(r.terminatorCond(), header) : r.terminatorCond();
        } else {
            bodyEntry = normalSuccessors(header).isEmpty() ? -1 : normalSuccessors(header).get(0);
            if (bodyEntry < 0 || !loop.contains(bodyEntry)) {
                return null;
            }
            cond = new Expr.Const(header, "true");
        }
        Stmt body = new Stmt.Block(-1, -1, List.of());
        int entryForBody = bodyEntry;
        if (entryForBody >= 0) {
            body = region(entryForBody, header, depth + 1);
        }
        List<Stmt> pre = new ArrayList<>(r.statements());
        Stmt whileStmt = new Stmt.While(firstInsnOf(header), lastInsnOf(header, follow), cond, body);
        if (pre.isEmpty()) {
            return whileStmt;
        }
        pre.add(whileStmt);
        return new Stmt.Block(firstInsnOf(header), lastInsnOf(header, follow), pre);
    }

    private static Expr negate(Expr e, int insn) {
        if (e == null) {
            return new Expr.Const(insn, "true");
        }
        if (e instanceof Expr.Cond c) {
            return new Expr.Cond(insn, c.op(), c.left(), c.right(), !c.negated());
        }
        return new Expr.Unary(insn, "!", e);
    }

    // ------------------------------------------------------------------
    // 分析辅助
    // ------------------------------------------------------------------

    private List<List<Integer>> computePredecessors() {
        int n = cfg.blockCount();
        List<List<Integer>> preds = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            preds.add(new ArrayList<>());
        }
        for (BasicBlock b : cfg.blocks()) {
            for (int s : normalSuccessors(b.id())) {
                if (s >= 0 && s < n) {
                    preds.get(s).add(b.id());
                }
            }
        }
        return preds;
    }

    private void detectLoops() {
        for (BasicBlock b : cfg.blocks()) {
            int v = b.id();
            if (!dom.isReachable(v)) {
                continue;
            }
            for (int s : normalSuccessors(v)) {
                if (s >= 0 && s < cfg.blockCount() && dom.isReachable(s) && dom.dominates(s, v)) {
                    // 边 v->s 是回边，s 是循环头。
                    loopHeaders.add(s);
                    backEdge.put(s, v);
                }
            }
        }
    }

    private final Map<Integer, Integer> backEdge = new HashMap<>();

    private int backEdgeSource(int header) {
        Integer s = backEdge.get(header);
        return s == null ? header : s;
    }

    private Set<Integer> naturalLoop(int header, int backSource) {
        Set<Integer> loop = new LinkedHashSet<>();
        loop.add(header);
        Deque<Integer> work = new ArrayDeque<>();
        if (backSource != header) {
            loop.add(backSource);
            work.add(backSource);
        }
        while (!work.isEmpty()) {
            int n = work.poll();
            for (int p : predecessors.get(n)) {
                if (loop.add(p)) {
                    work.add(p);
                }
            }
        }
        return loop;
    }

    private int loopExit(Set<Integer> loop, int header) {
        for (int n : loop) {
            for (int s : normalSuccessors(n)) {
                if (!loop.contains(s)) {
                    return s;
                }
            }
        }
        return -1;
    }

    private int joinOf(int a, int b) {
        Set<Integer> ra = reachableFrom(a);
        Set<Integer> rb = reachableFrom(b);
        int best = -1;
        for (int x : ra) {
            if (rb.contains(x) && (best < 0 || x < best)) {
                best = x;
            }
        }
        return best;
    }

    private Set<Integer> reachableFrom(int start) {
        Set<Integer> seen = new LinkedHashSet<>();
        if (start < 0) {
            return seen;
        }
        Deque<Integer> work = new ArrayDeque<>();
        work.add(start);
        seen.add(start);
        while (!work.isEmpty()) {
            int n = work.poll();
            for (int s : normalSuccessors(n)) {
                if (s >= 0 && seen.add(s)) {
                    work.add(s);
                }
            }
        }
        return seen;
    }

    private List<Integer> normalSuccessors(int blockId) {
        if (blockId < 0 || blockId >= cfg.blockCount()) {
            return List.of();
        }
        return cfg.block(blockId).successors();
    }

    private boolean endsWithCondBranch(int blockId) {
        BasicBlock b = cfg.block(blockId);
        if (b.lastInsn() < 0 || b.lastInsn() >= insns.size() || b.lastInsn() == b.firstInsn() - 1) {
            return false;
        }
        Insn last = insns.get(b.lastInsn());
        int op = last.opcode();
        return (op >= 153 && op <= 166) || op == 198 || op == 199;
    }

    private int skipToControlSuccessor(int block, int stop) {
        List<Integer> succs = normalSuccessors(block);
        for (int s : succs) {
            if (s == stop) {
                return stop;
            }
            if (!emitted[s]) {
                return s;
            }
        }
        return succs.isEmpty() ? -1 : succs.get(0);
    }

    private ExpressionBuilder.Result reconstruct(int blockId) {
        BasicBlock b = cfg.block(blockId);
        return new ExpressionBuilder(varNamer).build(insns, b.firstInsn(), b.lastInsn());
    }

    private Stmt linearFallback() {
        List<Stmt> out = new ArrayList<>();
        for (BasicBlock b : cfg.blocks()) {
            out.add(new Stmt.Label(b.firstInsn(), "L" + b.id()));
            ExpressionBuilder.Result r = reconstruct(b.id());
            out.addAll(r.statements());
        }
        return new Stmt.Block(0, Math.max(0, insns.size() - 1), out);
    }

    private int firstInsnOf(int block) {
        if (block < 0 || block >= cfg.blockCount()) {
            return -1;
        }
        return cfg.block(block).firstInsn();
    }

    private int lastInsnOf(int from, int to) {
        if (from < 0 || from >= cfg.blockCount()) {
            return -1;
        }
        int end = cfg.block(from).lastInsn();
        if (to >= 0 && to < cfg.blockCount()) {
            end = Math.max(end, cfg.block(to).firstInsn() - 1);
        }
        return end;
    }
}
