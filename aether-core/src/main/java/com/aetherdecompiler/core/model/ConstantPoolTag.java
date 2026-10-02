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
 * JVM 规范定义的常量池标签值。
 *
 * <p>这些常量属于字节码<em>模型</em>，而不属于任何特定的库。把它们记录在这里
 * 可让数据模型的词汇表保持自包含：内核自己为标签命名，而不是向 ASM
 * 借用名称。</p>
 *
 * <p>故事类比：零件目录上的物品编号，写在目录页本身 —— 库存这些零件的仓库
 * 则是另一回事。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class ConstantPoolTag {

    /** UTF-8 字符串。 */
    public static final int UTF8 = 1;
    /** 整数。 */
    public static final int INTEGER = 3;
    /** 单精度浮点数。 */
    public static final int FLOAT = 4;
    /** 长整数。 */
    public static final int LONG = 5;
    /** 双精度浮点数。 */
    public static final int DOUBLE = 6;
    /** 类引用。 */
    public static final int CLASS = 7;
    /** 字符串引用。 */
    public static final int STRING = 8;
    /** 字段引用。 */
    public static final int FIELDREF = 9;
    /** 方法引用。 */
    public static final int METHODREF = 10;
    /** 接口方法引用。 */
    public static final int INTERFACE_METHODREF = 11;
    /** 名称与类型。 */
    public static final int NAME_AND_TYPE = 12;
    /** 方法句柄。 */
    public static final int METHOD_HANDLE = 15;
    /** 方法类型。 */
    public static final int METHOD_TYPE = 16;
    /** 动态计算的常量。 */
    public static final int DYNAMIC = 17;
    /** 动态调用（invokedynamic）。 */
    public static final int INVOKE_DYNAMIC = 18;
    /** 模块。 */
    public static final int MODULE = 19;
    /** 包。 */
    public static final int PACKAGE = 20;

    private ConstantPoolTag() {
        throw new AssertionError("No instances.");
    }
}
