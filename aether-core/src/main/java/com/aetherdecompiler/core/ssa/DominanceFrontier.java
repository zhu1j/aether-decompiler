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

import java.util.ArrayList;
import java.util.List;

/**
 * 支配边界（dominance frontier）视图——SSA 构造中“phi 该插在哪里”的精确答案。
 *
 * <p>定义：节点 {@code d} 属于节点 {@code b} 的支配边界，当且仅当 {@code b}
 * <em>支配</em> {@code d} 的某条前驱，却<em>不严格支配</em> {@code d} 本身。直观地说，
 * 支配边界正是“{@code b} 的影响力恰好失效的边界”——也是“某变量在某处被定义，但该定义
 * 并不能支配所有到达那里的路径”这一情况首次出现的地方。因此，只要把 phi 放在支配边界上，
 * 就能在不需要任何数据流迭代的情况下，精确地合并多路定值。</p>
 *
 * <p>本实现使用 Cooper–Harvey–Kennedy 提出的经典上溯算法，在已经算好的
 * {@link DominatorTree} 上运行，时间复杂度为近线性：对每个多前驱块 {@code b}，沿每条
 * 前驱 {@code p} 的直接支配者链向 {@code idom(b)} 上爬，把 {@code b} 记入沿途每个节点的
 * 支配边界。</p>
 *
 * <p>故事类比：城市供水。上游水厂的影响一路向下，直到某个岔路口突然有多路水源汇合——
 * 那里就是水厂“独占话语权”的边界，也是必须装上“按来源选水”阀门（phi）的地方。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class DominanceFrontier {

    private final List<List<Integer>> frontier;

    private DominanceFrontier(List<List<Integer>> frontier) {
        this.frontier = frontier;
    }

    /**
     * 为一个 CFG 及其支配树计算支配边界。
     *
     * @param cfg 控制流图
     * @param dom 该 CFG 的支配树
     * @return 支配边界视图
     */
    public static DominanceFrontier of(ControlFlowGraph cfg, DominatorTree dom) {
        int n = cfg.blockCount();
        List<List<Integer>> df = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            df.add(new ArrayList<>());
        }
        for (int b = 0; b < n; b++) {
            if (!dom.isReachable(b)) {
                continue;
            }
            List<Integer> preds = reachablePredecessors(cfg, dom, b);
            // 单前驱块不会“汇合”多路定值，因此不产生贡献。
            if (preds.size() < 2) {
                continue;
            }
            int idomB = dom.immediateDominator(b);
            for (int p : preds) {
                int runner = p;
                while (runner != -1 && runner != idomB) {
                    if (!df.get(runner).contains(b)) {
                        df.get(runner).add(b);
                    }
                    runner = dom.immediateDominator(runner);
                }
            }
        }
        return new DominanceFrontier(df);
    }

    private static List<Integer> reachablePredecessors(ControlFlowGraph cfg, DominatorTree dom, int blockId) {
        List<Integer> preds = new ArrayList<>();
        for (BasicBlock b : cfg.blocks()) {
            if (!dom.isReachable(b.id())) {
                continue;
            }
            if (b.successors().contains(blockId) || b.exceptionSuccessors().contains(blockId)) {
                preds.add(b.id());
            }
        }
        return preds;
    }

    /**
     * @param blockId 块 id
     * @return 该块支配边界中的块 id 列表（永不为 {@code null}）
     */
    public List<Integer> frontierOf(int blockId) {
        return frontier.get(blockId);
    }

    /**
     * @param blockId 块 id
     * @param member  候选的支配边界成员
     * @return {@code member} 是否属于 {@code blockId} 的支配边界
     */
    public boolean contains(int blockId, int member) {
        return frontier.get(blockId).contains(member);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("DominanceFrontier{");
        for (int i = 0; i < frontier.size(); i++) {
            if (!frontier.get(i).isEmpty()) {
                sb.append("B").append(i).append("->").append(frontier.get(i)).append(' ');
            }
        }
        return sb.append('}').toString();
    }
}
