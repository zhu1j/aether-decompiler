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
import com.aetherdecompiler.core.model.LocalVariable;
import com.aetherdecompiler.core.model.MethodModel;

import java.util.LinkedHashMap;
import java.util.Map;
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
        ControlStructurer structurer =
                new ControlStructurer(cfg, dom, varNamer(method), method.tryCatchEntries());
        Stmt body = structurer.structure();
        return new MethodBody(owner.name(), method.name(), method.descriptor(),
                method.access(), body, structurer.isIrreducible(), localNames(method));
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
        ControlStructurer structurer =
                new ControlStructurer(cfg, dom, varNamer(method), method.tryCatchEntries());
        Stmt body = structurer.structure();
        return new MethodBody(owner.name(), method.name(), method.descriptor(),
                method.access(), body, structurer.isIrreducible(), localNames(method));
    }

    /**
     * 按“槽位 → Java 显示名”的规则构造命名器：优先采用 LocalVariableTable 提供的
     * 真实变量名（重名时以槽位为后缀去重），否则回退为 {@code this} / {@code vN}。
     *
     * <p>实例方法的槽 0 恒为 {@code this} 引用。当调试信息缺失（例如被混淆剥离）时，
     * 命名自然退化为槽位名，保持输出稳定可读。</p>
     */
    private static IntFunction<String> varNamer(MethodModel method) {
        boolean isStatic = AccessFlags.isStatic(method.access());
        Map<Integer, String> names = localNames(method);
        return slot -> {
            if (slot == 0 && !isStatic) {
                return "this";
            }
            String name = names.get(slot);
            return name != null ? name : ("v" + slot);
        };
    }

    /**
     * 把 LocalVariableTable 折叠为“槽位 → 去重后的显示名”。同一槽位可能存在多条
     * 生命周期互不重叠的记录（不同时间扮演不同变量）；这里保留每个槽位首次出现的
     * 名字，并对全表重名做槽位后缀去重，避免生成非法标识符冲突。
     */
    private static Map<Integer, String> localNames(MethodModel method) {
        Map<Integer, String> out = new LinkedHashMap<>();
        if (method.localVariables().isEmpty()) {
            return out;
        }
        java.util.Set<String> used = new java.util.HashSet<>();
        for (LocalVariable lv : method.localVariables()) {
            int slot = lv.index();
            if (out.containsKey(slot)) {
                continue;
            }
            String name = sanitize(lv.name());
            if (name.isEmpty() || !used.add(name)) {
                name = name.isEmpty() ? ("v" + slot) : (name + "_" + slot);
                used.add(name);
            }
            out.put(slot, name);
        }
        return out;
    }

    /** 把不合法的 Java 标识符字符替换为下划线，并对纯数字/关键字做保守处理。 */
    private static String sanitize(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            boolean ok = i == 0
                    ? (Character.isJavaIdentifierStart(c))
                    : (Character.isJavaIdentifierPart(c));
            sb.append(ok ? c : '_');
        }
        return sb.toString();
    }
}
