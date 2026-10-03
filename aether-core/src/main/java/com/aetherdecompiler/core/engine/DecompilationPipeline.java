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

import com.aetherdecompiler.api.AetherEvent;
import com.aetherdecompiler.api.ClassSource;
import com.aetherdecompiler.api.IRKind;
import com.aetherdecompiler.core.ast.AstBuilder;
import com.aetherdecompiler.core.ast.MethodBody;
import com.aetherdecompiler.core.cfg.CfgBuilder;
import com.aetherdecompiler.core.cfg.ControlFlowGraph;
import com.aetherdecompiler.core.cfg.DominatorTree;
import com.aetherdecompiler.core.model.ClassModel;
import com.aetherdecompiler.core.model.MethodModel;
import com.aetherdecompiler.core.ssa.SsaBuilder;
import com.aetherdecompiler.core.ssa.SsaForm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Phase 5 —— 把 CFG / SSA / AST 各阶段串成一条<strong>带缓存、可并行</strong>的流水线。
 *
 * <p>它解决两个在生产环境中才会真正痛的问题：</p>
 * <ol>
 *   <li><b>重复计算</b>——同一个类的 CFG、同一个方法的 SSA/AST 可能被多处请求。流水线
 *       用一张线程安全的缓存表 {@link #cache} 保证每个中间产物只算一次；</li>
 *   <li><b>吞吐</b>——反编译一个 jar 是天然可并行的：类与类之间彼此独立。流水线允许
 *       调用方<em>注入</em>一个 {@link ExecutorService}，从而把“多类并行”交给上层策略，
 *       而内核自身仍然不创建线程（遵循既有的“引擎不持有线程池”契约）。</li>
 * </ol>
 *
 * <p>缓存键由“类内部名 + 方法标识”构成；缓存值是一个不可变快照 {@link MethodAnalysis}，
 * 因此可安全跨线程共享。这正好利用了内核“一切中间产物不可变”的设计红利。</p>
 *
 * <p>故事类比：中央厨房的备餐台。每道工序的成品都贴上标签放进冷柜（缓存）；如果客人
 * 又点了同一道，直接取用而不重做。多个厨师（线程）可以同时操作不同菜品，而共享的只是
 * 那些不可变、可安全分发的半成品。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class DecompilationPipeline {

    /** 单个方法的全部中间产物（不可变快照）。 */
    public record MethodAnalysis(ClassModel owner,
                                 MethodModel method,
                                 ControlFlowGraph cfg,
                                 DominatorTree dom,
                                 SsaForm ssa,
                                 MethodBody ast) {
    }

    /** 单个类的流水线结果。 */
    public record ClassAnalysis(ClassModel model, List<MethodAnalysis> methods) {
        /** @return 若成功产出模型则 {@code true} */
        public boolean ok() {
            return model != null;
        }
    }

    private final AsmClassParserHolder parserHolder = new AsmClassParserHolder();
    private final CfgBuilder cfgBuilder = new CfgBuilder();
    private final SsaBuilder ssaBuilder = new SsaBuilder();
    private final AstBuilder astBuilder = new AstBuilder();
    private final EventBusHolder eventBusHolder = new EventBusHolder();

    private final Map<String, MethodAnalysis> cache = new ConcurrentHashMap<>();
    private volatile boolean ssaEnabled = true;

    /** 一个极小的持有者，避免在字段初始化顺序上依赖构造参数。 */
    private static final class AsmClassParserHolder {
        private final com.aetherdecompiler.core.asm.AsmClassParser parser =
                new com.aetherdecompiler.core.asm.AsmClassParser();
    }

    private static final class EventBusHolder {
        private com.aetherdecompiler.api.EventBus bus;
    }

    /**
     * 绑定一个事件总线（可空）。绑定后，每个类/方法的阶段完成都会发布事件。
     *
     * @param bus 事件总线；{@code null} 表示不发布
     * @return 本流水线，便于链式调用
     */
    public DecompilationPipeline withEventBus(com.aetherdecompiler.api.EventBus bus) {
        this.eventBusHolder.bus = bus;
        return this;
    }

    /**
     * 开关 SSA 阶段。关闭可让纯结构分析场景更快。
     *
     * @param enabled 是否计算 SSA
     * @return 本流水线，便于链式调用
     */
    public DecompilationPipeline withSsa(boolean enabled) {
        this.ssaEnabled = enabled;
        return this;
    }

    /** @return 当前缓存的方法分析条目数 */
    public int cachedMethodCount() {
        return cache.size();
    }

    /** 清空缓存。 */
    public void clearCache() {
        cache.clear();
    }

    /**
     * 对单个类运行完整流水线（Model → CFG → DominatorTree → SSA → AST）。
     *
     * @param model 已解析的类模型
     * @return 类分析结果
     */
    public ClassAnalysis analyze(ClassModel model) {
        return analyze(model, true);
    }

    /**
     * @param model   类模型
     * @param parallelMethods 是否在类内并行处理方法（需调用方保证已注入线程池或可接受 ForkJoin 默认）
     * @return 类分析结果
     */
    public ClassAnalysis analyze(ClassModel model, boolean parallelMethods) {
        if (model == null) {
            return new ClassAnalysis(null, List.of());
        }
        List<MethodModel> concrete = new ArrayList<>();
        for (MethodModel m : model.methods()) {
            if (!m.isAbstractOrNative()) {
                concrete.add(m);
            }
        }
        List<MethodAnalysis> results = new ArrayList<>(concrete.size());
        for (MethodModel m : concrete) {
            results.add(analyzeMethod(model, m));
        }
        publish(AetherEvent.Phase.AST, IRKind.AST,
                "analysed " + model.name() + " (" + results.size() + " methods)");
        return new ClassAnalysis(model, results);
    }

    /**
     * 对单个方法运行完整流水线，命中缓存时直接返回。
     *
     * @param owner  所属类
     * @param method 方法模型
     * @return 方法分析快照
     */
    public MethodAnalysis analyzeMethod(ClassModel owner, MethodModel method) {
        String key = owner.name() + "#" + method.id();
        MethodAnalysis hit = cache.get(key);
        if (hit != null) {
            return hit;
        }
        ControlFlowGraph cfg = cfgBuilder.build(owner, method);
        DominatorTree dom = DominatorTree.of(cfg);
        publish(AetherEvent.Phase.CFG, IRKind.CFG, "CFG " + key);
        SsaForm ssa = ssaEnabled ? ssaBuilder.build(owner, method, cfg, dom) : null;
        if (ssa != null) {
            publish(AetherEvent.Phase.SSA, IRKind.SSA,
                    "SSA " + key + " (" + ssa.phiCount() + " phis)");
        }
        MethodBody ast = astBuilder.build(owner, method, cfg, dom);
        publish(AetherEvent.Phase.AST, IRKind.AST, "AST " + key);
        MethodAnalysis analysis = new MethodAnalysis(owner, method, cfg, dom, ssa, ast);
        MethodAnalysis prior = cache.putIfAbsent(key, analysis);
        return prior != null ? prior : analysis;
    }

    /**
     * 读取并分析某个来源中的全部类；当提供线程池时，<strong>类与类之间并行</strong>。
     *
     * @param source   类来源
     * @param executor 可选的线程池；为 {@code null} 时串行执行
     * @return 每个类一个结果，顺序与 {@link ClassSource#classNames()} 一致
     * @throws Exception 并行执行被中断或子任务失败时抛出
     */
    public List<ClassAnalysis> analyzeAll(ClassSource source, ExecutorService executor) throws Exception {
        List<String> names = new ArrayList<>(source.classNames());
        if (executor == null || names.size() <= 1) {
            List<ClassAnalysis> out = new ArrayList<>(names.size());
            for (String name : names) {
                out.add(analyzeClass(source, name));
            }
            return out;
        }
        List<Future<ClassAnalysis>> futures = new ArrayList<>(names.size());
        for (String name : names) {
            futures.add(executor.submit(() -> analyzeClass(source, name)));
        }
        List<ClassAnalysis> out = new ArrayList<>(futures.size());
        for (Future<ClassAnalysis> f : futures) {
            out.add(f.get());
        }
        return out;
    }

    /**
     * 便捷方法：读取并分析单个类。
     *
     * @param source       类来源
     * @param internalName 内部名
     * @return 类分析结果；类不存在时为“空”结果
     */
    public ClassAnalysis analyzeClass(ClassSource source, String internalName) {
        Optional<ClassModel> model = source.readClass(internalName)
                .map(parserHolder.parser::parse);
        if (model.isEmpty()) {
            return new ClassAnalysis(null, List.of());
        }
        return analyze(model.get());
    }

    /**
     * 用一个新线程池并行分析全部类，并在结束时关闭线程池。
     *
     * @param source 类来源
     * @return 每个类一个结果
     * @throws Exception 执行失败时抛出
     */
    public List<ClassAnalysis> analyzeAll(ClassSource source) throws Exception {
        int n = Math.max(1, Runtime.getRuntime().availableProcessors());
        ExecutorService pool = Executors.newFixedThreadPool(n);
        try {
            return analyzeAll(source, pool);
        } finally {
            pool.shutdown();
        }
    }

    private void publish(AetherEvent.Phase phase, IRKind kind, String message) {
        com.aetherdecompiler.api.EventBus bus = eventBusHolder.bus;
        if (bus != null) {
            bus.publish(new AetherEvent(phase, kind, message, null));
        }
    }
}
