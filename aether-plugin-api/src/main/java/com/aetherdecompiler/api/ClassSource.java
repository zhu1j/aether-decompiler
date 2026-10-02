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

import java.util.List;
import java.util.Optional;

/**
 * A random-access source of class bytes.
 *
 * <p>The contract exists to satisfy the "progressive / lazy loading" hard
 * constraint: the engine must be able to list available class names cheaply and
 * read exactly one class's bytes on demand, without loading an entire jar into
 * memory. Implementations back this with jar files, directories, single
 * {@code .class} files, or in-memory byte arrays.</p>
 *
 * <p>Story analogy: a library card catalogue. You can read the index (cheap)
 * and then pull a single book from the shelf (on demand) — you never photocopy
 * the whole library.</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public interface ClassSource extends AutoCloseable {

    /**
     * @return a short, human-readable description of this source, for logs
     */
    String describe();

    /**
     * @return the internal binary names (e.g. {@code com/foo/Bar}) of all
     *         classes this source can provide, without reading their bytes
     */
    List<String> classNames();

    /**
     * @param internalName the internal binary name, e.g. {@code com/foo/Bar}
     * @return the raw class bytes, or {@link Optional#empty()} if absent
     */
    Optional<byte[]> readClass(String internalName);

    @Override
    void close();
}
