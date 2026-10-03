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
import com.aetherdecompiler.core.model.ClassModel;
import com.aetherdecompiler.core.model.MethodModel;
import com.aetherdecompiler.core.ssa.PhiNode;
import com.aetherdecompiler.core.ssa.SsaBuilder;
import com.aetherdecompiler.core.ssa.SsaForm;
import com.aetherdecompiler.core.ssa.SsaVariable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SSA 构造的单元测试：验证 phi 放置、版本化与定值-使用映射。
 *
 * @author Jerry Zhu (Zeek)
 */
class SsaBuilderTest {

    private ClassModel sample() throws Exception {
        String resource = Sample.class.getName().replace('.', '/') + ".class";
        try (var in = Sample.class.getClassLoader().getResourceAsStream(resource)) {
            return new AsmClassParser().parse(in.readAllBytes());
        }
    }

    private SsaForm ssaFor(ClassModel model, String name, String desc) {
        MethodModel method = model.findMethod(name, desc).orElseThrow();
        ControlFlowGraph cfg = new CfgBuilder().build(model, method);
        DominatorTree dom = DominatorTree.of(cfg);
        return new SsaBuilder().build(model, method, cfg, dom);
    }

    @Test
    void loopMethodHasPhisAndSingleAssignment() throws Exception {
        ClassModel model = sample();
        SsaForm ssa = ssaFor(model, "sumTo", "(I)I");
        // 循环里累加器与计数器都会被多路定值，因此必然出现 phi。
        assertTrue(ssa.phiCount() > 0, "sumTo loop should require at least one phi");
        // 每条被定义的 SSA 值都必须是“只赋值一次”的。
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (SsaVariable v : ssa.definitions().values()) {
            assertTrue(seen.add(v.name()), "SSA value " + v.name() + " defined twice");
        }
    }

    @Test
    void everyPhiOperandIsFilled() throws Exception {
        ClassModel model = sample();
        SsaForm ssa = ssaFor(model, "sumTo", "(I)I");
        for (PhiNode phi : ssa.phis()) {
            assertEquals(phi.predecessorBlocks().size(), phi.operands().length);
            for (SsaVariable operand : phi.operands()) {
                assertNotNull(operand, "phi operand left unfilled: " + phi.display());
            }
        }
    }

    @Test
    void loadsReferenceSomeDefinition() throws Exception {
        ClassModel model = sample();
        SsaForm ssa = ssaFor(model, "sumTo", "(I)I");
        assertTrue(ssa.uses().size() > 0, "a method that reads locals must record uses");
        for (java.util.List<SsaVariable> used : ssa.uses().values()) {
            for (SsaVariable v : used) {
                assertTrue(v.version() >= 0);
            }
        }
    }

    @Test
    void straightLineMethodHasNoPhis() throws Exception {
        ClassModel model = sample();
        SsaForm ssa = ssaFor(model, "square", "(I)I");
        assertEquals(0, ssa.phiCount(), "a straight-line method must not need any phi");
    }
}
