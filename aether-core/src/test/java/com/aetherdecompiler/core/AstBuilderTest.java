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

import com.aetherdecompiler.api.SourceMapping;
import com.aetherdecompiler.api.SourceTree;
import com.aetherdecompiler.core.asm.AsmClassParser;
import com.aetherdecompiler.core.ast.AstBuilder;
import com.aetherdecompiler.core.ast.JavaAstRenderer;
import com.aetherdecompiler.core.ast.MethodBody;
import com.aetherdecompiler.core.ast.Stmt;
import com.aetherdecompiler.core.model.ClassModel;
import com.aetherdecompiler.core.model.MethodModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AST 构造与渲染的单元测试。
 *
 * @author Jerry Zhu (Zeek)
 */
class AstBuilderTest {

    private ClassModel sample() throws Exception {
        String resource = Sample.class.getName().replace('.', '/') + ".class";
        try (var in = Sample.class.getClassLoader().getResourceAsStream(resource)) {
            return new AsmClassParser().parse(in.readAllBytes());
        }
    }

    private MethodBody bodyFor(ClassModel model, String name, String desc) {
        MethodModel method = model.findMethod(name, desc).orElseThrow();
        return new AstBuilder().build(model, method);
    }

    @Test
    void loopIsRecoveredAsStructuredStatement() throws Exception {
        MethodBody body = bodyFor(sample(), "sumTo", "(I)I");
        assertNotNull(body.body());
        assertTrue(containsWhile(body.body()), "sumTo should recover a while loop");
    }

    @Test
    void straightLineMethodHasNoLoop() throws Exception {
        MethodBody body = bodyFor(sample(), "square", "(I)I");
        assertFalse(containsWhile(body.body()), "square has no loop");
    }

    @Test
    void rendererProducesJavaAndMapping() throws Exception {
        MethodBody body = bodyFor(sample(), "sumTo", "(I)I");
        SourceTree tree = new JavaAstRenderer().render(body);
        assertNotNull(tree);
        assertTrue(tree.content().contains("class Sample"), tree.content());
        assertNotNull(tree.mapping(), "renderer must emit a source mapping");
        assertTrue(tree.mapping().spans().size() > 0, "mapping must contain spans");
        SourceMapping mapping = tree.mapping();
        SourceMapping.Span first = mapping.spans().get(0);
        assertNotNull(mapping.atGenerated(first.generatedStartLine(), first.generatedStartCol()));
    }

    private boolean containsWhile(com.aetherdecompiler.core.ast.AstNode node) {
        if (node instanceof Stmt.While) {
            return true;
        }
        for (com.aetherdecompiler.core.ast.AstNode child : node.children()) {
            if (containsWhile(child)) {
                return true;
            }
        }
        return false;
    }
}
