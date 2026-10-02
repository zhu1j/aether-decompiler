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
 * A semantic marker for an intermediate representation travelling through the
 * engine's event pipeline.
 *
 * <p>The kernel asserts the hard constraint that it never speaks Java syntax.
 * Its intermediates are therefore described only by abstract, language-neutral
 * kinds in {@link IRKind}, and an {@code IRObject} is simply a value that can
 * report which kind it currently is. The concrete class types live in the
 * kernel; plugins observe them through events without the API module depending
 * on the kernel.</p>
 *
 * <p>Story analogy: every parcel on the conveyor belt carries a plain label —
 * "raw metal", "machined part", "finished unit" — but the label never names a
 * particular brand of end product.</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public interface IRObject {

    /**
     * @return the language-neutral kind of this intermediate object
     */
    IRKind kind();
}
