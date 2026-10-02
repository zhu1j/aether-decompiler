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
package com.aetherdecompiler.plugins.source;

import com.aetherdecompiler.api.ClassSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * 由目录支撑的 {@link ClassSource}，自包含于本插件之内。
 *
 * <p>刻意与内核自带的目录来源分离：插件只能依赖 {@code aether-plugin-api}，
 * 因此不能借用内核的类。复制这一小段遍历代码，是换取这种隔离的正确代价。</p>
 *
 * <p>故事类比：插件自带手电筒，而不是借用工厂固定的顶灯。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class DirectorySourceAdapter implements ClassSource {

    private final Path root;

    /**
     * @param root 要扫描的目录
     */
    public DirectorySourceAdapter(Path root) {
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
            // 无法遍历的目录产出空索引。
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
        // 无需释放任何东西。
    }
}
