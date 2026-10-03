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

import com.aetherdecompiler.api.IRKind;
import com.aetherdecompiler.api.IRObject;

import java.util.List;

/**
 * 抽象语法树节点的<strong>公共基类</strong>。
 *
 * <p>这是内核流水线的第三层方法视图：它把命令式的字节码指令，还原为人类可读的
 * <em>表达式</em>与<em>语句</em>结构。整棵树由两大类节点构成——{@link Expr}
 * （求一个值）与 {@link Stmt}（做一件事）——它们都继承自本类。</p>
 *
 * <p>每个节点都记录它覆盖的<em>指令索引区间</em> {@code [firstInsn, lastInsn]}。
 * 这正是 Phase 4 源码映射（{@code Span}）能够在“生成文本 ↔ 字节码指令”之间
 * 建立双向跳转的锚点：只要 AST 节点还带着它的指令来源，映射就不会丢失。</p>
 *
 * <p><b>设计红线</b>：AST 中<em>绝不</em>出现任何 Java 关键字或语法记号。渲染成
 * Java、Kotlin、伪代码，是 {@code RenderPlugin} 的职责，而不是 AST 的职责。因此
 * 这里只保留与语言无关的“结构”，例如 {@code Binary(op="+")}，而不是 {@code "a + b;"}。</p>
 *
 * <p>故事类比：一份建筑结构的抽象模型——承重、通道、开口——它不说“这是砖砌的”，
 * 只说明“这里是一道墙、那里是一个门洞”。用什么材料打印出来，是打印机的事。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public abstract class AstNode implements IRObject {

    private final int firstInsn;
    private final int lastInsn;

    /**
     * @param firstInsn 含首的起始指令索引；未知时为 {@code -1}
     * @param lastInsn  含尾的结束指令索引；未知时为 {@code -1}
     */
    protected AstNode(int firstInsn, int lastInsn) {
        this.firstInsn = firstInsn;
        this.lastInsn = lastInsn;
    }

    /** @return 含首的起始指令索引 */
    public int firstInsn() {
        return firstInsn;
    }

    /** @return 含尾的结束指令索引 */
    public int lastInsn() {
        return lastInsn;
    }

    /** @return 本节点直接包含的子节点，按逻辑顺序 */
    public abstract List<AstNode> children();

    /**
     * @return 本节点的简短结构标签，例如 {@code "If"}、{@code "Binary(+)"}
     */
    public String label() {
        return getClass().getSimpleName();
    }

    @Override
    public IRKind kind() {
        return IRKind.AST;
    }

    @Override
    public String toString() {
        return label();
    }
}
