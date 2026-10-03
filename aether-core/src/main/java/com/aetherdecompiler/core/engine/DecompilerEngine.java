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
 * 内核的顶层编排者。
 *
 * <p>掌管流水线：原始字节 → {@link ClassModel} → 逐方法
 * {@link ControlFlowGraph}。它刻意对运行期数据保持无状态：所有可变状态都在
 * 每次调用时创建并丢弃，因此单个 {@code DecompilerEngine} 可以跨线程共享，
 * 并由应用层并行驱动。引擎绝不自行创建线程池 —— 那明确属于上层关注点。</p>
 *
 * <p>故事类比：引擎好比中央传送带控制器。它知道各工位的顺序，并让每个零件
 * 从产线启程，但自己并不持有任何零件 —— 零件只是流过，而控制器可以同时运行
 * 多条产线。</p>
 *
 * <p>Phase 0 实现“字节 → 模型”。Phase 1 在同一流水线上扩展出 CFG 构建，
 * 而这已经在此接通。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class DecompilerEngine {

    private final AsmClassParser parser;
    private final CfgBuilder cfgBuilder;
    private final EventBus eventBus;

    /**
     * 创建一个带有私有事件总线的引擎。
     */
    public DecompilerEngine() {
        this(new SimpleEventBus());
    }

    /**
     * @param eventBus 所有流水线事件都发布到的事件总线
     */
    public DecompilerEngine(EventBus eventBus) {
        this.eventBus = eventBus;
        this.parser = new AsmClassParser();
        this.cfgBuilder = new CfgBuilder();
    }

    /** @return 引擎的事件总线，供观察者订阅 */
    public EventBus eventBus() {
        return eventBus;
    }

    /**
     * 把原始类字节解析为不可变模型。
     *
     * @param bytes 类文件字节
     * @return 类模型
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
     * 按内部名从某个来源读取并解析一个类。
     *
     * @param source       类来源
     * @param internalName 内部二进制名，例如 {@code com/foo/Bar}
     * @return 类模型；若该类不存在则返回空
     */
    public Optional<ClassModel> parseClass(ClassSource source, String internalName) {
        return source.readClass(internalName).map(this::parseClass);
    }

    /**
     * 为单个方法构建控制流图。
     *
     * @param owner  所属类（用于诊断）
     * @param method 方法模型
     * @return 控制流图
     */
    public ControlFlowGraph buildCfg(ClassModel owner, MethodModel method) {
        ControlFlowGraph cfg = cfgBuilder.build(owner, method);
        eventBus.publish(new AetherEvent(AetherEvent.Phase.CFG, IRKind.CFG,
                "CFG for " + owner.name() + "." + method.id()
                        + " (" + cfg.blocks().size() + " blocks)", cfg));
        return cfg;
    }

    /**
     * 对某个来源中的每个类运行当前可用的完整流水线（模型 + 每个非抽象方法的 CFG）。
     * 这是供 CLI 使用的便捷入口；需要更细粒度控制的应用请逐个调用各步骤。
     *
     * @param source 类来源
     * @return 每个类一个结果，按列出顺序
     */
    public List<DecompileResult> decompileAll(ClassSource source) {
        List<DecompileResult> results = new java.util.ArrayList<>();
        for (String name : source.classNames()) {
            results.add(decompile(source, name));
        }
        return results;
    }

    /**
     * 对某个来源中指定的一个类运行当前可用的流水线。
     *
     * @param source       类来源
     * @param internalName 内部二进制名
     * @return 把模型与逐方法 CFG 配对的结果
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
     * 一个类经过当前可用流水线反编译后的结果。
     *
     * @param internalName 该结果对应的类
     * @param model        类模型；若无法读取则为 {@code null}
     * @param cfgs         每个具体方法一个控制流图
     * @author Jerry Zhu (Zeek)
     */
    public record DecompileResult(String internalName, ClassModel model, List<ControlFlowGraph> cfgs) {
        /**
         * @return 若该类成功产出模型则为 {@code true}
         */
        public boolean ok() {
            return model != null;
        }
    }
}
