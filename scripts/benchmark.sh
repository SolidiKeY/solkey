#!/usr/bin/env bash
# Prove every contract of keyext.solidity.examples/benchmark/ and print one line per contract.
#
# Usage: scripts/benchmark.sh [FILE.sol ...]
#
# Each line is `Contract  N/M`, N closed out of M obligations, or `Contract  load error: ...`.
# A file with several contracts is run once per contract. The last line is the total.

set -u

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUN_KEY="$SCRIPT_DIR/../run-key.sh"
BENCH_DIR="$SCRIPT_DIR/../keyext.solidity.examples/benchmark"

total_closed=0
total_obligations=0

load_error() {
    grep -v '^\s*$' <<<"$1" | grep -A2 -m1 'Error while' | tail -n +2 \
        | sed 's/^ *//' | grep -v -e '^Not possible to compile' -e '^(run with' | head -1 \
        | sed 's/^ParserError: //; s#"/[^"]*/#"#' | cut -c1-90
}

report() {
    local label="$1" out="$2" summary
    summary="$(grep -Eo '^[0-9]+/[0-9]+ closed' <<<"$out" | tail -1)"
    if [ -n "$summary" ]; then
        local closed="${summary%%/*}" rest="${summary#*/}"
        local obligations="${rest%% *}"
        total_closed=$((total_closed + closed))
        total_obligations=$((total_obligations + obligations))
        local reason
        reason="$(load_error "$out")"
        printf '%-28s %s%s\n' "$label" "$closed/$obligations" "${reason:+  (load error: $reason)}"
    else
        printf '%-28s load error: %s\n' "$label" "$(load_error "$out")"
    fi
}

if [ $# -gt 0 ]; then files=("$@"); else files=("$BENCH_DIR"/*.sol); fi

for file in "${files[@]}"; do
    name="$(basename "$file" .sol)"
    out="$("$RUN_KEY" "$file" --quiet 2>&1)"
    contracts="$(grep -o 'use --contract with one of: \[.*\]' <<<"$out" | sed 's/.*\[\(.*\)\]/\1/; s/,//g')"
    if [ -n "$contracts" ]; then
        for contract in $contracts; do
            report "$name:$contract" "$("$RUN_KEY" "$file" --contract "$contract" --quiet 2>&1)"
        done
    else
        report "$name" "$out"
    fi
done

printf '%-28s %s\n' "TOTAL" "$total_closed/$total_obligations"
