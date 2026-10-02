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
 * A bus for publishing engine events to any number of listeners.
 *
 * <p>The kernel publishes to a bus; it never references a listener directly.
 * This inverts the dependency and keeps observability a pure add-on.</p>
 *
 * <p>Story analogy: the PA microphone. The station speaks into the microphone
 * and does not know or care how many speakers are wired to it.</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public interface EventBus {

    /**
     * Subscribe a listener to all events.
     *
     * @param listener the listener to add
     * @param <E>      the event type accepted by the listener
     */
    <E extends AetherEvent> void subscribe(AetherListener<E> listener);

    /**
     * Publish an event to every subscriber.
     *
     * @param event the event to publish
     */
    void publish(AetherEvent event);
}
