/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.web;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.key_project.solidity.control.ProofSession;
import org.key_project.solidity.control.SolidityVerifier;
import org.key_project.solidity.program.parser.SolcWrapper;
import org.key_project.solidity.program.parser.SolidityOutline;
import org.key_project.solidity.program.parser.SoliditySources;
import org.key_project.solidity.proof.init.SolidityProblemSpec;
import org.key_project.solidity.proof.io.LoadErrors;
import org.key_project.solidity.speclang.natspec.KeyNatspec;

import org.graalvm.webimage.api.JS;
import org.graalvm.webimage.api.JSString;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

public final class WebProver {

    private static final Path CONTRACT = Path.of("/solkey-web/Contract.sol");

    private static final Path FILES = Path.of("/solkey-web/files");

    private static final int KEPT_SESSIONS = 8;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Map<Integer, ProofSession> SESSIONS = new LinkedHashMap<>();

    private static int nextSession = 1;

    private WebProver() {
    }

    public static void main(String[] args) {
        System.setProperty("key.disregardSettings", "true");
        SolcWrapper.useCompiler(new JsSolcCompiler());
        install((type, request) -> JSString.of(call(type.asString(), request.asString())));
    }

    @JS(args = { "call" },
        value = "globalThis.solkey = { call };"
            + " if (typeof globalThis.solkeyReady === 'function') globalThis.solkeyReady();")
    private static native void install(BiFunction<JSString, JSString, JSString> call);

    static String call(String type, String json) {
        ObjectNode response = MAPPER.createObjectNode();
        try {
            JsonNode request = MAPPER.readTree(json);
            switch (type) {
                case "functions" -> functions(request, response);
                case "verify" -> verify(request, response);
                case "problem" -> response.put("text",
                    SolidityVerifier.problem(register(request), spec(request)));
                case "choices" -> choices(request, response);
                case "load" -> load(request, response);
                case "tree" -> tree(session(request), response);
                case "node" -> node(request, response);
                case "rules" -> rules(request, response);
                case "apply" -> summary(response, session(request).apply(
                    request.path("serial").asInt(), request.path("index").asInt(),
                    strings(request.path("instantiations"))));
                case "prune" -> summary(response,
                    session(request).prune(request.path("serial").asInt()));
                case "auto" -> summary(response, session(request).runAuto(
                    request.hasNonNull("serial") ? request.path("serial").asInt() : null));
                case "save" -> {
                    response.put("proof", session(request).save());
                    response.put("sourcePath", CONTRACT.toString());
                }
                case "drop" -> SESSIONS.remove(request.path("session").asInt());
                default -> throw new IllegalArgumentException("unknown request " + type);
            }
        } catch (Exception e) {
            response.put("error", LoadErrors.describe(e));
        }
        return response.toString();
    }

    private static Path register(JsonNode request) {
        SoliditySources.register(CONTRACT, text(request, "source"));
        return CONTRACT;
    }

    private static SolidityProblemSpec spec(JsonNode request) {
        return new SolidityProblemSpec(optional(request, "contract"),
            optional(request, "function"), choiceList(request));
    }

    private static void functions(JsonNode request, ObjectNode response) throws Exception {
        Path file = register(request);
        SolidityOutline outline = SolidityOutline.of(file);
        ArrayNode contracts = response.putArray("contracts");
        outline.contractNames().forEach(contracts::add);
        String requested = optional(request, "contract");
        if (requested == null && outline.contracts().size() > 1) {
            response.put("needContract", true);
            return;
        }
        SolidityOutline.Contract contract = outline.requireContract(requested, file);
        response.put("contract", contract.name());
        byte[] bytes = SoliditySources.read(file).getBytes(StandardCharsets.UTF_8);
        ArrayNode invariants = response.putArray("invariants");
        try {
            KeyNatspec.of(contract.documentation()).invariants().forEach(invariants::add);
        } catch (RuntimeException e) {
            response.put("invariantError", LoadErrors.describe(e));
        }
        ArrayNode functions = response.putArray("functions");
        for (SolidityOutline.Function function : contract.functions()) {
            ObjectNode entry = functions.addObject();
            entry.put("name", function.name());
            entry.put("line", function.source().lineIn(bytes));
            function.unsupportedReason().ifPresent(reason -> entry.put("reason", reason));
            entry.put("provable", function.isProvable());
            try {
                KeyNatspec spec = function.natspec();
                ObjectNode clauses = entry.putObject("spec");
                spec.requires().forEach(clauses.putArray("requires")::add);
                spec.ensures().forEach(clauses.putArray("ensures")::add);
                clauses.put("box", spec.box());
                clauses.put("skip", spec.skip());
            } catch (RuntimeException e) {
                entry.put("provable", false);
                entry.put("reason", LoadErrors.describe(e));
            }
        }
    }

    private static void verify(JsonNode request, ObjectNode response) throws Exception {
        Path file = register(request);
        SolidityProblemSpec spec = spec(request);
        long started = System.currentTimeMillis();
        SolidityVerifier.Outcome outcome;
        ProofSession session = null;
        try {
            session = ProofSession.start(file, spec, limits(request));
            outcome = SolidityVerifier.outcome(session, String.valueOf(spec.function()),
                request.path("maxGoals").asInt(3), request.path("withProof").asBoolean(false),
                System.currentTimeMillis() - started);
        } catch (Exception e) {
            outcome = SolidityVerifier.failed(String.valueOf(spec.function()), e,
                System.currentTimeMillis() - started);
        }
        response.put("function", outcome.function());
        response.put("closed", outcome.closed());
        response.put("millis", outcome.millis());
        outcome.openGoals().forEach(response.putArray("openGoals")::add);
        if (outcome.error() != null) {
            response.put("error", outcome.error());
        }
        if (outcome.summary() != null) {
            stats(response, outcome.summary());
        }
        if (outcome.proof() != null) {
            response.put("proof", outcome.proof());
            response.put("sourcePath", CONTRACT.toString());
        }
        if (session != null && request.path("keep").asBoolean(false)) {
            response.put("session", keep(session));
        }
    }

    private static void choices(JsonNode request, ObjectNode response) throws Exception {
        Path file = register(request);
        ProofSession session =
            ProofSession.start(file, spec(request), new ProofSession.Limits(0, -1, Map.of()));
        ArrayNode categories = response.putArray("categories");
        for (SolidityVerifier.ChoiceCategory category : SolidityVerifier.choices(session)) {
            ObjectNode entry = categories.addObject();
            entry.put("category", category.category());
            category.choices().forEach(entry.putArray("choices")::add);
            entry.put("selected", category.selected());
        }
        ObjectNode strategy = response.putObject("strategy");
        ProofSession.STRATEGY_CHOICES.forEach(
            (key, values) -> values.forEach(strategy.withArray(key)::add));
    }

    private static void load(JsonNode request, ObjectNode response) throws Exception {
        Files.createDirectories(FILES);
        Path main = null;
        for (JsonNode file : request.path("files")) {
            String name = Path.of(file.path("name").asString()).getFileName().toString();
            Path path = FILES.resolve(name);
            String text = file.path("text").asString();
            Files.writeString(path, text);
            if (name.endsWith(".sol")) {
                SoliditySources.register(path, text);
            }
            if (name.equals(optional(request, "main"))) {
                main = path;
            }
        }
        if (main == null) {
            throw new IllegalArgumentException("no main file among the uploaded files");
        }
        ProofSession session =
            ProofSession.load(main, limits(request), request.path("prove").asBoolean(false));
        response.put("session", keep(session));
        summary(response, session.summary());
        session.replayErrors().forEach(response.putArray("replayErrors")::add);
        session.openGoalTexts(request.path("maxGoals").asInt(3))
                .forEach(response.putArray("openGoals")::add);
    }

    private static void tree(ProofSession session, ObjectNode response) {
        ArrayNode entries = response.putArray("nodes");
        for (ProofSession.TreeEntry e : session.tree()) {
            ArrayNode row = entries.addArray();
            row.add(e.serial());
            row.add(e.parent());
            row.add(e.name());
            if (e.branchLabel() == null) {
                row.addNull();
            } else {
                row.add(e.branchLabel());
            }
            row.add(e.state());
        }
        summary(response, session.summary());
    }

    private static void node(JsonNode request, ObjectNode response) throws Exception {
        ProofSession.NodeView view = session(request).node(request.path("serial").asInt(),
            request.path("pretty").asBoolean(true));
        response.put("serial", view.serial());
        response.put("sequent", view.sequent());
        response.put("rule", view.rule());
        response.put("ruleName", view.ruleName());
        response.put("taclet", view.taclet());
        response.put("openGoal", view.openGoal());
        response.put("children", view.children());
        response.put("branchLabel", view.branchLabel());
    }

    private static void rules(JsonNode request, ObjectNode response) throws Exception {
        ProofSession.RulesAt at = session(request).rulesAt(request.path("serial").asInt(),
            request.path("offset").asInt(-1));
        response.put("start", at.start());
        response.put("end", at.end());
        response.put("term", at.term());
        ArrayNode rules = response.putArray("rules");
        for (ProofSession.RuleOption option : at.rules()) {
            ObjectNode entry = rules.addObject();
            entry.put("index", option.index());
            entry.put("name", option.name());
            entry.put("displayName", option.displayName());
            entry.put("sequentWide", option.sequentWide());
            option.missing().forEach(entry.putArray("missing")::add);
        }
    }

    private static ProofSession.Limits limits(JsonNode request) {
        return new ProofSession.Limits(request.path("maxSteps").asInt(10000),
            request.path("timeout").asLong(-1), strings(request.path("strategy")));
    }

    private static void summary(ObjectNode response, ProofSession.Summary summary) {
        response.put("closed", summary.closed());
        response.put("openGoalCount", summary.openGoals());
        stats(response, summary);
    }

    private static void stats(ObjectNode response, ProofSession.Summary summary) {
        ObjectNode stats = response.putObject("stats");
        stats.put("nodes", summary.nodes());
        stats.put("branches", summary.branches());
        stats.put("autoModeMillis", summary.autoModeMillis());
    }

    private static int keep(ProofSession session) {
        int id = nextSession++;
        SESSIONS.put(id, session);
        while (SESSIONS.size() > KEPT_SESSIONS) {
            SESSIONS.remove(SESSIONS.keySet().iterator().next());
        }
        return id;
    }

    private static ProofSession session(JsonNode request) {
        ProofSession session = SESSIONS.get(request.path("session").asInt());
        if (session == null) {
            throw new IllegalArgumentException("the proof is no longer open; verify it again");
        }
        return session;
    }

    private static String text(JsonNode request, String key) {
        String value = optional(request, key);
        if (value == null) {
            throw new IllegalArgumentException("missing " + key);
        }
        return value;
    }

    private static @Nullable String optional(JsonNode request, String key) {
        JsonNode value = request.get(key);
        return value == null || value.isNull() || value.asString().isBlank() ? null
                : value.asString();
    }

    private static List<String> choiceList(JsonNode request) {
        String choices = optional(request, "choices");
        List<String> list = new ArrayList<>();
        if (choices != null) {
            for (String choice : choices.trim().split("[\\s,]+")) {
                if (!choice.isBlank()) {
                    list.add(choice);
                }
            }
        }
        return list;
    }

    private static Map<String, String> strings(JsonNode object) {
        Map<String, String> map = new LinkedHashMap<>();
        object.properties().forEach(e -> {
            if (!e.getValue().isNull() && !e.getValue().asString().isBlank()) {
                map.put(e.getKey(), e.getValue().asString());
            }
        });
        return map;
    }
}
