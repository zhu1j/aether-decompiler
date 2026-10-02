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
package com.aetherdecompiler.core;

import com.aetherdecompiler.core.asm.AsmClassParser;
import com.aetherdecompiler.core.cfg.CfgBuilder;
import com.aetherdecompiler.core.cfg.ControlFlowGraph;
import com.aetherdecompiler.core.cfg.DominatorTree;
import com.aetherdecompiler.core.model.BasicBlock;
import com.aetherdecompiler.core.model.ClassModel;
import com.aetherdecompiler.core.model.MethodModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for CFG construction and the dominator tree.
 *
 * @author Jerry Zhu (Zeek)
 */
class CfgBuilderTest {

    private ClassModel sample() throws Exception {
        String resource = Sample.class.getName().replace('.', '/') + ".class";
        try (var in = Sample.class.getClassLoader().getResourceAsStream(resource)) {
            return new AsmClassParser().parse(in.readAllBytes());
        }
    }

    private ControlFlowGraph cfgFor(String name, String desc) throws Exception {
        ClassModel model = sample();
        MethodModel method = model.findMethod(name, desc).orElseThrow();
        return new CfgBuilder().build(model, method);
    }

    @Test
    void branchyMethodHasMultipleBlocks() throws Exception {
        ControlFlowGraph cfg = cfgFor("sumTo", "(I)I");
        assertTrue(cfg.blockCount() > 1, "loop + guard should split into several blocks");
        // Entry block must exist and be flagged.
        assertTrue(cfg.blocks().get(0).isEntry());
        // Every successor id must be a valid block id (the graph is closed).
        for (BasicBlock b : cfg.blocks()) {
            for (int s : b.successors()) {
                assertTrue(s >= 0 && s < cfg.blockCount(), "dangling successor " + s);
            }
        }
    }

    @Test
    void tryCatchProducesExceptionEdges() throws Exception {
        ControlFlowGraph cfg = cfgFor("parse", "(Ljava/lang/String;)I");
        int exceptionEdges = cfg.blocks().stream()
                .mapToInt(b -> b.exceptionSuccessors().size())
                .sum();
        assertTrue(exceptionEdges > 0, "parse() has a try/catch and must expose an exception edge");
    }

    @Test
    void dominatorTreeEntryDominatesAllReachable() throws Exception {
        ControlFlowGraph cfg = cfgFor("sumTo", "(I)I");
        DominatorTree dom = DominatorTree.of(cfg);
        for (BasicBlock b : cfg.blocks()) {
            if (dom.isReachable(b.id())) {
                assertTrue(dom.dominates(0, b.id()),
                        "entry must dominate reachable block " + b.id());
            }
        }
    }

    @Test
    void switchProducesManySuccessors() throws Exception {
        ControlFlowGraph cfg = cfgFor("label", "(I)Ljava/lang/String;");
        int maxSucc = cfg.blocks().stream().mapToInt(b -> b.successors().size()).max().orElse(0);
        assertTrue(maxSucc >= 3, "tableswitch should branch to several blocks, got " + maxSucc);
    }

    @Test
    void blockOfInsnMapsEveryInstruction() throws Exception {
        ControlFlowGraph cfg = cfgFor("sumTo", "(I)I");
        for (int i = 0; i < cfg.instructions().size(); i++) {
            assertNotNull(cfg.blockOfInsn(i), "insn " + i + " is not covered by any block");
        }
    }
}
