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
 * Constant pool tag values defined by the JVM specification.
 *
 * <p>These constants are part of the bytecode <em>model</em>, not of any
 * particular library. Recording them here keeps the vocabulary of the data
 * model self-contained: the kernel names the tags itself rather than borrowing
 * names from ASM.</p>
 *
 * <p>Story analogy: a parts catalogue's item numbers, written on the catalogue
 * page itself — the warehouse that stocks the parts is a separate matter.</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class ConstantPoolTag {

    /** UTF-8 string. */
    public static final int UTF8 = 1;
    /** Integer. */
    public static final int INTEGER = 3;
    /** Float. */
    public static final int FLOAT = 4;
    /** Long. */
    public static final int LONG = 5;
    /** Double. */
    public static final int DOUBLE = 6;
    /** Class reference. */
    public static final int CLASS = 7;
    /** String reference. */
    public static final int STRING = 8;
    /** Field reference. */
    public static final int FIELDREF = 9;
    /** Method reference. */
    public static final int METHODREF = 10;
    /** Interface method reference. */
    public static final int INTERFACE_METHODREF = 11;
    /** Name and type. */
    public static final int NAME_AND_TYPE = 12;
    /** Method handle. */
    public static final int METHOD_HANDLE = 15;
    /** Method type. */
    public static final int METHOD_TYPE = 16;
    /** Dynamically computed constant. */
    public static final int DYNAMIC = 17;
    /** Invoke dynamic. */
    public static final int INVOKE_DYNAMIC = 18;
    /** Module. */
    public static final int MODULE = 19;
    /** Package. */
    public static final int PACKAGE = 20;

    private ConstantPoolTag() {
        throw new AssertionError("No instances.");
    }
}
