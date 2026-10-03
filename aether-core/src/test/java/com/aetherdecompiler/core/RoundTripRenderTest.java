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
import com.aetherdecompiler.core.ast.AstBuilder;
import com.aetherdecompiler.core.ast.JavaAstRenderer;
import com.aetherdecompiler.core.ast.MethodBody;
import com.aetherdecompiler.core.model.ClassModel;
import com.aetherdecompiler.core.model.MethodModel;
import org.junit.jupiter.api.Test;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 自研渲染器的 round-trip 回归：把真实字节码反编译为源码后，再造一遍
 * （用 JDK 的 {@link JavaCompiler} 编译生成的源码），以证明输出<strong>语法合法、
 * 可被 javac 接受</strong>。这是 P0~P3 修复的核心验收标准。
 *
 * <p>它直接以 JDK 自带的编译器 API 作为裁判，无需任何外部依赖，因此能在任何
 * JDK 17+ 环境里离线运行。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
class RoundTripRenderTest {

    @Test
    void sampleDecompilesToCompilableJava() throws Exception {
        byte[] bytes = readSampleBytes();
        ClassModel model = new AsmClassParser().parse(bytes);

        AstBuilder builder = new AstBuilder();
        List<MethodBody> bodies = new ArrayList<>();
        for (MethodModel m : model.methods()) {
            if (m.isAbstractOrNative()) {
                continue;
            }
            bodies.add(builder.build(model, m));
        }

        String source = new JavaAstRenderer().render(model, bodies).content();
        assertNotNull(source, "渲染结果不应为空");
        assertTrue(source.contains("class Sample"), "应输出类声明");

        String diagnostics = compile(source, model.name());
        assertTrue(diagnostics.isEmpty(),
                "自研渲染输出必须能通过 javac 编译，但出现错误：\n" + diagnostics + "\n---- 源码 ----\n" + source);
    }

    private static byte[] readSampleBytes() throws Exception {
        String resource = Sample.class.getName().replace('.', '/') + ".class";
        try (var in = Sample.class.getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(in, "测试夹具 Sample.class 必须可用");
            return in.readAllBytes();
        }
    }

    /** 用 JDK 编译器编译源码字符串；返回诊断文本（无错误则为空串）。 */
    private static String compile(String source, String className) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "需要 JDK（而非 JRE）才能运行本测试");
        String simple = className.substring(className.lastIndexOf('/') + 1);
        var src = new SimpleSource(simple + ".java", source);
        var out = new ByteArrayOutputStream();
        var err = new ByteArrayOutputStream();
        var fileManager = compiler.getStandardFileManager(null, null, null);
        boolean ok = compiler.getTask(new java.io.PrintWriter(err), fileManager, null,
                List.of("-proc:none"), null, List.of(src)).call();
        return ok ? "" : err.toString();
    }

    /** 内存中的单文件源码对象。 */
    private static final class SimpleSource extends javax.tools.SimpleJavaFileObject {
        private final String code;

        SimpleSource(String name, String code) {
            super(URI.create("string:///" + name), javax.tools.JavaFileObject.Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }
}
