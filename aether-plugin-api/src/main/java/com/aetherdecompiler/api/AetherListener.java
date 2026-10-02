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
 * A subscriber to pipeline events.
 *
 * <p>Hard constraint #8 mandates that every intermediate step (CFG, SSA, AST)
 * be observable. This interface is the single subscription surface: the kernel
 * emits typed {@link AetherEvent}s, and observation plugins (or the GUI's
 * progress bar, or a future AI-assisted reverse-engineering tool) consume them
 * without the kernel knowing who is listening.</p>
 *
 * <p>Story analogy: the factory floor's public-address system. Stations
 * announce "stage complete"; any number of listeners — supervisors, recorders,
 * dashboards — may tune in. The stations never address a specific listener.</p>
 *
 * <p>Implementations MUST be fast and MUST NOT throw: a slow or failing
 * listener would stall or break the pipeline.</p>
 *
 * @author Jerry Zhu (Zeek)
 * @param <E> the event type this listener accepts
 */
@FunctionalInterface
public interface AetherListener<E extends AetherEvent> {

    /**
     * Receive one event.
     *
     * @param event the event; never {@code null}
     */
    void onEvent(E event);
}
