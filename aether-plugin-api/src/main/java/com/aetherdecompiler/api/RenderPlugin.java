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
package com.aetherdecompiler.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Extension point #3 — an AST-to-text renderer.
 *
 * <p>Renderers turn the kernel's platform-independent AST into a concrete
 * textual notation (Java source, Graphviz DOT, pseudocode, Kotlin...). Because
 * the AST never contains Java keywords, a renderer is the <em>only</em> place
 * where an output language is known. This is the concrete embodiment of hard
 * constraint #1.</p>
 *
 * <p>A renderer receives the AST through a neutral, untyped handle — the
 * kernel's AST node type is passed as {@link IRObject} — and returns
 * {@link SourceTree} units. It never writes files.</p>
 *
 * <p>Story analogy: the same architectural blueprint (the AST) can be printed
 * as a floor plan, a 3-D render, or a wiring diagram — different printers, one
 * blueprint.</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public interface RenderPlugin {

    /**
     * @return a unique, stable plugin id, e.g. {@code "render.java"}
     */
    String id();

    /**
     * @return a human-readable name
     */
    String displayName();

    /**
     * @return the file extension this renderer emits, e.g. {@code "java"}
     */
    String fileExtension();

    /**
     * Render one decompiled class.
     *
     * @param ast     the class AST as a neutral {@link IRObject}
     * @param ctx     the host context
     * @return one or more rendered units (never {@code null})
     */
    List<SourceTree> render(IRObject ast, PluginContext ctx);

    /**
     * Convenience: render a class to a single string.
     *
     * @param ast the class AST
     * @param ctx the host context
     * @return the concatenated rendering of all units
     */
    default String renderToString(IRObject ast, PluginContext ctx) {
        StringBuilder sb = new StringBuilder();
        List<SourceTree> trees = render(ast, ctx);
        List<SourceTree> safe = trees == null ? Collections.emptyList() : new ArrayList<>(trees);
        for (SourceTree tree : safe) {
            sb.append(tree.content());
        }
        return sb.toString();
    }
}
