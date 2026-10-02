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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * A {@link ClassSource} backed by a jar/zip archive.
 *
 * <p>The archive's central directory is scanned once to build a name index;
 * individual entries are read on demand. This directly realises the
 * progressive/lazy-loading constraint: opening a large jar never decompresses
 * every class.</p>
 *
 * <p>Story analogy: a sealed shipping container with a printed manifest. You
 * read the manifest (the index) immediately; you open one crate inside only
 * when you actually need its contents.</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class JarClassSource implements ClassSource {

    private final ZipFile zip;
    private final List<String> names = new ArrayList<>();

    /**
     * @param zip an opened zip/jar file
     */
    public JarClassSource(ZipFile zip) {
        this.zip = zip;
        Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String name = entry.getName();
            if (!entry.isDirectory() && name.endsWith(".class")) {
                names.add(name.substring(0, name.length() - ".class".length()));
            }
        }
    }

    @Override
    public String describe() {
        return "jar:" + zip.getName() + " (" + names.size() + " classes)";
    }

    @Override
    public List<String> classNames() {
        return List.copyOf(names);
    }

    @Override
    public Optional<byte[]> readClass(String internalName) {
        ZipEntry entry = zip.getEntry(internalName + ".class");
        if (entry == null) {
            return Optional.empty();
        }
        try (InputStream in = zip.getInputStream(entry)) {
            return Optional.of(in.readAllBytes());
        } catch (IOException ex) {
            return Optional.empty();
        }
    }

    @Override
    public void close() {
        try {
            zip.close();
        } catch (IOException ex) {
            // Closing a read-only archive cannot fail meaningfully.
        }
    }
}
