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
import java.util.Optional;

/**
 * An immutable class model — the root artefact of the kernel's model layer.
 *
 * <p>Produced once from class bytes and then shared freely: it is safe to cache,
 * read from many threads, and stream in parallel. It contains only bytecode
 * facts (names, descriptors, flags, members) and no Java-syntax notion.</p>
 *
 * <p>Story analogy: the master blueprint of one building — its address, its
 * floor plan (methods), and its fixed fittings (fields) — copied rather than
 * redrawn each time someone needs to look at it.</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class ClassModel implements IRObject {

    private final String name;
    private final String superName;
    private final List<String> interfaces;
    private final int access;
    private final int majorVersion;
    private final int minorVersion;
    private final String signature;
    private final List<FieldModel> fields;
    private final List<MethodModel> methods;

    /**
     * @param name         the internal binary name, e.g. {@code "com/foo/Bar"}
     * @param superName    the internal super-class name, or {@code null} for {@code Object}
     * @param interfaces   the internal interface names (never {@code null})
     * @param access       the raw class access flags
     * @param majorVersion the class-file major version
     * @param minorVersion the class-file minor version
     * @param signature    the generic class signature, or {@code null}
     * @param fields       the immutable field list (never {@code null})
     * @param methods      the immutable method list (never {@code null})
     */
    public ClassModel(String name, String superName, List<String> interfaces, int access,
                      int majorVersion, int minorVersion, String signature,
                      List<FieldModel> fields, List<MethodModel> methods) {
        this.name = Objects.requireNonNull(name, "name");
        this.superName = superName;
        this.interfaces = List.copyOf(interfaces);
        this.access = access;
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
        this.signature = signature;
        this.fields = List.copyOf(fields);
        this.methods = List.copyOf(methods);
    }

    /** @return the internal binary name */
    public String name() {
        return name;
    }

    /** @return the internal super-class name, or {@code null} */
    public String superName() {
        return superName;
    }

    /** @return the internal interface names */
    public List<String> interfaces() {
        return interfaces;
    }

    /** @return the raw class access flags */
    public int access() {
        return access;
    }

    /** @return the class-file major version */
    public int majorVersion() {
        return majorVersion;
    }

    /** @return the class-file minor version */
    public int minorVersion() {
        return minorVersion;
    }

    /** @return the generic class signature, or {@code null} */
    public String signature() {
        return signature;
    }

    /** @return the immutable field list */
    public List<FieldModel> fields() {
        return fields;
    }

    /** @return the immutable method list */
    public List<MethodModel> methods() {
        return methods;
    }

    /** @return {@code true} if this class is an interface */
    public boolean isInterface() {
        return (access & AccessFlags.INTERFACE) != 0;
    }

    /**
     * @param name      the method name
     * @param descriptor the method descriptor
     * @return the first matching method, if present
     */
    public Optional<MethodModel> findMethod(String name, String descriptor) {
        return methods.stream()
                .filter(m -> m.name().equals(name) && m.descriptor().equals(descriptor))
                .findFirst();
    }

    /** @return the dotted, human-readable name, e.g. {@code com.foo.Bar} */
    public String dottedName() {
        return name.replace('/', '.');
    }

    @Override
    public IRKind kind() {
        return IRKind.CLASS_MODEL;
    }

    @Override
    public String toString() {
        return "ClassModel{" + name
                + ", fields=" + fields.size()
                + ", methods=" + methods.size()
                + ", major=" + majorVersion + "}";
    }
}
