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
package com.aetherdecompiler.core.model;

import com.aetherdecompiler.api.CfgView;

import java.util.List;
import java.util.Objects;

/**
 * An immutable basic block: a maximal run of instructions with a single entry
 * and a single exit.
 *
 * <p>A block owns an inclusive range of instruction indices and references its
 * successors by block id. Keeping successors as ids (not object references)
 * preserves immutability and makes the graph trivially serialisable and
 * cache-friendly.</p>
 *
 * <p>Story analogy: one paragraph of a procedure — entered at the top, left at
 * the bottom, never jumped into the middle.</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class BasicBlock implements CfgView.Block {

    private final int id;
    private final int firstInsn;
    private final int lastInsn;
    private final List<Integer> successors;
    private final List<Integer> exceptionSuccessors;
    private final boolean entry;

    /**
     * @param id                   the block id (equals its position in the CFG list)
     * @param firstInsn            inclusive first instruction index
     * @param lastInsn             inclusive last instruction index
     * @param successors           normal control-flow successor block ids
     * @param exceptionSuccessors  handler block ids reachable via the exception table
     * @param entry                whether this is the method entry block
     */
    public BasicBlock(int id, int firstInsn, int lastInsn,
                      List<Integer> successors, List<Integer> exceptionSuccessors,
                      boolean entry) {
        this.id = id;
        this.firstInsn = firstInsn;
        this.lastInsn = lastInsn;
        this.successors = List.copyOf(successors);
        this.exceptionSuccessors = List.copyOf(exceptionSuccessors);
        this.entry = entry;
    }

    /** @return the block id */
    public int id() {
        return id;
    }

    /** @return inclusive first instruction index */
    public int firstInsn() {
        return firstInsn;
    }

    /** @return inclusive last instruction index */
    public int lastInsn() {
        return lastInsn;
    }

    /** @return normal successor block ids */
    public List<Integer> successors() {
        return successors;
    }

    /** @return exception handler block ids */
    public List<Integer> exceptionSuccessors() {
        return exceptionSuccessors;
    }

    /** @return whether this is the entry block */
    public boolean isEntry() {
        return entry;
    }

    /** @return how many instructions this block spans */
    public int instructionCount() {
        return lastInsn - firstInsn + 1;
    }

    /** @return a short display label, e.g. {@code "B3[10..15]"} */
    public String label() {
        return "B" + id + "[" + firstInsn + ".." + lastInsn + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BasicBlock other)) {
            return false;
        }
        return id == other.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return label();
    }
}
