/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.control;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.key_project.solidity.program.parser.SolidityOutline;

import org.jspecify.annotations.Nullable;

public final class StepBudget {

    public static final int FLOOR = 1000;
    public static final int MIN_ESTIMATE = 10000;
    public static final int MAX_ESTIMATE = 100000;
    public static final int STEPS_PER_SOURCE_BYTE = 60;

    public record Entry(String status, int steps, String stopReason) {
        public boolean cut() {
            return stopReason.equals(ProofSession.StopReason.STEP_LIMIT.name())
                    || stopReason.equals(ProofSession.StopReason.TIMEOUT.name());
        }

        public int budget() {
            return cut() ? steps : steps + steps / 2 + FLOOR;
        }

        String line(String key) {
            return key + " " + status + " " + steps + " " + stopReason;
        }
    }

    private final Map<String, Entry> entries;

    private StepBudget(Map<String, Entry> entries) {
        this.entries = entries;
    }

    public static StepBudget empty() {
        return new StepBudget(new LinkedHashMap<>());
    }

    public static StepBudget load(Path file) throws IOException {
        Map<String, Entry> entries = new LinkedHashMap<>();
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            String trimmed = line.strip();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            String[] fields = trimmed.split("\\s+");
            if (fields.length < 2) {
                continue;
            }
            int steps = fields.length > 2 ? Integer.parseInt(fields[2]) : -1;
            String stop = fields.length > 3 ? fields[3] : "";
            entries.put(fields[0], new Entry(fields[1], steps, stop));
        }
        return new StepBudget(entries);
    }

    public static StepBudget loadIfExists(Path file) throws IOException {
        return Files.exists(file) ? load(file) : empty();
    }

    public Map<String, Entry> entries() {
        return entries;
    }

    public void put(String key, Entry entry) {
        entries.put(key, entry);
    }

    public static <T> T locked(Path file, IOCallable<T> action) throws IOException {
        Path lock = Path.of(System.getProperty("java.io.tmpdir"),
            "solkey-steps-" + Integer.toHexString(file.toAbsolutePath().normalize().hashCode())
                + ".lock");
        try (FileChannel channel = FileChannel.open(lock, StandardOpenOption.CREATE,
            StandardOpenOption.WRITE); FileLock ignored = channel.lock()) {
            return action.call();
        }
    }

    public interface IOCallable<T> {
        T call() throws IOException;
    }

    public void write(Path file) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("# function STATUS STEPS STOP_REASON — recorded rule applications; the budget is "
            + "STEPS, or STEPS*1.5+" + FLOOR + " unless the run was cut at a limit");
        entries.forEach((key, entry) -> lines.add(entry.line(key)));
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.write(tmp, lines, StandardCharsets.UTF_8);
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    public @Nullable Entry entry(@Nullable String contract, String function) {
        Entry qualified = contract == null ? null : entries.get(contract + "." + function);
        return qualified != null ? qualified : entries.get(function);
    }

    public @Nullable Integer recordedBudget(@Nullable String contract, String function) {
        Entry entry = entry(contract, function);
        return entry == null || entry.steps() < 0 ? null : entry.budget();
    }

    public static int estimate(Path solFile, @Nullable String contract, String function) {
        return estimate(sourceBytes(solFile, contract, function));
    }

    public static int sourceBytes(Path solFile, @Nullable String contract, String function) {
        try {
            for (var declared : SolidityOutline.of(solFile).contracts()) {
                if (contract == null || declared.name().equals(contract)) {
                    var found = declared.function(function);
                    if (found.isPresent()) {
                        return found.get().source().byteLength();
                    }
                }
            }
        } catch (IOException | RuntimeException e) {
            return 0;
        }
        return 0;
    }

    public static int estimate(int sourceBytes) {
        long estimate = (long) STEPS_PER_SOURCE_BYTE * sourceBytes;
        return (int) Math.max(MIN_ESTIMATE, Math.min(MAX_ESTIMATE, estimate));
    }
}
