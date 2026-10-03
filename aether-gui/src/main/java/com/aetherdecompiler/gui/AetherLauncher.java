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
package com.aetherdecompiler.gui;

import com.aetherdecompiler.api.AetherVersion;
import javafx.application.Application;

/**
 * GUI 入口点。
 *
 * <p>刻意保持极简，并与 {@link AetherStudio} 分离：一个自身不继承
 * {@link javafx.application.Application} 的启动器，可让 fat jar 在命令行未
 * 具名指定 JavaFX 模块的情况下也能启动，从而让打包产物可以双击运行。</p>
 *
 * <p>故事类比：工作台的前门。它只是转动把手然后退到一旁；
 * 里面的人干活。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class AetherLauncher {

    private AetherLauncher() {
    }

    /**
     * @param args 转发给 JavaFX 应用
     */
    public static void main(String[] args) {
        System.out.println(AetherVersion.attribution());
        System.out.println("\"" + AetherVersion.MOTTO + "\"");
        Application.launch(AetherStudio.class, args);
    }
}
