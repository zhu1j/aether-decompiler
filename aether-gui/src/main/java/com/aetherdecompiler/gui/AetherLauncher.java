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
package com.aetherdecompiler.gui;

import com.aetherdecompiler.api.AetherVersion;
import javafx.application.Application;

/**
 * The GUI entry point.
 *
 * <p>Kept deliberately trivial and separate from {@link AetherGuiApp}: a
 * launcher that does not itself extend {@link javafx.application.Application}
 * lets the fat jar start without the JavaFX modules being named on the command
 * line, which keeps the packaged artifact double-clickable.</p>
 *
 * <p>Story analogy: the front door of the studio. It only turns the handle and
 * steps aside; the people inside do the work.</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class AetherLauncher {

    private AetherLauncher() {
    }

    /**
     * @param args forwarded to the JavaFX application
     */
    public static void main(String[] args) {
        System.out.println(AetherVersion.attribution());
        System.out.println("\"" + AetherVersion.MOTTO + "\"");
        Application.launch(AetherGuiApp.class, args);
    }
}
