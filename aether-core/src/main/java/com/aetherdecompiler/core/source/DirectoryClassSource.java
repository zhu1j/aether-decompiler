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
package com.aetherdecompiler.core.source;

import com.aetherdecompiler.api.ClassSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * 由 {@code .class} 文件目录树支撑的 {@link ClassSource}。
 *
 * <p>列出时遍历一次目录树并记录相对类名；读取时按需恰好打开一个文件。不做任何
 * 预加载，满足惰性加载约束。</p>
 *
 * <p>故事类比：一个装着文件夹的文件柜。索引卡列出每个文件夹；只有当真的需要
 * 某个文件时，你才打开那个抽屉。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class DirectoryClassSource implements ClassSource {

    private final Path root;

    /**
     * @param root 要扫描的目录
     */
    public DirectoryClassSource(Path root) {
        this.root = root;
    }

    @Override
    public String describe() {
        return "directory:" + root;
    }

    @Override
    public List<String> classNames() {
        List<String> names = new ArrayList<>();
        if (!Files.isDirectory(root)) {
            return names;
        }
        try (Stream<Path> walk = Files.walk(root)) {
            walk.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".class"))
                    .forEach(p -> {
                        String rel = root.relativize(p).toString().replace('\\', '/');
                        names.add(rel.substring(0, rel.length() - ".class".length()));
                    });
        } catch (IOException ex) {
            // 无法遍历的目录直接产出空名称列表。
        }
        return names;
    }

    @Override
    public Optional<byte[]> readClass(String internalName) {
        Path file = root.resolve(internalName + ".class");
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readAllBytes(file));
        } catch (IOException ex) {
            return Optional.empty();
        }
    }

    @Override
    public void close() {
        // 目录来源无需释放任何东西。
    }
}
