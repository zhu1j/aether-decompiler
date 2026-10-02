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
package com.aetherdecompiler.core;

/**
 * A small, deliberately branchy sample used by the kernel's unit tests.
 *
 * <p>It contains a loop, a conditional, a switch, a try/catch, and a method with
 * no code (an interface), so the CFG builder and the dominator tree are
 * exercised against several shapes of real control flow.</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class Sample {

    /** A field, to exercise field modelling. */
    public int counter;

    /** A no-code interface, to exercise abstract methods. */
    public interface Transformer {
        /**
         * @param x the input
         * @return the transformed value
         */
        int apply(int x);
    }

    /**
     * Sum 1..n with a loop and a guard.
     *
     * @param n the upper bound
     * @return the sum, or -1 for negative input
     */
    public int sumTo(int n) {
        if (n < 0) {
            return -1;
        }
        int total = 0;
        for (int i = 1; i <= n; i++) {
            total += i;
            if (total > 1_000_000) {
                break;
            }
        }
        return total;
    }

    /**
     * A switch over small integers.
     *
     * @param kind the selector
     * @return a label
     */
    public String label(int kind) {
        switch (kind) {
            case 0:
                return "zero";
            case 1:
                return "one";
            case 2:
                return "two";
            default:
                return "many";
        }
    }

    /**
     * A try/catch, to exercise exception edges.
     *
     * @param s the candidate number text
     * @return the parsed value, or -1 on failure
     */
    public int parse(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
