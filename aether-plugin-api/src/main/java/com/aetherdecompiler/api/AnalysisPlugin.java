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

import java.util.Map;

/**
 * 扩展点 #4 —— 针对中间模型的附加分析。
 *
 * <p>分析产出补充事实（调用图、混淆检测、度量），而不触碰流水线的主路径。
 * 硬性约束 #7 把混淆分析定为插件的关注点而非内核的关注点；这里便是它的
 * 指定归属。</p>
 *
 * <p>结果以中性的“名称到值”映射返回，因此插件 API 永不依赖任何与分析
 * 相关的类型。</p>
 *
 * <p>故事类比：一位质检员，他在产线上测量零件并提交报告，
 * 但不改动零件本身。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public interface AnalysisPlugin {

    /**
     * @return 唯一、稳定的插件 id，例如 {@code "analysis.callgraph"}
     */
    String id();

    /**
     * @return 人类可读的名称
     */
    String displayName();

    /**
     * 分析一个中间对象（通常是类或方法模型）。
     *
     * @param subject 要分析的对象，以中性的 {@link IRObject} 表示
     * @param ctx     宿主上下文
     * @return 名称到结果的映射（永不为 {@code null}）
     */
    Map<String, Object> analyse(IRObject subject, PluginContext ctx);
}
