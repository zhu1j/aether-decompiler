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
package com.aetherdecompiler.core.source;

import com.aetherdecompiler.api.ClassSource;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * An in-memory {@link ClassSource} over a map of name to bytes.
 *
 * <p>Used by unit tests and by callers that already hold class bytes (for
 * example, a network plugin or an IDE buffer).</p>
 *
 * <p>Story analogy: a toolbox holding exactly the parts you already picked up —
 * no cabinet, no walk, just what is in hand.</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class InMemoryClassSource implements ClassSource {

    private final String label;
    private final Map<String, byte[]> classes;

    /**
     * @param label   a description for logs
     * @param classes the map of internal name to bytes
     */
    public InMemoryClassSource(String label, Map<String, byte[]> classes) {
        this.label = label == null ? "memory" : label;
        this.classes = Map.copyOf(classes);
    }

    @Override
    public String describe() {
        return "memory:" + label;
    }

    @Override
    public List<String> classNames() {
        return List.copyOf(classes.keySet());
    }

    @Override
    public Optional<byte[]> readClass(String internalName) {
        byte[] bytes = classes.get(internalName);
        return bytes == null ? Optional.empty() : Optional.of(bytes.clone());
    }

    @Override
    public void close() {
        // Nothing to release for an in-memory source.
    }
}
