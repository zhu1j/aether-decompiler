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
package com.aetherdecompiler.core.model;

import java.util.Objects;

/**
 * An immutable entry of a method's exception table.
 *
 * <p>Records the try range, the handler's entry offset, and the caught type
 * (or {@code null} for a {@code finally}/catch-all handler). Exception edges are
 * first-class in control-flow construction, so they are modelled explicitly
 * rather than derived on demand.</p>
 *
 * <p>Story analogy: a fire-escape map on the wall — "if trouble happens between
 * rooms 10 and 20, exit here, for this class of fire."</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class TryCatchEntry {

    private final int startIndex;
    private final int endIndex;
    private final int handlerIndex;
    private final String catchType;

    /**
     * @param startIndex   inclusive first instruction index of the protected range
     * @param endIndex     exclusive last instruction index of the protected range
     * @param handlerIndex the instruction index of the handler entry
     * @param catchType    the caught internal type name, or {@code null} for any
     */
    public TryCatchEntry(int startIndex, int endIndex, int handlerIndex, String catchType) {
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.handlerIndex = handlerIndex;
        this.catchType = catchType;
    }

    /** @return inclusive first instruction index of the protected range */
    public int startIndex() {
        return startIndex;
    }

    /** @return exclusive last instruction index of the protected range */
    public int endIndex() {
        return endIndex;
    }

    /** @return the handler entry instruction index */
    public int handlerIndex() {
        return handlerIndex;
    }

    /** @return the caught internal type name, or {@code null} for catch-all */
    public String catchType() {
        return catchType;
    }

    /** @return {@code true} when this is a catch-all / {@code finally} handler */
    public boolean isCatchAll() {
        return catchType == null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TryCatchEntry other)) {
            return false;
        }
        return startIndex == other.startIndex
                && endIndex == other.endIndex
                && handlerIndex == other.handlerIndex
                && Objects.equals(catchType, other.catchType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startIndex, endIndex, handlerIndex, catchType);
    }

    @Override
    public String toString() {
        return "try[" + startIndex + "," + endIndex + ") -> " + handlerIndex
                + " (" + (catchType == null ? "any" : catchType) + ")";
    }
}
