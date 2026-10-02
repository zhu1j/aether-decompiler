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
 * A read-mostly view of the host handed to every plugin at activation time.
 *
 * <p>This is the ONLY door a plugin uses to reach host services. It carries the
 * configuration snapshot, the shared option registry, the event bus, and a
 * resolver for reading further class bytes on demand. By routing everything
 * through one narrow interface, the host can evolve its internals without
 * breaking plugins.</p>
 *
 * <p>Story analogy: the visitor's badge and intercom at the factory gate. The
 * badge says what the visitor may see; the intercom lets them ask the front
 * desk. The visitor never wanders the plant unescorted.</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public interface PluginContext {

    /**
     * @return the immutable configuration snapshot for this run
     */
    Options options();

    /**
     * @return the shared option registry, so a plugin may publish its own options
     */
    OptionRegistry optionRegistry();

    /**
     * @return the event bus, so a plugin may publish or subscribe to events
     */
    EventBus eventBus();

    /**
     * Resolve class bytes by internal name using whatever {@link ClassSource}
     * the host is currently reading from.
     *
     * @param internalName the internal binary name, e.g. {@code com/foo/Bar}
     * @return the class bytes, or {@code null} if not resolvable
     */
    byte[] resolveClassBytes(String internalName);
}
