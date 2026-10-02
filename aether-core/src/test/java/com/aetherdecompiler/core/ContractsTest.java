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
 * Unit tests for the frozen Phase0 contracts: options and the error model.
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
