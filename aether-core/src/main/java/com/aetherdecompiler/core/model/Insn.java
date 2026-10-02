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
package com.aetherdecompiler.core.model;

import com.aetherdecompiler.api.IRKind;
import com.aetherdecompiler.api.IRObject;

import java.util.List;
import java.util.Objects;

/**
 * An immutable, bytecode-level instruction model.
 *
 * <p>This is deliberately close to the machine: an opcode number, a rendered
 * operand string for human display, and — crucially for control-flow
 * construction — the set of bytecode offsets this instruction can branch to.
 * It carries no Java meaning at all.</p>
 *
 * <p>Immutability is a hard constraint: the model can be cached, shared across
 * threads, and used as a key in maps without defensive copying.</p>
 *
 * <p>Story analogy: a single punched card on a player piano roll. The card
 * records a hole position and what it triggers; it says nothing about the tune
 * being a waltz or a march.</p>
 *
 * @author Jerry Zhu (Zeek)
 */
public final class Insn implements IRObject {

    private final int index;
    private final int opcode;
    private final String mnemonic;
    private final String operand;
    private final int[] branchTargets;

    /**
     * @param index         the instruction's ordinal position in the method
     * @param opcode        the JVM opcode number
     * @param mnemonic      the opcode mnemonic, e.g. {@code "if_icmpgt"}
     * @param operand       a rendered operand string, possibly empty
     * @param branchTargets bytecode offsets this instruction may jump to;
     *                      empty for straight-line instructions
     */
    public Insn(int index, int opcode, String mnemonic, String operand, int[] branchTargets) {
        this.index = index;
        this.opcode = opcode;
        this.mnemonic = Objects.requireNonNull(mnemonic, "mnemonic");
        this.operand = operand == null ? "" : operand;
        this.branchTargets = branchTargets == null ? new int[0] : branchTargets.clone();
    }

    /** @return the instruction's ordinal position in the method */
    public int index() {
        return index;
    }

    /** @return the JVM opcode number */
    public int opcode() {
        return opcode;
    }

    /** @return the opcode mnemonic */
    public String mnemonic() {
        return mnemonic;
    }

    /** @return the rendered operand string, possibly empty */
    public String operand() {
        return operand;
    }

    /**
     * @return a defensive copy of the branch targets; empty when straight-line
     */
    public int[] branchTargets() {
        return branchTargets.clone();
    }

    /** @return {@code true} if this instruction can branch */
    public boolean isBranch() {
        return branchTargets.length > 0;
    }

    /** @return a display form such as {@code "17: if_icmpgt 30"} */
    public String display() {
        return index + ": " + mnemonic + (operand.isEmpty() ? "" : " " + operand);
    }

    @Override
    public IRKind kind() {
        return IRKind.METHOD_MODEL;
    }

    @Override
    public String toString() {
        return display();
    }
}
