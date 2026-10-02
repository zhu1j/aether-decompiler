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
package com.aetherdecompiler.plugins.render.dot;

import com.aetherdecompiler.api.CfgView;
import com.aetherdecompiler.api.IRObject;
import com.aetherdecompiler.api.PluginContext;
import com.aetherdecompiler.api.RenderPlugin;
import com.aetherdecompiler.api.SourceTree;

import java.util.ArrayList;
import java.util.List;

/**
 * 官方 {@link RenderPlugin}：把控制流图渲染为 Graphviz DOT。
 *
 * <p>这是 Phase 1 的架构概念验证。内核从不知道 DOT 的存在；它产出一个中性的
 * {@link CfgView}，而本插件把它映射为一种文本记号。未来的 Java 渲染器会接入
 * 完全相同的接缝。本插件仅依赖 {@code aether-plugin-api}。</p>
 *
 * <p>故事类比：同一张蓝图可以打印成布线图。建筑师从未画过一根线；
 * 是打印机画的。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class DotRenderPlugin implements RenderPlugin {

    /** 稳定的插件 id。 */
    public static final String ID = "render.dot";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "Graphviz DOT Renderer";
    }

    @Override
    public String fileExtension() {
        return "dot";
    }

    @Override
    public List<SourceTree> render(IRObject ast, PluginContext ctx) {
        if (!(ast instanceof CfgView cfg)) {
            return List.of();
        }
        String dot = renderCfg(cfg);
        String path = sanitize(cfg.ownerClass()) + "." + sanitize(cfg.methodId()) + ".dot";
        return List.of(SourceTree.of(path, dot, null));
    }

    /**
     * 把中性的 CFG 视图渲染为 DOT 文本。公开是为了让测试与应用
     * 无需插件宿主即可直接调用它。
     *
     * @param cfg 控制流图视图
     * @return Graphviz DOT 源码
     */
    public String renderCfg(CfgView cfg) {
        List<String> insns = cfg.insnTexts();
        StringBuilder sb = new StringBuilder();
        sb.append("digraph \"").append(cfg.ownerClass()).append('.').append(cfg.methodId()).append("\" {\n");
        sb.append("  rankdir=TB;\n");
        sb.append("  node [shape=box, fontname=\"monospace\"];\n");

        for (CfgView.Block block : cfg.blocks()) {
            sb.append("  B").append(block.id()).append(" [label=\"");
            sb.append(escape("B" + block.id())).append("\\l");
            List<String> lines = new ArrayList<>();
            for (int i = block.firstInsn(); i <= block.lastInsn() && i < insns.size(); i++) {
                lines.add(insns.get(i));
            }
            sb.append(escape(String.join("\\l", lines))).append("\\l");
            sb.append("\"");
            if (block.isEntry()) {
                sb.append(", style=filled, fillcolor=\"#d0f0d0\"");
            }
            sb.append("];\n");
        }

        for (CfgView.Block block : cfg.blocks()) {
            for (int succ : block.successors()) {
                sb.append("  B").append(block.id()).append(" -> B").append(succ).append(";\n");
            }
            for (int succ : block.exceptionSuccessors()) {
                sb.append("  B").append(block.id()).append(" -> B").append(succ)
                        .append(" [style=dashed, color=red, label=\"exception\"];\n");
            }
        }
        sb.append("}\n");
        return sb.toString();
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String sanitize(String s) {
        return s.replaceAll("[^A-Za-z0-9_.-]", "_");
    }
}
