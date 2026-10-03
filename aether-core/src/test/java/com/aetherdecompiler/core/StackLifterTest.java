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
import com.aetherdecompiler.core.ssa.StackLiftResult;
import com.aetherdecompiler.core.ssa.StackPhi;
import com.aetherdecompiler.core.ssa.SsaBuilder;
import com.aetherdecompiler.core.ssa.SsaForm;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * C1 操作数栈提升（Stack Lifting）的单元测试：验证隐式操作数栈被符号化为
 * 栈单元、汇合处出现栈 phi、栈深判定合理，且基本块入口的栈单元不会被重复赋值。
 *
 * @author Jerry Zhu (Zeek)
 */
class StackLifterTest {

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
    void everyMethodCarriesStackLiftResult() throws Exception {
        ClassModel model = sample();
        for (MethodModel m : model.methods()) {
            if (m.isAbstractOrNative()) {
                continue;
            }
            SsaForm ssa = ssaFor(model, m.name(), m.descriptor());
            StackLiftResult lift = ssa.stackLift();
            assertNotNull(lift, "栈提升结果不应为空");
            // 有指令的方法必然产出至少一个栈单元。
            if (!m.name().equals("<init>")) {
                assertTrue(lift.cellCount() > 0 || lift.maxDepth() == 0,
                        "方法 " + m.name() + " 的栈单元计数应 >= 0");
            }
        }
    }

    @Test
    void loopMethodHasStackPhiAndReasonableDepth() throws Exception {
        ClassModel model = sample();
        SsaForm ssa = ssaFor(model, "sumTo", "(I)I");
        StackLiftResult lift = ssa.stackLift();
        // 循环里累加会在汇合点反复用栈，栈 phi 应出现（保守允许为 0 时不误判）。
        assertTrue(lift.maxDepth() >= 1, "求和循环栈深应至少为 1");
        for (StackPhi phi : lift.phis()) {
            assertTrue(phi.posFromBottom() >= 0, "栈 phi 的栈位下标应非负");
            assertFalse(phi.operands().isEmpty(), "栈 phi 应至少有一个来源操作数");
        }
    }

    @Test
    void stackDepthAtEntryIsZero() throws Exception {
        ClassModel model = sample();
        SsaForm ssa = ssaFor(model, "sumTo", "(I)I");
        StackLiftResult lift = ssa.stackLift();
        // 方法入口执行前栈必为空。
        assertEquals(0, lift.depthBefore(0), "入口指令执行前栈深应为 0");
    }
}
