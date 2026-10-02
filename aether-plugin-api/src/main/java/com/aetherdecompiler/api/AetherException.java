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
 * The single unchecked root exception of the engine.
 *
 * <p>Contract: the kernel never lets a third-party exception type escape its
 * boundary. Every {@code org.objectweb.asm.*} failure and every internal
 * failure is wrapped into an {@code AetherException} carrying an
 * {@link ErrorCode}. This is the concrete mechanism that keeps ASM from
 * leaking into the public API — callers only ever see aether types.</p>
 *
 * <p>Story analogy: the kernel is a sealed reactor hall. Whatever fails inside
 * — a foreign tool, a bad reading — is reported at the door under the
 * reactor's own label. Nothing foreign walks out.</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class AetherException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ErrorCode errorCode;

    /**
     * @param errorCode the stable machine-readable code (never {@code null})
     * @param message   a human-readable description
     */
    public AetherException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode == null ? ErrorCode.UNKNOWN : errorCode;
    }

    /**
     * @param errorCode the stable machine-readable code (never {@code null})
     * @param message   a human-readable description
     * @param cause     the wrapped cause (typically an ASM exception)
     */
    public AetherException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode == null ? ErrorCode.UNKNOWN : errorCode;
    }

    /**
     * @return the stable machine-readable error code
     */
    public ErrorCode errorCode() {
        return errorCode;
    }

    /**
     * Wrap any throwable, preserving an existing {@code AetherException} as-is.
     *
     * @param errorCode the code to use when wrapping
     * @param message   a human-readable description
     * @param cause     the throwable to wrap
     * @return an {@code AetherException} representing {@code cause}
     */
    public static AetherException wrap(ErrorCode errorCode, String message, Throwable cause) {
        if (cause instanceof AetherException aether) {
            return aether;
        }
        return new AetherException(errorCode, message, cause);
    }

    @Override
    public String getMessage() {
        return "[" + errorCode.tag() + "] " + super.getMessage();
    }

    /**
     * @return the bare message without the error-code prefix
     */
    public String rawMessage() {
        return super.getMessage();
    }
}
