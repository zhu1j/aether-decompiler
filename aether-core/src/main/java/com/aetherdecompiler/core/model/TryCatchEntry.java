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
package com.aetherdecompiler.core.model;

import java.util.Objects;

/**
 * 方法异常表中的一条不可变记录。
 *
 * <p>记录 try 区间、处理器的入口偏移以及被捕获的类型（对 {@code finally}/
 * 捕获所有的处理器则为 {@code null}）。异常边在控制流构建中是一等的，因此它们
 * 被显式建模，而不是按需推导。</p>
 *
 * <p>故事类比：墙上的逃生图 —— “如果在 10 号与 20 号房间之间出事，
 * 就从这个出口撤离，针对这类火情。”</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class TryCatchEntry {

    private final int startIndex;
    private final int endIndex;
    private final int handlerIndex;
    private final String catchType;

    /**
     * @param startIndex   受保护区间含首的起始指令索引
     * @param endIndex     受保护区间不含尾的结束指令索引
     * @param handlerIndex 处理器入口的指令索引
     * @param catchType    被捕获的内部类型名；捕获所有时为 {@code null}
     */
    public TryCatchEntry(int startIndex, int endIndex, int handlerIndex, String catchType) {
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.handlerIndex = handlerIndex;
        this.catchType = catchType;
    }

    /** @return 受保护区间含首的起始指令索引 */
    public int startIndex() {
        return startIndex;
    }

    /** @return 受保护区间不含尾的结束指令索引 */
    public int endIndex() {
        return endIndex;
    }

    /** @return 处理器入口指令索引 */
    public int handlerIndex() {
        return handlerIndex;
    }

    /** @return 被捕获的内部类型名；捕获所有时为 {@code null} */
    public String catchType() {
        return catchType;
    }

    /** @return 当这是捕获所有 / {@code finally} 处理器时为 {@code true} */
    public boolean isCatchAll() {
        return catchType == null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TryCatchEntry other)) {
            return false;
        }
        return startIndex == other.startIndex
                && endIndex == other.endIndex
                && handlerIndex == other.handlerIndex
                && Objects.equals(catchType, other.catchType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startIndex, endIndex, handlerIndex, catchType);
    }

    @Override
    public String toString() {
        return "try[" + startIndex + "," + endIndex + ") -> " + handlerIndex
                + " (" + (catchType == null ? "any" : catchType) + ")";
    }
}
