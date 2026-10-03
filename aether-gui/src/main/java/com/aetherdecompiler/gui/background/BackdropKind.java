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
package com.aetherdecompiler.gui.background;

/**
 * 背景所使用的媒体类型。
 *
 * @author Jerry Zhu (Zeek)
 */
public enum BackdropKind {
    /** 静止图像（png / jpg / gif / bmp / webp）。 */
    IMAGE,
    /** 循环视频（mp4 / m4v / mov / webm / flv）。 */
    VIDEO,
    /** 网页型壁纸（html / htm，通常是一个 Wallpaper Engine 的 {@code type=web} 工程）。 */
    WEB,
    /** 未设置任何内容。 */
    NONE
}
