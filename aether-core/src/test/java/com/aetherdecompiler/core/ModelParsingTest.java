/*
 * aether-decompiler — an independent, reusable JVM decompilation engine.
 * Copyright 2026 Jerry Zhu (Zeek) <zhujiejava1@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author Jerry Zhu (Zeek)
 * "Run the Code, Run the World!"
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
 * Unit tests for the ASM isolation seam and the immutable model.
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
