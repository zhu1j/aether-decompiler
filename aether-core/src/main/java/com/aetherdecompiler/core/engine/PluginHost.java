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
 * 基于 Java 原生 {@link ServiceLoader} 的最小插件宿主。
 *
 * <p>刻意手写、不依赖框架：没有 Spring，没有 Guava。它从类路径（或调用方提供的
 * 类加载器）中发现四类固定扩展点的实现，按类别暴露它们，并给每个实现一个
 * {@link PluginContext}。宿主是这四类插件被合并的<em>唯一</em>场所；内核
 * 自己从不枚举插件。</p>
 *
 * <p>故事类比：工厂的收货月台。四类已知的卡车抵达，被登记并停入正确的
 * 泊位 —— 装配线被告知的是“有一个渲染器可用”，而绝不是“它是哪家公司造的”。</p>
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
     * 发现对某个类加载器可见的所有插件，并把它们绑定到一个上下文。
     *
     * @param loader         要扫描的类加载器（例如应用类加载器）
     * @param options        配置快照
     * @param optionRegistry 共享的选项注册表
     * @param eventBus       共享的事件总线
     */
    public PluginHost(ClassLoader loader, Options options,
                      OptionRegistry optionRegistry, EventBus eventBus) {
        this.context = new DefaultPluginContext(options, optionRegistry, eventBus);
        load(loader);
    }

    private void load(ClassLoader loader) {
        // 每个类别独立加载；一个只提供一种
        // provider 的插件模块完全合法。
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
        // 变换顺序是契约的一部分：order() 越小越先运行。
        transforms.sort((a, b) -> Integer.compare(a.order(), b.order()));
    }

    /** @return 交给每个插件的插件上下文 */
    public PluginContext context() {
        return context;
    }

    /** @return 发现的类来源插件 */
    public List<ClassSourcePlugin> classSources() {
        return Collections.unmodifiableList(classSources);
    }

    /** @return 发现的渲染插件 */
    public List<RenderPlugin> renderers() {
        return Collections.unmodifiableList(renderers);
    }

    /** @return 发现的 AST 变换插件，已按 order 预排序 */
    public List<AstTransformPlugin> transforms() {
        return Collections.unmodifiableList(transforms);
    }

    /** @return 发现的分析插件 */
    public List<AnalysisPlugin> analyses() {
        return Collections.unmodifiableList(analyses);
    }

    /** @return 发现插件的总数 */
    public int pluginCount() {
        return classSources.size() + renderers.size() + transforms.size() + analyses.size();
    }

    /**
     * {@link PluginContext} 的默认只读实现。
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
