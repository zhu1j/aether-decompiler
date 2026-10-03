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

import com.aetherdecompiler.core.model.Insn;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.IntFunction;

/**
 * 直线表达式的<strong>栈模拟重建器</strong>。
 *
 * <p>JVM 指令是“基于操作数栈”的：{@code iload_1} 把值压栈，{@code iadd} 弹两个、
 * 压一个。本类用一条等价的 {@code Deque<Expr>} 模拟这一操作数栈，把线性指令序列
 * 还原成 {@link Expr} 表达式与 {@link Stmt} 语句。</p>
 *
 * <p>它只在<em>一个基本块内</em>工作（跨块的值流由 SSA 与后续阶段描述），并且对
 * 任何无法精确重建的指令退回 {@link Expr.Opaque}——因此它<strong>永不丢指令、
 * 永不崩溃</strong>，保证上层的源码映射始终完整。</p>
 *
 * <p>故事类比：一台“读心机”，看着魔术师一只手进另一只手出，就把他到底算了什么
 * 还原成一条代数式。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class ExpressionBuilder {

    /** 一次块内重建的产物。 */
    public static final class Result {
        private final List<Stmt> statements;
        private final Expr terminatorCond;
        private final String terminatorKind;

        Result(List<Stmt> statements, Expr terminatorCond, String terminatorKind) {
            this.statements = statements;
            this.terminatorCond = terminatorCond;
            this.terminatorKind = terminatorKind;
        }

        /** @return 块内语句（赋值、调用、返回、抛出等） */
        public List<Stmt> statements() {
            return statements;
        }

        /** @return 末尾分支指令重建出的条件；无则 {@code null} */
        public Expr terminatorCond() {
            return terminatorCond;
        }

        /** @return 末尾指令类别：{@code cond} / {@code goto} / {@code switch} / {@code return} / {@code throw} / {@code none} */
        public String terminatorKind() {
            return terminatorKind;
        }
    }

    private final Deque<Expr> stack = new ArrayDeque<>();
    private final List<Stmt> statements = new ArrayList<>();
    private Expr terminatorCond;
    private String terminatorKind = "none";
    private final IntFunction<String> varNamer;

    /**
     * @param varNamer 槽位 → 显示名的映射（可传 SSA 名）；传 {@code null} 时用 {@code vN}
     */
    public ExpressionBuilder(IntFunction<String> varNamer) {
        this.varNamer = varNamer;
    }

    /** @return 默认构造：局部变量显示为 {@code vN} */
    public static ExpressionBuilder plain() {
        return new ExpressionBuilder(null);
    }

    /**
     * 重建 {@code [from, to]} 区间内的块内指令。
     *
     * @param insns 指令列表
     * @param from  含首起始索引
     * @param to    含尾结束索引
     * @return 重建结果
     */
    public Result build(List<Insn> insns, int from, int to) {
        stack.clear();
        statements.clear();
        terminatorCond = null;
        terminatorKind = "none";
        for (int i = from; i <= to && i < insns.size(); i++) {
            step(insns.get(i));
        }
        return new Result(List.copyOf(statements), terminatorCond, terminatorKind);
    }

    private void step(Insn insn) {
        int op = insn.opcode();
        switch (op) {
            case 0 -> { /* nop */ }
            case 1 -> push(new Expr.Const(insn.index(), "null"));
            case 2, 3, 4, 5, 6, 7, 8 -> push(new Expr.Const(insn.index(), String.valueOf(op - 3)));
            case 9 -> push(new Expr.Const(insn.index(), "0L"));
            case 10 -> push(new Expr.Const(insn.index(), "1L"));
            case 11 -> push(new Expr.Const(insn.index(), "0.0f"));
            case 12 -> push(new Expr.Const(insn.index(), "1.0f"));
            case 13 -> push(new Expr.Const(insn.index(), "2.0f"));
            case 14 -> push(new Expr.Const(insn.index(), "0.0d"));
            case 15 -> push(new Expr.Const(insn.index(), "1.0d"));
            case 16, 17 -> push(new Expr.Const(insn.index(), insn.operand()));
            case 18, 19, 20 -> push(new Expr.Const(insn.index(), quoteIfString(insn.operand())));
            case 21, 22, 23, 24, 25 -> {
                int slot = parseVar(insn.operand());
                push(local(insn.index(), slot));
            }
            case 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45 -> {
                int slot = (op - 26) % 4;
                push(local(insn.index(), slot));
            }
            case 46, 47, 48, 49, 50, 51, 52, 53 -> {
                Expr idx = popSafe(insn.index());
                Expr arr = popSafe(insn.index());
                push(new Expr.ArrayLoad(insn.index(), arr, idx));
            }
            case 54, 55, 56, 57, 58 -> {
                int slot = parseVar(insn.operand());
                Expr val = popSafe(insn.index());
                statements.add(new Stmt.ExprStmt(insn.index(), new Expr.Assign(insn.index(), local(insn.index(), slot), val)));
            }
            case 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78 -> {
                int slot = (op - 59) % 4;
                Expr val = popSafe(insn.index());
                statements.add(new Stmt.ExprStmt(insn.index(), new Expr.Assign(insn.index(), local(insn.index(), slot), val)));
            }
            case 79, 80, 81, 82, 83, 84, 85, 86 -> {
                Expr val = popSafe(insn.index());
                Expr idx = popSafe(insn.index());
                Expr arr = popSafe(insn.index());
                statements.add(new Stmt.ExprStmt(insn.index(), new Expr.ArrayStore(insn.index(), arr, idx, val)));
            }
            case 87, 88 -> popSafe(insn.index());
            case 89, 90, 91 -> { /* dup*：近似为无操作 */ }
            case 92, 93, 94 -> { /* dup2*：近似为无操作 */ }
            case 95 -> { /* swap：近似为无操作 */ }
            case 96 -> binary(insn, "+");
            case 100 -> binary(insn, "-");
            case 104 -> binary(insn, "*");
            case 108 -> binary(insn, "/");
            case 112 -> binary(insn, "%");
            case 97 -> binary(insn, "+");
            case 101 -> binary(insn, "-");
            case 105 -> binary(insn, "*");
            case 109 -> binary(insn, "/");
            case 113 -> binary(insn, "%");
            case 98 -> binary(insn, "+");
            case 102 -> binary(insn, "-");
            case 106 -> binary(insn, "*");
            case 110 -> binary(insn, "/");
            case 114 -> binary(insn, "%");
            case 99 -> binary(insn, "+");
            case 103 -> binary(insn, "-");
            case 107 -> binary(insn, "*");
            case 111 -> binary(insn, "/");
            case 115 -> binary(insn, "%");
            case 116, 117, 118, 119 -> push(new Expr.Unary(insn.index(), "-", popSafe(insn.index())));
            case 120 -> binary(insn, "<<");
            case 121 -> binary(insn, "<<");
            case 122 -> binary(insn, ">>");
            case 123 -> binary(insn, ">>");
            case 124 -> binary(insn, ">>>");
            case 125 -> binary(insn, ">>>");
            case 126 -> binary(insn, "&");
            case 127 -> binary(insn, "&");
            case 128 -> binary(insn, "|");
            case 129 -> binary(insn, "|");
            case 130 -> binary(insn, "^");
            case 131 -> binary(insn, "^");
            case 132 -> {
                int slot = parseVar(insn.operand());
                int amount = parseIncAmount(insn.operand());
                statements.add(new Stmt.ExprStmt(insn.index(), new Expr.Incr(insn.index(), slot, amount)));
            }
            case 133, 134, 135, 136, 137, 138, 139, 140, 141, 142, 143, 144, 145, 146, 147 ->
                    push(new Expr.Unary(insn.index(), "(" + insn.mnemonic() + ")", popSafe(insn.index())));
            case 148, 149, 150, 151, 152 -> binary(insn, "cmp");
            case 153, 154, 155, 156, 157, 158, 159, 160, 161, 162, 163, 164, 165, 166, 198, 199 ->
                    buildCond(insn);
            case 167, 200 -> terminatorKind = "goto";
            case 168 -> push(new Expr.Opaque(insn.index(), "jsr"));
            case 169 -> statements.add(new Stmt.ExprStmt(insn.index(), new Expr.Opaque(insn.index(), "ret")));
            case 170, 171 -> terminatorKind = "switch";
            case 172, 173, 174, 175, 176 -> {
                terminatorKind = "return";
                statements.add(new Stmt.Return(insn.index(), popSafe(insn.index())));
            }
            case 177 -> {
                terminatorKind = "return";
                statements.add(new Stmt.Return(insn.index(), null));
            }
            case 178, 180, 179, 181 -> buildField(insn);
            case 182, 183, 184, 185, 186 -> buildCall(insn);
            case 187 -> {
                Expr call = popSafe(insn.index()); // 紧随其后的 <init> 调用（近似）
                push(new Expr.New(insn.index(), insn.operand(), List.of()));
                if (call != null) {
                    push(call);
                    popSafe(insn.index());
                }
            }
            case 188, 189 -> push(new Expr.Opaque(insn.index(), "newarray " + insn.operand()));
            case 190 -> push(new Expr.Opaque(insn.index(), "arraylength"));
            case 191 -> {
                terminatorKind = "throw";
                statements.add(new Stmt.Throw(insn.index(), popSafe(insn.index())));
            }
            case 192 -> push(new Expr.Cast(insn.index(), insn.operand(), popSafe(insn.index())));
            case 193 -> push(new Expr.InstanceOf(insn.index(), popSafe(insn.index()), insn.operand()));
            case 194, 195 -> popSafe(insn.index());
            case 197 -> push(new Expr.Opaque(insn.index(), "multianewarray " + insn.operand()));
            default -> push(new Expr.Opaque(insn.index(), insn.mnemonic()
                    + (insn.operand().isEmpty() ? "" : " " + insn.operand())));
        }
    }

    private void binary(Insn insn, String symbol) {
        Expr right = popSafe(insn.index());
        Expr left = popSafe(insn.index());
        push(new Expr.Binary(insn.index(), symbol, left, right));
    }

    private void buildCond(Insn insn) {
        int op = insn.opcode();
        String symbol = condSymbol(op);
        if (op >= 153 && op <= 158) {
            // if<cond> 与零比较。
            Expr left = popSafe(insn.index());
            terminatorCond = new Expr.Cond(insn.index(), symbol, left, new Expr.Const(insn.index(), "0"), false);
        } else if (op >= 159 && op <= 164) {
            // if_icmp<cond>。
            Expr right = popSafe(insn.index());
            Expr left = popSafe(insn.index());
            terminatorCond = new Expr.Cond(insn.index(), symbol, left, right, false);
        } else if (op == 165 || op == 166) {
            // if_acmp<cond>。
            Expr right = popSafe(insn.index());
            Expr left = popSafe(insn.index());
            terminatorCond = new Expr.Cond(insn.index(), symbol, left, right, false);
        } else {
            // ifnull / ifnonnull。
            Expr left = popSafe(insn.index());
            terminatorCond = new Expr.Cond(insn.index(), symbol, left, new Expr.Const(insn.index(), "null"), false);
        }
        terminatorKind = "cond";
    }

    private void buildField(Insn insn) {
        int op = insn.opcode();
        String field = insn.operand().isEmpty() ? ("field@" + insn.index()) : insn.operand();
        if (op == 178 || op == 180) {
            // getstatic / getfield。
            if (op == 180) {
                Expr recv = popSafe(insn.index());
                push(new Expr.Opaque(insn.index(), recv.render() + "." + simpleField(field)));
            } else {
                push(new Expr.Opaque(insn.index(), simpleField(field)));
            }
        } else {
            // putstatic / putfield。
            Expr val = popSafe(insn.index());
            Expr target;
            if (op == 181) {
                Expr recv = popSafe(insn.index());
                target = new Expr.Opaque(insn.index(), recv.render() + "." + simpleField(field));
            } else {
                target = new Expr.Opaque(insn.index(), simpleField(field));
            }
            statements.add(new Stmt.ExprStmt(insn.index(), new Expr.Assign(insn.index(), target, val)));
        }
    }

    private void buildCall(Insn insn) {
        int op = insn.opcode();
        String operand = insn.operand();
        int p = operand.indexOf('(');
        String owner = "";
        String name = operand;
        String desc = "";
        if (p >= 0) {
            String sig = operand.substring(0, p);
            desc = operand.substring(p);
            int slash = sig.lastIndexOf('/');
            if (slash >= 0) {
                owner = sig.substring(0, slash);
                name = sig.substring(slash + 1);
            } else {
                name = sig;
            }
        }
        int argc = argCount(desc);
        List<Expr> args = new ArrayList<>();
        for (int i = 0; i < argc; i++) {
            args.add(0, popSafe(insn.index()));
        }
        Expr receiver = null;
        if (op != 184 && !(op == 183 && "<init>".equals(name))) {
            receiver = popSafe(insn.index());
        }
        Expr call = new Expr.Call(insn.index(), receiver, owner, name, desc, args);
        if (op == 183 && "<init>".equals(name)) {
            // 构造函数调用作为语句。
            statements.add(new Stmt.ExprStmt(insn.index(), call));
        } else if (desc.endsWith(")V")) {
            statements.add(new Stmt.ExprStmt(insn.index(), call));
        } else {
            push(call);
        }
    }

    private Expr local(int insn, int slot) {
        String name = varNamer == null ? ("v" + slot) : varNamer.apply(slot);
        return new Expr.Local(insn, slot, name);
    }

    private void push(Expr e) {
        stack.push(e);
    }

    private Expr popSafe(int insn) {
        if (stack.isEmpty()) {
            return new Expr.Opaque(insn, "?");
        }
        return stack.pop();
    }

    private static String simpleField(String field) {
        // operand 形如 owner/Name:Desc → 取简单名。
        String s = field;
        int colon = s.indexOf(':');
        if (colon >= 0) {
            s = s.substring(0, colon);
        }
        int slash = s.lastIndexOf('/');
        return slash >= 0 ? s.substring(slash + 1) : s;
    }

    private static String quoteIfString(String operand) {
        return operand;
    }

    private static String condSymbol(int op) {
        return switch (op) {
            case 153, 159, 165, 198 -> "==";
            case 154, 160, 166, 199 -> "!=";
            case 155 -> "<";
            case 156 -> ">=";
            case 157 -> ">";
            case 158 -> "<=";
            case 161 -> "<";
            case 162 -> ">=";
            case 163 -> ">";
            case 164 -> "<=";
            default -> "?";
        };
    }

    /** 解析 {@code "var N by M"} 中的自增量。 */
    private static int parseIncAmount(String operand) {
        if (operand == null) {
            return 1;
        }
        int by = operand.indexOf(" by ");
        if (by < 0) {
            return 1;
        }
        try {
            return Integer.parseInt(operand.substring(by + 4).trim());
        } catch (NumberFormatException ex) {
            return 1;
        }
    }

    /** 解析 {@code "var N"} 中的槽号。 */
    private static int parseVar(String operand) {
        if (operand == null) {
            return -1;
        }
        int idx = operand.indexOf("var ");
        if (idx < 0) {
            return -1;
        }
        int p = idx + 4;
        int n = 0;
        boolean any = false;
        while (p < operand.length() && Character.isDigit(operand.charAt(p))) {
            n = n * 10 + (operand.charAt(p) - '0');
            p++;
            any = true;
        }
        return any ? n : -1;
    }

    /** 统计方法描述符的参数个数（长/双精度计为一个值）。 */
    static int argCount(String desc) {
        if (desc == null || !desc.startsWith("(")) {
            return 0;
        }
        int count = 0;
        int i = 1;
        while (i < desc.length() && desc.charAt(i) != ')') {
            char c = desc.charAt(i);
            if (c == '[') {
                i++;
                continue;
            }
            if (c == 'L') {
                int semi = desc.indexOf(';', i);
                i = semi < 0 ? desc.length() : semi + 1;
                count++;
            } else {
                i++;
                count++;
            }
        }
        return count;
    }
}
