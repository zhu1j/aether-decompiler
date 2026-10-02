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

import java.util.Objects;

/**
 * An immutable field model.
 *
 * <p>Fields carry no behaviour, so the model is small: access flags, name,
 * descriptor, and generic signature. It exists so the class model is complete
 * and a future renderer can emit field declarations without revisiting ASM.</p>
 *
 * <p>Story analogy: a nameplate riveted to a machine — it identifies the part,
 * but the part does nothing by itself.</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class FieldModel {

    private final int access;
    private final String name;
    private final String descriptor;
    private final String signature;

    /**
     * @param access     raw access flags
     * @param name       the field name
     * @param descriptor the field descriptor, e.g. {@code "Ljava/lang/String;"}
     * @param signature  the generic signature, or {@code null}
     */
    public FieldModel(int access, String name, String descriptor, String signature) {
        this.access = access;
        this.name = Objects.requireNonNull(name, "name");
        this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
        this.signature = signature;
    }

    /** @return the raw access flags */
    public int access() {
        return access;
    }

    /** @return the field name */
    public String name() {
        return name;
    }

    /** @return the field descriptor */
    public String descriptor() {
        return descriptor;
    }

    /** @return the generic signature, or {@code null} */
    public String signature() {
        return signature;
    }

    @Override
    public String toString() {
        return "FieldModel{" + name + ":" + descriptor + "}";
    }
}
