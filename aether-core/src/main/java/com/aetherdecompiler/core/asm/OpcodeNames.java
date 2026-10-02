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

/**
 * The kernel's OWN opcode mnemonic table.
 *
 * <p>This exists for a deliberate reason: the kernel must not depend on a
 * library's naming utility, and the model must be able to render a human label
 * for an opcode without importing anything from ASM. Owning the table keeps the
 * model layer self-contained and the ASM seam as thin as possible.</p>
 *
 * <p>Story analogy: the factory writes its own parts legend instead of
 * photocopying the supplier's catalogue page.</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class OpcodeNames {

    private static final String[] NAMES = new String[202];

    static {
        NAMES[0] = "nop";
        NAMES[1] = "aconst_null";
        NAMES[2] = "iconst_m1";
        NAMES[3] = "iconst_0";
        NAMES[4] = "iconst_1";
        NAMES[5] = "iconst_2";
        NAMES[6] = "iconst_3";
        NAMES[7] = "iconst_4";
        NAMES[8] = "iconst_5";
        NAMES[9] = "lconst_0";
        NAMES[10] = "lconst_1";
        NAMES[11] = "fconst_0";
        NAMES[12] = "fconst_1";
        NAMES[13] = "fconst_2";
        NAMES[14] = "dconst_0";
        NAMES[15] = "dconst_1";
        NAMES[16] = "bipush";
        NAMES[17] = "sipush";
        NAMES[18] = "ldc";
        NAMES[19] = "ldc_w";
        NAMES[20] = "ldc2_w";
        NAMES[21] = "iload";
        NAMES[22] = "lload";
        NAMES[23] = "fload";
        NAMES[24] = "dload";
        NAMES[25] = "aload";
        NAMES[26] = "iload_0";
        NAMES[27] = "iload_1";
        NAMES[28] = "iload_2";
        NAMES[29] = "iload_3";
        NAMES[30] = "lload_0";
        NAMES[31] = "lload_1";
        NAMES[32] = "lload_2";
        NAMES[33] = "lload_3";
        NAMES[34] = "fload_0";
        NAMES[35] = "fload_1";
        NAMES[36] = "fload_2";
        NAMES[37] = "fload_3";
        NAMES[38] = "dload_0";
        NAMES[39] = "dload_1";
        NAMES[40] = "dload_2";
        NAMES[41] = "dload_3";
        NAMES[42] = "aload_0";
        NAMES[43] = "aload_1";
        NAMES[44] = "aload_2";
        NAMES[45] = "aload_3";
        NAMES[46] = "iaload";
        NAMES[47] = "laload";
        NAMES[48] = "faload";
        NAMES[49] = "daload";
        NAMES[50] = "aaload";
        NAMES[51] = "baload";
        NAMES[52] = "caload";
        NAMES[53] = "saload";
        NAMES[54] = "istore";
        NAMES[55] = "lstore";
        NAMES[56] = "fstore";
        NAMES[57] = "dstore";
        NAMES[58] = "astore";
        NAMES[59] = "istore_0";
        NAMES[60] = "istore_1";
        NAMES[61] = "istore_2";
        NAMES[62] = "istore_3";
        NAMES[63] = "lstore_0";
        NAMES[64] = "lstore_1";
        NAMES[65] = "lstore_2";
        NAMES[66] = "lstore_3";
        NAMES[67] = "fstore_0";
        NAMES[68] = "fstore_1";
        NAMES[69] = "fstore_2";
        NAMES[70] = "fstore_3";
        NAMES[71] = "dstore_0";
        NAMES[72] = "dstore_1";
        NAMES[73] = "dstore_2";
        NAMES[74] = "dstore_3";
        NAMES[75] = "astore_0";
        NAMES[76] = "astore_1";
        NAMES[77] = "astore_2";
        NAMES[78] = "astore_3";
        NAMES[79] = "iastore";
        NAMES[80] = "lastore";
        NAMES[81] = "fastore";
        NAMES[82] = "dastore";
        NAMES[83] = "aastore";
        NAMES[84] = "bastore";
        NAMES[85] = "castore";
        NAMES[86] = "sastore";
        NAMES[87] = "pop";
        NAMES[88] = "pop2";
        NAMES[89] = "dup";
        NAMES[90] = "dup_x1";
        NAMES[91] = "dup_x2";
        NAMES[92] = "dup2";
        NAMES[93] = "dup2_x1";
        NAMES[94] = "dup2_x2";
        NAMES[95] = "swap";
        NAMES[96] = "iadd";
        NAMES[97] = "ladd";
        NAMES[98] = "fadd";
        NAMES[99] = "dadd";
        NAMES[100] = "isub";
        NAMES[101] = "lsub";
        NAMES[102] = "fsub";
        NAMES[103] = "dsub";
        NAMES[104] = "imul";
        NAMES[105] = "lmul";
        NAMES[106] = "fmul";
        NAMES[107] = "dmul";
        NAMES[108] = "idiv";
        NAMES[109] = "ldiv";
        NAMES[110] = "fdiv";
        NAMES[111] = "ddiv";
        NAMES[112] = "irem";
        NAMES[113] = "lrem";
        NAMES[114] = "frem";
        NAMES[115] = "drem";
        NAMES[116] = "ineg";
        NAMES[117] = "lneg";
        NAMES[118] = "fneg";
        NAMES[119] = "dneg";
        NAMES[120] = "ishl";
        NAMES[121] = "lshl";
        NAMES[122] = "ishr";
        NAMES[123] = "lshr";
        NAMES[124] = "iushr";
        NAMES[125] = "lushr";
        NAMES[126] = "iand";
        NAMES[127] = "land";
        NAMES[128] = "ior";
        NAMES[129] = "lor";
        NAMES[130] = "ixor";
        NAMES[131] = "lxor";
        NAMES[132] = "iinc";
        NAMES[133] = "i2l";
        NAMES[134] = "i2f";
        NAMES[135] = "i2d";
        NAMES[136] = "l2i";
        NAMES[137] = "l2f";
        NAMES[138] = "l2d";
        NAMES[139] = "f2i";
        NAMES[140] = "f2l";
        NAMES[141] = "f2d";
        NAMES[142] = "d2i";
        NAMES[143] = "d2l";
        NAMES[144] = "d2f";
        NAMES[145] = "i2b";
        NAMES[146] = "i2c";
        NAMES[147] = "i2s";
        NAMES[148] = "lcmp";
        NAMES[149] = "fcmpl";
        NAMES[150] = "fcmpg";
        NAMES[151] = "dcmpl";
        NAMES[152] = "dcmpg";
        NAMES[153] = "ifeq";
        NAMES[154] = "ifne";
        NAMES[155] = "iflt";
        NAMES[156] = "ifge";
        NAMES[157] = "ifgt";
        NAMES[158] = "ifle";
        NAMES[159] = "if_icmpeq";
        NAMES[160] = "if_icmpne";
        NAMES[161] = "if_icmplt";
        NAMES[162] = "if_icmpge";
        NAMES[163] = "if_icmpgt";
        NAMES[164] = "if_icmple";
        NAMES[165] = "if_acmpeq";
        NAMES[166] = "if_acmpne";
        NAMES[167] = "goto";
        NAMES[168] = "jsr";
        NAMES[169] = "ret";
        NAMES[170] = "tableswitch";
        NAMES[171] = "lookupswitch";
        NAMES[172] = "ireturn";
        NAMES[173] = "lreturn";
        NAMES[174] = "freturn";
        NAMES[175] = "dreturn";
        NAMES[176] = "areturn";
        NAMES[177] = "return";
        NAMES[178] = "getstatic";
        NAMES[179] = "putstatic";
        NAMES[180] = "getfield";
        NAMES[181] = "putfield";
        NAMES[182] = "invokevirtual";
        NAMES[183] = "invokespecial";
        NAMES[184] = "invokestatic";
        NAMES[185] = "invokeinterface";
        NAMES[186] = "invokedynamic";
        NAMES[187] = "new";
        NAMES[188] = "newarray";
        NAMES[189] = "anewarray";
        NAMES[190] = "arraylength";
        NAMES[191] = "athrow";
        NAMES[192] = "checkcast";
        NAMES[193] = "instanceof";
        NAMES[194] = "monitorenter";
        NAMES[195] = "monitorexit";
        NAMES[196] = "wide";
        NAMES[197] = "multianewarray";
        NAMES[198] = "ifnull";
        NAMES[199] = "ifnonnull";
        NAMES[200] = "goto_w";
        NAMES[201] = "jsr_w";
    }

    private OpcodeNames() {
        throw new AssertionError("No instances.");
    }

    /**
     * @param opcode the JVM opcode number
     * @return the mnemonic, or {@code "opcode_<n>"} if unknown
     */
    public static String name(int opcode) {
        if (opcode >= 0 && opcode < NAMES.length && NAMES[opcode] != null) {
            return NAMES[opcode];
        }
        return "opcode_" + opcode;
    }
}
