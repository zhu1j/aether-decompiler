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

import com.aetherdecompiler.api.SourceTree;
import com.aetherdecompiler.core.mapping.DefaultSourceMapping;

import java.util.List;

/**
 * 把语言中立的 {@link MethodBody} 渲染成 Java 风格的源码文本，并同步产出源码映射。
 *
 * <p>它是“AST 从不包含 Java 关键字”这一硬性约束的兑现者：整棵树只描述结构，而把
 * 关键字、缩进、分号、括号这些<em>打印细节</em>全部留给本类。每当写下一段文本，它就
 * 顺手记录一个 {@link DefaultSourceMapping.Builder#add 映射}——于是“生成文本 ↔ 字节码”
 * 的双向索引在渲染过程中被自然建立。</p>
 *
 * <p>本类刻意保守：无法识别的结构退化为可读的近似，而不是抛异常。它追求的是“一定
 * 能写出点东西、且映射不丢”，而非“一定要编译通过”。真正可编译的输出由更重的后端
 * （如 CFR）负责；本渲染器的价值在于把内核自己的 AST 直观地展示出来，用于教学与调试。</p>
 *
 * <p>故事类比：一位把结构草图誊清成正式图纸的描图员 —— 草图用什么符号都行，他负责
 * 用规范字母把每个线条标注清楚，并附上一张“图号 ↔ 原图”的对照表。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class JavaAstRenderer {

    private final StringBuilder out = new StringBuilder();
    private final DefaultSourceMapping.Builder mapping = DefaultSourceMapping.builder();
    private int indent;
    private int line = 1;
    private int col;
    private String className = "Unknown";

    /**
     * 渲染一个方法体。
     *
     * @param body 方法体 AST
     * @return 渲染单元（路径为 {@code <Owner>.java}，含源码映射）
     */
    public SourceTree render(MethodBody body) {
        if (body == null) {
            return SourceTree.of("Unknown.java", "// <no method body>\n", null);
        }
        String owner = body.ownerClass() == null ? "Unknown" : body.ownerClass();
        String simple = simpleName(owner);
        this.className = simple;
        out.setLength(0);
        line = 1;
        col = 0;
        indent = 0;

        emit("class ").emit(simple).emit(" {\n");
        indent++;
        emit("    ").emit(methodSignature(body)).emit(" {\n");
        indent++;
        statement(body.body());
        indent--;
        emit("    ").emit("}\n");
        indent--;
        emit("}\n");
        return SourceTree.of(simple + ".java", out.toString(), mapping.build());
    }

    private String methodSignature(MethodBody body) {
        StringBuilder sb = new StringBuilder();
        sb.append(com.aetherdecompiler.core.model.AccessFlags.isStatic(body.access())
                ? "static " : "public ");
        sb.append(body.methodName()).append('(').append(body.descriptor()).append(')');
        return sb.toString();
    }

    private void statement(Stmt s) {
        if (s == null) {
            return;
        }
        if (s instanceof Stmt.Block b) {
            if (b.statements().isEmpty()) {
                emitLine(b, "// empty");
                return;
            }
            for (Stmt inner : b.statements()) {
                statement(inner);
            }
        } else if (s instanceof Stmt.ExprStmt e) {
            emitLine(s, expr(e.expr()) + ";");
        } else if (s instanceof Stmt.Return r) {
            emitLine(s, r.value() == null ? "return;" : ("return " + expr(r.value()) + ";"));
        } else if (s instanceof Stmt.Throw t) {
            emitLine(s, "throw " + expr(t.value()) + ";");
        } else if (s instanceof Stmt.If i) {
            emitLine(i, "if (" + expr(i.cond()) + ") {");
            indent++;
            statement(i.thenBranch());
            indent--;
            if (i.elseBranch() != null) {
                emitLine(i, "} else {");
                indent++;
                statement(i.elseBranch());
                indent--;
            }
            emitLine(i, "}");
        } else if (s instanceof Stmt.While w) {
            emitLine(w, "while (" + expr(w.cond()) + ") {");
            indent++;
            statement(w.body());
            indent--;
            emitLine(w, "}");
        } else if (s instanceof Stmt.DoWhile d) {
            emitLine(d, "do {");
            indent++;
            statement(d.body());
            indent--;
            emitLine(d, "} while (" + expr(d.cond()) + ");");
        } else if (s instanceof Stmt.Switch sw) {
            emitLine(sw, "switch (" + expr(sw.selector()) + ") {");
            indent++;
            for (Stmt.SwitchCase c : sw.cases()) {
                emitLine(sw, "case " + c.key() + ":");
                indent++;
                statement(c.body());
                indent--;
            }
            if (sw.defaultBody() != null) {
                emitLine(sw, "default:");
                indent++;
                statement(sw.defaultBody());
                indent--;
            }
            indent--;
            emitLine(sw, "}");
        } else if (s instanceof Stmt.Label l) {
            emitLine(l, l.name() + ":");
        } else if (s instanceof Stmt.Goto g) {
            emitLine(g, "goto " + g.target() + "; // irreducible");
        } else if (s instanceof Stmt.Nop) {
            emitLine(s, "// nop");
        } else if (s instanceof Stmt.TryCatch tc) {
            emitLine(tc, "try {");
            indent++;
            statement(tc.body());
            indent--;
            emitLine(tc, "}");
            for (Stmt.CatchClause c : tc.catches()) {
                emitLine(tc, "catch (" + (c.type() == null ? "Throwable" : simpleName(c.type())) + ") {");
                indent++;
                statement(c.body());
                indent--;
                emitLine(tc, "}");
            }
        } else {
            emitLine(s, "// " + s.label());
        }
    }

    private String expr(Expr e) {
        if (e == null) {
            return "?";
        }
        return e.render();
    }

    // ---- 带映射的底层输出 ----

    private JavaAstRenderer emit(String text) {
        int startLine = line;
        int startCol = col;
        mapping.add(startLine, startCol, startLine, startCol + text.length(), -1, -1, -1);
        out.append(text);
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                line++;
                col = 0;
            } else {
                col++;
            }
        }
        return this;
    }

    /** 输出一整行，并把该行绑定到语句的指令区间。 */
    private void emitLine(AstNode node, String text) {
        for (int i = 0; i < indent; i++) {
            emit("    ");
        }
        int startLine = line;
        int startCol = col;
        int is = node == null ? -1 : node.firstInsn();
        int ie = node == null ? -1 : (node.lastInsn() + 1);
        mapping.add(startLine, startCol, startLine, startCol + text.length(), is, ie, -1);
        out.append(text).append('\n');
        line++;
        col = 0;
    }

    private static String simpleName(String internal) {
        if (internal == null) {
            return "Unknown";
        }
        int slash = internal.lastIndexOf('/');
        String s = slash >= 0 ? internal.substring(slash + 1) : internal;
        int semi = s.indexOf(';');
        return semi >= 0 ? s.substring(0, semi) : s;
    }
}
