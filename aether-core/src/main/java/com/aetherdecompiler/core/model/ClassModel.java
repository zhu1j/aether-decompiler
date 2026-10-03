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
import java.util.Optional;

/**
 * 不可变的类模型 —— 内核模型层的根产物。
 *
 * <p>由类字节一次性产出，之后可自由共享：它可以安全地被缓存、被多线程读取、
 * 被并行流式处理。它只包含字节码事实（名称、描述符、标志、成员），不含任何
 * Java 语法概念。</p>
 *
 * <p>故事类比：一栋建筑的总蓝图 —— 它的地址、楼层平面图（方法）与固定装置
 * （字段）—— 每当有人要看时就复印一份，而不是重新绘制。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class ClassModel implements IRObject {

    private final String name;
    private final String superName;
    private final List<String> interfaces;
    private final int access;
    private final int majorVersion;
    private final int minorVersion;
    private final String signature;
    private final List<FieldModel> fields;
    private final List<MethodModel> methods;

    /**
     * @param name         内部二进制名，例如 {@code "com/foo/Bar"}
     * @param superName    内部父类名；{@code Object} 时为 {@code null}
     * @param interfaces   内部接口名（永不为 {@code null}）
     * @param access       原始类访问标志
     * @param majorVersion 类文件主版本号
     * @param minorVersion 类文件次版本号
     * @param signature    泛型类签名，或 {@code null}
     * @param fields       不可变字段列表（永不为 {@code null}）
     * @param methods      不可变方法列表（永不为 {@code null}）
     */
    public ClassModel(String name, String superName, List<String> interfaces, int access,
                      int majorVersion, int minorVersion, String signature,
                      List<FieldModel> fields, List<MethodModel> methods) {
        this.name = Objects.requireNonNull(name, "name");
        this.superName = superName;
        this.interfaces = List.copyOf(interfaces);
        this.access = access;
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
        this.signature = signature;
        this.fields = List.copyOf(fields);
        this.methods = List.copyOf(methods);
    }

    /** @return 内部二进制名 */
    public String name() {
        return name;
    }

    /** @return 内部父类名，或 {@code null} */
    public String superName() {
        return superName;
    }

    /** @return 内部接口名 */
    public List<String> interfaces() {
        return interfaces;
    }

    /** @return 原始类访问标志 */
    public int access() {
        return access;
    }

    /** @return 类文件主版本号 */
    public int majorVersion() {
        return majorVersion;
    }

    /** @return 类文件次版本号 */
    public int minorVersion() {
        return minorVersion;
    }

    /** @return 泛型类签名，或 {@code null} */
    public String signature() {
        return signature;
    }

    /** @return 不可变字段列表 */
    public List<FieldModel> fields() {
        return fields;
    }

    /** @return 不可变方法列表 */
    public List<MethodModel> methods() {
        return methods;
    }

    /** @return 若该类是接口则返回 {@code true} */
    public boolean isInterface() {
        return (access & AccessFlags.INTERFACE) != 0;
    }

    /**
     * @param name       方法名
     * @param descriptor 方法描述符
     * @return 第一个匹配的方法（若存在）
     */
    public Optional<MethodModel> findMethod(String name, String descriptor) {
        return methods.stream()
                .filter(m -> m.name().equals(name) && m.descriptor().equals(descriptor))
                .findFirst();
    }

    /** @return 点分的人类可读名，例如 {@code com.foo.Bar} */
    public String dottedName() {
        return name.replace('/', '.');
    }

    @Override
    public IRKind kind() {
        return IRKind.CLASS_MODEL;
    }

    @Override
    public String toString() {
        return "ClassModel{" + name
                + ", fields=" + fields.size()
                + ", methods=" + methods.size()
                + ", major=" + majorVersion + "}";
    }
}
