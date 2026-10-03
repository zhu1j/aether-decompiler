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

/**
 * 工作台的视图层：三视图反编译工作区的各个独立面板。这里的每个类都是一个
 * 纯 JavaFX 部件，渲染应用层交给它的任何内容，并针对用户意图抛出回调；
 * 它们都不引用内核引擎，因此整个视图层都可以由 CSS 重新设定样式，
 * 或在不触碰反编译逻辑的情况下被替换。
 *
 * @author Jerry Zhu (Zeek)
 */
package com.aetherdecompiler.gui.view;
