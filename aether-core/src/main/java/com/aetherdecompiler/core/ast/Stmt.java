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

import java.util.ArrayList;
import java.util.List;

/**
 * 语句节点：一切“执行一个动作”的 AST 结构——顺序、分支、循环、返回、抛出、异常处理。
 *
 * <p>控制结构恢复（structuring）的产物就在这里：把 CFG 上的基本块与边，还原成
 * {@link If}、{@link While}、{@link DoWhile}、{@link Switch}、{@link TryCatch}
 * 这样的嵌套结构。当一个 CFG 不可规约（irreducible）而无法完全结构化时，本文提供
 * {@link Label} 与保守的 {@link Block} 兜底，保证“永不丢块、永不崩溃”。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public abstract class Stmt extends AstNode {

    protected Stmt(int firstInsn, int lastInsn) {
        super(firstInsn, lastInsn);
    }

    /** 顺序块：一组顺序执行的语句。 */
    public static final class Block extends Stmt {
        private final List<Stmt> statements;

        public Block(int firstInsn, int lastInsn, List<Stmt> statements) {
            super(firstInsn, lastInsn);
            this.statements = List.copyOf(statements);
        }

        public List<Stmt> statements() {
            return statements;
        }

        @Override
        public List<AstNode> children() {
            return new ArrayList<>(statements);
        }

        @Override
        public String label() {
            return "Block(" + statements.size() + ")";
        }
    }

    /** 表达式语句：一次只求值、不求结果的运算（常用于调用）。 */
    public static final class ExprStmt extends Stmt {
        private final Expr expr;

        public ExprStmt(int insn, Expr expr) {
            super(insn, insn);
            this.expr = expr;
        }

        public Expr expr() {
            return expr;
        }

        @Override
        public List<AstNode> children() {
            return List.of(expr);
        }
    }

    /** 条件语句：{@code if (cond) then [else]}。 */
    public static final class If extends Stmt {
        private final Expr cond;
        private final Stmt thenBranch;
        private final Stmt elseBranch;

        public If(int firstInsn, int lastInsn, Expr cond, Stmt thenBranch, Stmt elseBranch) {
            super(firstInsn, lastInsn);
            this.cond = cond;
            this.thenBranch = thenBranch;
            this.elseBranch = elseBranch;
        }

        public Expr cond() {
            return cond;
        }

        public Stmt thenBranch() {
            return thenBranch;
        }

        public Stmt elseBranch() {
            return elseBranch;
        }

        @Override
        public List<AstNode> children() {
            List<AstNode> kids = new ArrayList<>();
            kids.add(cond);
            kids.add(thenBranch);
            if (elseBranch != null) {
                kids.add(elseBranch);
            }
            return kids;
        }
    }

    /** 前测试循环：{@code while (cond) body}。 */
    public static final class While extends Stmt {
        private final Expr cond;
        private final Stmt body;

        public While(int firstInsn, int lastInsn, Expr cond, Stmt body) {
            super(firstInsn, lastInsn);
            this.cond = cond;
            this.body = body;
        }

        public Expr cond() {
            return cond;
        }

        public Stmt body() {
            return body;
        }

        @Override
        public List<AstNode> children() {
            return List.of(cond, body);
        }
    }

    /** 后测试循环：{@code do body while (cond)}。 */
    public static final class DoWhile extends Stmt {
        private final Stmt body;
        private final Expr cond;

        public DoWhile(int firstInsn, int lastInsn, Stmt body, Expr cond) {
            super(firstInsn, lastInsn);
            this.body = body;
            this.cond = cond;
        }

        public Stmt body() {
            return body;
        }

        public Expr cond() {
            return cond;
        }

        @Override
        public List<AstNode> children() {
            return List.of(body, cond);
        }
    }

    /** switch 的一个分支。 */
    public static final class SwitchCase {
        private final int key;
        private final Stmt body;

        public SwitchCase(int key, Stmt body) {
            this.key = key;
            this.body = body;
        }

        public int key() {
            return key;
        }

        public Stmt body() {
            return body;
        }
    }

    /** 多路分支：{@code switch (selector) { ... }}。 */
    public static final class Switch extends Stmt {
        private final Expr selector;
        private final List<SwitchCase> cases;
        private final Stmt defaultBody;

        public Switch(int firstInsn, int lastInsn, Expr selector,
                      List<SwitchCase> cases, Stmt defaultBody) {
            super(firstInsn, lastInsn);
            this.selector = selector;
            this.cases = List.copyOf(cases);
            this.defaultBody = defaultBody;
        }

        public Expr selector() {
            return selector;
        }

        public List<SwitchCase> cases() {
            return cases;
        }

        public Stmt defaultBody() {
            return defaultBody;
        }

        @Override
        public List<AstNode> children() {
            List<AstNode> kids = new ArrayList<>();
            kids.add(selector);
            for (SwitchCase c : cases) {
                kids.add(c.body());
            }
            if (defaultBody != null) {
                kids.add(defaultBody);
            }
            return kids;
        }
    }

    /** 返回语句：{@code return [value]}。 */
    public static final class Return extends Stmt {
        private final Expr value;

        public Return(int insn, Expr value) {
            super(insn, insn);
            this.value = value;
        }

        public Expr value() {
            return value;
        }

        @Override
        public List<AstNode> children() {
            return value == null ? List.of() : List.of(value);
        }
    }

    /** 抛出语句：{@code throw value}。 */
    public static final class Throw extends Stmt {
        private final Expr value;

        public Throw(int insn, Expr value) {
            super(insn, insn);
            this.value = value;
        }

        public Expr value() {
            return value;
        }

        @Override
        public List<AstNode> children() {
            return value == null ? List.of() : List.of(value);
        }
    }

    /** 跳转标签：不可结构化区域中保底使用的锚点。 */
    public static final class Label extends Stmt {
        private final String name;

        public Label(int insn, String name) {
            super(insn, insn);
            this.name = name;
        }

        public String name() {
            return name;
        }

        @Override
        public java.util.List<AstNode> children() {
            return java.util.List.of();
        }

        @Override
        public String label() {
            return "Label(" + name + ")";
        }
    }

    /** 无条件跳转：仅在不可结构化区域中作为保底出现。 */
    public static final class Goto extends Stmt {
        private final String target;

        public Goto(int insn, String target) {
            super(insn, insn);
            this.target = target;
        }

        public String target() {
            return target;
        }

        @Override
        public java.util.List<AstNode> children() {
            return java.util.List.of();
        }
    }

    /** 空语句占位。 */
    public static final class Nop extends Stmt {
        public Nop(int insn) {
            super(insn, insn);
        }

        @Override
        public java.util.List<AstNode> children() {
            return java.util.List.of();
        }
    }

    /** 一个 catch 子句。 */
    public static final class CatchClause {
        private final String type;
        private final int varSlot;
        private final Stmt body;

        /** 兼容旧签名的构造器：无变量槽（渲染时回退为 {@code e}）。 */
        public CatchClause(String type, Stmt body) {
            this(type, -1, body);
        }

        /**
         * @param type    被捕获的内部类型名；捕获所有时为 {@code null}
         * @param varSlot 捕获变量所在局部槽位；未知为 {@code -1}
         * @param body    处理器语句体
         */
        public CatchClause(String type, int varSlot, Stmt body) {
            this.type = type;
            this.varSlot = varSlot;
            this.body = body;
        }

        public String type() {
            return type;
        }

        /** @return 捕获变量所在局部槽位；未知为 {@code -1} */
        public int varSlot() {
            return varSlot;
        }

        public Stmt body() {
            return body;
        }
    }

    /** 异常处理：{@code try body catch (...) { ... }}。 */
    public static final class TryCatch extends Stmt {
        private final Stmt body;
        private final List<CatchClause> catches;

        public TryCatch(int firstInsn, int lastInsn, Stmt body, List<CatchClause> catches) {
            super(firstInsn, lastInsn);
            this.body = body;
            this.catches = List.copyOf(catches);
        }

        public Stmt body() {
            return body;
        }

        public List<CatchClause> catches() {
            return catches;
        }

        @Override
        public List<AstNode> children() {
            List<AstNode> kids = new ArrayList<>();
            kids.add(body);
            for (CatchClause c : catches) {
                kids.add(c.body());
            }
            return kids;
        }
    }

    /**
     * 同步块：{@code synchronized (lock) { body }}。
     *
     * <p>字节码里它被 javac 展开为 {@code monitorenter} + 受异常保护的
     * {@code monitorexit}。这里把它还原为源码里的同步语句，而不是泄漏底层监视器
     * 指令。</p>
     */
    public static final class Synchronized extends Stmt {
        private final Expr lock;
        private final Stmt body;

        /**
         * @param firstInsn 起始指令索引
         * @param lastInsn  结束指令索引
         * @param lock      锁表达式（{@code synchronized} 的括号内容）
         * @param body      同步体
         */
        public Synchronized(int firstInsn, int lastInsn, Expr lock, Stmt body) {
            super(firstInsn, lastInsn);
            this.lock = lock;
            this.body = body;
        }

        /** @return 锁表达式 */
        public Expr lock() {
            return lock;
        }

        /** @return 同步体 */
        public Stmt body() {
            return body;
        }

        @Override
        public List<AstNode> children() {
            List<AstNode> kids = new ArrayList<>();
            if (lock != null) {
                kids.add(lock);
            }
            kids.add(body);
            return kids;
        }
    }
}
