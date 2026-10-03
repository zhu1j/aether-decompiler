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
import com.aetherdecompiler.core.model.AccessFlags;
import com.aetherdecompiler.core.model.ClassModel;
import com.aetherdecompiler.core.model.FieldModel;
import com.aetherdecompiler.core.model.MethodModel;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 把语言中立的 {@link MethodBody} 渲染成 Java 风格的源码文本，并同步产出源码映射。
 *
 * <p>它是“AST 从不包含 Java 关键字”这一硬性约束的兑现者：整棵树只描述结构，而把
 * 关键字、缩进、分号、括号这些<em>打印细节</em>全部留给本类。每当写下一段文本，它就
 * 顺手记录一个 {@link DefaultSourceMapping.Builder#add 映射}——于是“生成文本 ↔ 字节码”
 * 的双向索引在渲染过程中被自然建立。</p>
 *
 * <p>本类刻意保守：无法识别的结构退化为可读的近似，而不是抛异常。它追求的是“一定
 * 能写出点东西、且映射不丢”。同时它承担<strong>意译</strong>职责：把字节码层的描述符、
 * {@code <init>}/{@code <clinit>} 等 IR 记号翻译成合法的 Java 语法记号（构造方法、
 * 静态初始化块、参数列表、类型名），从而使自研渲染器的输出可被 {@code javac} 编译。</p>
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

    /** 当前方法内“槽位 → 推断出的 Java 类型”，用于补出局部变量声明。 */
    private final Map<Integer, String> localTypes = new LinkedHashMap<>();
    /** 形参槽位 → 其声明类型（供类型推断解析参数引用）。 */
    private final Map<Integer, String> paramTypes = new LinkedHashMap<>();
    /** 当前方法形参所占的槽位集合（这些槽位不重复声明）。 */
    private final Set<Integer> paramSlots = new java.util.HashSet<>();
    /** 当前方法声明的返回类型（用于未解析返回值的降级与末尾兜底 return）。 */
    private String currentReturnType = "void";
    /** 当前渲染的类是否为接口（决定带体方法是否需加 {@code default}）。 */
    private boolean currentIsInterface = false;
    /** 当前方法局部槽位 → 显示名（来自 LocalVariableTable；无调试信息时为空）。 */
    private Map<Integer, String> localNames = Map.of();
    /** 当前方法是否为静态（影响槽 0 的 this 语义）。 */
    private boolean currentIsStatic = false;
    /** 无法精确重建时的策略；默认宽松（注释占位），可切换为严格（抛错）。 */
    private OpaqueMode opaqueMode = OpaqueMode.LENIENT;

    /** 槽位 → 显示名：优先真实变量名，否则回退 {@code this}/{@code vN}。 */
    private String nameOf(int slot) {
        if (slot == 0 && !currentIsStatic) {
            return "this";
        }
        String n = localNames.get(slot);
        return n != null ? n : ("v" + slot);
    }

    /**
     * 渲染一个方法体（单方法视图）。
     *
     * @param body 方法体 AST
     * @return 渲染单元（路径为 {@code <Owner>.java}，含源码映射）
     */
    /**
     * 设置“无法精确重建”时的策略。
     *
     * <p>{@link OpaqueMode#LENIENT}：以注释占位，尽量保持可编译（默认）；
     * {@link OpaqueMode#STRICT}：遇到无法重建的片段立即抛
     * {@link UnresolvedCodeException}。</p>
     *
     * @param mode 目标策略；传 {@code null} 视为宽松
     */
    public void setOpaqueMode(OpaqueMode mode) {
        this.opaqueMode = mode == null ? OpaqueMode.LENIENT : mode;
    }

    /** @return 当前策略 */
    public OpaqueMode opaqueMode() {
        return opaqueMode;
    }

    /**
     * 按当前策略校验一个表达式是否可精确重建：严格模式遇 {@link Expr.Opaque} 抛错，
     * 宽松模式静默返回。供自动化校验与回归测试直接调用。
     *
     * @param e 待校验表达式
     */
    public void strictCheck(Expr e) {
        if (e instanceof Expr.Opaque) {
            unresolved(e, "unresolved expression");
        }
    }

    /**
     * 严格模式下遇到无法重建的片段立即抛错；宽松模式返回 {@code null}，由调用方按占位处理。
     *
     * @param e 待检查的表达式
     * @return 宽松模式下恒为 {@code null}
     */
    private Void unresolved(Expr e, String detail) {
        if (opaqueMode == OpaqueMode.STRICT) {
            throw new UnresolvedCodeException(e == null ? -1 : e.firstInsn(), detail);
        }
        return null;
    }

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
        emitMethod(body);
        indent--;
        emit("}\n");
        return SourceTree.of(simple + ".java", out.toString(), mapping.build());
    }

    /**
     * 渲染整个类的骨架：类声明 + 字段声明 + 全部方法体。这是自研渲染器走向
     * “可编译输出”的入口——把散落的方法体与字段重新装配成一个完整类型。
     *
     * @param cls     类模型（提供类名、父类、字段）
     * @param bodies  该类每个具体方法的方法体 AST（顺序即输出顺序）
     * @return 渲染单元（路径为 {@code <SimpleName>.java}，含源码映射）
     */
    public SourceTree render(ClassModel cls, List<MethodBody> bodies) {
        if (cls == null) {
            return SourceTree.of("Unknown.java", "// <no class>\n", null);
        }
        String simple = simpleName(cls.name());
        this.className = simple;
        this.currentIsInterface = cls.isInterface();
        out.setLength(0);
        line = 1;
        col = 0;
        indent = 0;

        StringBuilder head = new StringBuilder();
        if (cls.isInterface()) {
            head.append("interface ").append(simple);
        } else {
            head.append("class ").append(simple);
            String superName = cls.superName();
            if (superName != null && !superName.isEmpty()
                    && !"java/lang/Object".equals(superName)) {
                head.append(" extends ").append(simpleName(superName));
            }
        }
        if (!cls.interfaces().isEmpty()) {
            head.append(cls.isInterface() ? " extends " : " implements ");
            for (int i = 0; i < cls.interfaces().size(); i++) {
                if (i > 0) {
                    head.append(", ");
                }
                head.append(simpleName(cls.interfaces().get(i)));
            }
        }
        head.append(" {\n");
        emit(head.toString());
        indent++;

        for (FieldModel f : cls.fields()) {
            emitLine(null, fieldDecl(f));
        }
        if (!cls.fields().isEmpty() && bodies != null && !bodies.isEmpty()) {
            emit("\n");
        }

        if (bodies != null) {
            for (int i = 0; i < bodies.size(); i++) {
                emitMethod(bodies.get(i));
                if (i < bodies.size() - 1) {
                    emit("\n");
                }
            }
        }

        // 抽象/原生方法无方法体：仅声明签名（接口/抽象类的必要成员）。
        for (MethodModel m : cls.methods()) {
            if (m.isAbstractOrNative()) {
                emitLine(null, methodSignature(cls.name(), m.name(), m.descriptor(), m.access()) + ";");
            }
        }

        indent--;
        emit("}\n");
        return SourceTree.of(simple + ".java", out.toString(), mapping.build());
    }

    /** 渲染单个方法（含 {@code <init>}/{@code <clinit>} 的意译）。 */
    private void emitMethod(MethodBody body) {
        String name = body.methodName();
        this.localNames = body.localNames();
        this.currentIsStatic = AccessFlags.isStatic(body.access());
        if ("<clinit>".equals(name)) {
            // 类静态初始化器不是普通方法：渲染为 static { ... } 块。
            currentReturnType = "void";
            emit("    ").emit("static {\n");
            indent++;
            prepareLocals(body);
            emitDeclarations();
            emitBodyDroppingTrailingReturn(body.body());
            indent--;
            emit("    ").emit("}\n");
            return;
        }
        emit("    ").emit(defaultPrefix(body)).emit(methodSignature(body)).emit(" {\n");
        indent++;
        boolean ctor = "<init>".equals(name);
        currentReturnType = ctor ? "void" : returnType(body.descriptor() == null ? "()V" : body.descriptor());
        prepareLocals(body);
        emitDeclarations();
        Stmt s = body.body();
        if (ctor) {
            // 构造方法：丢弃末尾冗余的裸 return;（构造器无返回值）。
            emitBodyDroppingTrailingReturn(s);
        } else {
            statement(s);
        }
        // 非 void 方法：若结构化后的最后一条语句不是 return/throw，补一个默认返回以免“缺失返回”。
        if (!"void".equals(currentReturnType) && !endsWithReturnOrThrow(s)) {
            emitLine(null, "return " + defaultLiteral(currentReturnType) + "; // default (fallthrough)");
        }
        indent--;
        emit("    ").emit("}\n");
    }

    /** 接口中的带体方法需加 {@code default} 修饰符；其余情况返回空串。 */
    private String defaultPrefix(MethodBody body) {
        if (currentIsInterface && !"<init>".equals(body.methodName())) {
            return "default ";
        }
        return "";
    }

    /** 判断语句（递归）是否以 return 或 throw 结束。 */
    private static boolean endsWithReturnOrThrow(Stmt s) {
        if (s == null) {
            return false;
        }
        if (s instanceof Stmt.Return || s instanceof Stmt.Throw) {
            return true;
        }
        if (s instanceof Stmt.Block b) {
            return !b.statements().isEmpty()
                    && endsWithReturnOrThrow(b.statements().get(b.statements().size() - 1));
        }
        if (s instanceof Stmt.If i) {
            return i.elseBranch() != null
                    && endsWithReturnOrThrow(i.thenBranch())
                    && endsWithReturnOrThrow(i.elseBranch());
        }
        return false;
    }

    /**
     * 布尔语义提升：把以 {@code 0/1} 模拟布尔返回值的表达式还原为 {@code false/true}。
     * 其余表达式原样渲染（例如已经重建出的比较表达式）。
     */
    private static String booleanLiteral(Expr e) {
        if (e instanceof Expr.Const c) {
            String v = c.value();
            if ("0".equals(v)) {
                return "false";
            }
            if ("1".equals(v)) {
                return "true";
            }
        }
        return e.render();
    }

    /**
     * 条件表达式渲染：把“布尔变量与 0/1 常量比较”还原为与 {@code false/true} 比较。
     *
     * <p>字节码中布尔值以 {@code int} 承载，比较时出现 {@code ifeq}/{@code ifne} 等，
     * 会被重建为 {@code v1 == 0}。若 {@code v1} 的声明类型是 {@code boolean}，这种
     * 写法在 Java 里非法；这里据类型把它提升为 {@code v1 == false}。非布尔情形
     * 原样渲染。</p>
     */
    private String condText(Expr e) {
        if (e instanceof Expr.Binary b) {
            String op = b.op();
            if (("==".equals(op) || "!=".equals(op))) {
                Expr l = b.left();
                Expr r = b.right();
                if (isBooleanOperand(l) && isBoolConst(r)) {
                    return expr(l) + " " + op + " " + boolConst(r);
                }
                if (isBooleanOperand(r) && isBoolConst(l)) {
                    return boolConst(l) + " " + op + " " + expr(r);
                }
            }
        }
        return expr(e);
    }

    private boolean isBooleanOperand(Expr e) {
        return "boolean".equals(inferType(e));
    }

    private static boolean isBoolConst(Expr e) {
        return e instanceof Expr.Const c && ("0".equals(c.value()) || "1".equals(c.value()));
    }

    private static String boolConst(Expr e) {
        return "1".equals(((Expr.Const) e).value()) ? "true" : "false";
    }

    /** 按类型给出默认字面量，用于未解析返回值的降级与末尾兜底。 */
    private static String defaultLiteral(String type) {
        return switch (type) {
            case "boolean" -> "false";
            case "long" -> "0L";
            case "float" -> "0.0f";
            case "double" -> "0.0d";
            case "void" -> "";
            default -> type.endsWith("[]") || Character.isUpperCase(type.charAt(0))
                    ? "null" : "0";
        };
    }

    /** 逐条输出语句，但丢弃末尾的裸 {@code return;}——Java 中 void 块/构造器无需它。 */
    private void emitBodyDroppingTrailingReturn(Stmt s) {
        if (s instanceof Stmt.Block b) {
            List<Stmt> stmts = b.statements();
            for (int i = 0; i < stmts.size(); i++) {
                Stmt cur = stmts.get(i);
                boolean lastBareReturn = cur instanceof Stmt.Return r && r.value() == null;
                if (lastBareReturn && i == stmts.size() - 1) {
                    continue;
                }
                statement(cur);
            }
        } else {
            statement(s);
        }
    }

    // ---- 局部变量声明（P1：类型推断 + 补声明）----

    /** 逐语句收集“被赋值的局部槽位”及其推断类型，并登记形参槽位。 */
    private void prepareLocals(MethodBody body) {
        localTypes.clear();
        paramSlots.clear();
        paramTypes.clear();
        boolean isStatic = AccessFlags.isStatic(body.access());
        String desc = body.descriptor() == null ? "()V" : body.descriptor();
        int slot = isStatic ? 0 : 1;
        int i = 1;
        while (i < desc.length() && desc.charAt(i) != ')') {
            TypeRef t = readType(desc, i);
            i = t.next;
            paramSlots.add(slot);
            paramTypes.put(slot, t.java);
            slot += t.slots;
        }
        collectLocals(body.body());
    }

    /** 输出局部变量声明：非形参、且已被赋值的槽位，统一在方法体开头以推断类型声明。 */
    private void emitDeclarations() {
        boolean any = false;
        for (Map.Entry<Integer, String> en : localTypes.entrySet()) {
            int slot = en.getKey();
            if (paramSlots.contains(slot) || (slot == 0 && !currentIsStatic)) {
                continue;
            }
            emitLine(null, en.getValue() + " " + nameOf(slot) + ";");
            any = true;
        }
        if (any) {
            emit("\n");
        }
    }

    private void collectLocals(Stmt s) {
        if (s == null) {
            return;
        }
        if (s instanceof Stmt.Block b) {
            for (Stmt inner : b.statements()) {
                collectLocals(inner);
            }
        } else if (s instanceof Stmt.ExprStmt e) {
            scanExpr(e.expr());
        } else if (s instanceof Stmt.Return r) {
            scanExpr(r.value());
        } else if (s instanceof Stmt.Throw t) {
            scanExpr(t.value());
        } else if (s instanceof Stmt.If i) {
            scanExpr(i.cond());
            collectLocals(i.thenBranch());
            collectLocals(i.elseBranch());
        } else if (s instanceof Stmt.While w) {
            scanExpr(w.cond());
            collectLocals(w.body());
        } else if (s instanceof Stmt.DoWhile d) {
            collectLocals(d.body());
            scanExpr(d.cond());
        } else if (s instanceof Stmt.Switch sw) {
            scanExpr(sw.selector());
            for (Stmt.SwitchCase c : sw.cases()) {
                collectLocals(c.body());
            }
            collectLocals(sw.defaultBody());
        } else if (s instanceof Stmt.TryCatch tc) {
            collectLocals(tc.body());
            for (Stmt.CatchClause c : tc.catches()) {
                collectLocals(c.body());
            }
        } else if (s instanceof Stmt.Synchronized sy) {
            collectLocals(sy.body());
            scanExpr(sy.lock());
        }
    }

    private void scanExpr(Expr e) {
        if (e == null) {
            return;
        }
        if (e instanceof Expr.Assign a && a.target() instanceof Expr.Local l) {
            localTypes.putIfAbsent(l.slot(), inferType(a.value()));
        } else if (e instanceof Expr.Incr inc) {
            localTypes.putIfAbsent(inc.slot(), "int");
        }
        for (AstNode child : e.children()) {
            if (child instanceof Expr ce) {
                scanExpr(ce);
            }
        }
    }

    /** 轻量类型推断：据表达式形状推断其 Java 类型，用于补出局部变量声明。 */
    private String inferType(Expr e) {
        if (e == null) {
            return "Object";
        }
        if (e instanceof Expr.Const c) {
            return literalType(c.value());
        }
        if (e instanceof Expr.New n) {
            return simpleName(n.type());
        }
        if (e instanceof Expr.NewArray a) {
            return a.type() + "[]".repeat(1 + a.extraDims());
        }
        if (e instanceof Expr.NewMultiArray a) {
            return a.type() + "[]".repeat(a.sizes().size());
        }
        if (e instanceof Expr.ArrayLength) {
            return "int";
        }
        if (e instanceof Expr.Cast c) {
            return c.type() == null ? "Object" : c.type().replace('/', '.');
        }
        if (e instanceof Expr.Call c) {
            return returnType(c.descriptor() == null ? "()V" : c.descriptor());
        }
        if (e instanceof Expr.Binary b) {
            String op = b.op();
            if (op.equals("==") || op.equals("!=") || op.equals("<") || op.equals(">")
                    || op.equals("<=") || op.equals(">=") || op.equals("&&") || op.equals("||")) {
                return "boolean";
            }
            String lt = inferType(b.left());
            String rt = inferType(b.right());
            if (op.equals("+") && ("String".equals(lt) || "String".equals(rt))) {
                return "String";
            }
            return promote(lt, rt);
        }
        if (e instanceof Expr.Unary u) {
            String op = u.op();
            if (op.startsWith("(")) {
                return op.replace("(", "").replace(")", "").replace('/', '.');
            }
            return inferType(u.operand());
        }
        if (e instanceof Expr.Local l) {
            String pt = paramTypes.get(l.slot());
            if (pt != null) {
                return pt;
            }
            return localTypes.getOrDefault(l.slot(), "int");
        }
        if (e instanceof Expr.ArrayLoad al) {
            String at = inferType(al.array());
            return at.endsWith("[]") ? at.substring(0, at.length() - 2) : "int";
        }
        if (e instanceof Expr.InstanceOf || e instanceof Expr.Cond) {
            return "boolean";
        }
        if (e instanceof Expr.Incr) {
            return "int";
        }
        return "Object";
    }

    private static String literalType(String v) {
        if (v == null || v.equals("null")) {
            return "Object";
        }
        if (v.startsWith("\"")) {
            return "String";
        }
        char last = v.charAt(v.length() - 1);
        if (last == 'L' || last == 'l') {
            return "long";
        }
        if (last == 'f' || last == 'F') {
            return "float";
        }
        if (last == 'd' || last == 'D') {
            return "double";
        }
        return "int";
    }

    private static String promote(String a, String b) {
        if ("double".equals(a) || "double".equals(b)) {
            return "double";
        }
        if ("float".equals(a) || "float".equals(b)) {
            return "float";
        }
        if ("long".equals(a) || "long".equals(b)) {
            return "long";
        }
        return "int";
    }

    private String methodSignature(MethodBody body) {
        return methodSignature(body.ownerClass(), body.methodName(), body.descriptor(), body.access(),
                body.localNames());
    }

    /** 由 owner/name/descriptor/access 直接生成方法签名（用于抽象/原生方法的声明）。 */
    private String methodSignature(String owner, String name, String descriptor, int access) {
        return methodSignature(owner, name, descriptor, access, Map.of());
    }

    /** 由描述符与局部变量名表生成方法签名（形参名与体中的引用保持一致）。 */
    private String methodSignature(String owner, String name, String descriptor, int access,
                                   Map<Integer, String> names) {
        String desc = descriptor == null ? "()V" : descriptor;
        String simple = simpleName(owner);
        boolean isStatic = AccessFlags.isStatic(access);
        if ("<init>".equals(name)) {
            return "public " + simple + "(" + paramList(desc, false, names) + ")";
        }
        String mod = isStatic ? "public static " : "public ";
        return mod + returnType(desc) + " " + name + "(" + paramList(desc, isStatic, names) + ")";
    }

    /** 字段声明：访问标志 + 类型 + 名字（+ 常量初始化式）。 */
    private String fieldDecl(FieldModel f) {
        StringBuilder sb = new StringBuilder();
        sb.append(accessText(f.access())).append(typeName(f.descriptor())).append(' ').append(f.name());
        Object cv = f.constantValue();
        if (cv != null) {
            sb.append(" = ").append(constantLiteral(cv));
        }
        sb.append(';');
        return sb.toString();
    }

    /** 把 ConstantValue 属性值格式化为 Java 字面量。 */
    private static String constantLiteral(Object v) {
        if (v instanceof String s) {
            return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        }
        if (v instanceof Long l) {
            return l + "L";
        }
        if (v instanceof Float f) {
            return f + "f";
        }
        if (v instanceof Double d) {
            return d + "d";
        }
        if (v instanceof Character c) {
            return "'" + c + "'";
        }
        if (v instanceof Boolean b) {
            return b.toString();
        }
        return String.valueOf(v);
    }

    // ---- 描述符 → Java 类型（意译核心）----

    /** 解析方法描述符的参数部分为 Java 形参列表，形参名优先取真实变量名。 */
    private static String paramList(String desc, boolean isStatic) {
        return paramList(desc, isStatic, Map.of());
    }

    /** 解析方法描述符的参数部分，形参名按局部变量名表解析，缺失时回退 {@code vN}。 */
    private static String paramList(String desc, boolean isStatic, Map<Integer, String> names) {
        StringBuilder sb = new StringBuilder();
        int slot = isStatic ? 0 : 1;
        int i = 1;
        boolean first = true;
        while (i < desc.length() && desc.charAt(i) != ')') {
            TypeRef t = readType(desc, i);
            i = t.next;
            if (!first) {
                sb.append(", ");
            }
            String pn = names.get(slot);
            sb.append(t.java).append(" ").append(pn != null ? pn : ("v" + slot));
            first = false;
            slot += t.slots;
        }
        return sb.toString();
    }

    /** 解析方法描述符的返回类型为 Java 类型名。 */
    private static String returnType(String desc) {
        int p = desc.indexOf(')');
        if (p < 0 || p + 1 >= desc.length()) {
            return "void";
        }
        return typeName(desc.substring(p + 1));
    }

    /** 把单个类型描述符（字段/返回值/参数）翻译为 Java 类型名。 */
    private static String typeName(String desc) {
        if (desc == null || desc.isEmpty()) {
            return "Object";
        }
        TypeRef t = readType(desc, 0);
        return t.java;
    }

    private static final class TypeRef {
        String java;
        int next;
        int slots;
    }

    /** 读取 {@code desc} 自 {@code i} 起的一个类型描述符，返回 Java 名、结束位置与槽位数。 */
    private static TypeRef readType(String desc, int i) {
        int dims = 0;
        while (i < desc.length() && desc.charAt(i) == '[') {
            dims++;
            i++;
        }
        if (i >= desc.length()) {
            TypeRef r = new TypeRef();
            r.java = "Object";
            r.next = i;
            r.slots = 1;
            return r;
        }
        char c = desc.charAt(i);
        String base;
        int slots = 1;
        if (c == 'L') {
            int semi = desc.indexOf(';', i);
            if (semi < 0) {
                semi = desc.length();
            }
            base = simpleName(desc.substring(i + 1, semi));
            i = semi + 1;
        } else {
            base = primitiveName(c);
            if (c == 'J' || c == 'D') {
                slots = 2;
            }
            i++;
        }
        StringBuilder sb = new StringBuilder(base);
        for (int d = 0; d < dims; d++) {
            sb.append("[]");
        }
        TypeRef r = new TypeRef();
        r.java = sb.toString();
        r.next = i;
        r.slots = slots;
        return r;
    }

    private static String primitiveName(char c) {
        return switch (c) {
            case 'V' -> "void";
            case 'Z' -> "boolean";
            case 'B' -> "byte";
            case 'C' -> "char";
            case 'S' -> "short";
            case 'I' -> "int";
            case 'J' -> "long";
            case 'F' -> "float";
            case 'D' -> "double";
            default -> "Object";
        };
    }

    /** 访问标志 → Java 修饰符文本（顺序：可见性 → static → final）。 */
    private static String accessText(int flags) {
        StringBuilder sb = new StringBuilder();
        if (AccessFlags.isPublic(flags)) {
            sb.append("public ");
        } else if ((flags & AccessFlags.PRIVATE) != 0) {
            sb.append("private ");
        } else if ((flags & AccessFlags.PROTECTED) != 0) {
            sb.append("protected ");
        }
        if (AccessFlags.isStatic(flags)) {
            sb.append("static ");
        }
        if (AccessFlags.isFinal(flags)) {
            sb.append("final ");
        }
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
            if (r.value() == null) {
                emitLine(s, "return;");
            } else if (r.value() instanceof Expr.Opaque) {
                // 严格模式：返回值无法重建即报错；宽松模式：按声明类型给合法默认值并注明，保持可编译。
                unresolved(r.value(), "unresolved return value");
                emitLine(s, "return " + defaultLiteral(currentReturnType) + "; // unresolved");
            } else if ("boolean".equals(currentReturnType)) {
                // 布尔语义提升：ireturn 0/1 还原为 return false/true。
                emitLine(s, "return " + booleanLiteral(r.value()) + ";");
            } else {
                emitLine(s, "return " + expr(r.value()) + ";");
            }
        } else if (s instanceof Stmt.Throw t) {
            emitLine(s, "throw " + expr(t.value()) + ";");
        } else if (s instanceof Stmt.If i) {
            emitLine(i, "if (" + condText(i.cond()) + ") {");
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
            emitLine(w, "while (" + condText(w.cond()) + ") {");
            indent++;
            statement(w.body());
            indent--;
            emitLine(w, "}");
        } else if (s instanceof Stmt.DoWhile d) {
            emitLine(d, "do {");
            indent++;
            statement(d.body());
            indent--;
            emitLine(d, "} while (" + condText(d.cond()) + ");");
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
            // goto 不是合法 Java：以注释占位，保留可追溯性而不破坏语法。
            emitLine(g, "// goto " + g.target() + " (irreducible)");
        } else if (s instanceof Stmt.Nop) {
            emitLine(s, "// nop");
        } else if (s instanceof Stmt.TryCatch tc) {
            emitLine(tc, "try {");
            indent++;
            statement(tc.body());
            indent--;
            emitLine(tc, "}");
            for (Stmt.CatchClause c : tc.catches()) {
                String typeName = c.type() == null ? "Throwable" : simpleName(c.type());
                String varName = c.varSlot() >= 0 ? nameOf(c.varSlot()) : "e";
                emitLine(tc, "catch (" + typeName + " " + varName + ") {");
                indent++;
                statement(c.body());
                indent--;
                emitLine(tc, "}");
            }
        } else if (s instanceof Stmt.Synchronized sy) {
            emitLine(sy, "synchronized (" + expr(sy.lock()) + ") {");
            indent++;
            statement(sy.body());
            indent--;
            emitLine(sy, "}");
        } else {
            emitLine(s, "// " + s.label());
        }
    }

    private String expr(Expr e) {
        if (e == null) {
            return "?";
        }
        if (e instanceof Expr.Opaque) {
            // 宽松模式：随后由 Expr.render() 输出注释占位；严格模式：立即抛错并保留上下文。
            unresolved(e, "unresolved expression");
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
