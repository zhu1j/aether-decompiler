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
 * <strong>The single ASM isolation seam.</strong>
 *
 * <p>This is the ONLY class in the entire kernel that imports
 * {@code org.objectweb.asm.*}. Its job is to translate ASM's mutable,
 * library-specific tree into aether's immutable, library-agnostic model — and
 * to convert every ASM failure into an {@link AetherException}. No ASM type ever
 * escapes this class.</p>
 *
 * <p>Design consequence: to swap ASM for another bytecode library, exactly one
 * file changes. This is what "thin wrapper" means concretely.</p>
 *
 * <p>Story analogy: the customs booth at a border. Cargo is repacked into local
 * crates here; the foreign crate design is never seen inside the country.</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class AsmClassParser {

    /**
     * Parse raw class bytes into the immutable model.
     *
     * @param bytes the class file bytes
     * @return the immutable class model
     * @throws AetherException on any malformed or unsupported input
     */
    public ClassModel parse(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new AetherException(ErrorCode.INPUT_MALFORMED, "Empty class bytes");
        }
        try {
            ClassReader reader = new ClassReader(bytes);
            ClassNode node = new ClassNode();
            // EXPAND_FRAMES normalises stack-map frames to a single form.
            reader.accept(node, ClassReader.EXPAND_FRAMES);
            return toModel(node);
        } catch (IllegalArgumentException ex) {
            // ASM reports an unsupported class-file version this way.
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

        // Pass 1: assign an ordinal index to every "real" instruction and
        // record where each label points (a label points to the next real
        // instruction).
        Map<LabelNode, Integer> labelIndex = new HashMap<>();
        int count = 0;
        for (AbstractInsnNode n = list.getFirst(); n != null; n = n.getNext()) {
            if (n instanceof LabelNode label) {
                labelIndex.put(label, count);
            } else if (n instanceof LineNumberNode || n instanceof FrameNode) {
                // Pseudo-instructions: no bytecode, no index.
            } else {
                count++;
            }
        }

        // Pass 2: build immutable Insn models.
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
     * Confirm the parser's ASM backend supports a given class-file major
     * version. Exposed so callers can pre-flight a jar before parsing.
     *
     * @param majorVersion the class-file major version
     * @return {@code true} if supported by the bundled ASM
     */
    public boolean supportsMajorVersion(int majorVersion) {
        return majorVersion <= Opcodes.V_PREVIEW;
    }
}
