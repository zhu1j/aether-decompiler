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
 * 扩展点 #2 —— 一次 AST 到 AST 的变换。
 *
 * <p>变换会重写与平台无关的 AST 以改善可读性：去混淆、常量折叠、变量名推断、
 * 死分支裁剪。由于它作用于中性的 AST，变换与语言无关且可组合：多个变换在
 * 渲染器运行之前串联执行。</p>
 *
 * <p>内核仅通过不透明的 {@link IRObject} 句柄暴露 AST 节点，因此变换永远
 * 不需要对内核的编译期依赖。Phase 3+ 提供具体 AST；Phase 0/1 交付本契约，
 * 但尚未实际启用。</p>
 *
 * <p>故事类比：一位编辑，在书稿付印之前修订其结构。多位编辑可以依次修订；
 * 他们中没人决定最终的印刷厂。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public interface AstTransformPlugin {

    /**
     * @return 唯一、稳定的插件 id，例如 {@code "ast.simplify"}
     */
    String id();

    /**
     * @return 人类可读的名称
     */
    String displayName();

    /**
     * @return 一个排序提示；数值越小在变换链中越先运行
     */
    default int order() {
        return 100;
    }

    /**
     * 变换一个 AST，返回可能是新的 AST。
     *
     * @param ast 输入 AST，以中性的 {@link IRObject} 表示
     * @param ctx 宿主上下文
     * @return 变换后的 AST（若未改变，可能是同一实例）
     */
    IRObject transform(IRObject ast, PluginContext ctx);
}
