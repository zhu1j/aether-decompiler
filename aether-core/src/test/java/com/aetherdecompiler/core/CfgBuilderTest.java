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
 * CFG 构建与支配树的单元测试。
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
        // 入口块必须存在并被标记。
        assertTrue(cfg.blocks().get(0).isEntry());
        // 每个后继 id 都必须是合法的块 id（图是闭合的）。
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
