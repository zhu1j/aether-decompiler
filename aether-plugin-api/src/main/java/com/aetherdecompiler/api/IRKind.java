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
package com.aetherdecompiler.api;

/**
 * 引擎产出的、与语言无关的各种中间表示种类，按流水线顺序排列。
 *
 * <p>本枚举是流水线可观测的契约。它从不提及 Java：它止于“抽象语法树”。
 * 一个 Java 渲染器把 AST 映射为 Java；未来的 Kotlin 渲染器把同一 AST
 * 映射为 Kotlin。</p>
 *
 * <p>故事类比：装配线的各工位 —— 原始字节、基本块、单赋值形式、带类型形式、
 * 抽象语法。顾客最终下单要什么（Java、DOT、伪代码）由渲染工位决定，
 * 而不是由这些工位决定。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public enum IRKind {

    /** 原始的、未解析的类字节（输入）。 */
    BYTES,

    /** 持有元数据与方法的不可变类模型。 */
    CLASS_MODEL,

    /** 不可变方法模型（指令列表、maxs、异常表）。 */
    METHOD_MODEL,

    /** 单个方法的基本块控制流图。 */
    CFG,

    /** 由 CFG 推导出的支配树。 */
    DOMINATOR_TREE,

    /** 单个方法的静态单赋值（SSA）形式。 */
    SSA,

    /** 与平台无关的抽象语法树（非 Java 专有）。 */
    AST
}
