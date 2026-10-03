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

/**
 * 处理“无法精确重建”的字节码片段时所采取的策略。
 *
 * <p>反编译器不可能对任意字节码都还原出完美的源码。当遇到无法精确重建的指令时，
 * 库需要让调用方自行选择立场：是“尽量输出、容错优先”，还是“宁缺毋滥、严格把关”。
 * 这正是本枚举要表达的分野。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public enum OpaqueMode {

    /**
     * 宽松模式：无法重建的指令以注释占位（{@code /* unresolved: ... *}{@code /}）保留，
     * 尽最大努力让整份源码保持可编译、可读。这是默认值，适合日常浏览与批量反编译。
     */
    LENIENT,

    /**
     * 严格模式：一旦遇到无法精确重建的指令，立即抛出
     * {@link UnresolvedCodeException}，并带上指令索引与上下文。
     *
     * <p>适合把反编译结果用于自动化校验、回归比对，或要求“要么全对、要么报错”的场景。</p>
     */
    STRICT
}
