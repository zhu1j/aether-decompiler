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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 扩展点 #3 —— 一个 AST 到文本的渲染器。
 *
 * <p>渲染器把内核与平台无关的 AST 转成具体的文本记号（Java 源码、Graphviz
 * DOT、伪代码、Kotlin……）。由于 AST 从不包含 Java 关键字，渲染器是输出语言
 * 被知晓的<em>唯一</em>场所。这是硬性约束 #1 的具体体现。</p>
 *
 * <p>渲染器通过一个中性的、无类型的句柄接收 AST —— 内核的 AST 节点类型以
 * {@link IRObject} 传入 —— 并返回 {@link SourceTree} 单元。它从不写文件。</p>
 *
 * <p>故事类比：同一张建筑蓝图（AST）可以打印成楼层平面图、三维渲染图或
 * 布线图 —— 不同的打印机，一张蓝图。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public interface RenderPlugin {

    /**
     * @return 唯一、稳定的插件 id，例如 {@code "render.java"}
     */
    String id();

    /**
     * @return 人类可读的名称
     */
    String displayName();

    /**
     * @return 本渲染器输出的文件扩展名，例如 {@code "java"}
     */
    String fileExtension();

    /**
     * 渲染一个已反编译的类。
     *
     * @param ast 类 AST，以中性的 {@link IRObject} 表示
     * @param ctx 宿主上下文
     * @return 一个或多个渲染单元（永不为 {@code null}）
     */
    List<SourceTree> render(IRObject ast, PluginContext ctx);

    /**
     * 便捷方法：把类渲染为单个字符串。
     *
     * @param ast 类 AST
     * @param ctx 宿主上下文
     * @return 所有单元的拼接渲染结果
     */
    default String renderToString(IRObject ast, PluginContext ctx) {
        StringBuilder sb = new StringBuilder();
        List<SourceTree> trees = render(ast, ctx);
        List<SourceTree> safe = trees == null ? Collections.emptyList() : new ArrayList<>(trees);
        for (SourceTree tree : safe) {
            sb.append(tree.content());
        }
        return sb.toString();
    }
}
