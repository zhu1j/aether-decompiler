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

import com.aetherdecompiler.core.asm.AsmClassParser;
import com.aetherdecompiler.core.model.ClassModel;
import com.aetherdecompiler.core.model.MethodModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 针对 ASM 隔离接缝与不可变模型的单元测试。
 *
 * @author Jerry Zhu (Zeek)
 */
class ModelParsingTest {

    private byte[] bytesOf(Class<?> c) throws Exception {
        String resource = c.getName().replace('.', '/') + ".class";
        try (var in = c.getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(in, "class resource missing: " + resource);
            return in.readAllBytes();
        }
    }

    @Test
    void parsesClassModelWithMembers() throws Exception {
        ClassModel model = new AsmClassParser().parse(bytesOf(Sample.class));
        assertEquals("com/aetherdecompiler/core/Sample", model.name());
        assertEquals("java/lang/Object", model.superName());
        assertFalse(model.isInterface());
        assertTrue(model.fields().stream().anyMatch(f -> f.name().equals("counter")));
        assertTrue(model.methods().stream().anyMatch(m -> m.name().equals("sumTo")));
    }

    @Test
    void modelsTheNoCodeInterface() throws Exception {
        ClassModel model = new AsmClassParser().parse(bytesOf(Sample.Transformer.class));
        assertTrue(model.isInterface());
        MethodModel apply = model.findMethod("apply", "(I)I").orElseThrow();
        assertTrue(apply.isAbstractOrNative());
        assertTrue(apply.instructions().isEmpty());
    }

    @Test
    void parsesMethodInstructionsAndTryCatch() throws Exception {
        ClassModel model = new AsmClassParser().parse(bytesOf(Sample.class));
        MethodModel parse = model.findMethod("parse", "(Ljava/lang/String;)I").orElseThrow();
        assertFalse(parse.instructions().isEmpty());
        assertFalse(parse.tryCatchEntries().isEmpty());
        assertNotNull(parse.instructions().get(0).mnemonic());
    }

    @Test
    void rejectsEmptyBytes() {
        try {
            new AsmClassParser().parse(new byte[0]);
            org.junit.jupiter.api.Assertions.fail("expected AetherException");
        } catch (com.aetherdecompiler.api.AetherException ex) {
            assertEquals(com.aetherdecompiler.api.ErrorCode.INPUT_MALFORMED, ex.errorCode());
        }
    }
}
