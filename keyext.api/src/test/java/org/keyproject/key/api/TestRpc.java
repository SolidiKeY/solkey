/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.keyproject.key.api;

import java.io.File;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.json.StreamMessageProducer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keyproject.key.api.data.LoadParams;
import org.keyproject.key.api.data.Uri;
import org.keyproject.key.api.remoteapi.KeyApi;
import org.keyproject.key.api.remoteclient.ClientApi;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestRpc {
    private Future<Void> clientListening, serverListening;
    private KeyApi keyApi;

    @BeforeEach
    void setup() throws IOException {
        PipedInputStream inClient = new PipedInputStream();
        PipedOutputStream outClient = new PipedOutputStream();
        PipedInputStream inServer = new PipedInputStream();
        PipedOutputStream outServer = new PipedOutputStream();

        inClient.connect(outServer);
        outClient.connect(inServer);

        KeYtherApiImpl impl = new KeYtherApiImpl();
        Launcher<ClientApi> serverLauncher = StartServer.launch(outServer, inServer, impl);
        impl.setClientApi(serverLauncher.getRemoteProxy());

        var client = new SimpleClient();
        Launcher<KeyApi> clientLauncher = new Launcher.Builder<KeyApi>()
                .setLocalService(client)
                .setRemoteInterfaces(Collections.singleton(KeyApi.class))
                .setInput(inClient)
                .setOutput(outClient)
                .configureGson(StartServer::configureJson)
                .traceMessages(new PrintWriter(System.err))
                .create();

        Logger logger = Logger.getLogger(StreamMessageProducer.class.getName());
        logger.setLevel(Level.SEVERE);

        clientListening = clientLauncher.startListening();
        serverListening = serverLauncher.startListening();

        keyApi = clientLauncher.getRemoteProxy();
    }

    @AfterEach
    void teardown() {
        serverListening.cancel(true);
        clientListening.cancel(true);
    }


    @Test
    public void hello() {

    }

    /// Exercises the load and save support over the JSON-RPC boundary: load a problem,
    /// save the proof to a file and load it back. A saved `.proof` file must reload as a
    /// proof (the right side of the {@code Either}), not just an environment.
    @Test
    public void saveAndLoadRoundTrip() throws Exception {
        var proofId = keyApi.loadTerm("true").get();
        assertNotNull(proofId);

        File saved = File.createTempFile("keyext-api-roundtrip-", ".proof");
        try {
            Boolean ok = keyApi.save(proofId, saved.getAbsolutePath()).get();
            assertTrue(ok, "save should report success");
            assertTrue(saved.length() > 0, "saved proof file should not be empty");

            var loaded = keyApi.load(new LoadParams(Uri.from(saved), null)).get();
            assertTrue(loaded.isRight(), "a saved .proof should reload as a proof");
            assertNotNull(loaded.getRight());
        } finally {
            saved.delete();
        }
    }

    // @Test
    // public void listMacros() throws ExecutionException, InterruptedException {
    // var examples = keyApi.getAvailableMacros().get();
    // System.out.println(examples);
    // }
    //
    // @Test
    // public void listExamples() throws ExecutionException, InterruptedException {
    // var examples = keyApi.examples().get();
    // System.out.println(examples);
    // }
}
