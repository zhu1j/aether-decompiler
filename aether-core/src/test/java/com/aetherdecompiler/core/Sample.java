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
package com.aetherdecompiler.core;

/**
 * 一个刻意包含多种分支的小示例，供内核单元测试使用。
 *
 * <p>它包含一个循环、一个条件判断、一个 switch、一个 try/catch，以及一个
 * 无代码的方法（接口），从而让 CFG 构建器与支配树在多种真实控制流形态下
 * 得到检验。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class Sample {

    /** 一个字段，用于检验字段建模。 */
    public int counter;

    /** 一个无代码接口，用于检验抽象方法。 */
    public interface Transformer {
        /**
         * @param x 输入
         * @return 变换后的值
         */
        int apply(int x);
    }

    /**
     * 用循环与守卫求 1..n 之和。
     *
     * @param n 上界
     * @return 和；对负数输入返回 -1
     */
    public int sumTo(int n) {
        if (n < 0) {
            return -1;
        }
        int total = 0;
        for (int i = 1; i <= n; i++) {
            total += i;
            if (total > 1_000_000) {
                break;
            }
        }
        return total;
    }

    /**
     * 对小编号整数做 switch。
     *
     * @param kind 选择子
     * @return 一个标签
     */
    public String label(int kind) {
        switch (kind) {
            case 0:
                return "zero";
            case 1:
                return "one";
            case 2:
                return "two";
            default:
                return "many";
        }
    }

    /**
     * 一个 try/catch，用于检验异常边。
     *
     * @param s 候选数字文本
     * @return 解析后的值；失败时返回 -1
     */
    public int parse(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
