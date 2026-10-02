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
 * Builds a {@link ControlFlowGraph} from an instruction list.
 *
 * <p>Algorithm: leaders-first block splitting. A new basic block starts at the
 * method entry, at every branch target, and at the instruction after any branch
 * (including the instructions after a switch and after each exception handler
 * entry). This classic O(n) pass yields a maximal single-entry block partition
 * without consulting a library.</p>
 *
 * <p>Story analogy: parcelling a long ribbon into segments. You cut at the
 * start, at every junction where traffic can enter, and just after every
 * junction where traffic can leave — the result is segments no one can enter
 * midway.</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class CfgBuilder {

    /**
     * Build the CFG for one method.
     *
     * @param owner  the owning class (for diagnostics)
     * @param method the method model
     * @return the control-flow graph
     * @throws AetherException if a jump targets an instruction that does not exist
     */
    public ControlFlowGraph build(ClassModel owner, MethodModel method) {
        List<Insn> insns = method.instructions();
        int n = insns.size();
        if (n == 0) {
            return new ControlFlowGraph(owner.name(), method.id(), insns, List.of());
        }

        // 1. Identify leaders: entries of basic blocks.
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
                    // A target equal to n means "fall off the end": legal; it
                    // simply ends the method and creates no new block.
                    if (target < n) {
                        isLeader[target] = true;
                    }
                }
                if (i + 1 < n) {
                    isLeader[i + 1] = true;
                }
            }
        }
        // Exception handler entries are leaders too.
        for (TryCatchEntry tce : method.tryCatchEntries()) {
            if (tce.handlerIndex() >= 0 && tce.handlerIndex() < n) {
                isLeader[tce.handlerIndex()] = true;
            }
        }

        // 2. Cut the instruction stream into blocks at the leaders.
        List<int[]> ranges = new ArrayList<>(); // [firstInsn, lastInsn]
        int start = 0;
        for (int i = 1; i < n; i++) {
            if (isLeader[i]) {
                ranges.add(new int[]{start, i - 1});
                start = i;
            }
        }
        ranges.add(new int[]{start, n - 1});

        // 3. Map instruction index -> block id.
        int[] blockOfInsn = new int[n];
        for (int b = 0; b < ranges.size(); b++) {
            int[] r = ranges.get(b);
            for (int i = r[0]; i <= r[1]; i++) {
                blockOfInsn[i] = b;
            }
        }

        // 4. Compute successors for each block from its last instruction.
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
                // Conditional branches fall through; unconditional ones do not.
                if (isConditional(tail.opcode()) && last + 1 < n) {
                    succ.add(blockOfInsn[last + 1]);
                }
            } else if (!isTerminal(tail.opcode()) && last + 1 < n) {
                succ.add(blockOfInsn[last + 1]);
            }

            blocks.add(new BasicBlock(b, r[0], r[1],
                    new ArrayList<>(succ), List.of(), b == 0));
        }

        // 5. Attach exception edges: for each protected range, its blocks gain
        //    an edge to the handler's block.
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
        // 0x99..0xA6 are the two-operand integer/reference conditional jumps,
        // 0xC6/0xC7 are ifnull/ifnonnull. goto/jsr are unconditional.
        return (opcode >= 0x99 && opcode <= 0xA6) || opcode == 0xC6 || opcode == 0xC7;
    }

    private boolean isTerminal(int opcode) {
        // return family (0xAC..0xB1), athrow (0xBF), goto/jsr and switch are
        // handled by the branch path; ret (0xA9) also ends a block.
        return (opcode >= 0xAC && opcode <= 0xB1) || opcode == 0xBF || opcode == 0xA9;
    }
}
