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

import java.util.Objects;

/**
 * Base type for every event emitted by the engine pipeline.
 *
 * <p>An event always carries: the pipeline phase it belongs to, the abstract
 * kind of the object at that point, a human message, and the current time. It
 * optionally carries the {@link IRObject} itself so a listener can inspect the
 * intermediate result.</p>
 *
 * <p>Story analogy: a shipping label plus a peek-hole. The label says which
 * station and which stage; the peek-hole optionally lets a listener see the
 * physical part.</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public class AetherEvent {

    /**
     * The coarse pipeline phase an event belongs to. Finer granularity than
     * this is intentionally avoided so listeners stay cheap.
     */
    public enum Phase {
        /** Reading raw class bytes. */
        READ,
        /** Building the class / method models. */
        MODEL,
        /** Building the CFG and dominator tree. */
        CFG,
        /** Building SSA and inferring types. */
        SSA,
        /** Building the abstract syntax tree. */
        AST,
        /** A plugin-specific phase. */
        PLUGIN
    }

    private final Phase phase;
    private final IRKind kind;
    private final String message;
    private final long timestampNanos;
    private final IRObject payload;

    /**
     * @param phase         the pipeline phase (never {@code null})
     * @param kind          the kind of intermediate object (never {@code null})
     * @param message       a human-readable description
     * @param payload       the intermediate object, or {@code null} if none
     */
    public AetherEvent(Phase phase, IRKind kind, String message, IRObject payload) {
        this.phase = Objects.requireNonNull(phase, "phase");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.message = message == null ? "" : message;
        this.payload = payload;
        this.timestampNanos = System.nanoTime();
    }

    /** @return the pipeline phase */
    public Phase phase() {
        return phase;
    }

    /** @return the kind of intermediate object */
    public IRKind kind() {
        return kind;
    }

    /** @return a human-readable description */
    public String message() {
        return message;
    }

    /** @return the monotonic timestamp captured when the event was created */
    public long timestampNanos() {
        return timestampNanos;
    }

    /** @return the intermediate object, or {@code null} if not attached */
    public IRObject payload() {
        return payload;
    }

    @Override
    public String toString() {
        return "[" + phase + "/" + kind + "] " + message;
    }
}
