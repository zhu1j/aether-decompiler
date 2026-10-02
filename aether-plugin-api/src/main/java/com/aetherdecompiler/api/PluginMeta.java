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

import java.util.Objects;

/**
 * Immutable, declarative metadata describing a plugin.
 *
 * <p>Metadata lets the host list, order, and version-check plugins without
 * instantiating their heavy code. The {@code apiVersion} field is the seam that
 * enforces the backwards-compatibility promise: the host refuses to activate a
 * plugin built against a newer, incompatible plugin API major.</p>
 *
 * <p>Story analogy: the nameplate and electrical rating stamped on every
 * appliance. The factory checks the rating before plugging anything in.</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class PluginMeta {

    private final String id;
    private final String displayName;
    private final String version;
    private final String apiVersion;
    private final String vendor;

    private PluginMeta(String id, String displayName, String version,
                       String apiVersion, String vendor) {
        this.id = Objects.requireNonNull(id, "id");
        this.displayName = displayName == null ? id : displayName;
        this.version = version == null ? "0.0.0" : version;
        this.apiVersion = apiVersion == null ? AetherVersion.VERSION : apiVersion;
        this.vendor = vendor == null ? "" : vendor;
    }

    /**
     * @param id          unique plugin id, e.g. {@code "render.dot"}
     * @param displayName human-readable name
     * @param version     the plugin's own version
     * @param apiVersion  the aether plugin-API version it targets
     * @param vendor      the author/vendor string
     * @return a new metadata record
     */
    public static PluginMeta of(String id, String displayName, String version,
                                String apiVersion, String vendor) {
        return new PluginMeta(id, displayName, version, apiVersion, vendor);
    }

    /** @return the unique plugin id */
    public String id() {
        return id;
    }

    /** @return the human-readable name */
    public String displayName() {
        return displayName;
    }

    /** @return the plugin's own version */
    public String version() {
        return version;
    }

    /** @return the targeted plugin-API version */
    public String apiVersion() {
        return apiVersion;
    }

    /** @return the author/vendor string */
    public String vendor() {
        return vendor;
    }

    @Override
    public String toString() {
        return displayName + " " + version + " (" + id + ", api " + apiVersion + ")";
    }
}
