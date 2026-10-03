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
package com.aetherdecompiler.core.model;

import com.aetherdecompiler.api.IRKind;
import com.aetherdecompiler.api.IRObject;

import java.util.List;
import java.util.Objects;

/**
 * 不可变的方法模型。
 *
 * <p>它捕获内核推理一个方法所需的全部信息，而无需再次接触 ASM：访问标志、
 * 名称、描述符、线性指令列表、异常表以及帧大小提示。没有代码的方法
 * （抽象/原生）拥有空指令列表。</p>
 *
 * <p>故事类比：一张食谱卡 —— 它的标题、按顺序排列的配料表，以及底部的
 * “万一锅着火”的应急行。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class MethodModel implements IRObject {

    private final int access;
    private final String name;
    private final String descriptor;
    private final String signature;
    private final List<Insn> instructions;
    private final List<TryCatchEntry> tryCatchEntries;
    private final List<LocalVariable> localVariables;
    private final int maxStack;
    private final int maxLocals;

    /**
     * 兼容旧签名的构造器：不含局部变量表。缺省局部变量表为空，
     * 下游命名回退为 {@code vN}。
     *
     * @param access          原始访问标志
     * @param name            方法名
     * @param descriptor      方法描述符，例如 {@code "(I)J"}
     * @param signature       泛型签名，或 {@code null}
     * @param instructions    不可变指令列表（永不为 {@code null}）
     * @param tryCatchEntries 不可变异常表（永不为 {@code null}）
     * @param maxStack        最大操作数栈深度；若为抽象方法则为 {@code -1}
     * @param maxLocals       最大局部变量槽数；若为抽象方法则为 {@code -1}
     */
    public MethodModel(int access, String name, String descriptor, String signature,
                       List<Insn> instructions, List<TryCatchEntry> tryCatchEntries,
                       int maxStack, int maxLocals) {
        this(access, name, descriptor, signature, instructions, tryCatchEntries,
                List.of(), maxStack, maxLocals);
    }

    /**
     * @param access          原始访问标志
     * @param name            方法名
     * @param descriptor      方法描述符，例如 {@code "(I)J"}
     * @param signature       泛型签名，或 {@code null}
     * @param instructions    不可变指令列表（永不为 {@code null}）
     * @param tryCatchEntries 不可变异常表（永不为 {@code null}）
     * @param localVariables  不可变局部变量表（永不为 {@code null}，无调试信息时为空）
     * @param maxStack        最大操作数栈深度；若为抽象方法则为 {@code -1}
     * @param maxLocals       最大局部变量槽数；若为抽象方法则为 {@code -1}
     */
    public MethodModel(int access, String name, String descriptor, String signature,
                       List<Insn> instructions, List<TryCatchEntry> tryCatchEntries,
                       List<LocalVariable> localVariables, int maxStack, int maxLocals) {
        this.access = access;
        this.name = Objects.requireNonNull(name, "name");
        this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
        this.signature = signature;
        this.instructions = List.copyOf(instructions);
        this.tryCatchEntries = List.copyOf(tryCatchEntries);
        this.localVariables = List.copyOf(localVariables);
        this.maxStack = maxStack;
        this.maxLocals = maxLocals;
    }

    /** @return 原始访问标志 */
    public int access() {
        return access;
    }

    /** @return 方法名 */
    public String name() {
        return name;
    }

    /** @return 方法描述符 */
    public String descriptor() {
        return descriptor;
    }

    /** @return 泛型签名，或 {@code null} */
    public String signature() {
        return signature;
    }

    /** @return 不可变指令列表 */
    public List<Insn> instructions() {
        return instructions;
    }

    /** @return 不可变异常表 */
    public List<TryCatchEntry> tryCatchEntries() {
        return tryCatchEntries;
    }

    /** @return 不可变局部变量表（无调试信息时为空） */
    public List<LocalVariable> localVariables() {
        return localVariables;
    }

    /** @return 最大操作数栈深度，或 {@code -1} */
    public int maxStack() {
        return maxStack;
    }

    /** @return 最大局部变量槽数，或 {@code -1} */
    public int maxLocals() {
        return maxLocals;
    }

    /** @return 若该方法不含代码则返回 {@code true} */
    public boolean isAbstractOrNative() {
        return AccessFlags.isAbstract(access) || (access & AccessFlags.NATIVE) != 0;
    }

    /** @return 完全限定标识符 {@code name + descriptor} */
    public String id() {
        return name + descriptor;
    }

    @Override
    public IRKind kind() {
        return IRKind.METHOD_MODEL;
    }

    @Override
    public String toString() {
        return "MethodModel{" + id() + ", " + instructions.size() + " insns}";
    }
}
