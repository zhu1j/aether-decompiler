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

import java.util.Objects;

/**
 * 局部变量表（LocalVariableTable）中的一条不可变记录。
 *
 * <p>它把“某个局部槽位在某段字节码区间内叫什么名字、是什么类型”这件事，
 * 从可选调试属性提升为一等的模型数据。没有它，反编译输出只能退化为
 * {@code v1}/{@code v2}；有了它，方法签名与体中的引用才能恢复成源码里的
 * 真实变量名。</p>
 *
 * <p>故事类比：一张给舞台演员准备的化名对照表 —— “2 号槽位在 10~40 号台词间
 * 扮演‘count’”。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class LocalVariable implements IRObject {

    private final String name;
    private final String descriptor;
    private final int index;
    private final int startIndex;
    private final int endIndex;

    /**
     * @param name       变量名
     * @param descriptor 类型描述符，例如 {@code "I"} 或 {@code "Ljava/lang/String;"}
     * @param index      局部变量槽位号
     * @param startIndex 生效区间含首的起始指令索引
     * @param endIndex   生效区间不含尾的结束指令索引
     */
    public LocalVariable(String name, String descriptor, int index, int startIndex, int endIndex) {
        this.name = name;
        this.descriptor = descriptor;
        this.index = index;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
    }

    /** @return 变量名 */
    public String name() {
        return name;
    }

    /** @return 类型描述符 */
    public String descriptor() {
        return descriptor;
    }

    /** @return 局部变量槽位号 */
    public int index() {
        return index;
    }

    /** @return 生效区间含首的起始指令索引 */
    public int startIndex() {
        return startIndex;
    }

    /** @return 生效区间不含尾的结束指令索引 */
    public int endIndex() {
        return endIndex;
    }

    @Override
    public IRKind kind() {
        return IRKind.METHOD_MODEL;
    }

    @Override
    public String toString() {
        return "LocalVariable{" + index + ": " + descriptor + " " + name
                + " [" + startIndex + "," + endIndex + ")}";
    }
}
