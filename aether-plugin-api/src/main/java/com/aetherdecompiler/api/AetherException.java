/*
 * aether-decompiler —— 一个独立、可复用的 JVM 反编译引擎。
 * Copyright 2026 Jerry Zhu (Zeek) <zhujiejava1@gmail.com>
 *
 * 依据 Apache License, Version 2.0（下称“本许可证”）授权；
 * 除非遵守本许可证，否则你不得使用本文件。
 * 你可以在以下地址获取本许可证副本：
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * 除非适用法律要求或书面同意，依据本许可证分发的软件
 * 均按“原样（AS IS）”提供，不附带任何明示或默示的担保，
 * 包括但不限于对适销性、特定用途适用性的担保。
 * 关于本许可证下具体权限与限制的表述，请参见本许可证。
 *
 * @author Jerry Zhu (Zeek)
 * “Run the Code, Run the World!”
 */
package com.aetherdecompiler.api;

/**
 * 引擎唯一的不受检根异常。
 *
 * <p>契约：内核绝不让第三方异常类型逃逸其边界。每一次
 * {@code org.objectweb.asm.*} 失败以及每一次内部失败，都被包装进一个携带
 * {@link ErrorCode} 的 {@code AetherException}。这就是防止 ASM 泄漏进公开
 * API 的具体机制 —— 调用方只会看到 aether 类型。</p>
 *
 * <p>故事类比：内核是一座密封的反应堆大厅。里面无论出什么故障 —— 外来的
 * 工具、异常的读数 —— 都在门口以反应堆自己的标签上报。没有任何异物
 * 走出去。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class AetherException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ErrorCode errorCode;

    /**
     * @param errorCode 稳定的机器可读错误码（永不为 {@code null}）
     * @param message   人类可读的描述
     */
    public AetherException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode == null ? ErrorCode.UNKNOWN : errorCode;
    }

    /**
     * @param errorCode 稳定的机器可读错误码（永不为 {@code null}）
     * @param message   人类可读的描述
     * @param cause     被包装的原因（通常是 ASM 异常）
     */
    public AetherException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode == null ? ErrorCode.UNKNOWN : errorCode;
    }

    /**
     * @return 稳定的机器可读错误码
     */
    public ErrorCode errorCode() {
        return errorCode;
    }

    /**
     * 包装任意可抛出对象，并把已有的 {@code AetherException} 原样保留。
     *
     * @param errorCode 包装时使用的错误码
     * @param message   人类可读的描述
     * @param cause     要包装的可抛出对象
     * @return 代表 {@code cause} 的 {@code AetherException}
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
     * @return 去除错误码前缀后的裸消息
     */
    public String rawMessage() {
        return super.getMessage();
    }
}
