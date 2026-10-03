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

/**
 * 访问标志位的人类可读名称，以及 JVM 类文件格式所用的位掩码。
 *
 * <p>访问标志是纯粹的字节码事实；在这里为它们命名，可让模型独立于 ASM 的
 * {@code Opcodes} 常量（后者重新暴露了同样的位，但若在模型中直接使用，
 * 会泄漏一个库类型）。</p>
 *
 * <p>故事类比：原理图上印的图例 —— “这些小数字表示 public、final、
 * static”。图例属于图纸，而不属于铅笔制造商。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class AccessFlags {

    public static final int PUBLIC = 0x0001;
    public static final int PRIVATE = 0x0002;
    public static final int PROTECTED = 0x0004;
    public static final int STATIC = 0x0008;
    public static final int FINAL = 0x0010;
    public static final int SUPER = 0x0020;
    public static final int SYNCHRONIZED = 0x0020;
    public static final int VOLATILE = 0x0040;
    public static final int BRIDGE = 0x0040;
    public static final int TRANSIENT = 0x0080;
    public static final int VARARGS = 0x0080;
    public static final int NATIVE = 0x0100;
    public static final int INTERFACE = 0x0200;
    public static final int ABSTRACT = 0x0400;
    public static final int STRICT = 0x0800;
    public static final int SYNTHETIC = 0x1000;
    public static final int ANNOTATION = 0x2000;
    public static final int ENUM = 0x4000;
    public static final int MODULE = 0x8000;

    private AccessFlags() {
        throw new AssertionError("No instances.");
    }

    /**
     * @param flags 原始访问位
     * @return 若 {@code public} 位被置位则返回 {@code true}
     */
    public static boolean isPublic(int flags) {
        return (flags & PUBLIC) != 0;
    }

    /**
     * @param flags 原始访问位
     * @return 若 {@code static} 位被置位则返回 {@code true}
     */
    public static boolean isStatic(int flags) {
        return (flags & STATIC) != 0;
    }

    /**
     * @param flags 原始访问位
     * @return 若 {@code final} 位被置位则返回 {@code true}
     */
    public static boolean isFinal(int flags) {
        return (flags & FINAL) != 0;
    }

    /**
     * @param flags 原始访问位
     * @return 若 {@code abstract} 位被置位则返回 {@code true}
     */
    public static boolean isAbstract(int flags) {
        return (flags & ABSTRACT) != 0;
    }
}
