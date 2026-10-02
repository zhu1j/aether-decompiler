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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 由 jar/zip 归档支撑的 {@link ClassSource}。
 *
 * <p>归档的中央目录被扫描一次以构建名称索引；各个条目按需读取。这直接实现了
 * 渐进/惰性加载约束：打开一个大 jar 绝不会解压每一个类。</p>
 *
 * <p>故事类比：一个贴有清单的密封货运集装箱。你立即读取清单（索引）；
 * 只有当真的需要其中内容时，才打开里面的一只板条箱。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class JarClassSource implements ClassSource {

    private final ZipFile zip;
    private final List<String> names = new ArrayList<>();

    /**
     * @param zip 一个已打开的 zip/jar 文件
     */
    public JarClassSource(ZipFile zip) {
        this.zip = zip;
        Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String name = entry.getName();
            if (!entry.isDirectory() && name.endsWith(".class")) {
                names.add(name.substring(0, name.length() - ".class".length()));
            }
        }
    }

    @Override
    public String describe() {
        return "jar:" + zip.getName() + " (" + names.size() + " classes)";
    }

    @Override
    public List<String> classNames() {
        return List.copyOf(names);
    }

    @Override
    public Optional<byte[]> readClass(String internalName) {
        ZipEntry entry = zip.getEntry(internalName + ".class");
        if (entry == null) {
            return Optional.empty();
        }
        try (InputStream in = zip.getInputStream(entry)) {
            return Optional.of(in.readAllBytes());
        } catch (IOException ex) {
            return Optional.empty();
        }
    }

    @Override
    public void close() {
        try {
            zip.close();
        } catch (IOException ex) {
            // 关闭一个只读归档不会有实质性的失败。
        }
    }
}
