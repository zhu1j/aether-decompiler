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
package com.aetherdecompiler.api;

import java.util.List;

/**
 * A neutral, read-only view of a control-flow graph.
 *
 * <p>This interface is the linchpin that lets a plugin consume a kernel-built
 * CFG <em>without depending on the kernel</em>. The plugin API declares the
 * shape; the kernel's concrete {@code ControlFlowGraph} implements it. A plugin
 * therefore only ever names {@code aether}-API types.</p>
 *
 * <p>Story analogy: a standard measuring gauge handed to a contractor. The
 * contractor reads the building's dimensions through the gauge; they never need
 * the architect's private CAD software.</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public interface CfgView extends IRObject {

    /** @return the owning class internal name */
    String ownerClass();

    /** @return the method identifier ({@code name + descriptor}) */
    String methodId();

    /**
     * @return the display text of every instruction, indexed by instruction id
     */
    List<String> insnTexts();

    /**
     * @return the blocks of the graph, in id order
     */
    List<? extends Block> blocks();

    /**
     * A neutral, read-only view of one basic block.
     *
     * @author Jerry Zhu (Zeek)
     */
    interface Block {

        /** @return the block id */
        int id();

        /** @return inclusive first instruction index */
        int firstInsn();

        /** @return inclusive last instruction index */
        int lastInsn();

        /** @return normal successor block ids */
        List<Integer> successors();

        /** @return exception handler block ids */
        List<Integer> exceptionSuccessors();

        /** @return whether this is the method entry block */
        boolean isEntry();
    }
}
