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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * An immutable snapshot of engine configuration.
 *
 * <p>Design contract (frozen at 1.0-major): the kernel consumes an
 * {@code Options} value and never parses command-line strings itself. Option
 * <em>definitions</em> (name, type, default, help text) are contributed by
 * plugins through {@link OptionRegistry}; the application layer (CLI/GUI)
 * parses user input into an {@code Options} and hands it down. This keeps the
 * kernel free of any UI concern and lets plugins add options without the
 * kernel changing.</p>
 *
 * <p>Story analogy: {@code Options} is the sealed, signed work-order that
 * travels from the front desk into the factory floor. The floor never takes
 * verbal requests; it only executes a fully-specified signed order.</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class Options {

    private static final Options EMPTY = new Options(Collections.emptyMap());

    private final Map<String, Object> values;

    private Options(Map<String, Object> values) {
        this.values = values;
    }

    /**
     * @return the empty option set (all defaults)
     */
    public static Options empty() {
        return EMPTY;
    }

    /**
     * @return a fresh builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return the raw value for a key, or {@code null}
     */
    public Object raw(String key) {
        return values.get(key);
    }

    /**
     * @param key the option key
     * @return the string value for a key, if present
     */
    public Optional<String> getString(String key) {
        Object v = values.get(key);
        return v == null ? Optional.empty() : Optional.of(String.valueOf(v));
    }

    /**
     * @param key          the option key
     * @param defaultValue value returned when absent
     * @return the boolean value, coerced from string/boolean storage
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        Object v = values.get(key);
        if (v == null) {
            return defaultValue;
        }
        if (v instanceof Boolean b) {
            return b;
        }
        return Boolean.parseBoolean(String.valueOf(v));
    }

    /**
     * @param key          the option key
     * @param defaultValue value returned when absent or unparseable
     * @return the integer value
     */
    public int getInt(String key, int defaultValue) {
        Object v = values.get(key);
        if (v == null) {
            return defaultValue;
        }
        if (v instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * @param key          the option key
     * @param defaultValue value returned when absent or unparseable
     * @return the long value
     */
    public long getLong(String key, long defaultValue) {
        Object v = values.get(key);
        if (v == null) {
            return defaultValue;
        }
        if (v instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * @return an immutable view of all set values
     */
    public Map<String, Object> asMap() {
        return values;
    }

    /**
     * Returns a new {@code Options} with one key added or replaced, leaving
     * this instance untouched.
     *
     * @param key   the option key
     * @param value the new value
     * @return a new immutable snapshot
     */
    public Options with(String key, Object value) {
        Objects.requireNonNull(key, "key");
        Map<String, Object> copy = new LinkedHashMap<>(values);
        copy.put(key, value);
        return new Options(Collections.unmodifiableMap(copy));
    }

    /**
     * Fluent builder for {@code Options}. Insertion order is preserved so
     * debugging dumps are deterministic.
     *
     * @author Jerry Zhu (Zeek)
     */
    public static final class Builder {
        private final Map<String, Object> values = new LinkedHashMap<>();

        private Builder() {
        }

        /**
         * @param key   the option key
         * @param value the value
         * @return this builder
         */
        public Builder set(String key, Object value) {
            Objects.requireNonNull(key, "key");
            values.put(key, value);
            return this;
        }

        /**
         * @return the immutable {@code Options}
         */
        public Options build() {
            if (values.isEmpty()) {
                return EMPTY;
            }
            return new Options(Collections.unmodifiableMap(new LinkedHashMap<>(values)));
        }
    }

    @Override
    public String toString() {
        return "Options" + values;
    }
}
