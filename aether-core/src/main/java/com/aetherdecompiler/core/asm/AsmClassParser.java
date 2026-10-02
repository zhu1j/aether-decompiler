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
package com.aetherdecompiler.core.asm;

import com.aetherdecompiler.api.AetherException;
import com.aetherdecompiler.api.ErrorCode;
import com.aetherdecompiler.core.model.ClassModel;
import com.aetherdecompiler.core.model.FieldModel;
import com.aetherdecompiler.core.model.Insn;
import com.aetherdecompiler.core.model.MethodModel;
import com.aetherdecompiler.core.model.TryCatchEntry;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <strong>唯一的 ASM 隔离接缝。</strong>
 *
 * <p>这是整个内核中唯一导入 {@code org.objectweb.asm.*} 的类。它的职责是把 ASM 可变的、
 * 与库相关的树结构，翻译成 aether 不可变的、与库无关的模型 —— 并把每一次 ASM 失败
 * 转换为 {@link AetherException}。任何 ASM 类型都不会逃逸出这个类。</p>
 *
 * <p>设计后果：要把 ASM 换成别的字节码库，只需改动恰好一个文件。这就是“薄包装”
 * 的具体含义。</p>
 *
 * <p>故事类比：边境口岸的海关柜台。货物在这里被重新装入本地板条箱；
 * 国外板条箱的样式在国内永远看不到。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class AsmClassParser {

    /**
     * 把原始类字节解析为不可变模型。
     *
     * @param bytes 类文件的字节
     * @return 不可变的类模型
     * @throws AetherException 当输入格式错误或不受支持时抛出
     */
    public ClassModel parse(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new AetherException(ErrorCode.INPUT_MALFORMED, "Empty class bytes");
        }
        try {
            ClassReader reader = new ClassReader(bytes);
            ClassNode node = new ClassNode();
            // EXPAND_FRAMES 会把栈映射帧统一为单一形式。
            reader.accept(node, ClassReader.EXPAND_FRAMES);
            return toModel(node);
        } catch (IllegalArgumentException ex) {
            // ASM 就是这样报告不支持的类文件版本的。
            String msg = ex.getMessage() == null ? "" : ex.getMessage();
            if (msg.toLowerCase().contains("unsupported class file")) {
                throw new AetherException(ErrorCode.INPUT_UNSUPPORTED_VERSION, msg, ex);
            }
            throw new AetherException(ErrorCode.INPUT_MALFORMED, "Malformed class: " + msg, ex);
        } catch (RuntimeException ex) {
            throw AetherException.wrap(ErrorCode.INPUT_MALFORMED, "Failed to parse class bytes", ex);
        }
    }

    private ClassModel toModel(ClassNode cn) {
        List<FieldModel> fields = new ArrayList<>();
        for (FieldNode fn : cn.fields) {
            fields.add(new FieldModel(fn.access, fn.name, fn.desc, fn.signature));
        }

        List<MethodModel> methods = new ArrayList<>();
        for (MethodNode mn : cn.methods) {
            methods.add(toMethod(mn));
        }

        return new ClassModel(
                cn.name,
                cn.superName,
                cn.interfaces == null ? List.of() : cn.interfaces,
                cn.access,
                cn.version & 0xFFFF,
                (cn.version >>> 16) & 0xFFFF,
                cn.signature,
                fields,
                methods);
    }

    private MethodModel toMethod(MethodNode mn) {
        boolean hasCode = mn.instructions != null && mn.instructions.size() > 0;
        if (!hasCode) {
            return new MethodModel(mn.access, mn.name, mn.desc, mn.signature,
                    List.of(), List.of(), -1, -1);
        }

        InsnList list = mn.instructions;

        // 第一遍：为每条“真实”指令分配一个序号索引，并
        // 记录每个标签指向的位置（标签指向下一条真实
        // 指令）。
        Map<LabelNode, Integer> labelIndex = new HashMap<>();
        int count = 0;
        for (AbstractInsnNode n = list.getFirst(); n != null; n = n.getNext()) {
            if (n instanceof LabelNode label) {
                labelIndex.put(label, count);
            } else if (n instanceof LineNumberNode || n instanceof FrameNode) {
                // 伪指令：没有字节码，也没有索引。
            } else {
                count++;
            }
        }

        // 第二遍：构建不可变的 Insn 模型。
        List<Insn> insns = new ArrayList<>(count);
        int idx = 0;
        for (AbstractInsnNode n = list.getFirst(); n != null; n = n.getNext()) {
            if (n instanceof LabelNode || n instanceof LineNumberNode || n instanceof FrameNode) {
                continue;
            }
            insns.add(toInsn(idx, n, labelIndex));
            idx++;
        }

        List<TryCatchEntry> handlers = new ArrayList<>();
        if (mn.tryCatchBlocks != null) {
            for (TryCatchBlockNode tcb : mn.tryCatchBlocks) {
                int start = resolveLabel(labelIndex, tcb.start, count);
                int end = resolveLabel(labelIndex, tcb.end, count);
                int handler = resolveLabel(labelIndex, tcb.handler, count);
                handlers.add(new TryCatchEntry(start, end, handler, tcb.type));
            }
        }

        return new MethodModel(mn.access, mn.name, mn.desc, mn.signature,
                insns, handlers, mn.maxStack, mn.maxLocals);
    }

    private Insn toInsn(int index, AbstractInsnNode n, Map<LabelNode, Integer> labelIndex) {
        int opcode = n.getOpcode();
        String mnemonic = OpcodeNames.name(opcode);
        String operand = "";
        int[] targets = new int[0];

        if (n instanceof JumpInsnNode jump) {
            targets = new int[]{resolveLabel(labelIndex, jump.label, -1)};
            operand = "-> " + targets[0];
        } else if (n instanceof TableSwitchInsnNode ts) {
            int nTargets = ts.labels.size() + 1;
            targets = new int[nTargets];
            targets[0] = resolveLabel(labelIndex, ts.dflt, -1);
            for (int i = 0; i < ts.labels.size(); i++) {
                targets[i + 1] = resolveLabel(labelIndex, ts.labels.get(i), -1);
            }
            operand = "[" + ts.min + ".." + ts.max + "] default->" + targets[0];
        } else if (n instanceof LookupSwitchInsnNode ls) {
            int nTargets = ls.labels.size() + 1;
            targets = new int[nTargets];
            targets[0] = resolveLabel(labelIndex, ls.dflt, -1);
            for (int i = 0; i < ls.labels.size(); i++) {
                targets[i + 1] = resolveLabel(labelIndex, ls.labels.get(i), -1);
            }
            operand = "keys=" + ls.keys + " default->" + targets[0];
        } else if (n instanceof VarInsnNode v) {
            operand = "var " + v.var;
        } else if (n instanceof IincInsnNode ii) {
            operand = "var " + ii.var + " by " + ii.incr;
        } else if (n instanceof IntInsnNode in) {
            operand = String.valueOf(in.operand);
        } else if (n instanceof LdcInsnNode ldc) {
            operand = String.valueOf(ldc.cst);
        } else if (n instanceof TypeInsnNode t) {
            operand = t.desc;
        } else if (n instanceof MethodInsnNode m) {
            operand = m.owner + "." + m.name + m.desc;
        }

        return new Insn(index, opcode, mnemonic, operand, targets);
    }

    private int resolveLabel(Map<LabelNode, Integer> labelIndex, LabelNode label, int fallback) {
        if (label == null) {
            return fallback;
        }
        return labelIndex.getOrDefault(label, fallback);
    }

    /**
     * 确认解析器的 ASM 后端是否支持给定的类文件主版本号。
     * 对外暴露，便于调用方在解析前预检某个 jar。
     *
     * @param majorVersion 类文件主版本号
     * @return 若内置 ASM 支持则返回 {@code true}
     */
    public boolean supportsMajorVersion(int majorVersion) {
        return majorVersion <= Opcodes.V_PREVIEW;
    }
}
