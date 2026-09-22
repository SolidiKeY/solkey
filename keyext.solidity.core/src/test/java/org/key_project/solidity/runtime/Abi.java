/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.runtime;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;

import org.key_project.solidity.program.parser.SolidityOutline;

import org.apache.tuweni.bytes.Bytes;
import org.bouncycastle.jcajce.provider.digest.Keccak;

import static java.nio.charset.StandardCharsets.UTF_8;

/// The sliver of contract ABI encoding the runtime cross-check needs: 4-byte selectors and
/// calls whose arguments are all 32-byte integer words (provable example functions only take
/// `int`/`uint` parameters, see `SolidityOutline.Function#isProvable`).
final class Abi {

    private Abi() {}

    /// Canonical signature of an example function, e.g. `pushWithArgument(uint256)`. The
    /// parameter types come from solc's `typeString`, which is already canonical.
    static String signatureOf(SolidityOutline.Function function) {
        return function.name() + function.parameters().stream()
                .map(SolidityOutline.Parameter::type)
                .collect(Collectors.joining(",", "(", ")"));
    }

    static Bytes selector(String signature) {
        return Bytes.wrap(new Keccak.Digest256().digest(signature.getBytes(UTF_8))).slice(0, 4);
    }

    /// Selector plus one sign-extended 32-byte big-endian word per argument.
    static Bytes encodeCall(String signature, List<BigInteger> args) {
        Bytes[] parts = new Bytes[args.size() + 1];
        parts[0] = selector(signature);
        for (int i = 0; i < args.size(); i++) {
            parts[i + 1] = word(args.get(i));
        }
        return Bytes.concatenate(parts);
    }

    private static Bytes word(BigInteger value) {
        byte[] word = new byte[32];
        if (value.signum() < 0) {
            java.util.Arrays.fill(word, (byte) 0xFF);
        }
        byte[] raw = value.toByteArray();
        System.arraycopy(raw, 0, word, 32 - raw.length, raw.length);
        return Bytes.wrap(word);
    }
}
