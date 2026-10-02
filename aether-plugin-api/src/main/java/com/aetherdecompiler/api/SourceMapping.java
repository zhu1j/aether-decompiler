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

/**
 * An immutable map from a range of generated source text back to the bytecode
 * that produced it.
 *
 * <p>Hard constraint #3 (configuration/mapping frozen at Phase2) makes this a
 * first-class, always-present capability rather than an optional plugin. The
 * mapping is the foundation of GUI click-to-bytecode navigation and of any
 * future debugger or AI-assisted reverse-engineering tool.</p>
 *
 * <p>The interface is expressed in terms of neutral {@link Span}s so the plugin
 * API never depends on the kernel's concrete AST types.</p>
 *
 * <p>Story analogy: the cross-reference index at the back of a translated book,
 * telling you which paragraph of the original each translated sentence came
 * from.</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public interface SourceMapping {

    /**
     * A half-open range of generated text (by line and column) tied to a range
     * of original bytecode (by instruction index) and an optional source line.
     *
     * @param generatedStartLine first generated line (1-based, inclusive)
     * @param generatedStartCol  first generated column (0-based, inclusive)
     * @param generatedEndLine   last generated line (1-based, inclusive)
     * @param generatedEndCol    last generated column (0-based, exclusive)
     * @param insnStart          first instruction index (inclusive)
     * @param insnEnd            last instruction index (exclusive)
     * @param sourceLine         original source line, or {@code -1} if unknown
     */
    record Span(int generatedStartLine, int generatedStartCol,
                int generatedEndLine, int generatedEndCol,
                int insnStart, int insnEnd, int sourceLine) {
    }

    /**
     * @return all spans in this mapping, in generated order
     */
    java.util.List<Span> spans();

    /**
     * Look up the bytecode span that produced a generated position.
     *
     * @param generatedLine the 1-based generated line
     * @param generatedCol  the 0-based generated column
     * @return the matching span, or {@code null} if none
     */
    Span atGenerated(int generatedLine, int generatedCol);
}
