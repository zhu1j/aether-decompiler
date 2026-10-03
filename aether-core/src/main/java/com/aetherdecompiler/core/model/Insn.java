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

import com.aetherdecompiler.api.IRKind;
import com.aetherdecompiler.api.IRObject;

import java.util.List;
import java.util.Objects;

/**
 * 不可变、字节码级的指令模型。
 *
 * <p>它刻意贴近机器：一个操作码编号、一个供人显示的渲染后操作数字符串，以及
 * —— 对控制流构建至关重要的 —— 该指令可以跳转到的字节码偏移集合。它完全不
 * 携带 Java 语义。</p>
 *
 * <p>不可变性是硬性约束：模型可以被缓存、跨线程共享，并可直接作为 map 的键
 * 而无需防御性拷贝。</p>
 *
 * <p>故事类比：自动演奏钢琴卷帘上的一张打孔卡。卡片记录一个孔的位置及其触发
 * 的内容；它并不说明曲子是华尔兹还是进行曲。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class Insn implements IRObject {

    private final int index;
    private final int opcode;
    private final String mnemonic;
    private final String operand;
    private final int[] branchTargets;

    /**
     * @param index         指令在方法中的序号位置
     * @param opcode        JVM 操作码编号
     * @param mnemonic      操作码助记符，例如 {@code "if_icmpgt"}
     * @param operand       渲染后的操作数字符串，可能为空
     * @param branchTargets 该指令可能跳转到的字节码偏移；
     *                      直线执行指令为空
     */
    public Insn(int index, int opcode, String mnemonic, String operand, int[] branchTargets) {
        this.index = index;
        this.opcode = opcode;
        this.mnemonic = Objects.requireNonNull(mnemonic, "mnemonic");
        this.operand = operand == null ? "" : operand;
        this.branchTargets = branchTargets == null ? new int[0] : branchTargets.clone();
    }

    /** @return 指令在方法中的序号位置 */
    public int index() {
        return index;
    }

    /** @return JVM 操作码编号 */
    public int opcode() {
        return opcode;
    }

    /** @return 操作码助记符 */
    public String mnemonic() {
        return mnemonic;
    }

    /** @return 渲染后的操作数字符串，可能为空 */
    public String operand() {
        return operand;
    }

    /**
     * @return 跳转目标的防御性副本；直线执行时为空
     */
    public int[] branchTargets() {
        return branchTargets.clone();
    }

    /** @return 若该指令可以分支则返回 {@code true} */
    public boolean isBranch() {
        return branchTargets.length > 0;
    }

    /** @return 显示形式，例如 {@code "17: if_icmpgt 30"} */
    public String display() {
        return index + ": " + mnemonic + (operand.isEmpty() ? "" : " " + operand);
    }

    @Override
    public IRKind kind() {
        return IRKind.METHOD_MODEL;
    }

    @Override
    public String toString() {
        return display();
    }
}
