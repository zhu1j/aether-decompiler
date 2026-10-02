/*
 * aether-decompiler — an independent, reusable JVM decompilation engine.
 * Copyright 2026 Jerry Zhu (Zeek) <zhujiejava1@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author Jerry Zhu (Zeek)
 * "Run the Code, Run the World!"
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
 * Official {@link RenderPlugin}: renders a control-flow graph as Graphviz DOT.
 *
 * <p>This is the architectural proof-of-concept for Phase 1. The kernel never
 * knew DOT existed; it produced a neutral {@link CfgView} and this plugin mapped
 * it to a text notation. A future Java renderer plugs into exactly the same
 * seam. The plugin depends only on {@code aether-plugin-api}.</p>
 *
 * <p>Story analogy: the same blueprint can be printed as a wiring diagram. The
 * architect never drew a wire; the printer did.</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class DotRenderPlugin implements RenderPlugin {

    /** Stable plugin id. */
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
     * Render a neutral CFG view to DOT text. Public so tests and applications
     * can call it directly without the plugin host.
     *
     * @param cfg the control-flow graph view
     * @return Graphviz DOT source
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
