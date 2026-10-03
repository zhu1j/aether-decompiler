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
import com.aetherdecompiler.core.model.TryCatchEntry;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
    /** 方法异常表：用于把“受保护区间 + 处理器”还原为 try/catch。 */
    private final List<TryCatchEntry> tryEntries;
    /** 异常处理器所在局部槽位（对每个处理器块，取入口后的首个 astore）。 */
    private final List<TryCatchEntry> tryEntryList;
    /** 起始块 id → 以该块为受保护区起点的异常表条目。 */
    private final Map<Integer, List<TryCatchEntry>> tryByStartBlock = new LinkedHashMap<>();
    /** 已被 try 结构化消费的起始块，避免 region 递归时重复触发。 */
    private final Set<Integer> tryHandled = new HashSet<>();
    /** 起始块 id → 结构化该 try 之后应继续的块 id。 */
    private final Map<Integer, Integer> tryFollow = new HashMap<>();

    /**
     * @param cfg 控制流图
     * @param dom 支配树
     */
    public ControlStructurer(ControlFlowGraph cfg, DominatorTree dom) {
        this(cfg, dom, null, List.of());
    }

    /**
     * @param cfg      控制流图
     * @param dom      支配树
     * @param varNamer 槽位 → 显示名的映射（可传 SSA 名或 {@code this}）；{@code null} 时用 {@code vN}
     */
    public ControlStructurer(ControlFlowGraph cfg, DominatorTree dom,
                             java.util.function.IntFunction<String> varNamer) {
        this(cfg, dom, varNamer, List.of());
    }

    /**
     * @param cfg        控制流图
     * @param dom        支配树
     * @param varNamer   槽位 → 显示名的映射（可传 SSA 名或 {@code this}）；{@code null} 时用 {@code vN}
     * @param tryEntries 方法异常表；用于还原 try/catch 结构，空表示无异常处理
     */
    public ControlStructurer(ControlFlowGraph cfg, DominatorTree dom,
                             java.util.function.IntFunction<String> varNamer,
                             List<TryCatchEntry> tryEntries) {
        this.cfg = cfg;
        this.dom = dom;
        this.varNamer = varNamer;
        this.tryEntries = List.copyOf(tryEntries);
        this.tryEntryList = this.tryEntries;
        this.insns = cfg.instructions();
        this.emitted = new boolean[Math.max(1, cfg.blockCount())];
        this.predecessors = computePredecessors();
        detectLoops();
        indexTryEntries();
    }

    /** 把异常表按“受保护区间的起始块”建索引，供结构化主循环识别 try 入口。 */
    private void indexTryEntries() {
        for (TryCatchEntry e : tryEntries) {
            int startBlock = blockIdOfInsn(e.startIndex());
            if (startBlock < 0) {
                continue;
            }
            tryByStartBlock.computeIfAbsent(startBlock, k -> new ArrayList<>()).add(e);
        }
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
            // 同步块：当前块以 monitorenter 结尾，其后由编译器生成的 finally
            // （monitorexit + athrow）守护。优先还原为 synchronized，避免泄漏
            // 监视器指令或退化出错误的 try/finally。
            if (endsWithMonitorEnter(cur) && !tryHandled.contains(cur)) {
                Stmt sync = structureSynchronized(cur, depth);
                if (sync != null) {
                    out.add(sync);
                    Integer follow = syncFollow.get(cur);
                    cur = follow == null ? -1 : follow;
                    continue;
                }
            }
            // 异常处理：若当前块是某个受保护区间的起点，优先还原为 try/catch。
            if (tryByStartBlock.containsKey(cur) && !tryHandled.contains(cur)) {
                Stmt tryStmt = structureTry(cur, depth);
                if (tryStmt != null) {
                    out.add(tryStmt);
                    Integer follow = tryFollow.get(cur);
                    cur = follow == null ? -1 : follow;
                    continue;
                }
            }
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
    /** 同步块头部块 id → 结构化该同步块之后应继续的块 id。 */
    private final Map<Integer, Integer> syncFollow = new HashMap<>();
    /** 块 id → 基本块对象，供同步块还原时读取后继等结构信息。 */
    private final Map<Integer, BasicBlock> blockById = new HashMap<>();

    /**
     * 把一个受保护区间还原为 {@link Stmt.TryCatch}。
     *
     * <p>算法：以异常表中“起始块”命中的条目为入口，取受保护区间的首尾界定 try 体范围；
     * try 体由 {@code region} 在普通后继上结构化（异常边不参与普通规约，因此处理器
     * 不会被误并入 try 体）。随后把同一受保护区间的每个处理器结构化为一个
     * {@link Stmt.CatchClause}。若边界不干净（起止落在同一块内）或无可还原的处理器，
     * 则返回 {@code null}，交回线性结构化主流程，绝不硬掰出错误的树。</p>
     *
     * @param startBlock 受保护区间的起始块 id
     * @param depth      递归深度
     * @return 还原出的 try/catch；无法还原时为 {@code null}
     */
    private Stmt structureTry(int startBlock, int depth) {
        List<TryCatchEntry> entries = tryByStartBlock.get(startBlock);
        if (entries == null || entries.isEmpty()) {
            return null;
        }
        TryCatchEntry first = entries.get(0);
        int endInsn = first.endIndex();
        int stopBlock = endInsn >= insns.size() ? -1 : blockIdOfInsn(endInsn);
        // 起止落在同一块内：无法在块粒度上切出干净的 try 区间，诚实地放弃。
        if (stopBlock == startBlock) {
            return null;
        }
        tryHandled.add(startBlock);
        Stmt body = region(startBlock, stopBlock, depth + 1);

        List<Stmt.CatchClause> catches = new ArrayList<>();
        for (TryCatchEntry e : entries) {
            if (e.startIndex() != first.startIndex() || e.endIndex() != first.endIndex()) {
                continue;
            }
            int hBlock = blockIdOfInsn(e.handlerIndex());
            if (hBlock < 0) {
                continue;
            }
            int slot = catchVarSlot(e.handlerIndex());
            // 处理器入口的 astore 把“栈上的异常对象”存入捕获变量。异常对象来自隐式
            // 栈顶，无法在表达式层建模，且 catch 形参已声明该变量；因此剥离这条首语句，
            // 否则会渲染出形如“ex = <unresolved>;”的噪声。
            Stmt hBody = stripCatchStore(region(hBlock, stopBlock, depth + 1), slot);
            catches.add(new Stmt.CatchClause(e.catchType(), slot, hBody));
        }
        if (catches.isEmpty()) {
            return null;
        }
        tryFollow.put(startBlock, stopBlock);
        return new Stmt.TryCatch(firstInsnOf(startBlock), lastInsnOf(startBlock, stopBlock),
                body, catches);
    }

    /** 判断某块是否以 {@code monitorenter} 结尾（同步块的头部特征）。 */
    private boolean endsWithMonitorEnter(int blockId) {
        BasicBlock b = cfg.block(blockId);
        if (b == null || b.lastInsn() < 0 || b.lastInsn() >= insns.size()) {
            return false;
        }
        return insns.get(b.lastInsn()).opcode() == 194;
    }

    /**
     * 把一个同步块还原为 {@link Stmt.Synchronized}。
     *
     * <p>javac 把 {@code synchronized (lock) { body }} 展开为：计算锁引用 + {@code dup}
     * + {@code astore} 暂存 + {@code monitorenter}，随后是被编译器生成的 finally
     * （{@code monitorexit} + {@code athrow}）保护的同步体。这里识别“以 monitorenter
     * 结尾的头部块”，取回锁表达式，把受保护区间结构化为同步体，并跳到监视器释放后的
     * 汇合点继续。无法干净还原时返回 {@code null}，交回普通流程，绝不硬掰。</p>
     *
     * @param headerBlock 同步块头部块 id（以 monitorenter 结尾）
     * @param depth       递归深度
     * @return 还原出的同步语句；无法还原时为 {@code null}
     */
    private Stmt structureSynchronized(int headerBlock, int depth) {
        BasicBlock header = cfg.block(headerBlock);
        if (header == null) {
            return null;
        }
        int mEnter = header.lastInsn();
        if (mEnter < 0 || mEnter >= insns.size() || insns.get(mEnter).opcode() != 194) {
            return null;
        }
        List<Integer> hSucc = normalSuccessors(headerBlock);
        if (hSucc.size() != 1) {
            return null;
        }
        int bodyStart = hSucc.get(0);
        int bodyFirstInsn = firstInsnOf(bodyStart);
        TryCatchEntry guard = null;
        for (TryCatchEntry e : tryEntries) {
            if (e.startIndex() == bodyFirstInsn) {
                guard = e;
                break;
            }
        }
        if (guard == null) {
            return null;
        }
        int endBlock = blockIdOfInsn(guard.endIndex() - 1);
        int follow = -1;
        if (endBlock >= 0) {
            List<Integer> es = normalSuccessors(endBlock);
            if (!es.isEmpty()) {
                follow = es.get(0);
            }
        }
        if (follow < 0) {
            return null;
        }
        Expr lock = buildLockExpr(headerBlock, mEnter);
        tryHandled.add(headerBlock);
        tryHandled.add(bodyStart);
        Stmt body = region(bodyStart, follow, depth + 1);
        syncFollow.put(headerBlock, follow);
        return new Stmt.Synchronized(header.firstInsn(), mEnter, lock, body);
    }

    /**
     * 取回同步块的锁表达式：在头部块上（不含 monitorenter）做一次块内重建，栈顶即锁。
     */
    private Expr buildLockExpr(int headerBlock, int mEnter) {
        ExpressionBuilder eb = new ExpressionBuilder(varNamer);
        eb.build(insns, firstInsnOf(headerBlock), mEnter - 1);
        Expr lock = eb.topExpr();
        return lock != null ? lock : new Expr.Opaque(mEnter, "monitor");
    }

    /**
     * 探测某个异常处理器入口的捕获变量槽位：处理器入口通常紧接一条 {@code astore}，
     * 把抛出的异常存入局部变量。找到则返回该槽位，供渲染器恢复 {@code catch (T name)}。
     */
    private int catchVarSlot(int handlerIndex) {
        int limit = Math.min(insns.size(), handlerIndex + 4);
        for (int i = Math.max(0, handlerIndex); i < limit; i++) {
            int op = insns.get(i).opcode();
            if (op == 58) { // astore
                return parseStoreSlot(insns.get(i).operand());
            }
            if (op >= 75 && op <= 78) { // astore_0..3
                return op - 75;
            }
            if (op == 87) { // pop：捕获变量被丢弃
                return -1;
            }
        }
        return -1;
    }

    /** 从 {@code astore var <slot>} 的操作数中解析槽位号（操作数形如 {@code "var 4"}）。 */
    private static int parseStoreSlot(String operand) {
        if (operand == null) {
            return -1;
        }
        String s = operand.trim();
        if (s.startsWith("var ")) {
            s = s.substring(4).trim();
        }
        try {
            return Integer.parseInt(s);
        } catch (RuntimeException ex) {
            return -1;
        }
    }

    private int blockIdOfInsn(int insnIndex) {
        if (insnIndex < 0) {
            return -1;
        }
        BasicBlock b = cfg.blockOfInsn(insnIndex);
        return b == null ? -1 : b.id();
    }

    /**
     * 剥离 catch 体开头的“异常对象入槽”赋值语句。
     *
     * <p>处理器入口的 {@code astore} 把隐式栈顶的异常对象存入捕获变量，这条赋值在
     * 表达式层没有对应来源（渲染为占位注释）。既然 catch 形参已经声明了该变量，
     * 这里就把这条首语句移除，避免输出噪声。</p>
     */
    private static Stmt stripCatchStore(Stmt body, int slot) {
        if (slot < 0 || !(body instanceof Stmt.Block b)) {
            return body;
        }
        List<Stmt> ss = b.statements();
        if (ss.isEmpty() || !isStoreTo(ss.get(0), slot)) {
            return body;
        }
        List<Stmt> rest = new ArrayList<>(ss.subList(1, ss.size()));
        if (rest.size() == 1) {
            return rest.get(0);
        }
        return new Stmt.Block(b.firstInsn(), b.lastInsn(), rest);
    }

    /** 判断一条语句是否为“对指定局部槽位的赋值”。 */
    private static boolean isStoreTo(Stmt s, int slot) {
        if (s instanceof Stmt.ExprStmt es && es.expr() instanceof Expr.Assign a) {
            return a.target() instanceof Expr.Local l && l.slot() == slot;
        }
        return false;
    }

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
