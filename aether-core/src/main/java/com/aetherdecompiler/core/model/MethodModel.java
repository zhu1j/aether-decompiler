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

import com.aetherdecompiler.api.IRKind;
import com.aetherdecompiler.api.IRObject;

import java.util.List;
import java.util.Objects;

/**
 * An immutable method model.
 *
 * <p>Captures everything the kernel needs to reason about a method without
 * touching ASM again: access flags, name, descriptor, the linear instruction
 * list, the exception table, and the frame size hints. Methods with no code
 * (abstract / native) have an empty instruction list.</p>
 *
 * <p>Story analogy: a single recipe card — its title, its ingredient list in
 * order, and the "if the pan catches fire" contingency lines at the bottom.</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class MethodModel implements IRObject {

    private final int access;
    private final String name;
    private final String descriptor;
    private final String signature;
    private final List<Insn> instructions;
    private final List<TryCatchEntry> tryCatchEntries;
    private final int maxStack;
    private final int maxLocals;

    /**
     * @param access          raw access flags
     * @param name            the method name
     * @param descriptor      the method descriptor, e.g. {@code "(I)J"}
     * @param signature       the generic signature, or {@code null}
     * @param instructions    the immutable instruction list (never {@code null})
     * @param tryCatchEntries the immutable exception table (never {@code null})
     * @param maxStack        the max operand-stack depth, or {@code -1} if abstract
     * @param maxLocals       the max local-variable slots, or {@code -1} if abstract
     */
    public MethodModel(int access, String name, String descriptor, String signature,
                       List<Insn> instructions, List<TryCatchEntry> tryCatchEntries,
                       int maxStack, int maxLocals) {
        this.access = access;
        this.name = Objects.requireNonNull(name, "name");
        this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
        this.signature = signature;
        this.instructions = List.copyOf(instructions);
        this.tryCatchEntries = List.copyOf(tryCatchEntries);
        this.maxStack = maxStack;
        this.maxLocals = maxLocals;
    }

    /** @return the raw access flags */
    public int access() {
        return access;
    }

    /** @return the method name */
    public String name() {
        return name;
    }

    /** @return the method descriptor */
    public String descriptor() {
        return descriptor;
    }

    /** @return the generic signature, or {@code null} */
    public String signature() {
        return signature;
    }

    /** @return the immutable instruction list */
    public List<Insn> instructions() {
        return instructions;
    }

    /** @return the immutable exception table */
    public List<TryCatchEntry> tryCatchEntries() {
        return tryCatchEntries;
    }

    /** @return the max operand-stack depth, or {@code -1} */
    public int maxStack() {
        return maxStack;
    }

    /** @return the max local-variable slots, or {@code -1} */
    public int maxLocals() {
        return maxLocals;
    }

    /** @return {@code true} if this method carries no code */
    public boolean isAbstractOrNative() {
        return AccessFlags.isAbstract(access) || (access & AccessFlags.NATIVE) != 0;
    }

    /** @return the fully-qualified identifier {@code name + descriptor} */
    public String id() {
        return name + descriptor;
    }

    @Override
    public IRKind kind() {
        return IRKind.METHOD_MODEL;
    }

    @Override
    public String toString() {
        return "MethodModel{" + id() + ", " + instructions.size() + " insns}";
    }
}
