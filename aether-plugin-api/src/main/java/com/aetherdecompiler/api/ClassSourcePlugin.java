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
 * Extension point #1 — a provider of class bytes.
 *
 * <p>Implementations live in plugins (e.g. {@code plugin-source-jar}) and are
 * discovered through {@link java.util.ServiceLoader}. Keeping class-origin
 * behind a plugin means the kernel never hardcodes "jar" or "directory": adding
 * a new origin (network, memory, encrypted archive) is pure plugin work.</p>
 *
 * <p>Story analogy: the extension point is a power socket of a fixed shape.
 * Any appliance — a jar, a folder, a memory buffer — can be plugged in without
 * rewiring the factory.</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public interface ClassSourcePlugin {

    /**
     * @return a unique, stable plugin id, e.g. {@code "source.jar"}
     */
    String id();

    /**
     * @return a human-readable name for UI display
     */
    String displayName();

    /**
     * Open a class source for the given, plugin-specific locator.
     *
     * @param locator a plugin-defined locator string (path, URL, key, ...)
     * @param options the engine configuration snapshot
     * @return an open {@link ClassSource}; caller is responsible for closing it
     */
    ClassSource open(String locator, Options options);
}
