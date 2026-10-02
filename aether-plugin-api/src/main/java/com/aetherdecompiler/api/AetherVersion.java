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
package com.aetherdecompiler.api;

/**
 * 不可变的、项目级的身份常量。
 *
 * <p>本类是项目人类身份的唯一事实来源。每个应用层（CLI Banner、GUI 关于框、
 * IDE 插件）都从这里读取署名，因此作者的签名从不重复，也不会漂移。</p>
 *
 * <p>故事类比：这是按进工厂运出的每一块砖上的制作者印记 —— 印记是产品的
 * 一部分，而非装饰。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class AetherVersion {

    /** 引擎的语义化版本。MAJOR 刻意保持稳定。 */
    public static final String VERSION = "0.1.0";

    /** 项目名称。 */
    public static final String PROJECT = "aether-decompiler";

    /** 作者的英文名。 */
    public static final String AUTHOR = "Jerry Zhu";

    /** 作者的笔名 / 昵称。 */
    public static final String AUTHOR_PEN_NAME = "Zeek";

    /** 作者的公开联系邮箱。 */
    public static final String AUTHOR_EMAIL = "zhujiejava1@gmail.com";

    /** 作者的座右铭，由每个应用入口打印。 */
    public static final String MOTTO = "Run the Code, Run the World!";

    /** SPDX 许可证标识符。 */
    public static final String LICENSE = "Apache-2.0";

    private AetherVersion() {
        throw new AssertionError("No instances.");
    }

    /**
     * 一行、可直接打印的署名字符串。
     *
     * @return 例如 {@code "aether-decompiler 0.1.0 — by Jerry Zhu (Zeek)"}
     */
    public static String attribution() {
        return PROJECT + " " + VERSION + " \u2014 by " + AUTHOR + " (" + AUTHOR_PEN_NAME + ")";
    }

    /**
     * CLI 启动时使用的完整多行 Banner。
     *
     * @return 一个嵌入项目身份与座右铭的 Banner 字符串
     */
    public static String banner() {
        return ""
                + "  ___   _____ _  _ _____ ____\n"
                + " / _ \\ | ____| || |_   _|  _ \\\n"
                + "| |_| ||  _| | __ | | | | |_) |\n"
                + "|  _  || |___| |_| | | | |  _ <\n"
                + "|_| |_||_____|\\___/  |_| |_| \\_\\\n"
                + "\n"
                + "  " + attribution() + "\n"
                + "  \"" + MOTTO + "\"\n";
    }
}
