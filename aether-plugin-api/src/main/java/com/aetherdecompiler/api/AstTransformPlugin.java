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
 * Extension point #2 — an AST-to-AST transform.
 *
 * <p>Transforms rewrite the platform-independent AST to improve readability:
 * de-obfuscation, constant folding, variable-name inference, dead-branch
 * pruning. Because it operates on the neutral AST, a transform is language
 * agnostic and composable: several transforms chain before a renderer runs.</p>
 *
 * <p>The kernel exposes AST nodes only through the opaque {@link IRObject}
 * handle, so a transform never needs a compile-time dependency on the kernel.
 * Phase 3+ supplies the concrete AST; Phase 0/1 ship this contract and leave it
 * unexercised.</p>
 *
 * <p>Story analogy: an editor revising a manuscript's structure before it goes
 * to press. Multiple editors can revise in sequence; none of them chooses the
 * final printing house.</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public interface AstTransformPlugin {

    /**
     * @return a unique, stable plugin id, e.g. {@code "ast.simplify"}
     */
    String id();

    /**
     * @return a human-readable name
     */
    String displayName();

    /**
     * @return an ordering hint; lower runs earlier in the transform chain
     */
    default int order() {
        return 100;
    }

    /**
     * Transform an AST, returning the possibly-new AST.
     *
     * @param ast the input AST as a neutral {@link IRObject}
     * @param ctx the host context
     * @return the transformed AST (may be the same instance if unchanged)
     */
    IRObject transform(IRObject ast, PluginContext ctx);
}
