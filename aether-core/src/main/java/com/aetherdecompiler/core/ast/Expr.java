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
 * 表达式节点：一切“求出一个值”的 AST 结构。
 *
 * <p>字节码是<em>基于栈</em>的：运算在操作数栈上进行。表达式重建的过程，本质上是
 * 一次“栈模拟”——把 {@code iload} 压成 {@code Local}，把 {@code iadd} 弹两压一变成
 * {@code Binary("+")}，把 {@code invokestatic} 的各类弹出物变成 {@code Call} 的实参。
 * 这座 {@code Expr} 层级，就是那次模拟的产出结果。</p>
 *
 * <p>具体子类见下（均以嵌套静态类给出，便于与基类同文件阅读）。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public abstract class Expr extends AstNode {

    protected Expr(int firstInsn, int lastInsn) {
        super(firstInsn, lastInsn);
    }

    /** 取内部名的简单名：{@code java/lang/Object} → {@code Object}。 */
    protected static String simpleName(String internal) {
        if (internal == null) {
            return "?";
        }
        String s = internal;
        int semi = s.indexOf(';');
        if (semi >= 0) {
            s = s.substring(0, semi);
        }
        int slash = s.lastIndexOf('/');
        return slash >= 0 ? s.substring(slash + 1) : s;
    }

    /** @return 本表达式的结构渲染，用于调试与教学展示（非最终源码） */
    public abstract String render();

    @Override
    public List<AstNode> children() {
        return List.of();
    }

    @Override
    public String label() {
        return getClass().getSimpleName();
    }

    /** 常量：来自 {@code iconst_*}/{@code bipush}/{@code ldc} 等。 */
    public static final class Const extends Expr {
        private final String value;

        public Const(int insn, String value) {
            super(insn, insn);
            this.value = value;
        }

        public String value() {
            return value;
        }

        @Override
        public String render() {
            return value;
        }
    }

    /** 局部变量读取：来自 {@code *load}，{@code name} 可为 SSA 名如 {@code v2_3}。 */
    public static final class Local extends Expr {
        private final int slot;
        private final String name;

        public Local(int insn, int slot, String name) {
            super(insn, insn);
            this.slot = slot;
            this.name = name;
        }

        public int slot() {
            return slot;
        }

        public String name() {
            return name;
        }

        @Override
        public String render() {
            return name == null ? ("slot" + slot) : name;
        }
    }

    /** 二元运算：{@code +}、{@code -}、{@code *}、{@code /}、{@code %}、移位、位运算等。 */
    public static final class Binary extends Expr {
        private final String op;
        private final Expr left;
        private final Expr right;

        public Binary(int insn, String op, Expr left, Expr right) {
            super(insn, insn);
            this.op = op;
            this.left = left;
            this.right = right;
        }

        public String op() {
            return op;
        }

        public Expr left() {
            return left;
        }

        public Expr right() {
            return right;
        }

        @Override
        public List<AstNode> children() {
            List<AstNode> kids = new ArrayList<>();
            kids.add(left);
            kids.add(right);
            return kids;
        }

        @Override
        public String render() {
            return "(" + left.render() + " " + op + " " + right.render() + ")";
        }

        @Override
        public String label() {
            return "Binary(" + op + ")";
        }
    }

    /** 一元运算：取负、位取反、数值转换（{@code i2l} 等）。 */
    public static final class Unary extends Expr {
        private final String op;
        private final Expr operand;

        public Unary(int insn, String op, Expr operand) {
            super(insn, insn);
            this.op = op;
            this.operand = operand;
        }

        public String op() {
            return op;
        }

        public Expr operand() {
            return operand;
        }

        @Override
        public List<AstNode> children() {
            return List.of(operand);
        }

        @Override
        public String render() {
            return op + operand.render();
        }

        @Override
        public String label() {
            return "Unary(" + op + ")";
        }
    }

    /** 赋值：来自 {@code *store}，{@code target} 通常是一个 {@link Local}。 */
    public static final class Assign extends Expr {
        private final Expr target;
        private final Expr value;

        public Assign(int insn, Expr target, Expr value) {
            super(insn, insn);
            this.target = target;
            this.value = value;
        }

        public Expr target() {
            return target;
        }

        public Expr value() {
            return value;
        }

        @Override
        public List<AstNode> children() {
            return List.of(target, value);
        }

        @Override
        public String render() {
            return target.render() + " = " + value.render();
        }
    }

    /** 方法调用：{@code invokevirtual/static/special/interface}。 */
    public static final class Call extends Expr {
        private final Expr receiver;
        private final String owner;
        private final String name;
        private final String descriptor;
        private final List<Expr> args;

        public Call(int insn, Expr receiver, String owner, String name,
                    String descriptor, List<Expr> args) {
            super(insn, insn);
            this.receiver = receiver;
            this.owner = owner;
            this.name = name;
            this.descriptor = descriptor;
            this.args = List.copyOf(args);
        }

        public Expr receiver() {
            return receiver;
        }

        public String owner() {
            return owner;
        }

        public String name() {
            return name;
        }

        public String descriptor() {
            return descriptor;
        }

        public List<Expr> args() {
            return args;
        }

        @Override
        public List<AstNode> children() {
            List<AstNode> kids = new ArrayList<>();
            if (receiver != null) {
                kids.add(receiver);
            }
            kids.addAll(args);
            return kids;
        }

        @Override
        public String render() {
            StringBuilder sb = new StringBuilder();
            if ("<init>".equals(name) && receiver instanceof Local l && l.slot() == 0) {
                // 实例构造里对 this 的 <init> 调用即父类构造调用，惯用 super(...)。
                sb.append("super");
            } else {
                if (receiver != null) {
                    sb.append(receiver.render()).append('.');
                } else if (owner != null) {
                    sb.append(owner.replace('/', '.')).append('.');
                }
                sb.append(name);
            }
            sb.append('(');
            for (int i = 0; i < args.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(args.get(i).render());
            }
            return sb.append(')').toString();
        }
    }

    /** 对象创建：{@code new} + {@code <init>}。 */
    public static final class New extends Expr {
        private final String type;
        private final List<Expr> args;

        public New(int insn, String type, List<Expr> args) {
            super(insn, insn);
            this.type = type;
            this.args = List.copyOf(args);
        }

        public String type() {
            return type;
        }

        public List<Expr> args() {
            return args;
        }

        @Override
        public List<AstNode> children() {
            return new ArrayList<>(args);
        }

        @Override
        public String render() {
            StringBuilder sb = new StringBuilder("new ").append(simpleName(type)).append('(');
            for (int i = 0; i < args.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(args.get(i).render());
            }
            return sb.append(')').toString();
        }
    }

    /** 强制类型转换：{@code checkcast}。 */
    public static final class Cast extends Expr {
        private final String type;
        private final Expr operand;

        public Cast(int insn, String type, Expr operand) {
            super(insn, insn);
            this.type = type;
            this.operand = operand;
        }

        public String type() {
            return type;
        }

        public Expr operand() {
            return operand;
        }

        @Override
        public List<AstNode> children() {
            return List.of(operand);
        }

        @Override
        public String render() {
            return "(" + (type == null ? "?" : type.replace('/', '.')) + ") " + operand.render();
        }
    }

    /** 数组读取：{@code *aload} 家族。 */
    public static final class ArrayLoad extends Expr {
        private final Expr array;
        private final Expr index;

        public ArrayLoad(int insn, Expr array, Expr index) {
            super(insn, insn);
            this.array = array;
            this.index = index;
        }

        public Expr array() {
            return array;
        }

        public Expr index() {
            return index;
        }

        @Override
        public List<AstNode> children() {
            return List.of(array, index);
        }

        @Override
        public String render() {
            return array.render() + "[" + index.render() + "]";
        }
    }

    /** 数组写入：{@code *astore} 家族。 */
    public static final class ArrayStore extends Expr {
        private final Expr array;
        private final Expr index;
        private final Expr value;

        public ArrayStore(int insn, Expr array, Expr index, Expr value) {
            super(insn, insn);
            this.array = array;
            this.index = index;
            this.value = value;
        }

        public Expr array() {
            return array;
        }

        public Expr index() {
            return index;
        }

        public Expr value() {
            return value;
        }

        @Override
        public List<AstNode> children() {
            return List.of(array, index, value);
        }

        @Override
        public String render() {
            return array.render() + "[" + index.render() + "] = " + value.render();
        }
    }

    /** {@code instanceof} 判定。 */
    public static final class InstanceOf extends Expr {
        private final Expr operand;
        private final String type;

        public InstanceOf(int insn, Expr operand, String type) {
            super(insn, insn);
            this.operand = operand;
            this.type = type;
        }

        public Expr operand() {
            return operand;
        }

        public String type() {
            return type;
        }

        @Override
        public List<AstNode> children() {
            return List.of(operand);
        }

        @Override
        public String render() {
            return operand.render() + " instanceof " + (type == null ? "?" : type.replace('/', '.'));
        }
    }

    /**
     * 分支条件：由 {@code if*} 指令重建的比较表达式。
     *
     * <p>例如 {@code if_icmpgt} 重建为 {@code Cond(">")}。若该比较只涉及单个操作数
     * （{@code ifeq}/{@code ifnull} 等），则 {@code right} 为 {@code null}，语义上是
     * “与零/null 比较”。</p>
     */
    public static final class Cond extends Expr {
        private final String op;
        private final Expr left;
        private final Expr right;
        private final boolean negated;

        public Cond(int insn, String op, Expr left, Expr right, boolean negated) {
            super(insn, insn);
            this.op = op;
            this.left = left;
            this.right = right;
            this.negated = negated;
        }

        public String op() {
            return op;
        }

        public Expr left() {
            return left;
        }

        public Expr right() {
            return right;
        }

        public boolean negated() {
            return negated;
        }

        @Override
        public List<AstNode> children() {
            List<AstNode> kids = new ArrayList<>();
            if (left != null) {
                kids.add(left);
            }
            if (right != null) {
                kids.add(right);
            }
            return kids;
        }

        @Override
        public String render() {
            String core = right == null
                    ? (left == null ? "?" : left.render()) + " " + op
                    : (left == null ? "?" : left.render()) + " " + op + " " + (right == null ? "?" : right.render());
            return negated ? "!(" + core + ")" : core;
        }

        @Override
        public String label() {
            return "Cond(" + op + ")";
        }
    }

    /** 自增：{@code iinc}。 */
    public static final class Incr extends Expr {
        private final int slot;
        private final int amount;

        public Incr(int insn, int slot, int amount) {
            super(insn, insn);
            this.slot = slot;
            this.amount = amount;
        }

        public int slot() {
            return slot;
        }

        public int amount() {
            return amount;
        }

        @Override
        public String render() {
            return "v" + slot + " += " + amount;
        }
    }

    /**
     * 字段访问：{@code getfield}/{@code getstatic} 读取，或作为 {@code putfield}/{@code putstatic} 的赋值目标。
     *
     * <p>{@code receiver} 为 {@code null} 表示静态字段；实例字段则渲染为 {@code recv.name}。</p>
     */
    public static final class FieldAccess extends Expr {
        private final Expr receiver;
        private final String owner;
        private final String name;

        public FieldAccess(int insn, Expr receiver, String owner, String name) {
            super(insn, insn);
            this.receiver = receiver;
            this.owner = owner;
            this.name = name;
        }

        public Expr receiver() {
            return receiver;
        }

        public String owner() {
            return owner;
        }

        public String name() {
            return name;
        }

        @Override
        public List<AstNode> children() {
            return receiver == null ? List.of() : List.of(receiver);
        }

        @Override
        public String render() {
            if (receiver != null) {
                return receiver.render() + "." + name;
            }
            return (owner == null || owner.isEmpty() ? "" : simpleName(owner) + ".") + name;
        }
    }

    /** 数组长度：{@code arraylength} → {@code arr.length}。 */
    public static final class ArrayLength extends Expr {
        private final Expr array;

        public ArrayLength(int insn, Expr array) {
            super(insn, insn);
            this.array = array;
        }

        public Expr array() {
            return array;
        }

        @Override
        public List<AstNode> children() {
            return List.of(array);
        }

        @Override
        public String render() {
            return array.render() + ".length";
        }
    }

    /** 数组创建：{@code newarray}/{@code anewarray} → {@code new T[size]}。 */
    public static final class NewArray extends Expr {
        private final String type;
        private final Expr size;

        public NewArray(int insn, String type, Expr size) {
            super(insn, insn);
            this.type = type;
            this.size = size;
        }

        public String type() {
            return type;
        }

        public Expr size() {
            return size;
        }

        @Override
        public List<AstNode> children() {
            return size == null ? List.of() : List.of(size);
        }

        @Override
        public String render() {
            String t = type == null || type.isEmpty() ? "Object" : type.replace('/', '.');
            return "new " + t + "[" + (size == null ? "" : size.render()) + "]";
        }
    }

    /**
     * 数组字面量初始化：{@code new int[]{1,2,3}} 编译后的 {@code newarray;dup;idx;val;astore...}
     * 序列被折叠为可读的数组初始化式。
     */
    public static final class ArrayInit extends Expr {
        private final String type;
        private final List<Expr> elements;

        public ArrayInit(int insn, String type, List<Expr> elements) {
            super(insn, insn);
            this.type = type;
            this.elements = elements;
        }

        public String type() {
            return type;
        }

        public List<Expr> elements() {
            return elements;
        }

        @Override
        public List<AstNode> children() {
            return new ArrayList<>(elements);
        }

        @Override
        public String render() {
            String t = type == null || type.isEmpty() ? "Object" : type.replace('/', '.');
            StringBuilder sb = new StringBuilder("new ").append(t).append("[]{");
            for (int i = 0; i < elements.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(elements.get(i).render());
            }
            return sb.append('}').toString();
        }

        @Override
        public String label() {
            return "ArrayInit";
        }
    }

    /** 多维数组创建：{@code multianewarray} → {@code new T[s0][s1]...}。 */
    public static final class NewMultiArray extends Expr {
        private final String type;
        private final List<Expr> sizes;

        public NewMultiArray(int insn, String type, List<Expr> sizes) {
            super(insn, insn);
            this.type = type;
            this.sizes = sizes;
        }

        public String type() {
            return type;
        }

        public List<Expr> sizes() {
            return sizes;
        }

        @Override
        public List<AstNode> children() {
            return new ArrayList<>(sizes);
        }

        @Override
        public String render() {
            StringBuilder sb = new StringBuilder("new ").append(type == null ? "Object" : type);
            for (Expr s : sizes) {
                sb.append('[').append(s == null ? "" : s.render()).append(']');
            }
            return sb.toString();
        }

        @Override
        public String label() {
            return "NewMultiArray";
        }
    }

    /**
     * 兜底表达式：当某条指令无法被精确重建时，保底记录它的助记符与操作数，
     * 使 AST 始终完整、永不丢指令（便于源码映射与教学观察）。
     */
    public static final class Opaque extends Expr {
        private final String text;

        public Opaque(int insn, String text) {
            super(insn, insn);
            this.text = text;
        }

        public String text() {
            return text;
        }

        @Override
        public String render() {
            // 未精确重建的指令不以裸 '?' 污染源码，而以注释占位，保持可编译与可追溯。
            return "/* unresolved: " + text + " */";
        }
    }
}
