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

import java.util.Objects;

/**
 * 描述插件的不可变、声明式元数据。
 *
 * <p>元数据让宿主无需实例化插件的重型代码就能列出、排序并做版本检查。
 * {@code apiVersion} 字段是强制向后兼容承诺的接缝：宿主拒绝激活一个针对
 * 更新的、不兼容的插件 API 主版本构建的插件。</p>
 *
 * <p>故事类比：铭刻在每件电器上的铭牌与电气额定值。工厂在插电之前先核对
 * 额定值。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class PluginMeta {

    private final String id;
    private final String displayName;
    private final String version;
    private final String apiVersion;
    private final String vendor;

    private PluginMeta(String id, String displayName, String version,
                       String apiVersion, String vendor) {
        this.id = Objects.requireNonNull(id, "id");
        this.displayName = displayName == null ? id : displayName;
        this.version = version == null ? "0.0.0" : version;
        this.apiVersion = apiVersion == null ? AetherVersion.VERSION : apiVersion;
        this.vendor = vendor == null ? "" : vendor;
    }

    /**
     * @param id          唯一插件 id，例如 {@code "render.dot"}
     * @param displayName 人类可读名称
     * @param version     插件自身的版本
     * @param apiVersion  它所针对的 aether 插件 API 版本
     * @param vendor      作者/供应商字符串
     * @return 新的元数据记录
     */
    public static PluginMeta of(String id, String displayName, String version,
                                String apiVersion, String vendor) {
        return new PluginMeta(id, displayName, version, apiVersion, vendor);
    }

    /** @return 唯一插件 id */
    public String id() {
        return id;
    }

    /** @return 人类可读名称 */
    public String displayName() {
        return displayName;
    }

    /** @return 插件自身的版本 */
    public String version() {
        return version;
    }

    /** @return 所针对的插件 API 版本 */
    public String apiVersion() {
        return apiVersion;
    }

    /** @return 作者/供应商字符串 */
    public String vendor() {
        return vendor;
    }

    @Override
    public String toString() {
        return displayName + " " + version + " (" + id + ", api " + apiVersion + ")";
    }
}
