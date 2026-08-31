/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.runtime;

import java.math.BigInteger;
import java.util.Deque;
import java.util.List;

import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.datatypes.Wei;
import org.hyperledger.besu.evm.EVM;
import org.hyperledger.besu.evm.MainnetEVMs;
import org.hyperledger.besu.evm.fluent.SimpleBlockValues;
import org.hyperledger.besu.evm.fluent.SimpleWorld;
import org.hyperledger.besu.evm.frame.MessageFrame;
import org.hyperledger.besu.evm.internal.EvmConfiguration;
import org.hyperledger.besu.evm.precompile.PrecompileContractRegistry;
import org.hyperledger.besu.evm.processor.MessageCallProcessor;
import org.hyperledger.besu.evm.tracing.OperationTracer;

/// Executes one example contract's functions on an in-process Besu EVM.
///
/// Every [#call] runs against a freshly deployed contract with all-zero storage, so runs are
/// independent of each other, exactly like the proof obligations. Deployment installs the
/// runtime bytecode directly instead of executing the creation code; the test fixture
/// guarantees this is equivalent by rejecting contracts with a constructor or an initialized
/// state variable.
final class EvmContractRunner {

    /// The final disposition of a call: `revertData` is non-null exactly for [Status#REVERT]
    /// (and may be empty, a bare `revert`), `haltReason` exactly for
    /// [Status#EXCEPTIONAL_HALT].
    record CallResult(Status status, Bytes revertData, String haltReason) {
        enum Status {
            SUCCESS, REVERT, EXCEPTIONAL_HALT
        }
    }

    private static final Address SENDER =
        Address.fromHexString("0x00000000000000000000000000000000cafebabe");
    private static final Address CONTRACT =
        Address.fromHexString("0x00000000000000000000000000000000deadbeef");
    private static final long GAS_LIMIT = 30_000_000L;

    private final EVM evm = MainnetEVMs.cancun(EvmConfiguration.DEFAULT);
    private final Bytes runtimeCode;

    EvmContractRunner(String runtimeBinHex) {
        this.runtimeCode = Bytes.fromHexString(runtimeBinHex);
    }

    CallResult call(String signature, List<BigInteger> args) {
        SimpleWorld world = new SimpleWorld();
        world.createAccount(SENDER, 0, Wei.of(BigInteger.TEN.pow(18)));
        world.createAccount(CONTRACT, 1, Wei.ZERO).setCode(runtimeCode);

        MessageFrame frame = MessageFrame.builder()
                .type(MessageFrame.Type.MESSAGE_CALL)
                .worldUpdater(world.updater())
                .initialGas(GAS_LIMIT)
                .address(CONTRACT)
                .contract(CONTRACT)
                .sender(SENDER)
                .originator(SENDER)
                .gasPrice(Wei.ZERO)
                .value(Wei.ZERO)
                .apparentValue(Wei.ZERO)
                .inputData(Abi.encodeCall(signature, args))
                .code(evm.getCodeUncached(runtimeCode))
                .blockValues(new SimpleBlockValues())
                .miningBeneficiary(Address.ZERO)
                .blockHashLookup(number -> Hash.ZERO)
                .completer(f -> {
                })
                .build();

        MessageCallProcessor processor =
            new MessageCallProcessor(evm, new PrecompileContractRegistry());
        Deque<MessageFrame> stack = frame.getMessageFrameStack();
        while (!stack.isEmpty()) {
            processor.process(stack.peekFirst(), OperationTracer.NO_TRACING);
        }

        if (frame.getState() == MessageFrame.State.COMPLETED_SUCCESS) {
            return new CallResult(CallResult.Status.SUCCESS, null, null);
        }
        return frame.getRevertReason()
                .map(reason -> new CallResult(CallResult.Status.REVERT, reason, null))
                .orElseGet(() -> new CallResult(CallResult.Status.EXCEPTIONAL_HALT, null,
                    frame.getExceptionalHaltReason().map(Object::toString).orElse("unknown")));
    }
}
