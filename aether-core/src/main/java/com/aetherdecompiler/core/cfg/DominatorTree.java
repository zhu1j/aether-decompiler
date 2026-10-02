/*
 * aether-decompiler — an independent, reusable JVM decompilation engine.
 * Copyright 2026 Jerry Zhu (Zeek) <zhujiejava1@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author Jerry Zhu (Zeek)
 * "Run the Code, Run the World!"
 */
package com.aetherdecompiler.core.cfg;

import com.aetherdecompiler.core.model.BasicBlock;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Computes the dominator tree of a {@link ControlFlowGraph}.
 *
 * <p>Node {@code d} dominates node {@code n} when every path from the entry to
 * {@code n} passes through {@code d}. The immediate dominator of {@code n} is
 * the closest such node. Dominance is the backbone of loop detection and of
 * structured control-flow reconstruction, so it is computed here as a first
 * class view.</p>
 *
 * <p>Implementation uses the classic iterative Cooper–Harvey–Kennedy data-flow
 * formulation over reverse-postorder numbers: simple, deterministic, and
 * dependency-free.</p>
 *
 * <p>Story analogy: the chain of command. To reach a soldier every order must
 * pass through their direct superior — the immediate dominator.</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class DominatorTree {

    private final int[] idom;      // immediate dominator per block id (-1 for entry)
    private final int[] rpoNumber; // block id -> reverse-postorder number
    private final int[] byRpo;     // reverse-postorder number -> block id

    private DominatorTree(int[] idom, int[] rpoNumber, int[] byRpo) {
        this.idom = idom;
        this.rpoNumber = rpoNumber;
        this.byRpo = byRpo;
    }

    /**
     * Build the dominator tree for a CFG.
     *
     * @param cfg the control-flow graph
     * @return the dominator tree
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

        // 1. Reverse postorder from the entry block.
        int[] order = reversePostorder(cfg);
        int[] rpoNumber = new int[n];
        for (int i = 0; i < n; i++) {
            rpoNumber[i] = -1;
        }
        for (int i = 0; i < order.length; i++) {
            rpoNumber[order[i]] = i;
        }
        int[] byRpo = order.clone();

        // 2. Iterative immediate-dominator fixpoint in RPO.
        idom[0] = 0;
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int i = 1; i < byRpo.length; i++) {
                int b = byRpo[i];
                int newIdom = -1;
                for (int pred : predecessors(cfg, b)) {
                    if (rpoNumber[pred] == -1) {
                        continue; // unreachable predecessor
                    }
                    // Skip predecessors whose own idom is not yet assigned.
                    // Processing in reverse postorder guarantees at least one
                    // predecessor is already assigned, so newIdom is set.
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

        // Entry dominates itself but has no parent.
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
     * @param blockId a block id
     * @return the immediate dominator's block id, or {@code -1} for the entry
     *         or an unreachable block
     */
    public int immediateDominator(int blockId) {
        return idom[blockId];
    }

    /**
     * @param blockId a block id
     * @return whether the block is reachable from the entry
     */
    public boolean isReachable(int blockId) {
        return rpoNumber[blockId] != -1;
    }

    /**
     * @param dominator a candidate dominator block id
     * @param node      a block id
     * @return whether {@code dominator} dominates {@code node}
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
     * @param blockId a block id
     * @return the block ids dominated by {@code blockId} (children in the tree),
     *         not including itself
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
