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
 * Declarative metadata for a single configuration option.
 *
 * <p>Plugins declare their options as {@code OptionSpec}s and register them in
 * the {@link OptionRegistry}. The kernel and application layers can then
 * discover, validate, document, and parse those options generically — without
 * knowing anything about which plugin declared them.</p>
 *
 * <p>Story analogy: this is the label on a control-panel switch: its name, its
 * physical type, its factory default, and the sticker that explains it.</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class OptionSpec {

    /**
     * The value domain an option accepts. Kept deliberately small and
     * dependency-free.
     */
    public enum Kind {
        /** A yes/no flag. */
        BOOLEAN,
        /** A whole number. */
        INTEGER,
        /** A long integer. */
        LONG,
        /** Arbitrary text. */
        STRING
    }

    private final String key;
    private final Kind kind;
    private final String defaultValue;
    private final String description;
    private final String declaredBy;

    private OptionSpec(String key, Kind kind, String defaultValue,
                       String description, String declaredBy) {
        this.key = Objects.requireNonNull(key, "key");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.defaultValue = defaultValue;
        this.description = description == null ? "" : description;
        this.declaredBy = declaredBy == null ? "aether" : declaredBy;
    }

    /**
     * @param key          the option key, e.g. {@code "output.dir"}
     * @param kind         the value domain
     * @param defaultValue the default value as text (may be {@code null})
     * @param description  human help text
     * @param declaredBy   the declaring plugin id (or {@code "aether"} for core)
     * @return a new spec
     */
    public static OptionSpec of(String key, Kind kind, String defaultValue,
                                String description, String declaredBy) {
        return new OptionSpec(key, kind, defaultValue, description, declaredBy);
    }

    /**
     * @param key         the option key
     * @param defaultValue the default text value
     * @param description help text
     * @param declaredBy  the declaring plugin id
     * @return a {@code STRING} spec
     */
    public static OptionSpec string(String key, String defaultValue, String description, String declaredBy) {
        return new OptionSpec(key, Kind.STRING, defaultValue, description, declaredBy);
    }

    /**
     * @param key         the option key
     * @param defaultValue the default boolean as text
     * @param description help text
     * @param declaredBy  the declaring plugin id
     * @return a {@code BOOLEAN} spec
     */
    public static OptionSpec bool(String key, boolean defaultValue, String description, String declaredBy) {
        return new OptionSpec(key, Kind.BOOLEAN, Boolean.toString(defaultValue), description, declaredBy);
    }

    /** @return the option key */
    public String key() {
        return key;
    }

    /** @return the value domain */
    public Kind kind() {
        return kind;
    }

    /** @return the default value as text, or {@code null} */
    public String defaultValue() {
        return defaultValue;
    }

    /** @return the human help text */
    public String description() {
        return description;
    }

    /** @return the declaring plugin id, or {@code "aether"} for core */
    public String declaredBy() {
        return declaredBy;
    }

    @Override
    public String toString() {
        return "OptionSpec{" + key + " : " + kind
                + (defaultValue != null ? " = " + defaultValue : "")
                + " by " + declaredBy + "}";
    }
}
