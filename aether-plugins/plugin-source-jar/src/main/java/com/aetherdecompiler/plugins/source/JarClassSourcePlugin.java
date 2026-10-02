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

import com.aetherdecompiler.api.AetherException;
import com.aetherdecompiler.api.ClassSource;
import com.aetherdecompiler.api.ClassSourcePlugin;
import com.aetherdecompiler.api.ErrorCode;
import com.aetherdecompiler.api.Options;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;

/**
 * Official {@link ClassSourcePlugin}: opens classes from a jar/zip file or a
 * directory of {@code .class} files.
 *
 * <p>Discovered through {@code ServiceLoader}. The plugin's locator is a file
 * path; the plugin decides whether the path denotes an archive or a directory.
 * It depends only on {@code aether-plugin-api} — never on the kernel — which is
 * exactly the isolation the architecture demands of third-party plugins.</p>
 *
 * <p>Story analogy: a multi-format media reader. Hand it a tape or a disc; it
 * recognises the medium and opens the right tray.</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class JarClassSourcePlugin implements ClassSourcePlugin {

    /** Stable plugin id. */
    public static final String ID = "source.jar";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "Jar / Directory Class Source";
    }

    @Override
    public ClassSource open(String locator, Options options) {
        Path path = Path.of(locator);
        if (!Files.exists(path)) {
            throw new AetherException(ErrorCode.INPUT_NOT_FOUND, "Input does not exist: " + locator);
        }
        if (Files.isDirectory(path)) {
            // Directory support lives in the kernel; a plugin may also reuse it.
            return new com.aetherdecompiler.plugins.source.DirectorySourceAdapter(path);
        }
        try {
            return new JarClassSource(new ZipFile(path.toFile()));
        } catch (IOException ex) {
            throw new AetherException(ErrorCode.INPUT_MALFORMED,
                    "Cannot open archive: " + locator, ex);
        }
    }
}
