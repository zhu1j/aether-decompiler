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

import com.aetherdecompiler.core.engine.DecompilationPipeline;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 5 流水线缓存行为的单元测试。
 *
 * @author Jerry Zhu (Zeek)
 */
class DecompilationPipelineTest {

    @Test
    void cacheReturnsSameInstanceForRepeatedRequest() throws Exception {
        byte[] bytes;
        String resource = Sample.class.getName().replace('.', '/') + ".class";
        try (var in = Sample.class.getClassLoader().getResourceAsStream(resource)) {
            bytes = in.readAllBytes();
        }
        var model = new com.aetherdecompiler.core.asm.AsmClassParser().parse(bytes);
        var pipeline = new DecompilationPipeline();
        var method = model.methods().stream().filter(m -> m.name().equals("sumTo")).findFirst().orElseThrow();

        var first = pipeline.analyzeMethod(model, method);
        var second = pipeline.analyzeMethod(model, method);

        assertSame(first, second, "second request must hit the cache and return the identical instance");
        assertEquals(1, pipeline.cachedMethodCount());
        assertNotNull(first.ssa(), "SSA should be produced");
        assertNotNull(first.ast(), "AST should be produced");
        assertTrue(first.ssa().phiCount() > 0, "sumTo requires phis");
    }
}
