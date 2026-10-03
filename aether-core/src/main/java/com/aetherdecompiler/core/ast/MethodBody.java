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

/**
 * 单个方法的抽象语法树（AST）根：一条语句树 + 元数据。
 *
 * <p>它居于内核流水线的第三层：{@code ClassModel}（字节）→ {@code CFG}（控制流）
 * → {@code SSA}（值流）→ <b>{@code MethodBody}（结构）</b>。方法体把 CFG 上散落的
 * 基本块，还原成嵌套的 {@link Stmt} 语句树；表达式重建（{@link Expr}）则把基于栈的
 * 指令重写成可读的值表达式。</p>
 *
 * <p>与内核其余部分一致：本对象不可变、语言中立、可缓存、可并行读取。渲染成
 * Java/Kotlin/伪代码的职责属于 {@code RenderPlugin}，而非本类型。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class MethodBody implements IRObject {

    private final String ownerClass;
    private final String methodName;
    private final String descriptor;
    private final int access;
    private final Stmt body;
    private final boolean irreducible;

    /**
     * @param ownerClass  所属类内部名
     * @param methodName  方法名
     * @param descriptor  方法描述符
     * @param access      原始访问标志
     * @param body        语句树根
     * @param irreducible 是否因不可规约控制流而退回线性/标签模式
     */
    public MethodBody(String ownerClass, String methodName, String descriptor, int access,
                      Stmt body, boolean irreducible) {
        this.ownerClass = ownerClass;
        this.methodName = methodName;
        this.descriptor = descriptor;
        this.access = access;
        this.body = body;
        this.irreducible = irreducible;
    }

    /** @return 所属类内部名 */
    public String ownerClass() {
        return ownerClass;
    }

    /** @return 方法名 */
    public String methodName() {
        return methodName;
    }

    /** @return 方法描述符 */
    public String descriptor() {
        return descriptor;
    }

    /** @return 原始访问标志 */
    public int access() {
        return access;
    }

    /** @return 语句树根（永不为 {@code null}） */
    public Stmt body() {
        return body;
    }

    /** @return 是否因不可规约控制流而退回线性模式 */
    public boolean isIrreducible() {
        return irreducible;
    }

    /** @return 方法标识符 {@code name + descriptor} */
    public String id() {
        return methodName + descriptor;
    }

    @Override
    public IRKind kind() {
        return IRKind.AST;
    }

    @Override
    public String toString() {
        return "MethodBody{" + ownerClass + "." + id()
                + (irreducible ? ", irreducible" : "") + "}";
    }
}
