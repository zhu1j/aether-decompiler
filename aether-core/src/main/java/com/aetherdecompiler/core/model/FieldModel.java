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

import java.util.Objects;

/**
 * 不可变的字段模型。
 *
 * <p>字段不携带行为，所以模型很小：访问标志、名称、描述符与泛型签名。它存在
 * 是为了让类模型完整，并让未来的渲染器无需再碰 ASM 就能输出字段声明。</p>
 *
 * <p>故事类比：铆在机器上的铭牌 —— 它标识零件，但零件自身什么也不做。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class FieldModel {

    private final int access;
    private final String name;
    private final String descriptor;
    private final String signature;
    private final Object constantValue;

    /**
     * @param access     原始访问标志
     * @param name       字段名
     * @param descriptor 字段描述符，例如 {@code "Ljava/lang/String;"}
     * @param signature  泛型签名，或 {@code null}
     */
    public FieldModel(int access, String name, String descriptor, String signature) {
        this(access, name, descriptor, signature, null);
    }

    /**
     * @param access        原始访问标志
     * @param name          字段名
     * @param descriptor    字段描述符，例如 {@code "Ljava/lang/String;"}
     * @param signature     泛型签名，或 {@code null}
     * @param constantValue 编译期常量值（ConstantValue 属性），无则为 {@code null}
     */
    public FieldModel(int access, String name, String descriptor, String signature,
                      Object constantValue) {
        this.access = access;
        this.name = Objects.requireNonNull(name, "name");
        this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
        this.signature = signature;
        this.constantValue = constantValue;
    }

    /** @return 原始访问标志 */
    public int access() {
        return access;
    }

    /** @return 字段名 */
    public String name() {
        return name;
    }

    /** @return 字段描述符 */
    public String descriptor() {
        return descriptor;
    }

    /** @return 泛型签名，或 {@code null} */
    public String signature() {
        return signature;
    }

    /** @return 编译期常量值（ConstantValue 属性），无则为 {@code null} */
    public Object constantValue() {
        return constantValue;
    }

    @Override
    public String toString() {
        return "FieldModel{" + name + ":" + descriptor + "}";
    }
}
