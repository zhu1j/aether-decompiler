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
package com.aetherdecompiler.core.ssa;

import java.util.Objects;

/**
 * SSA 形式中的一个<strong>定值（definition）标识</strong>：一个局部变量槽位的一次
 * 特定写入。
 *
 * <p>经典 SSA 的核心思想是“每个变量只被赋值一次”。JVM 字节码是命令式的：同一个
 * 局部变量槽（例如 {@code iload_2}/{@code istore_2} 里的槽 2）在方法执行过程中会被
 * 反复覆盖。SSA 通过给每一次写入分配一个<em>版本号</em>，把“一个会被覆盖的槽”拆成
 * 一串“只写一次的值”，从而让后续的定值-使用链、常量传播、死代码消除成为一次简单的
 * 图遍历。</p>
 *
 * <p>一个 {@code SsaVariable} 由 {@code (slot, version)} 唯一确定，其显示名形如
 * {@code v2_3}——“槽 2 的第 3 版”。版本 {@code 0} 保留给方法入口处的隐式定值
 * （参数或未初始化的入口值）。</p>
 *
 * <p>故事类比：同一个储物柜（槽）被反复使用。每次你往里放东西，管理员就贴一张新编号
 * 的标签；旧标签仍然能指向当时那个具体物件，不会因为后来换过内容而混淆。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class SsaVariable {

    private final int slot;
    private final int version;

    /**
     * @param slot    局部变量槽位（{@code >= 0}）
     * @param version 版本号，从 {@code 0} 起单调递增
     */
    public SsaVariable(int slot, int version) {
        this.slot = slot;
        this.version = version;
    }

    /** @return 局部变量槽位 */
    public int slot() {
        return slot;
    }

    /** @return 版本号 */
    public int version() {
        return version;
    }

    /**
     * @return 人类可读的 SSA 名，例如 {@code "v2_3"}
     */
    public String name() {
        return "v" + slot + "_" + version;
    }

    /** @return 若该值处于方法入口（版本 0）则返回 {@code true} */
    public boolean isEntryValue() {
        return version == 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SsaVariable other)) {
            return false;
        }
        return slot == other.slot && version == other.version;
    }

    @Override
    public int hashCode() {
        return Objects.hash(slot, version);
    }

    @Override
    public String toString() {
        return name();
    }
}
