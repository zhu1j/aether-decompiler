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
 * 由一段生成的源码文本映射回产生它的字节码的不可变映射。
 *
 * <p>硬性约束 #3（配置/映射在 Phase2 冻结）使这成为一项一等的、始终存在的
 * 能力，而非可选插件。该映射是 GUI 点击跳转字节码导航以及任何未来调试器或
 * AI 辅助逆向工程工具的基础。</p>
 *
 * <p>该接口以中性的 {@link Span} 表述，因此插件 API 永不依赖内核的具体
 * AST 类型。</p>
 *
 * <p>故事类比：一本译作书末的交叉引用索引，告诉你每句译文来自原文的
 * 哪一段。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public interface SourceMapping {

    /**
     * 一段半开区间的生成文本（按行、列），绑定到一段原始字节码
     * （按指令索引）以及一个可选的源码行。
     *
     * @param generatedStartLine 首生成行（从 1 开始，含首）
     * @param generatedStartCol  首生成列（从 0 开始，含首）
     * @param generatedEndLine   末生成行（从 1 开始，含尾）
     * @param generatedEndCol    末生成列（从 0 开始，不含尾）
     * @param insnStart          首指令索引（含首）
     * @param insnEnd            末指令索引（不含尾）
     * @param sourceLine         原始源码行；若未知则为 {@code -1}
     */
    record Span(int generatedStartLine, int generatedStartCol,
                int generatedEndLine, int generatedEndCol,
                int insnStart, int insnEnd, int sourceLine) {
    }

    /**
     * @return 本映射中的全部 span，按生成顺序
     */
    java.util.List<Span> spans();

    /**
     * 查找产生了某个生成位置的字节码 span。
     *
     * @param generatedLine 从 1 开始的生成行
     * @param generatedCol  从 0 开始的生成列
     * @return 匹配的 span；若无则为 {@code null}
     */
    Span atGenerated(int generatedLine, int generatedCol);
}
