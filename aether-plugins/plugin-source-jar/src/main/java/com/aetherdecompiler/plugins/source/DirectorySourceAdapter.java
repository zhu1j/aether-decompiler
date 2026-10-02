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
package com.aetherdecompiler.plugins.source;

import com.aetherdecompiler.api.ClassSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A directory-backed {@link ClassSource}, self-contained inside this plugin.
 *
 * <p>Kept separate from the kernel's own directory source on purpose: a plugin
 * must depend only on {@code aether-plugin-api}, so it cannot borrow kernel
 * classes. Duplicating this tiny walk is the correct price for that isolation.</p>
 *
 * <p>Story analogy: the plugin carries its own pocket torch instead of borrowing
 * the factory's fixed ceiling light.</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class DirectorySourceAdapter implements ClassSource {

    private final Path root;

    /**
     * @param root the directory to scan
     */
    public DirectorySourceAdapter(Path root) {
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
            // Unwalkable directory yields an empty index.
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
        // Nothing to release.
    }
}
