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
package com.aetherdecompiler.core.model;

/**
 * Human-readable names for access-flag bits, plus the bit masks used by the JVM
 * class file format.
 *
 * <p>Access flags are pure bytecode facts; naming them here keeps the model
 * independent of ASM's {@code Opcodes} constants (which re-expose the same bits
 * but would leak a library type if used directly in models).</p>
 *
 * <p>Story analogy: the legend printed on a schematic — "these little numbers
 * mean public, final, static". The legend belongs to the drawing, not to the
 * pencil maker.</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class AccessFlags {

    public static final int PUBLIC = 0x0001;
    public static final int PRIVATE = 0x0002;
    public static final int PROTECTED = 0x0004;
    public static final int STATIC = 0x0008;
    public static final int FINAL = 0x0010;
    public static final int SUPER = 0x0020;
    public static final int SYNCHRONIZED = 0x0020;
    public static final int VOLATILE = 0x0040;
    public static final int BRIDGE = 0x0040;
    public static final int TRANSIENT = 0x0080;
    public static final int VARARGS = 0x0080;
    public static final int NATIVE = 0x0100;
    public static final int INTERFACE = 0x0200;
    public static final int ABSTRACT = 0x0400;
    public static final int STRICT = 0x0800;
    public static final int SYNTHETIC = 0x1000;
    public static final int ANNOTATION = 0x2000;
    public static final int ENUM = 0x4000;
    public static final int MODULE = 0x8000;

    private AccessFlags() {
        throw new AssertionError("No instances.");
    }

    /**
     * @param flags the raw access bits
     * @return {@code true} if the {@code public} bit is set
     */
    public static boolean isPublic(int flags) {
        return (flags & PUBLIC) != 0;
    }

    /**
     * @param flags the raw access bits
     * @return {@code true} if the {@code static} bit is set
     */
    public static boolean isStatic(int flags) {
        return (flags & STATIC) != 0;
    }

    /**
     * @param flags the raw access bits
     * @return {@code true} if the {@code final} bit is set
     */
    public static boolean isFinal(int flags) {
        return (flags & FINAL) != 0;
    }

    /**
     * @param flags the raw access bits
     * @return {@code true} if the {@code abstract} bit is set
     */
    public static boolean isAbstract(int flags) {
        return (flags & ABSTRACT) != 0;
    }
}
