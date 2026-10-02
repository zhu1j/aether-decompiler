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
 * 由 {@link RenderPlugin} 产出的单个渲染输出单元。
 *
 * <p>这是已冻结的“输出模型”契约：渲染器自身从不写磁盘。它返回一个或多个
 * {@code SourceTree} 单元（文件的路径、其文本内容，以及可选的回指字节码的
 * 源码映射）。应用层（CLI/GUI）决定写入、显示还是流式输出它们。这让内核与
 * 渲染插件不含任何 I/O 关注点。</p>
 *
 * <p>故事类比：渲染器是一台产出纸张的打印机。它从不决定纸张归入何处 ——
 * 前台决定。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class SourceTree {

    private final String path;
    private final String content;
    private final SourceMapping mapping;

    private SourceTree(String path, String content, SourceMapping mapping) {
        this.path = path;
        this.content = content;
        this.mapping = mapping;
    }

    /**
     * @param path    相对输出路径，例如 {@code com/foo/Bar.java}
     * @param content 渲染后的文本
     * @param mapping 源码映射；不可用时为 {@code null}
     * @return 新的渲染单元
     */
    public static SourceTree of(String path, String content, SourceMapping mapping) {
        return new SourceTree(path, content, mapping);
    }

    /** @return 相对输出路径 */
    public String path() {
        return path;
    }

    /** @return 渲染后的文本内容 */
    public String content() {
        return content;
    }

    /** @return 源码映射；若渲染器未产出则为 {@code null} */
    public SourceMapping mapping() {
        return mapping;
    }

    @Override
    public String toString() {
        return "SourceTree{" + path + ", " + (content == null ? 0 : content.length()) + " chars}";
    }
}
