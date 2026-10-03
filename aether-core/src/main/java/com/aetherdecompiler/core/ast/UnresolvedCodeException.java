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
 * 严格模式（{@link OpaqueMode#STRICT}）下遇到无法精确重建的字节码时抛出的异常。
 *
 * <p>它携带出问题指令的索引与简短上下文，便于调用方定位、记录与回归比对。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public class UnresolvedCodeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int insnIndex;
    private final String detail;

    /**
     * @param insnIndex 无法重建的指令索引
     * @param detail    简短上下文，例如指令助记符或来源说明
     */
    public UnresolvedCodeException(int insnIndex, String detail) {
        super("严格模式下无法精确重建指令 #" + insnIndex
                + (detail == null || detail.isEmpty() ? "" : "（" + detail + "）"));
        this.insnIndex = insnIndex;
        this.detail = detail == null ? "" : detail;
    }

    /** @return 无法重建的指令索引 */
    public int insnIndex() {
        return insnIndex;
    }

    /** @return 简短上下文 */
    public String detail() {
        return detail;
    }
}
