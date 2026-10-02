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

/**
 * The studio's view layer: the individual panes of the three-view decompiler
 * workspace. Every class here is a pure JavaFX widget that renders whatever the
 * application layer hands it and raises callbacks for user intent; none of them
 * reference the kernel's engine, so the whole view layer can be restyled by CSS
 * or swapped without touching decompilation logic.
 *
 * @author Jerry Zhu (Zeek)
 */
package com.aetherdecompiler.gui.view;
