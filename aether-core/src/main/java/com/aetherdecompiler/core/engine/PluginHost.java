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

import com.aetherdecompiler.api.AetherException;
import com.aetherdecompiler.api.AnalysisPlugin;
import com.aetherdecompiler.api.AstTransformPlugin;
import com.aetherdecompiler.api.ClassSourcePlugin;
import com.aetherdecompiler.api.ErrorCode;
import com.aetherdecompiler.api.EventBus;
import com.aetherdecompiler.api.OptionRegistry;
import com.aetherdecompiler.api.Options;
import com.aetherdecompiler.api.PluginContext;
import com.aetherdecompiler.api.RenderPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;

/**
 * A minimal plugin host built on Java's native {@link ServiceLoader}.
 *
 * <p>Deliberately hand-written and framework-free: no Spring, no Guava. It
 * discovers the four fixed extension-point implementations from the classpath
 * (or a caller-supplied class loader), exposes them by category, and hands each
 * a {@link PluginContext}. The host is the <em>only</em> place the four plugin
 * categories are merged; the kernel never enumerates plugins itself.</p>
 *
 * <p>Story analogy: the factory's receiving dock. Trucks of four known kinds
 * arrive, are logged, and parked in the right bay — the assembly line is told
 * "a renderer is available", never "which company made it".</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class PluginHost {

    private final List<ClassSourcePlugin> classSources = new ArrayList<>();
    private final List<RenderPlugin> renderers = new ArrayList<>();
    private final List<AstTransformPlugin> transforms = new ArrayList<>();
    private final List<AnalysisPlugin> analyses = new ArrayList<>();
    private final PluginContext context;

    /**
     * Discover all plugins visible to a class loader and bind them to a context.
     *
     * @param loader       the class loader to scan (e.g. the app class loader)
     * @param options      the configuration snapshot
     * @param optionRegistry the shared option registry
     * @param eventBus     the shared event bus
     */
    public PluginHost(ClassLoader loader, Options options,
                      OptionRegistry optionRegistry, EventBus eventBus) {
        this.context = new DefaultPluginContext(options, optionRegistry, eventBus);
        load(loader);
    }

    private void load(ClassLoader loader) {
        // Each category is loaded independently; a plugin module that only
        // ships one kind of provider is perfectly valid.
        for (ClassSourcePlugin p : ServiceLoader.load(ClassSourcePlugin.class, loader)) {
            classSources.add(p);
        }
        for (RenderPlugin p : ServiceLoader.load(RenderPlugin.class, loader)) {
            renderers.add(p);
        }
        for (AstTransformPlugin p : ServiceLoader.load(AstTransformPlugin.class, loader)) {
            transforms.add(p);
        }
        for (AnalysisPlugin p : ServiceLoader.load(AnalysisPlugin.class, loader)) {
            analyses.add(p);
        }
        // Transform order is part of the contract: lower order() runs earlier.
        transforms.sort((a, b) -> Integer.compare(a.order(), b.order()));
    }

    /** @return the plugin context handed to every plugin */
    public PluginContext context() {
        return context;
    }

    /** @return discovered class-source plugins */
    public List<ClassSourcePlugin> classSources() {
        return Collections.unmodifiableList(classSources);
    }

    /** @return discovered render plugins */
    public List<RenderPlugin> renderers() {
        return Collections.unmodifiableList(renderers);
    }

    /** @return discovered AST transform plugins, pre-sorted by order */
    public List<AstTransformPlugin> transforms() {
        return Collections.unmodifiableList(transforms);
    }

    /** @return discovered analysis plugins */
    public List<AnalysisPlugin> analyses() {
        return Collections.unmodifiableList(analyses);
    }

    /** @return the total number of discovered plugins */
    public int pluginCount() {
        return classSources.size() + renderers.size() + transforms.size() + analyses.size();
    }

    /**
     * Default, read-only implementation of {@link PluginContext}.
     *
     * @author Jerry Zhu (Zeek)
     */
    private static final class DefaultPluginContext implements PluginContext {

        private final Options options;
        private final OptionRegistry optionRegistry;
        private final EventBus eventBus;
        private volatile byte[] currentClassBytes;

        DefaultPluginContext(Options options, OptionRegistry optionRegistry, EventBus eventBus) {
            this.options = options;
            this.optionRegistry = optionRegistry;
            this.eventBus = eventBus;
        }

        @Override
        public Options options() {
            return options;
        }

        @Override
        public OptionRegistry optionRegistry() {
            return optionRegistry;
        }

        @Override
        public EventBus eventBus() {
            return eventBus;
        }

        @Override
        public byte[] resolveClassBytes(String internalName) {
            byte[] bytes = currentClassBytes;
            if (bytes == null) {
                throw new AetherException(ErrorCode.INPUT_MISSING_REFERENCE,
                        "No active class in context to resolve references from: " + internalName);
            }
            return bytes;
        }
    }
}
