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
 * 在引擎事件流水线中传输的中间表示的语义标记。
 *
 * <p>内核坚持一条硬性约束：它从不使用 Java 语法。因此它的中间结果只由
 * {@link IRKind} 中抽象的、与语言无关的种类来描述，而一个 {@code IRObject}
 * 只是一个能够报告自己当前属于哪种类型的值。具体类类型存在于内核中；
 * 插件通过事件观察它们，而 API 模块无需依赖内核。</p>
 *
 * <p>故事类比：传送带上每个包裹都贴着一张朴素标签 —— “原金属”“机加工件”
 * “成品单元” —— 但标签从不指明最终产品是哪个品牌。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public interface IRObject {

    /**
     * @return 本中间对象的、与语言无关的种类
     */
    IRKind kind();
}
