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
 * A single rendered output unit produced by a {@link RenderPlugin}.
 *
 * <p>This is the frozen "output model" contract: a renderer never writes to
 * disk itself. It returns one or more {@code SourceTree} units (a file's path,
 * its text content, and an optional source map back to bytecode). The
 * application layer (CLI/GUI) decides whether to write, display, or stream
 * them. This keeps the kernel and render plugins free of any I/O concern.</p>
 *
 * <p>Story analogy: a renderer is a printer that produces sheets of paper. It
 * never decides where the sheets are filed — the front office does.</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class SourceTree {

    private final String path;
    private final String content;
    private final SourceMapping mapping;

    private SourceTree(String path, String content, SourceMapping mapping) {
        this.path = path;
        this.content = content;
        this.mapping = mapping;
    }

    /**
     * @param path    the relative output path, e.g. {@code com/foo/Bar.java}
     * @param content the rendered text
     * @param mapping the source map, or {@code null} when unavailable
     * @return a new rendered unit
     */
    public static SourceTree of(String path, String content, SourceMapping mapping) {
        return new SourceTree(path, content, mapping);
    }

    /** @return the relative output path */
    public String path() {
        return path;
    }

    /** @return the rendered text content */
    public String content() {
        return content;
    }

    /** @return the source map, or {@code null} if the renderer did not produce one */
    public SourceMapping mapping() {
        return mapping;
    }

    @Override
    public String toString() {
        return "SourceTree{" + path + ", " + (content == null ? 0 : content.length()) + " chars}";
    }
}
