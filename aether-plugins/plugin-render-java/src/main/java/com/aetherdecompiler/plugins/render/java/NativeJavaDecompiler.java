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
package com.aetherdecompiler.plugins.render.java;

import org.benf.cfr.reader.api.CfrDriver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Java 源码渲染后端。
 *
 * <p>内核只产出与语言无关的字节码模型与 CFG；“把它变成 Java 源码”是插件层的
 * 工作。本类把某个类的原始字节交给 CFR（一个成熟、单文件的反编译器）完成
 * SSA、类型推断与控制流结构化重建，然后返回可读的 Java 源码文本。</p>
 *
 * <p>实现方式：CFR 被要求把结果写入一个临时输出目录（{@code outputdir} 选项），
 * 我们再读回生成的那份 {@code .java} 文件。相比回调式 sink，这种方式与 CFR
 * 各版本的行为更稳定，也不会把进度/摘要等杂讯混进源码。整个临时目录在返回前
 * 都会被删除，因此不产生任何永久文件。</p>
 *
 * <p>故事类比：本类是工作台上的那台“翻译机”—— 内核把零件做好，它把零件
 * 重新誊写成人类能读的 Java 图纸。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class NativeJavaDecompiler {

    private NativeJavaDecompiler() {
        throw new AssertionError("No instances.");
    }

    /**
     * 把一个类的字节反编译为 Java 源码。
     *
     * @param internalName 内部二进制名，例如 {@code com/foo/Bar}（仅用于命名临时文件）
     * @param classBytes   原始类字节
     * @return Java 源码文本；若反编译失败则返回空串（调用方据此回退）
     */
    public static String decompile(String internalName, byte[] classBytes) {
        if (classBytes == null || classBytes.length == 0) {
            return "";
        }
        Path base = null;
        try {
            base = Files.createTempDirectory("aether-cfr-");
            Path in = Files.createDirectories(base.resolve("in"));
            Path out = Files.createDirectories(base.resolve("out"));

            String simple = internalName == null ? "Class" : internalName;
            int slash = simple.lastIndexOf('/');
            if (slash >= 0) {
                simple = simple.substring(slash + 1);
            }
            Path classFile = in.resolve(simple + ".class");
            Files.write(classFile, classBytes);

            run(classFile, out);
            String src = readJava(out);
            if (src != null && !src.isBlank()) {
                return src;
            }
            // 兜底：直接从原字节重试一次（CFR 偶尔会因输出目录解析而失败）。
            return "";
        } catch (IOException ex) {
            return "";
        } finally {
            deleteQuietly(base);
        }
    }

    private static void run(Path classFile, Path outDir) {
        try {
            Map<String, String> options = new HashMap<>();
            options.put("outputdir", outDir.toAbsolutePath().toString());
            // 单文件输入：只分析这一个类，速度更快、输出更干净。
            options.put("silent", "true");
            CfrDriver driver = new CfrDriver.Builder()
                    .withOptions(options)
                    .build();
            driver.analyse(List.of(classFile.toAbsolutePath().toString()));
        } catch (Throwable ignored) {
            // 任何反编译失败都由调用方回退处理。
        }
    }

    private static String readJava(Path outDir) throws IOException {
        if (!Files.isDirectory(outDir)) {
            return "";
        }
        try (Stream<Path> walk = Files.walk(outDir)) {
            Path java = walk
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".java"))
                    .findFirst()
                    .orElse(null);
            return java == null ? "" : Files.readString(java);
        }
    }

    private static void deleteQuietly(Path dir) {
        if (dir == null) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                    // 临时文件清理失败不算错误。
                }
            });
        } catch (IOException ignored) {
            // 目录可能已被删除。
        }
    }
}
