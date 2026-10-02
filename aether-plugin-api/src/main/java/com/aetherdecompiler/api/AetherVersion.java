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
 * Immutable, project-wide identity constants.
 *
 * <p>This class is the single source of truth for the human identity of the
 * project. Every application layer (CLI banner, GUI about-box, IDE plugin)
 * reads its attribution from here so the author's signature is never
 * duplicated or allowed to drift.</p>
 *
 * <p>Story analogy: this is the maker's stamp pressed into every brick the
 * factory ships — the stamp is part of the product, not decoration.</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class AetherVersion {

    /** Semantic version of the engine. MAJOR is deliberately held stable. */
    public static final String VERSION = "0.1.0";

    /** Project name. */
    public static final String PROJECT = "aether-decompiler";

    /** The author's English name. */
    public static final String AUTHOR = "Jerry Zhu";

    /** The author's pen name / handle. */
    public static final String AUTHOR_PEN_NAME = "Zeek";

    /** Public contact address of the author. */
    public static final String AUTHOR_EMAIL = "zhujiejava1@gmail.com";

    /** The author's motto, printed by every application entry point. */
    public static final String MOTTO = "Run the Code, Run the World!";

    /** SPDX license identifier. */
    public static final String LICENSE = "Apache-2.0";

    private AetherVersion() {
        throw new AssertionError("No instances.");
    }

    /**
     * A one-line, ready-to-print attribution string.
     *
     * @return e.g. {@code "aether-decompiler 0.1.0 — by Jerry Zhu (Zeek)"}
     */
    public static String attribution() {
        return PROJECT + " " + VERSION + " \u2014 by " + AUTHOR + " (" + AUTHOR_PEN_NAME + ")";
    }

    /**
     * The full, multi-line banner used by the CLI on startup.
     *
     * @return a banner string embedding the project identity and motto
     */
    public static String banner() {
        return ""
                + "  ___   _____ _  _ _____ ____\n"
                + " / _ \\ | ____| || |_   _|  _ \\\n"
                + "| |_| ||  _| | __ | | | | |_) |\n"
                + "|  _  || |___| |_| | | | |  _ <\n"
                + "|_| |_||_____|\\___/  |_| |_| \\_\\\n"
                + "\n"
                + "  " + attribution() + "\n"
                + "  \"" + MOTTO + "\"\n";
    }
}
