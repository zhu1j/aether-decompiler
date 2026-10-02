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
 * The language-neutral kinds of intermediate representations the engine
 * produces, in pipeline order.
 *
 * <p>This enumeration is the observable contract of the pipeline. It never
 * mentions Java: it stops at "abstract syntax tree". A Java renderer maps the
 * AST to Java; a future Kotlin renderer maps the same AST to Kotlin.</p>
 *
 * <p>Story analogy: the assembly line's stations — raw bytes, blocks, single
 * assignment form, typed form, abstract syntax. What the customer orders at the
 * end (Java, DOT, pseudocode) is decided by the render station, not by these.</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public enum IRKind {

    /** Raw, unparsed class bytes (the input). */
    BYTES,

    /** An immutable class model holding metadata and methods. */
    CLASS_MODEL,

    /** An immutable method model (instruction list, maxs, exception table). */
    METHOD_MODEL,

    /** A control-flow graph of basic blocks for one method. */
    CFG,

    /** A dominator tree derived from a CFG. */
    DOMINATOR_TREE,

    /** A static-single-assignment form of one method. */
    SSA,

    /** A platform-independent abstract syntax tree (not Java-specific). */
    AST
}
