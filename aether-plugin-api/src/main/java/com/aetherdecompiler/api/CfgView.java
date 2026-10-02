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

import java.util.List;

/**
 * 控制流图的中性、只读视图。
 *
 * <p>本接口是让插件能够消费内核构建的 CFG <em>而不依赖内核</em>的关键所在。
 * 插件 API 声明其形态；内核的具体 {@code ControlFlowGraph} 实现它。因此插件
 * 只会命名 {@code aether}-API 类型。</p>
 *
 * <p>故事类比：递给承包商的一把标准量尺。承包商通过量尺读取建筑的尺寸；
 * 他们永远不需要建筑师私有的 CAD 软件。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public interface CfgView extends IRObject {

    /** @return 所属类的内部名 */
    String ownerClass();

    /** @return 方法标识符（{@code name + descriptor}） */
    String methodId();

    /**
     * @return 每条指令的显示文本，按指令 id 索引
     */
    List<String> insnTexts();

    /**
     * @return 图的各个块，按 id 顺序
     */
    List<? extends Block> blocks();

    /**
     * 单个基本块的中性、只读视图。
     *
     * @author Jerry Zhu (Zeek)
     */
    interface Block {

        /** @return 块 id */
        int id();

        /** @return 含首的起始指令索引 */
        int firstInsn();

        /** @return 含尾的结束指令索引 */
        int lastInsn();

        /** @return 普通后继块 id */
        List<Integer> successors();

        /** @return 异常处理器块 id */
        List<Integer> exceptionSuccessors();

        /** @return 是否为方法入口块 */
        boolean isEntry();
    }
}
