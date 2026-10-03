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
package com.aetherdecompiler.core.mapping;

import com.aetherdecompiler.api.SourceMapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link SourceMapping} 的默认实现：一组建好即冻结的 {@link Span}，外加双向查询索引。
 *
 * <p>这是 Phase 4 的核心交付物。它把“渲染器写下的每一段文本”与“产生它的字节码指令
 * 区间”逐条绑定，使 GUI 能够：点击某行源码 → 立刻定位到对应字节码；或选中某条字节码
 * → 高亮其全部来源文本。这一切之所以可能，是因为 AST 节点从一开始就携带了它的指令
 * 来源（{@code firstInsn/lastInsn}）—— 映射从未真正“丢失”，只是在此被物化成索引。</p>
 *
 * <p>为了支持“指令 → 文本”这个反向查询（接口本身只承诺正向的 {@code atGenerated}），
 * 本实现额外维护了一张从指令索引到 span 的反查表 {@link #atInsn(int)}。这属于对接口的
 * 诚实的<em>能力增强</em>，而非替代。</p>
 *
 * <p>故事类比：一本译作书末的双向索引 —— 既可以从译文找原文，也可以从原文找译文。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class DefaultSourceMapping implements SourceMapping {

    private final List<Span> spans;
    private final Map<Long, Span> byStart;

    private DefaultSourceMapping(List<Span> spans) {
        this.spans = List.copyOf(spans);
        Map<Long, Span> index = new HashMap<>();
        for (Span s : spans) {
            index.putIfAbsent(key(s.generatedStartLine(), s.generatedStartCol()), s);
        }
        this.byStart = Collections.unmodifiableMap(index);
    }

    /**
     * @param spans 全部 span（会在构造时复制并冻结）
     * @return 不可变映射
     */
    public static DefaultSourceMapping of(List<Span> spans) {
        return new DefaultSourceMapping(spans);
    }

    /** @return 一个构建器 */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public List<Span> spans() {
        return spans;
    }

    @Override
    public Span atGenerated(int generatedLine, int generatedCol) {
        Span exact = byStart.get(key(generatedLine, generatedCol));
        if (exact != null) {
            return exact;
        }
        // 退而求其次：命中同一行、列落在其区间内的 span。
        for (Span s : spans) {
            if (s.generatedStartLine() == generatedLine
                    && generatedCol >= s.generatedStartCol()
                    && generatedCol < Math.max(s.generatedEndCol(), s.generatedStartCol() + 1)) {
                return s;
            }
        }
        return null;
    }

    /**
     * 反向查询：给定一条字节码指令索引，返回所有覆盖它的 span。
     *
     * @param insnIndex 指令索引
     * @return 覆盖该指令的 span 列表（永不为 {@code null}）
     */
    public List<Span> atInsn(int insnIndex) {
        List<Span> out = new ArrayList<>();
        for (Span s : spans) {
            if (insnIndex >= s.insnStart() && insnIndex < Math.max(s.insnEnd(), s.insnStart() + 1)) {
                out.add(s);
            }
        }
        return out;
    }

    /** @return span 数量 */
    public int size() {
        return spans.size();
    }

    private static long key(int line, int col) {
        return ((long) line << 32) ^ (col & 0xffffffffL);
    }

    @Override
    public String toString() {
        return "DefaultSourceMapping{" + spans.size() + " spans}";
    }

    /**
     * {@link DefaultSourceMapping} 的流式构建器。
     *
     * @author Jerry Zhu (Zeek)
     */
    public static final class Builder {
        private final List<Span> spans = new ArrayList<>();

        /**
         * 追加一个 span。
         *
         * @param generatedStartLine 首生成行，从 1 开始
         * @param generatedStartCol  首生成列，从 0 开始
         * @param generatedEndLine   末生成行
         * @param generatedEndCol    末生成列（不含尾）
         * @param insnStart          首指令索引
         * @param insnEnd            末指令索引（不含尾）
         * @param sourceLine         原始源码行；未知为 {@code -1}
         * @return 本构建器
         */
        public Builder add(int generatedStartLine, int generatedStartCol,
                           int generatedEndLine, int generatedEndCol,
                           int insnStart, int insnEnd, int sourceLine) {
            spans.add(new Span(generatedStartLine, generatedStartCol,
                    generatedEndLine, generatedEndCol, insnStart, insnEnd, sourceLine));
            return this;
        }

        /** @return 冻结的不可变映射 */
        public DefaultSourceMapping build() {
            return new DefaultSourceMapping(spans);
        }
    }
}
