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
package com.aetherdecompiler.core.ssa;

import com.aetherdecompiler.core.cfg.ControlFlowGraph;
import com.aetherdecompiler.core.cfg.DominatorTree;
import com.aetherdecompiler.core.model.BasicBlock;
import com.aetherdecompiler.core.model.ClassModel;
import com.aetherdecompiler.core.model.Insn;
import com.aetherdecompiler.core.model.MethodModel;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 操作数栈提升（<strong>Stack Lifting</strong>）。
 *
 * <p>字节码的表达是“栈机器”式的：中间结果推入一个隐式的操作数栈，随取随用，栈本身
 * 没有名字。真实的 Java 源码却是“寄存器/变量”式的。Stack Lifting 要做的，就是把
 * 隐式的栈单元翻译成<strong>符号化的只赋值一次的值</strong>，从而为后续的表达式
 * 重建、临时变量生成与栈消解铺路。</p>
 *
 * <p>本实现分两步，全程零依赖、确定且可重入：</p>
 * <ol>
 *   <li><b>栈深分析</b>——以指令为单位计算进入/离开时的栈深（含 long/double 占两格），
 *       沿普通控制流做工作队列传播；在汇合点若入栈深不一致，则标记为“近似”。</li>
 *   <li><b>符号化栈单元 + 栈 phi</b>——按块序重放：块入口的每个栈位成为一个栈单元；
 *       单一前驱则直接沿用前驱出口的单元，多前驱则新建 {@link StackPhi} 合并各路来源；
 *       块内每条会压栈的指令为其栈顶结果分配一个新的唯一单元 id。</li>
 * </ol>
 *
 * <p>故事类比：把一条流水线上无名的周转筐，全部贴上“来自哪道工序”的编号，并在
 * 合流处给筐登记“按来源择号”的对照牌——于是每个筐都能追溯到它的出处。</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class StackLifter {

    /** 一个待处理块的栈形态状态，用于工作队列传播。 */
    private static final class BlockState {
        int entryDepth = -1;
    }

    /**
     * 对单个方法做操作数栈提升。
     *
     * @param owner  所属类（用于诊断）
     * @param method 方法模型
     * @param cfg    控制流图
     * @param dom    支配树（保留参数，便于未来按支配序优化）
     * @return 不可变的提升结果
     */
    public StackLiftResult lift(ClassModel owner, MethodModel method,
                                ControlFlowGraph cfg, DominatorTree dom) {
        List<Insn> insns = cfg.instructions();
        int blockCount = cfg.blockCount();
        if (blockCount == 0 || insns.isEmpty()) {
            return StackLiftResult.empty(owner.name(), method.id());
        }

        // ---- 第 1 步：栈深分析（普通控制流） ----
        Map<Integer, Integer> depthBefore = new HashMap<>();
        Map<Integer, Integer> depthAfter = new HashMap<>();
        Map<Integer, Integer> entryDepth = new HashMap<>();
        boolean[] approximate = {false};
        StringBuilder reason = new StringBuilder();

        List<BasicBlock> blocks = new ArrayList<>(cfg.blocks());
        BasicBlock entry = cfg.block(0);
        if (entry != null) {
            entryDepth.put(0, 0);
        }

        Deque<Integer> work = new ArrayDeque<>();
        Set<Integer> queued = new HashSet<>();
        if (entry != null) {
            work.add(0);
            queued.add(0);
        }
        int guard = 0;
        int guardLimit = blockCount * 8 + 64;
        while (!work.isEmpty() && guard++ < guardLimit) {
            int bid = work.poll();
            queued.remove(bid);
            BasicBlock b = cfg.block(bid);
            if (b == null) {
                continue;
            }
            int d = entryDepth.getOrDefault(bid, -1);
            if (d < 0) {
                continue;
            }
            int cur = d;
            for (int i = b.firstInsn(); i <= b.lastInsn() && i < insns.size(); i++) {
                if (i < 0) {
                    continue;
                }
                Insn in = insns.get(i);
                depthBefore.put(i, cur);
                int[] delta = stackDelta(in);
                if (delta == null) {
                    approximate[0] = true;
                    if (reason.length() == 0) {
                        reason.append("指令 #").append(i).append(" ").append(in.mnemonic())
                                .append(" 的栈效应未知");
                    }
                    delta = new int[]{0, 0};
                }
                cur = Math.max(0, cur - delta[0]) + delta[1];
                depthAfter.put(i, cur);
            }
            // 传播到普通后继。
            for (int s : b.successors()) {
                if (s < 0 || s >= blockCount) {
                    continue;
                }
                Integer known = entryDepth.get(s);
                if (known == null) {
                    entryDepth.put(s, cur);
                    if (queued.add(s)) {
                        work.add(s);
                    }
                } else if (known != cur) {
                    // 入栈深不一致：取较大者继续，并标记近似。
                    approximate[0] = true;
                    if (reason.length() == 0) {
                        reason.append("块 B").append(s).append(" 入栈深不一致（")
                                .append(known).append(" vs ").append(cur).append("）");
                    }
                    int merged = Math.max(known, cur);
                    if (merged != known) {
                        entryDepth.put(s, merged);
                        if (queued.add(s)) {
                            work.add(s);
                        }
                    }
                }
            }
            // 异常处理器：以“抛出对象”占 1 格作为入栈深（无普通入度时）。
            for (int h : b.exceptionSuccessors()) {
                if (h < 0 || h >= blockCount) {
                    continue;
                }
                if (!entryDepth.containsKey(h)) {
                    entryDepth.put(h, 1);
                    if (queued.add(h)) {
                        work.add(h);
                    }
                }
            }
        }

        // ---- 第 2 步：符号化栈单元 + 栈 phi ----
        int[] nextCell = {0};
        Map<Integer, List<Integer>> blockEntryCells = new LinkedHashMap<>();
        Map<Integer, List<Integer>> blockExitCells = new HashMap<>();
        Map<Integer, Integer> insnProducedCell = new HashMap<>();
        List<StackPhi> phis = new ArrayList<>();
        List<List<Integer>> preds = predecessors(cfg, blockCount);

        // 稳定遍历顺序：按块 id 升序，保证确定输出。
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < blockCount; i++) {
            if (entryDepth.containsKey(i)) {
                order.add(i);
            }
        }
        order.sort(Integer::compareTo);

        for (int bid : order) {
            BasicBlock b = cfg.block(bid);
            if (b == null) {
                continue;
            }
            int d = entryDepth.getOrDefault(bid, 0);
            List<Integer> cur = new ArrayList<>();
            List<Integer> predList = preds.get(bid);
            if (bid == 0 || predList.isEmpty()) {
                // 入口块：栈从空开始（深度按分析值补齐为“未定义单元”）。
                for (int k = 0; k < d; k++) {
                    cur.add(undefinedCell());
                }
            } else if (predList.size() == 1 && blockExitCells.containsKey(predList.get(0))) {
                cur.addAll(padTo(blockExitCells.get(predList.get(0)), d));
            } else {
                // 多前驱汇合：逐栈位做 phi。
                for (int k = 0; k < d; k++) {
                    List<Integer> operands = new ArrayList<>();
                    boolean needPhi = false;
                    int firstOperand = -1;
                    for (int p : predList) {
                        List<Integer> pe = blockExitCells.get(p);
                        int cell = (pe != null && k < pe.size()) ? pe.get(k) : undefinedCell();
                        operands.add(cell);
                        if (firstOperand < 0) {
                            firstOperand = cell;
                        } else if (cell != firstOperand) {
                            needPhi = true;
                        }
                    }
                    if (needPhi) {
                        int id = nextCell[0]++;
                        phis.add(new StackPhi(id, bid, k, operands));
                        cur.add(id);
                    } else {
                        cur.add(firstOperand < 0 ? undefinedCell() : firstOperand);
                    }
                }
            }
            blockEntryCells.put(bid, new ArrayList<>(cur));

            for (int i = b.firstInsn(); i <= b.lastInsn() && i < insns.size(); i++) {
                if (i < 0) {
                    continue;
                }
                Insn in = insns.get(i);
                int[] delta = stackDelta(in);
                if (delta == null) {
                    delta = new int[]{0, 0};
                }
                for (int p = 0; p < delta[0]; p++) {
                    if (!cur.isEmpty()) {
                        cur.remove(cur.size() - 1);
                    }
                }
                int produced = -1;
                for (int q = 0; q < delta[1]; q++) {
                    int id = nextCell[0]++;
                    int pos = cur.size();
                    cur.add(id);
                    produced = id;
                    // 记录栈位（供未来按位追踪）；此处仅保留栈顶。
                    if (q == delta[1] - 1) {
                        insnProducedCell.put(i, id);
                    }
                    // 每个单元都登记一次“自底向上位置”，用负值域避开真实 id。
                    posOfCell.put(id, pos);
                }
                if (produced < 0) {
                    insnProducedCell.put(i, -1);
                }
            }
            blockExitCells.put(bid, new ArrayList<>(cur));
        }

        // 计算最大深度。
        int maxDepth = 0;
        for (Map.Entry<Integer, Integer> e : entryDepth.entrySet()) {
            BasicBlock b = cfg.block(e.getKey());
            if (b == null) {
                continue;
            }
            int d = e.getValue();
            for (int i = b.firstInsn(); i <= b.lastInsn(); i++) {
                Integer af = depthAfter.get(i);
                if (af != null) {
                    d = af;
                }
            }
            maxDepth = Math.max(maxDepth, Math.max(e.getValue(), d));
        }

        return new StackLiftResult(owner.name(), method.id(), maxDepth, nextCell[0],
                phis, depthBefore, blockEntryCells, insnProducedCell,
                approximate[0], reason.toString());
    }

    /** 栈单元的“自底向上位置”登记（仅用于诊断，暂不外露）。 */
    private final Map<Integer, Integer> posOfCell = new HashMap<>();

    /** 生成一个“未定义单元”的占位 id。 */
    private static int undefinedCell() {
        return -1;
    }

    /** 把某个前驱出口的栈单元补齐/截断到目标深度。 */
    private static List<Integer> padTo(List<Integer> cells, int depth) {
        List<Integer> out = new ArrayList<>(depth);
        for (int k = 0; k < depth; k++) {
            if (k < cells.size()) {
                out.add(cells.get(k));
            } else {
                out.add(undefinedCell());
            }
        }
        return out;
    }

    /** 计算每个块（可达）的普通前驱列表。 */
    private static List<List<Integer>> predecessors(ControlFlowGraph cfg, int blockCount) {
        List<List<Integer>> preds = new ArrayList<>(blockCount);
        for (int i = 0; i < blockCount; i++) {
            preds.add(new ArrayList<>());
        }
        for (BasicBlock b : cfg.blocks()) {
            for (int s : b.successors()) {
                if (s >= 0 && s < blockCount && !preds.get(s).contains(b.id())) {
                    preds.get(s).add(b.id());
                }
            }
        }
        return preds;
    }

    /**
     * 计算一条指令的栈效应，单位是“栈格”（long/double 占两格）。
     *
     * @param in 指令
     * @return {@code [pops, pushes]}；未知指令返回 {@code null}
     */
    private int[] stackDelta(Insn in) {
        int op = in.opcode();
        String operand = in.operand();
        switch (op) {
            case 0: return new int[]{0, 0}; // nop
            case 1: case 2: case 3: case 4: case 5: case 6: case 7: case 8:
                return new int[]{0, 1}; // aconst_null, iconst_*
            case 9: case 10: // lconst_*
                return new int[]{0, 2};
            case 11: case 12: case 13: // fconst_*
                return new int[]{0, 1};
            case 14: case 15: // dconst_*
                return new int[]{0, 2};
            case 16: case 17: // bipush, sipush
                return new int[]{0, 1};
            case 18: // ldc
                return new int[]{0, 1};
            case 19: // ldc_w
                return new int[]{0, 1};
            case 20: // ldc2_w
                return new int[]{0, 2};
            case 21: case 23: case 25: // iload/fload/aload
                return new int[]{0, 1};
            case 22: case 24: // lload/dload
                return new int[]{0, 2};
            case 26: case 27: case 28: case 29: // iload_*
            case 34: case 35: case 36: case 37: // fload_*
            case 42: case 43: case 44: case 45: // aload_*
                return new int[]{0, 1};
            case 30: case 31: case 32: case 33: // lload_*
            case 38: case 39: case 40: case 41: // dload_*
                return new int[]{0, 2};
            case 46: case 48: case 50: case 51: case 52: case 53: // iaload/faload/aaload/baload/caload/saload
                return new int[]{2, 1};
            case 47: case 49: // laload/daload
                return new int[]{2, 2};
            case 54: case 56: case 58: // istore/fstore/astore
                return new int[]{1, 0};
            case 55: case 57: // lstore/dstore
                return new int[]{2, 0};
            case 59: case 60: case 61: case 62: // istore_*
            case 67: case 68: case 69: case 70: // fstore_*
            case 75: case 76: case 77: case 78: // astore_*
                return new int[]{1, 0};
            case 63: case 64: case 65: case 66: // lstore_*
            case 71: case 72: case 73: case 74: // dstore_*
                return new int[]{2, 0};
            case 79: case 81: case 83: case 84: case 85: case 86: // iastore/fastore/aastore/bastore/castore/sastore
                return new int[]{3, 0};
            case 80: case 82: // lastore/dastore
                return new int[]{4, 0};
            case 87: return new int[]{1, 0}; // pop
            case 88: return new int[]{2, 0}; // pop2
            case 89: return new int[]{1, 2}; // dup
            case 90: return new int[]{2, 3}; // dup_x1
            case 91: return new int[]{3, 4}; // dup_x2
            case 92: return new int[]{2, 4}; // dup2
            case 93: return new int[]{3, 5}; // dup2_x1
            case 94: return new int[]{4, 6}; // dup2_x2
            case 95: return new int[]{2, 2}; // swap
            case 96: case 98: case 100: case 102: case 104: case 106:
            case 108: case 110: case 112: case 114: // i/f 二元算术
            case 120: case 122: case 124: // ishl/ishr/iushr
            case 126: case 128: case 130: // iand/ior/ixor
                return new int[]{2, 1};
            case 97: case 99: case 101: case 103: case 105: case 107:
            case 109: case 111: case 113: case 115: // l/d 二元算术
            case 127: case 129: case 131: // land/lor/lxor
                return new int[]{4, 2};
            case 121: case 123: case 125: // lshl/lshr/lushr
                return new int[]{3, 2};
            case 116: case 118: // ineg/fneg
                return new int[]{1, 1};
            case 117: case 119: // lneg/dneg
                return new int[]{2, 2};
            case 132: // i2l
                return new int[]{1, 2};
            case 133: case 144: case 145: case 146: // i2f/i2b/i2c/i2s
                return new int[]{1, 1};
            case 134: // i2d
                return new int[]{1, 2};
            case 135: case 136: case 143: // l2i/l2f/d2f
                return new int[]{2, 1};
            case 137: case 142: // l2d/d2l
                return new int[]{2, 2};
            case 138: // f2i
                return new int[]{1, 1};
            case 139: // f2l
                return new int[]{1, 2};
            case 140: // f2d
                return new int[]{1, 2};
            case 141: // d2i
                return new int[]{2, 1};
            case 147: case 150: case 151: // lcmp/dcmpl/dcmpg
                return new int[]{4, 1};
            case 148: case 149: // fcmpl/fcmpg
                return new int[]{2, 1};
            case 153: case 154: case 155: case 156: case 157: case 158: // ifeq..ifle
                return new int[]{1, 0};
            case 159: case 160: case 161: case 162: case 163: case 164: // if_icmp*
                return new int[]{2, 0};
            case 165: case 166: // if_acmp*
                return new int[]{2, 0};
            case 167: return new int[]{0, 0}; // goto
            case 168: return new int[]{0, 1}; // jsr
            case 169: return new int[]{0, 0}; // ret
            case 170: case 171: // tableswitch / lookupswitch
                return new int[]{1, 0};
            case 172: case 174: case 176: // ireturn/freturn/areturn
                return new int[]{1, 0};
            case 173: case 175: // lreturn/dreturn
                return new int[]{2, 0};
            case 177: return new int[]{0, 0}; // return
            case 178: // getstatic
                return new int[]{0, slotsOf(fieldType(operand))};
            case 179: // putstatic
                return new int[]{slotsOf(fieldType(operand)), 0};
            case 180: // getfield
                return new int[]{1, slotsOf(fieldType(operand))};
            case 181: // putfield
                return new int[]{1 + slotsOf(fieldType(operand)), 0};
            case 182: case 183: case 184: case 185: // invokevirtual/special/static/interface
                return invokeDelta(operand, op != 184);
            case 186: // invokedynamic
                return invokeDelta(operand, false);
            case 187: return new int[]{0, 1}; // new
            case 188: case 189: // newarray / anewarray
                return new int[]{1, 1};
            case 190: return new int[]{1, 1}; // arraylength
            case 191: return new int[]{1, 0}; // athrow
            case 192: case 193: // checkcast / instanceof
                return new int[]{1, 1};
            case 194: case 195: // monitorenter / monitorexit
                return new int[]{1, 0};
            case 197: // multianewarray
                return new int[]{dimsOf(operand), 1};
            case 196: // wide
                return new int[]{0, 0};
            default:
                return null;
        }
    }

    /** 由 invoke 指令的操作数（{@code owner.name desc}）计算栈效应。 */
    private int[] invokeDelta(String operand, boolean hasReceiver) {
        String desc = trailingDescriptor(operand);
        int args = argSlots(desc);
        int ret = returnSlots(desc);
        int pops = args + (hasReceiver ? 1 : 0);
        return new int[]{pops, ret};
    }

    /** 从字段操作数（{@code owner.name:desc}）中提取字段类型描述符。 */
    private static String fieldType(String operand) {
        if (operand == null) {
            return "I";
        }
        int colon = operand.lastIndexOf(':');
        return colon >= 0 ? operand.substring(colon + 1) : "I";
    }

    /** 从方法操作数末尾提取方法描述符（形如 {@code (...)X}）。 */
    private static String trailingDescriptor(String operand) {
        if (operand == null) {
            return "()V";
        }
        int p = operand.indexOf('(');
        return p >= 0 ? operand.substring(p) : "()V";
    }

    /** 计算方法描述符中参数占用的栈格总数。 */
    private static int argSlots(String methodDesc) {
        if (methodDesc == null || methodDesc.isEmpty() || methodDesc.charAt(0) != '(') {
            return 0;
        }
        int total = 0;
        int i = 1;
        while (i < methodDesc.length() && methodDesc.charAt(i) != ')') {
            char c = methodDesc.charAt(i);
            if (c == 'L') {
                int semi = methodDesc.indexOf(';', i);
                i = semi < 0 ? methodDesc.length() : semi + 1;
                total += 1;
            } else if (c == '[') {
                int j = i;
                while (j < methodDesc.length() && methodDesc.charAt(j) == '[') {
                    j++;
                }
                if (j < methodDesc.length() && methodDesc.charAt(j) == 'L') {
                    int semi = methodDesc.indexOf(';', j);
                    j = semi < 0 ? methodDesc.length() : semi + 1;
                } else {
                    j++;
                }
                i = j;
                total += 1;
            } else if (c == 'J' || c == 'D') {
                total += 2;
                i++;
            } else {
                total += 1;
                i++;
            }
        }
        return total;
    }

    /** 计算方法返回类型占用的栈格数。 */
    private static int returnSlots(String methodDesc) {
        if (methodDesc == null) {
            return 0;
        }
        int p = methodDesc.indexOf(')');
        if (p < 0 || p + 1 >= methodDesc.length()) {
            return 0;
        }
        return slotsOf(methodDesc.substring(p + 1));
    }

    /** 单个类型描述符占用的栈格数（long/double 为 2，其余为 1，void 为 0）。 */
    private static int slotsOf(String desc) {
        if (desc == null || desc.isEmpty()) {
            return 0;
        }
        char c = desc.charAt(0);
        if (c == 'V') {
            return 0;
        }
        if (c == 'J' || c == 'D') {
            return 2;
        }
        return 1;
    }

    /** 从 {@code «dims|desc»} 形式的操作数中解析维度数。 */
    private static int dimsOf(String operand) {
        if (operand == null) {
            return 1;
        }
        int bar = operand.indexOf('|');
        String dims = bar < 0 ? operand : operand.substring(0, bar);
        try {
            int d = Integer.parseInt(dims.trim());
            return d <= 0 ? 1 : d;
        } catch (RuntimeException ex) {
            return 1;
        }
    }
}
