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
package com.aetherdecompiler.core;

import com.aetherdecompiler.api.AetherException;
import com.aetherdecompiler.api.ErrorCode;
import com.aetherdecompiler.api.AetherVersion;
import com.aetherdecompiler.api.OptionRegistry;
import com.aetherdecompiler.api.Options;
import com.aetherdecompiler.api.OptionSpec;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 针对已冻结的 Phase0 契约的单元测试：选项与错误模型。
 *
 * @author Jerry Zhu (Zeek)
 */
class ContractsTest {

    @Test
    void optionsAreImmutableSnapshots() {
        Options base = Options.empty();
        Options a = base.with("x", 1);
        Options b = a.with("y", "hello");
        assertEquals(0, base.asMap().size());
        assertEquals(1, a.asMap().size());
        assertEquals(2, b.asMap().size());
        assertEquals(1, b.getInt("x", -1));
        assertEquals("hello", b.getString("y").orElseThrow());
        assertTrue(b.getBoolean("missing", true));
    }

    @Test
    void optionRegistryKeepsInsertionOrderAndReplaces() {
        OptionRegistry reg = new OptionRegistry();
        reg.register(OptionSpec.string("output.dir", ".", "output directory", "aether"));
        reg.register(OptionSpec.bool("quiet", false, "suppress output", "aether"));
        assertEquals(2, reg.all().size());
        reg.register(OptionSpec.string("output.dir", "out", "changed", "aether"));
        assertEquals(2, reg.all().size());
        assertEquals("out", reg.find("output.dir").orElseThrow().defaultValue());
    }

    @Test
    void aetherExceptionCarriesAStableCodeAndTag() {
        AetherException ex = new AetherException(ErrorCode.INPUT_MALFORMED, "boom");
        assertEquals(ErrorCode.INPUT_MALFORMED, ex.errorCode());
        assertEquals("AETHER-1002", ex.errorCode().tag());
        assertTrue(ex.getMessage().contains("AETHER-1002"));
        assertEquals("boom", ex.rawMessage());
    }

    @Test
    void wrappingIsIdempotentForAetherExceptions() {
        AetherException original = new AetherException(ErrorCode.INPUT_NOT_FOUND, "missing");
        AetherException wrapped = AetherException.wrap(ErrorCode.UNKNOWN, "outer", original);
        assertEquals(original, wrapped, "wrapping an AetherException must return it unchanged");
    }

    @Test
    void bannerEmbedsAuthorAndMotto() {
        String banner = AetherVersion.banner();
        assertTrue(banner.contains("Jerry Zhu"));
        assertTrue(banner.contains("Zeek"));
        assertTrue(banner.contains("Run the Code, Run the World!"));
        assertFalse(banner.isBlank());
    }
}
