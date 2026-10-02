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

/**
 * 历史类名兼容入口。
 *
 * <p>真正的应用外壳已经改名为 {@link AetherStudio} —— 它刻意不再继承
 * {@link javafx.application.Application}，因此即便 JavaFX 是以类路径方式提供
 * 的（普通 fat jar 即是如此），也不需要具名模块即可启动。</p>
 *
 * <p>本类只保留旧的类名，<em>不是</em> {@code Application} 子类，绝不能被当作
 * JavaFX 主类直接启动；它只把调用转发给规范入口 {@link AetherLauncher}。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class AetherGuiApp {

    private AetherGuiApp() {
    }

    /**
     * 兼容入口：转发到 {@link AetherLauncher}。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        AetherLauncher.main(args);
    }
}
