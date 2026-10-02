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
 * The command-line application of aether-decompiler.
 *
 * <p>Pure caller of the kernel: it discovers plugins through the SPI host, opens
 * a class source, drives the engine, and writes rendered output. The kernel and
 * plugins contain no reference to this class; deleting it would not disturb
 * them. This is the concrete evidence for the "CLI is only an application"
 * principle.</p>
 *
 * <p>Story analogy: the showroom at the factory gate. Customers describe what
 * they want; the showroom relays it to the factory. The factory does not know a
 * showroom exists.</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class Main {

    private Main() {
    }

    /**
     * Entry point.
     *
     * @param args {@code <input.jar|dir> [--outputdir <dir>] [--quiet] [--banner]}
     */
    public static void main(String[] args) {
        // The author's signature, printed by default on every run.
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

        // Observable pipeline: print every stage event (hard constraint #8).
        if (!quiet) {
            engine.eventBus().subscribe((AetherEvent e) ->
                    System.out.println("  [event] " + e));
        }

        // Find a source plugin that can open the input.
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
