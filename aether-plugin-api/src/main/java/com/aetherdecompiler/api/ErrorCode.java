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
package com.aetherdecompiler.api;

/**
 * Stable, machine-readable error codes emitted by the engine.
 *
 * <p>Codes are grouped by a two-digit family prefix so callers can branch
 * cheaply (e.g. {@code AETHER-1xxx} = input/source problems). The numeric code
 * is part of the public contract and must never be reused for a different
 * meaning.</p>
 *
 * <p>Story analogy: these are the fault lamps on a factory control panel. The
 * factory never speaks in prose to the operator — it lights a specific,
 * documented lamp.</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public enum ErrorCode {

    /** A class file / jar could not be located or opened. */
    INPUT_NOT_FOUND(1001, "Input not found"),

    /** The bytes handed to the engine are not a valid class file. */
    INPUT_MALFORMED(1002, "Malformed class file"),

    /** The class file version is newer than this engine's ASM backend supports. */
    INPUT_UNSUPPORTED_VERSION(1003, "Unsupported class file version"),

    /** A referenced class the engine expected to read was absent. */
    INPUT_MISSING_REFERENCE(1004, "Missing class reference"),

    /** The control-flow graph could not be constructed for a method. */
    CFG_CONSTRUCTION_FAILED(2001, "CFG construction failed"),

    /** A method body referenced a bytecode offset that has no basic block. */
    CFG_DANGLING_JUMP_TARGET(2002, "Dangling jump target"),

    /** A method failed during SSA construction. */
    SSA_CONSTRUCTION_FAILED(3001, "SSA construction failed"),

    /** Type inference reached an inconsistent state for a stack slot. */
    TYPE_INFERENCE_FAILED(3002, "Type inference failed"),

    /** A plugin refused or failed to fulfil a request. */
    PLUGIN_FAILURE(4001, "Plugin failure"),

    /** A plugin declared metadata that violates the contract. */
    PLUGIN_INVALID_METADATA(4002, "Invalid plugin metadata"),

    /** An internal invariant of the engine was violated. */
    INTERNAL_INVARIANT(9001, "Internal invariant violated"),

    /** A catch-all for errors that do not yet have a dedicated code. */
    UNKNOWN(9999, "Unknown error");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * @return the stable numeric code, part of the public contract
     */
    public int code() {
        return code;
    }

    /**
     * @return a short human-readable label for this code
     */
    public String message() {
        return message;
    }

    /**
     * @return the zero-padded, prefixed form used in logs and CLI output,
     *         e.g. {@code "AETHER-1002"}
     */
    public String tag() {
        return "AETHER-" + code;
    }
}
