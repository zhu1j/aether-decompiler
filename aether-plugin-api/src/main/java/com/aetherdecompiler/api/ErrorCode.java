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
 * 引擎发出的稳定、机器可读的错误码。
 *
 * <p>错误码按两位数的族群前缀分组，便于调用方廉价地分支（例如
 * {@code AETHER-1xxx} = 输入/来源问题）。数字码是公开契约的一部分，
 * 绝不可为别的含义重用。</p>
 *
 * <p>故事类比：工厂控制面板上的故障灯。工厂从不用散文对操作员讲话 ——
 * 它点亮一盏特定的、有据可查的灯。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public enum ErrorCode {

    /** 无法定位或打开某个类文件 / jar。 */
    INPUT_NOT_FOUND(1001, "Input not found"),

    /** 交给引擎的字节不是合法的类文件。 */
    INPUT_MALFORMED(1002, "Malformed class file"),

    /** 类文件版本比本引擎的 ASM 后端所支持的更新。 */
    INPUT_UNSUPPORTED_VERSION(1003, "Unsupported class file version"),

    /** 引擎期望读取的某个被引用类缺失。 */
    INPUT_MISSING_REFERENCE(1004, "Missing class reference"),

    /** 无法为某个方法构建控制流图。 */
    CFG_CONSTRUCTION_FAILED(2001, "CFG construction failed"),

    /** 某个方法体引用了不存在基本块的字节码偏移。 */
    CFG_DANGLING_JUMP_TARGET(2002, "Dangling jump target"),

    /** 某个方法在 SSA 构建期间失败。 */
    SSA_CONSTRUCTION_FAILED(3001, "SSA construction failed"),

    /** 类型推断对某个栈槽达到不一致状态。 */
    TYPE_INFERENCE_FAILED(3002, "Type inference failed"),

    /** 某个插件拒绝或未能完成某个请求。 */
    PLUGIN_FAILURE(4001, "Plugin failure"),

    /** 某个插件声明的元数据违反了契约。 */
    PLUGIN_INVALID_METADATA(4002, "Invalid plugin metadata"),

    /** 引擎的某个内部不变量被违反。 */
    INTERNAL_INVARIANT(9001, "Internal invariant violated"),

    /** 对尚无专用错误码的错误的兜底。 */
    UNKNOWN(9999, "Unknown error");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * @return 稳定的数字码，是公开契约的一部分
     */
    public int code() {
        return code;
    }

    /**
     * @return 该错误码的简短人类可读标签
     */
    public String message() {
        return message;
    }

    /**
     * @return 用于日志与 CLI 输出的、零填充带前缀形式，
     *         例如 {@code "AETHER-1002"}
     */
    public String tag() {
        return "AETHER-" + code;
    }
}
