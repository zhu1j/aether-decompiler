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

import java.util.Map;

/**
 * Extension point #4 — an additional analysis over the intermediate model.
 *
 * <p>Analyses produce supplementary facts (call graphs, obfuscation detection,
 * metrics) without touching the pipeline's main path. Hard constraint #7 makes
 * obfuscation analysis a plugin concern, not a kernel concern; this is its
 * designated home.</p>
 *
 * <p>Results are returned as a neutral name-to-value map so the plugin API
 * never depends on any analysis-specific type.</p>
 *
 * <p>Story analogy: a quality-control inspector who measures parts on the line
 * and files a report, without altering the parts themselves.</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public interface AnalysisPlugin {

    /**
     * @return a unique, stable plugin id, e.g. {@code "analysis.callgraph"}
     */
    String id();

    /**
     * @return a human-readable name
     */
    String displayName();

    /**
     * Analyse an intermediate object (typically a class or method model).
     *
     * @param subject the object to analyse, as a neutral {@link IRObject}
     * @param ctx     the host context
     * @return a name-to-result map (never {@code null})
     */
    Map<String, Object> analyse(IRObject subject, PluginContext ctx);
}
