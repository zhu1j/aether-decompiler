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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A registry of {@link OptionSpec}s contributed by core and plugins.
 *
 * <p>The kernel exposes an {@code OptionRegistry} to every plugin during
 * activation so plugins can publish their options. The application layer later
 * queries the same registry to build its CLI/GUI. This is the mechanism that
 * keeps "which options exist" out of the kernel's source code.</p>
 *
 * <p>Story analogy: the registry is the factory's master switchboard index —
 * every department screws its switches onto the same board, and the operator
 * reads the whole board from one place.</p>
 *
 * <p>This class is deliberately narrow and not thread-safe; plugins register
 * during a single-threaded activation phase.</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class OptionRegistry {

    private final Map<String, OptionSpec> specs = new LinkedHashMap<>();

    /**
     * Register a spec. Re-registering the same key replaces the previous spec;
     * this makes plugin reloading idempotent.
     *
     * @param spec the spec to register
     * @return this registry, for chaining
     */
    public OptionRegistry register(OptionSpec spec) {
        specs.put(spec.key(), spec);
        return this;
    }

    /**
     * @param key the option key
     * @return the spec for a key, if defined
     */
    public Optional<OptionSpec> find(String key) {
        return Optional.ofNullable(specs.get(key));
    }

    /**
     * @return all registered specs, in registration order
     */
    public Collection<OptionSpec> all() {
        return Collections.unmodifiableCollection(specs.values());
    }

    /**
     * @return an immutable copy of the key-to-spec map
     */
    public Map<String, OptionSpec> asMap() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(specs));
    }
}
