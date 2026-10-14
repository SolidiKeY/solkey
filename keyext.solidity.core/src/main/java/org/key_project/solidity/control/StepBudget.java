/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.control;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

public final class StepBudget {

    public static final String FILE_NAME = "steps.txt";
    public static final int FLOOR = 1000;
    public static final int MIN_ESTIMATE = 5000;
    public static final int MAX_ESTIMATE = 100000;
    public static final int STEPS_PER_SOURCE_BYTE = 25;

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

    public void write(Path file) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("# function STATUS STEPS STOP_REASON — recorded rule applications; the budget is "
            + "STEPS, or STEPS*1.5+" + FLOOR + " unless the run was cut at a limit");
        entries.forEach((key, entry) -> lines.add(entry.line(key)));
        Files.write(file, lines, StandardCharsets.UTF_8);
    }

    public @Nullable Entry entry(@Nullable String contract, String function) {
        Entry qualified = contract == null ? null : entries.get(contract + "." + function);
        return qualified != null ? qualified : entries.get(function);
    }

    public @Nullable Integer recordedBudget(@Nullable String contract, String function) {
        Entry entry = entry(contract, function);
        return entry == null || entry.steps() < 0 ? null : entry.budget();
    }

    public static int estimate(int sourceBytes) {
        long estimate = (long) STEPS_PER_SOURCE_BYTE * sourceBytes;
        return (int) Math.max(MIN_ESTIMATE, Math.min(MAX_ESTIMATE, estimate));
    }
}
