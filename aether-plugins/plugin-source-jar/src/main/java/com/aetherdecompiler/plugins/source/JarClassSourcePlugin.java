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

import com.aetherdecompiler.api.AetherException;
import com.aetherdecompiler.api.ClassSource;
import com.aetherdecompiler.api.ClassSourcePlugin;
import com.aetherdecompiler.api.ErrorCode;
import com.aetherdecompiler.api.Options;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;

/**
 * 官方 {@link ClassSourcePlugin}：从 jar/zip 文件或 {@code .class} 文件目录
 * 打开类。
 *
 * <p>通过 {@code ServiceLoader} 被发现。本插件的定位符是一个文件路径；插件
 * 自行判断该路径表示归档还是目录。它仅依赖 {@code aether-plugin-api} ——
 * 绝不依赖内核 —— 这正是架构要求第三方插件所具备的隔离。</p>
 *
 * <p>故事类比：一台多格式媒体读取器。给它一盘磁带或一张光盘；它识别介质
 * 并打开正确的托盘。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class JarClassSourcePlugin implements ClassSourcePlugin {

    /** 稳定的插件 id。 */
    public static final String ID = "source.jar";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "Jar / Directory Class Source";
    }

    @Override
    public ClassSource open(String locator, Options options) {
        Path path = Path.of(locator);
        if (!Files.exists(path)) {
            throw new AetherException(ErrorCode.INPUT_NOT_FOUND, "Input does not exist: " + locator);
        }
        if (Files.isDirectory(path)) {
            // 目录支持位于内核中；插件也可以复用它。
            return new com.aetherdecompiler.plugins.source.DirectorySourceAdapter(path);
        }
        try {
            return new JarClassSource(new ZipFile(path.toFile()));
        } catch (IOException ex) {
            throw new AetherException(ErrorCode.INPUT_MALFORMED,
                    "Cannot open archive: " + locator, ex);
        }
    }
}
