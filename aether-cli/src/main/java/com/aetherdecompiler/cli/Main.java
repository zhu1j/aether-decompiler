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
package com.aetherdecompiler.cli;

import com.aetherdecompiler.api.AetherEvent;
import com.aetherdecompiler.api.AetherVersion;
import com.aetherdecompiler.api.ClassSource;
import com.aetherdecompiler.api.ClassSourcePlugin;
import com.aetherdecompiler.api.EventBus;
import com.aetherdecompiler.api.OptionRegistry;
import com.aetherdecompiler.api.Options;
import com.aetherdecompiler.api.PluginContext;
import com.aetherdecompiler.api.RenderPlugin;
import com.aetherdecompiler.api.SourceTree;
import com.aetherdecompiler.core.cfg.ControlFlowGraph;
import com.aetherdecompiler.core.engine.DecompilerEngine;
import com.aetherdecompiler.core.engine.PluginHost;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * aether-decompiler 的命令行应用。
 *
 * <p>它是内核的纯调用方：通过 SPI 宿主发现插件、打开类来源、驱动引擎并写出渲染结果。
 * 内核与插件都不引用本类；删掉它也不会影响它们。这正是“CLI 只是一个应用”
 * 原则的具体佐证。</p>
 *
 * <p>故事类比：工厂门口的展厅。顾客描述想要什么，展厅再转达给工厂；
 * 工厂并不知道展厅的存在。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class Main {

    private Main() {
    }

    /**
     * 入口点。
     *
     * @param args {@code <输入.jar|目录> [--outputdir <目录>] [--quiet] [--banner]}
     */
    public static void main(String[] args) {
        // 作者的署名，默认在每次运行时打印。
        System.out.println(AetherVersion.banner());
        System.out.println("  " + AetherVersion.PROJECT + " \u00b7 " + AetherVersion.LICENSE
                + " \u00b7 " + AetherVersion.AUTHOR_EMAIL);
        System.out.println();

        if (args.length == 0 || "help".equalsIgnoreCase(args[0]) || "--help".equals(args[0])) {
            printUsage();
            return;
        }

        String input = args[0];
        Path outputDir = null;
        boolean quiet = false;
        for (int i = 1; i < args.length; i++) {
            if ("--outputdir".equals(args[i]) && i + 1 < args.length) {
                outputDir = Path.of(args[++i]);
            } else if ("--quiet".equals(args[i])) {
                quiet = true;
            }
        }

        if (outputDir != null) {
            try {
                Files.createDirectories(outputDir);
            } catch (IOException ex) {
                System.err.println("Cannot create output directory: " + outputDir);
                System.exit(2);
            }
        }

        Options options = Options.builder()
                .set("output.dir", outputDir == null ? "." : outputDir.toString())
                .build();
        OptionRegistry registry = new OptionRegistry();

        DecompilerEngine engine = new DecompilerEngine();
        PluginHost host = new PluginHost(Main.class.getClassLoader(), options, registry, engine.eventBus());

        if (!quiet) {
            System.out.println("Discovered " + host.pluginCount() + " plugin(s):");
            for (ClassSourcePlugin p : host.classSources()) {
                System.out.println("  [source] " + p.id() + " \u2014 " + p.displayName());
            }
            for (RenderPlugin p : host.renderers()) {
                System.out.println("  [render] " + p.id() + " \u2014 " + p.displayName());
            }
            System.out.println();
        }

        // 可观测的流水线：打印每个阶段事件（硬性约束 #8）。
        if (!quiet) {
            engine.eventBus().subscribe((AetherEvent e) ->
                    System.out.println("  [event] " + e));
        }

        // 找到一个能够打开该输入的来源插件。
        ClassSourcePlugin sourcePlugin = host.classSources().stream()
                .filter(p -> p.id().equals("source.jar"))
                .findFirst()
                .orElse(host.classSources().isEmpty() ? null : host.classSources().get(0));
        if (sourcePlugin == null) {
            System.err.println("No ClassSourcePlugin available. Is plugin-source-jar on the classpath?");
            System.exit(1);
        }

        ClassSource source = sourcePlugin.open(input, options);
        try {
            List<DecompilerEngine.DecompileResult> results = engine.decompileAll(source);
            int classCount = 0;
            int dotCount = 0;
            RenderPlugin dot = host.renderers().stream()
                    .filter(p -> p.id().equals("render.dot"))
                    .findFirst()
                    .orElse(null);

            for (DecompilerEngine.DecompileResult result : results) {
                if (!result.ok()) {
                    System.err.println("  [skip] " + result.internalName() + " (unreadable)");
                    continue;
                }
                classCount++;
                if (dot != null && outputDir != null) {
                    dotCount += writeDot(dot, host.context(), result, outputDir);
                }
                if (!quiet) {
                    System.out.println("  " + result.model().name()
                            + " \u2014 " + result.model().methods().size() + " methods, "
                            + result.cfgs().size() + " CFGs");
                }
            }

            System.out.println();
            System.out.println("Done: " + classCount + " class(es), " + dotCount + " DOT file(s).");
            System.out.println("\"" + AetherVersion.MOTTO + "\"");
        } finally {
            source.close();
        }
    }

    private static int writeDot(RenderPlugin dot, PluginContext ctx,
                                DecompilerEngine.DecompileResult result, Path outputDir) {
        int written = 0;
        for (ControlFlowGraph cfg : result.cfgs()) {
            List<SourceTree> trees = dot.render(cfg, ctx);
            for (SourceTree tree : trees) {
                try {
                    Path out = outputDir.resolve(tree.path());
                    Files.writeString(out, tree.content());
                    written++;
                } catch (IOException ex) {
                    System.err.println("Cannot write " + tree.path() + ": " + ex.getMessage());
                }
            }
        }
        return written;
    }

    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  java -jar aether-cli.jar <input.jar|classes-dir> [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --outputdir <dir>   write rendered artifacts (e.g. DOT) here");
        System.out.println("  --quiet             suppress progress output");
        System.out.println();
        System.out.println("  " + AetherVersion.attribution());
    }
}
