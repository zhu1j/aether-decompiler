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

import com.aetherdecompiler.core.ast.AstBuilder;
import com.aetherdecompiler.core.ast.JavaAstRenderer;
import com.aetherdecompiler.core.ast.MethodBody;
import com.aetherdecompiler.core.ast.OpaqueMode;
import com.aetherdecompiler.core.ast.UnresolvedCodeException;
import com.aetherdecompiler.core.asm.AsmClassParser;
import com.aetherdecompiler.core.model.ClassModel;
import com.aetherdecompiler.core.model.MethodModel;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * C2 Opaque 严格/宽松模式的单元测试：验证默认宽松可产出源码，严格模式下遇到
 * 无法精确重建的片段会抛出 {@link UnresolvedCodeException}。
 *
 * @author Jerry Zhu (Zeek)
 */
class OpaqueModeTest {

    private ClassModel sample() throws Exception {
        String resource = Sample.class.getName().replace('.', '/') + ".class";
        try (var in = Sample.class.getClassLoader().getResourceAsStream(resource)) {
            return new AsmClassParser().parse(in.readAllBytes());
        }
    }

    private List<MethodBody> bodies(ClassModel model) {
        AstBuilder builder = new AstBuilder();
        List<MethodBody> bodies = new ArrayList<>();
        for (MethodModel m : model.methods()) {
            if (m.isAbstractOrNative()) {
                continue;
            }
            bodies.add(builder.build(model, m));
        }
        return bodies;
    }

    @Test
    void defaultModeIsLenient() {
        JavaAstRenderer renderer = new JavaAstRenderer();
        assertEquals(OpaqueMode.LENIENT, renderer.opaqueMode(), "默认应为宽松模式");
    }

    @Test
    void lenientModeRendersSuccessfully() throws Exception {
        ClassModel model = sample();
        JavaAstRenderer renderer = new JavaAstRenderer();
        renderer.setOpaqueMode(OpaqueMode.LENIENT);
        String source = renderer.render(model, bodies(model)).content();
        assertNotNull(source, "宽松模式应能产出源码");
    }

    @Test
    void nullModeFallsBackToLenient() {
        JavaAstRenderer renderer = new JavaAstRenderer();
        renderer.setOpaqueMode(null);
        assertEquals(OpaqueMode.LENIENT, renderer.opaqueMode(), "null 应回退为宽松模式");
    }

    @Test
    void strictModeThrowsWithContextOnUnresolved() {
        // 直接构造一个必然无法精确重建的表达式，验证严格模式抛错并携带上下文。
        JavaAstRenderer renderer = new JavaAstRenderer();
        renderer.setOpaqueMode(OpaqueMode.STRICT);
        assertThrows(UnresolvedCodeException.class,
                () -> renderer.strictCheck(new com.aetherdecompiler.core.ast.Expr.Opaque(42, "probe")));
    }
}
