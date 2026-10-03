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
package com.aetherdecompiler.core.ast;

import com.aetherdecompiler.core.cfg.CfgBuilder;
import com.aetherdecompiler.core.cfg.ControlFlowGraph;
import com.aetherdecompiler.core.cfg.DominatorTree;
import com.aetherdecompiler.core.model.AccessFlags;
import com.aetherdecompiler.core.model.ClassModel;
import com.aetherdecompiler.core.model.MethodModel;

import java.util.function.IntFunction;

/**
 * AST 编排器：把“方法模型 + CFG + 支配树”推进到一棵可读的语句树。
 *
 * <p>它是流水线第三层的入口，职责单一：构建 CFG/支配树、驱动 {@link ControlStructurer}
 * 完成控制结构恢复、装配 {@link MethodBody}。它<strong>不做</strong>任何语言相关的文本
 * 生成——那是渲染插件的事。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class AstBuilder {

    private final CfgBuilder cfgBuilder = new CfgBuilder();

    /**
     * 为单个方法构建 AST。
     *
     * @param owner  所属类
     * @param method 方法模型
     * @return 方法体 AST
     */
    public MethodBody build(ClassModel owner, MethodModel method) {
        ControlFlowGraph cfg = cfgBuilder.build(owner, method);
        DominatorTree dom = DominatorTree.of(cfg);
        ControlStructurer structurer = new ControlStructurer(cfg, dom, varNamer(method));
        Stmt body = structurer.structure();
        return new MethodBody(owner.name(), method.name(), method.descriptor(),
                method.access(), body, structurer.isIrreducible());
    }

    /**
     * 基于已算好的 CFG/支配树构建 AST（供流水线复用，避免重复计算）。
     *
     * @param owner  所属类
     * @param method 方法模型
     * @param cfg    已构建的控制流图
     * @param dom    已构建的支配树
     * @return 方法体 AST
     */
    public MethodBody build(ClassModel owner, MethodModel method,
                            ControlFlowGraph cfg, DominatorTree dom) {
        ControlStructurer structurer = new ControlStructurer(cfg, dom, varNamer(method));
        Stmt body = structurer.structure();
        return new MethodBody(owner.name(), method.name(), method.descriptor(),
                method.access(), body, structurer.isIrreducible());
    }

    /**
     * 按“槽位 → Java 显示名”的规则构造命名器：
     * 实例方法的槽 0 是 {@code this} 引用；其余槽位（含形参）统一命名为 {@code vN}，
     * 与渲染器生成的形参名保持一致，从而让方法签名与体中的引用互相对应。
     *
     * <p>将来接入 LocalVariableTable / SSA 时，只需替换本命名器即可恢复真实变量名。</p>
     */
    private static IntFunction<String> varNamer(MethodModel method) {
        boolean isStatic = AccessFlags.isStatic(method.access());
        return slot -> (slot == 0 && !isStatic) ? "this" : ("v" + slot);
    }
}
