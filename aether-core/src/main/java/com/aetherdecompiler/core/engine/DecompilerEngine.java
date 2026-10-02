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
package com.aetherdecompiler.core.engine;

import com.aetherdecompiler.api.ClassSource;
import com.aetherdecompiler.api.EventBus;
import com.aetherdecompiler.core.asm.AsmClassParser;
import com.aetherdecompiler.core.cfg.CfgBuilder;
import com.aetherdecompiler.core.cfg.ControlFlowGraph;
import com.aetherdecompiler.core.event.SimpleEventBus;
import com.aetherdecompiler.core.model.ClassModel;
import com.aetherdecompiler.core.model.MethodModel;

import com.aetherdecompiler.api.AetherEvent;
import com.aetherdecompiler.api.IRKind;

import java.util.List;
import java.util.Optional;

/**
 * The kernel's top-level orchestrator.
 *
 * <p>Owns the pipeline: raw bytes → {@link ClassModel} → per-method
 * {@link ControlFlowGraph}. It is deliberately stateless with respect to run
 * data: all mutable state is created per call and discarded, so a single
 * {@code DecompilerEngine} can be shared across threads and driven in parallel
 * by an application layer. The engine never creates its own thread pool — that
 * is explicitly an upper-layer concern.</p>
 *
 * <p>Story analogy: the engine is the central conveyor controller. It knows the
 * sequence of stations and starts each part down the line, but it holds no part
 * itself — parts flow through, and the controller can run many lines at once.</p>
 *
 * <p>Phase 0 realises bytes → model. Phase 1 extends the same pipeline with CFG
 * construction, which is already wired here.</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class DecompilerEngine {

    private final AsmClassParser parser;
    private final CfgBuilder cfgBuilder;
    private final EventBus eventBus;

    /**
     * Create an engine with a private event bus.
     */
    public DecompilerEngine() {
        this(new SimpleEventBus());
    }

    /**
     * @param eventBus the bus every pipeline event is published to
     */
    public DecompilerEngine(EventBus eventBus) {
        this.eventBus = eventBus;
        this.parser = new AsmClassParser();
        this.cfgBuilder = new CfgBuilder();
    }

    /** @return the engine's event bus, for observers to subscribe to */
    public EventBus eventBus() {
        return eventBus;
    }

    /**
     * Parse raw class bytes into the immutable model.
     *
     * @param bytes the class file bytes
     * @return the class model
     */
    public ClassModel parseClass(byte[] bytes) {
        eventBus.publish(new AetherEvent(AetherEvent.Phase.READ, IRKind.BYTES,
                "read " + (bytes == null ? 0 : bytes.length) + " bytes", null));
        ClassModel model = parser.parse(bytes);
        eventBus.publish(new AetherEvent(AetherEvent.Phase.MODEL, IRKind.CLASS_MODEL,
                "modelled " + model.name() + " (" + model.methods().size() + " methods)", model));
        return model;
    }

    /**
     * Read and parse one class from a source by internal name.
     *
     * @param source       the class source
     * @param internalName the internal binary name, e.g. {@code com/foo/Bar}
     * @return the class model, or empty if the class is absent
     */
    public Optional<ClassModel> parseClass(ClassSource source, String internalName) {
        return source.readClass(internalName).map(this::parseClass);
    }

    /**
     * Build the control-flow graph for a single method.
     *
     * @param owner  the owning class (for diagnostics)
     * @param method the method model
     * @return the control-flow graph
     */
    public ControlFlowGraph buildCfg(ClassModel owner, MethodModel method) {
        ControlFlowGraph cfg = cfgBuilder.build(owner, method);
        eventBus.publish(new AetherEvent(AetherEvent.Phase.CFG, IRKind.CFG,
                "CFG for " + owner.name() + "." + method.id()
                        + " (" + cfg.blocks().size() + " blocks)", cfg));
        return cfg;
    }

    /**
     * Run the full available pipeline (model + CFG per non-abstract method) for
     * every class in a source. This is a convenience entry point for the CLI;
     * applications that want finer control call the individual steps.
     *
     * @param source the class source
     * @return one result per class, in listing order
     */
    public List<DecompileResult> decompileAll(ClassSource source) {
        List<DecompileResult> results = new java.util.ArrayList<>();
        for (String name : source.classNames()) {
            results.add(decompile(source, name));
        }
        return results;
    }

    /**
     * Run the available pipeline for one class named in a source.
     *
     * @param source       the class source
     * @param internalName the internal binary name
     * @return a result pairing the model with per-method CFGs
     */
    public DecompileResult decompile(ClassSource source, String internalName) {
        ClassModel model = parseClass(source, internalName).orElse(null);
        if (model == null) {
            return new DecompileResult(internalName, null, List.of());
        }
        List<ControlFlowGraph> cfgs = new java.util.ArrayList<>();
        for (MethodModel method : model.methods()) {
            if (!method.isAbstractOrNative()) {
                cfgs.add(buildCfg(model, method));
            }
        }
        return new DecompileResult(internalName, model, cfgs);
    }

    /**
     * The outcome of decompiling one class through the available pipeline.
     *
     * @param internalName the class the result is for
     * @param model        the class model, or {@code null} if it could not be read
     * @param cfgs         one control-flow graph per concrete method
     * @author Jerry Zhu (Zeek)
     */
    public record DecompileResult(String internalName, ClassModel model, List<ControlFlowGraph> cfgs) {
        /**
         * @return {@code true} if this class produced a model
         */
        public boolean ok() {
            return model != null;
        }
    }
}
