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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A {@link ClassSource} backed by a directory tree of {@code .class} files.
 *
 * <p>Listing walks the tree once and records relative class names; reading
 * opens exactly one file on demand. Nothing is preloaded, satisfying the lazy
 * loading constraint.</p>
 *
 * <p>Story analogy: a filing cabinet of folders. The index card lists every
 * folder; you open one drawer only when you actually need its file.</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class DirectoryClassSource implements ClassSource {

    private final Path root;

    /**
     * @param root the directory to scan
     */
    public DirectoryClassSource(Path root) {
        this.root = root;
    }

    @Override
    public String describe() {
        return "directory:" + root;
    }

    @Override
    public List<String> classNames() {
        List<String> names = new ArrayList<>();
        if (!Files.isDirectory(root)) {
            return names;
        }
        try (Stream<Path> walk = Files.walk(root)) {
            walk.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".class"))
                    .forEach(p -> {
                        String rel = root.relativize(p).toString().replace('\\', '/');
                        names.add(rel.substring(0, rel.length() - ".class".length()));
                    });
        } catch (IOException ex) {
            // A directory that cannot be walked simply yields no names.
        }
        return names;
    }

    @Override
    public Optional<byte[]> readClass(String internalName) {
        Path file = root.resolve(internalName + ".class");
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readAllBytes(file));
        } catch (IOException ex) {
            return Optional.empty();
        }
    }

    @Override
    public void close() {
        // Nothing to release for a directory source.
    }
}
