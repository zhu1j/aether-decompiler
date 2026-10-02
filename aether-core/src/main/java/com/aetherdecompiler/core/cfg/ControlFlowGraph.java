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

import com.aetherdecompiler.api.CfgView;
import com.aetherdecompiler.api.IRKind;
import com.aetherdecompiler.core.model.BasicBlock;
import com.aetherdecompiler.core.model.Insn;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * An immutable control-flow graph for one method.
 *
 * <p>A CFG is the method's instruction list plus the partition of those
 * instructions into {@link BasicBlock}s and the edges between them. It is the
 * kernel's first structural view of a method and the substrate for the
 * dominator tree, loop detection (Phase 1) and structured reconstruction
 * (Phase 3).</p>
 *
 * <p>Story analogy: a city road map. The instructions are addresses; the basic
 * blocks are intersections where route choices happen; the edges are the roads
 * between them.</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class ControlFlowGraph implements CfgView {

    private final String ownerClass;
    private final String methodId;
    private final List<Insn> instructions;
    private final List<BasicBlock> blocks;

    /**
     * @param ownerClass   the owning class internal name
     * @param methodId     the method identifier {@code name + descriptor}
     * @param instructions the full instruction list
     * @param blocks       the basic blocks, indexed by their id
     */
    public ControlFlowGraph(String ownerClass, String methodId,
                            List<Insn> instructions, List<BasicBlock> blocks) {
        this.ownerClass = Objects.requireNonNull(ownerClass, "ownerClass");
        this.methodId = Objects.requireNonNull(methodId, "methodId");
        this.instructions = List.copyOf(instructions);
        this.blocks = List.copyOf(blocks);
    }

    /** @return the owning class internal name */
    public String ownerClass() {
        return ownerClass;
    }

    /** @return the method identifier */
    public String methodId() {
        return methodId;
    }

    /** @return the full instruction list */
    public List<Insn> instructions() {
        return instructions;
    }

    /** @return the basic blocks, indexed by id */
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
     * @param id a block id
     * @return the block with that id
     */
    public BasicBlock block(int id) {
        return blocks.get(id);
    }

    /**
     * @param insnIndex an instruction index
     * @return the block that contains that instruction, or {@code null}
     */
    public BasicBlock blockOfInsn(int insnIndex) {
        for (BasicBlock block : blocks) {
            if (insnIndex >= block.firstInsn() && insnIndex <= block.lastInsn()) {
                return block;
            }
        }
        return null;
    }

    /** @return the number of blocks */
    public int blockCount() {
        return blocks.size();
    }

    /** @return the number of edges, counting normal and exception edges */
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
