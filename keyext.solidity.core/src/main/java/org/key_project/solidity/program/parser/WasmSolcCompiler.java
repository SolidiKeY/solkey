/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.parser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.concurrent.locks.ReentrantLock;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotAccess;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.io.IOAccess;

import static java.nio.charset.StandardCharsets.UTF_8;

/// solc itself, running on the JVM.
///
/// `soljson.js` is the compiler's official WebAssembly build — the one `solc-js` ships — hosted
/// by GraalJS with GraalWasm underneath it. Both are plain Java bytecode, so a proof needs no
/// native executable and no forked process, and the AST is the one solc would have printed.
///
/// Instantiating the module is what costs; a compile afterwards is a call into the warmed
/// instance. The instance is therefore created once per JVM and reused, which also makes it
/// shared mutable state: a [Context] does not admit concurrent evaluation, so [#compile] is
/// serialized.
public final class WasmSolcCompiler implements SolcCompiler {

    private static final String SOLJSON_RESOURCE = "/soljson.js";

    private static final String BIND_ENTRY_POINTS =
        """
                ({
                  compile: Module.cwrap('solidity_compile', 'string',
                      ['string', 'number', 'number']),
                  version: Module.cwrap('solidity_version', 'string', [])
                })
                """;

    private static final class Instance {
        private static final WasmSolcCompiler INSTANCE = new WasmSolcCompiler();
    }

    public static WasmSolcCompiler get() {
        return Instance.INSTANCE;
    }

    private final ReentrantLock lock = new ReentrantLock();

    private Context context;
    private Value compile;
    private Value version;

    private WasmSolcCompiler() {
    }

    @Override
    public String compile(String standardJsonInput) throws IOException {
        lock.lock();
        try {
            initialize();
            return compile.execute(standardJsonInput, 0, 0).asString();
        } catch (PolyglotException e) {
            throw new IOException("solc failed on the standard JSON input", e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String version() {
        lock.lock();
        try {
            initialize();
            return version.execute().asString();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            lock.unlock();
        }
    }

    private void initialize() throws IOException {
        if (compile != null) {
            return;
        }
        Context built = Context.newBuilder("js", "wasm")
                .allowExperimentalOptions(true)
                .option("js.webassembly", "true")
                .option("engine.WarnInterpreterOnly", "false")
                .allowPolyglotAccess(PolyglotAccess.ALL)
                .allowHostAccess(HostAccess.NONE)
                .allowIO(IOAccess.NONE)
                .build();
        try {
            built.eval(soljsonSource());
            Value entryPoints = built.eval("js", BIND_ENTRY_POINTS);
            compile = entryPoints.getMember("compile");
            version = entryPoints.getMember("version");
        } catch (PolyglotException e) {
            built.close(true);
            throw new IOException("could not start the bundled solc", e);
        } catch (IOException | RuntimeException e) {
            built.close(true);
            throw e;
        }
        context = built;
    }

    private static Source soljsonSource() throws IOException {
        InputStream stream = WasmSolcCompiler.class.getResourceAsStream(SOLJSON_RESOURCE);
        if (stream == null) {
            throw new IOException("no " + SOLJSON_RESOURCE + " on the classpath; "
                + "the downloadSoljson Gradle task provides it");
        }
        try (Reader reader = new InputStreamReader(stream, UTF_8)) {
            return Source.newBuilder("js", reader, "soljson.js").build();
        }
    }
}
