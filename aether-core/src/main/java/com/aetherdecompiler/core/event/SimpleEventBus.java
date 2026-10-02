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
package com.aetherdecompiler.core.event;

import com.aetherdecompiler.api.AetherEvent;
import com.aetherdecompiler.api.AetherListener;
import com.aetherdecompiler.api.EventBus;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A minimal, dependency-free {@link EventBus} implementation.
 *
 * <p>Uses a copy-on-write list so subscription is safe while the pipeline may
 * be publishing; iteration never blocks and never throws
 * {@code ConcurrentModificationException}. A listener that throws is isolated
 * so one bad observer cannot stall the engine.</p>
 *
 * <p>Story analogy: a notice board with a rule that says "pin your notice and
 * walk away; a torn notice is ignored, not allowed to bring down the wall."</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class SimpleEventBus implements EventBus {

    private final List<AetherListener<?>> listeners = new CopyOnWriteArrayList<>();

    @Override
    public <E extends AetherEvent> void subscribe(AetherListener<E> listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    @Override
    public void publish(AetherEvent event) {
        for (AetherListener<?> listener : listeners) {
            try {
                publishTo(listener, event);
            } catch (RuntimeException ex) {
                // Observation must never break the pipeline.
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends AetherEvent> void publishTo(AetherListener<E> listener, AetherEvent event) {
        listener.onEvent((E) event);
    }

    /** @return the number of subscribed listeners */
    public int listenerCount() {
        return listeners.size();
    }
}
