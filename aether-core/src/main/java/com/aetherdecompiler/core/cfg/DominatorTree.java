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

import com.aetherdecompiler.core.model.BasicBlock;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * 计算 {@link ControlFlowGraph} 的支配树。
 *
 * <p>当从入口到 {@code n} 的每条路径都经过 {@code d} 时，节点 {@code d} 支配
 * 节点 {@code n}。{@code n} 的直接支配者就是最近的那个这样的节点。支配关系是
 * 循环检测与结构化控制流重建的骨架，因此在这里作为一等视图计算出来。</p>
 *
 * <p>实现采用在逆后序编号上的经典迭代式 Cooper–Harvey–Kennedy 数据流
 * 形式：简单、确定且无依赖。</p>
 *
 * <p>故事类比：指挥链。要让命令到达一名士兵，每条命令都必须经过他的
 * 直接上级 —— 那便是直接支配者。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class DominatorTree {

    private final int[] idom;      // 每个块 id 的直接支配者（入口为 -1）
    private final int[] rpoNumber; // 块 id -> 逆后序编号
    private final int[] byRpo;     // 逆后序编号 -> 块 id

    private DominatorTree(int[] idom, int[] rpoNumber, int[] byRpo) {
        this.idom = idom;
        this.rpoNumber = rpoNumber;
        this.byRpo = byRpo;
    }

    /**
     * 为 CFG 构建支配树。
     *
     * @param cfg 控制流图
     * @return 支配树
     */
    public static DominatorTree of(ControlFlowGraph cfg) {
        int n = cfg.blockCount();
        int[] idom = new int[n];
        for (int i = 0; i < n; i++) {
            idom[i] = -1;
        }
        if (n == 0) {
            return new DominatorTree(idom, new int[0], new int[0]);
        }

        // 1. 从入口块开始做逆后序。
        int[] order = reversePostorder(cfg);
        int[] rpoNumber = new int[n];
        for (int i = 0; i < n; i++) {
            rpoNumber[i] = -1;
        }
        for (int i = 0; i < order.length; i++) {
            rpoNumber[order[i]] = i;
        }
        int[] byRpo = order.clone();

        // 2. 在逆后序上迭代求直接支配者不动点。
        idom[0] = 0;
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int i = 1; i < byRpo.length; i++) {
                int b = byRpo[i];
                int newIdom = -1;
                for (int pred : predecessors(cfg, b)) {
                    if (rpoNumber[pred] == -1) {
                        continue; // 不可达前驱
                    }
                    // 跳过尚未分配自身直接支配者的前驱。
                    // 按逆后序处理可保证至少有一个
                    // 前驱已被赋值，因此 newIdom 一定有值。
                    if (pred != 0 && idom[pred] == -1) {
                        continue;
                    }
                    if (newIdom == -1) {
                        newIdom = pred;
                    } else {
                        newIdom = intersect(newIdom, pred, idom, rpoNumber);
                    }
                }
                if (newIdom != -1 && idom[b] != newIdom) {
                    idom[b] = newIdom;
                    changed = true;
                }
            }
        }

        // 入口支配它自己，但没有父节点。
        idom[0] = -1;
        return new DominatorTree(idom, rpoNumber, byRpo);
    }

    private static int intersect(int a, int b, int[] idom, int[] rpoNumber) {
        while (a != b) {
            while (rpoNumber[a] > rpoNumber[b]) {
                a = idom[a];
                if (a < 0) {
                    return b;
                }
            }
            while (rpoNumber[b] > rpoNumber[a]) {
                b = idom[b];
                if (b < 0) {
                    return a;
                }
            }
        }
        return a;
    }

    private static int[] reversePostorder(ControlFlowGraph cfg) {
        int n = cfg.blockCount();
        boolean[] visited = new boolean[n];
        List<Integer> post = new ArrayList<>(n);
        Deque<int[]> stack = new ArrayDeque<>();
        stack.push(new int[]{0, 0});
        visited[0] = true;
        while (!stack.isEmpty()) {
            int[] frame = stack.peek();
            int node = frame[0];
            List<Integer> succ = cfg.block(node).successors();
            if (frame[1] < succ.size()) {
                int next = succ.get(frame[1]++);
                if (!visited[next]) {
                    visited[next] = true;
                    stack.push(new int[]{next, 0});
                }
            } else {
                post.add(node);
                stack.pop();
            }
        }
        int[] result = new int[post.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = post.get(post.size() - 1 - i);
        }
        return result;
    }

    private static List<Integer> predecessors(ControlFlowGraph cfg, int blockId) {
        List<Integer> preds = new ArrayList<>();
        for (BasicBlock b : cfg.blocks()) {
            if (b.successors().contains(blockId) || b.exceptionSuccessors().contains(blockId)) {
                preds.add(b.id());
            }
        }
        return preds;
    }

    /**
     * @param blockId 块 id
     * @return 直接支配者的块 id；对入口或不可达块返回 {@code -1}
     */
    public int immediateDominator(int blockId) {
        return idom[blockId];
    }

    /**
     * @param blockId 块 id
     * @return 该块是否可从入口到达
     */
    public boolean isReachable(int blockId) {
        return rpoNumber[blockId] != -1;
    }

    /**
     * @param dominator 候选支配者块 id
     * @param node      块 id
     * @return {@code dominator} 是否支配 {@code node}
     */
    public boolean dominates(int dominator, int node) {
        if (!isReachable(node) || !isReachable(dominator)) {
            return false;
        }
        int cur = node;
        while (cur != -1) {
            if (cur == dominator) {
                return true;
            }
            cur = idom[cur];
        }
        return false;
    }

    /**
     * @param blockId 块 id
     * @return 被 {@code blockId} 支配的块 id（树中的子节点），
     *         不包含它自身
     */
    public List<Integer> children(int blockId) {
        List<Integer> children = new ArrayList<>();
        for (int i = 0; i < idom.length; i++) {
            if (idom[i] == blockId && i != blockId) {
                children.add(i);
            }
        }
        return children;
    }
}
