#!/usr/bin/env bash
# Regenerates every ANTLR grammar and lists the tool's warnings, one per line as
# `module: warning(N): file:line:col: message`. On GitHub Actions each warning also
# becomes a `::warning` annotation and the list goes into the job summary.
# Never fails because of warnings: they are a reminder, not a gate.
set -uo pipefail
cd "$(dirname "$0")/.."

declare -A grammarDir=(
    [key.ncore]=key.ncore/src/main/antlr
    [key.core]=key.core/src/main/antlr4
    [keyext.solidity.core]=keyext.solidity.core/src/main/antlr
)

out=$(mktemp)
total=0
for module in key.ncore key.core keyext.solidity.core; do
    log=$(mktemp)
    if ! ./gradlew -q ":$module:runAntlr4Key" --rerun >"$log" 2>&1; then
        cat "$log"
        echo "antlr-warnings: :$module:runAntlr4Key failed" >&2
        exit 1
    fi
    while IFS= read -r line; do
        total=$((total + 1))
        echo "$module: $line" | tee -a "$out"
        [[ -n "${GITHUB_ACTIONS:-}" ]] || continue
        file=$(sed -E 's/^warning\([0-9]+\): ([^:]+):([0-9]+):([0-9]+):.*/\1/' <<<"$line")
        lineNo=$(sed -E 's/^warning\([0-9]+\): [^:]+:([0-9]+):.*/\1/' <<<"$line")
        msg=$(sed -E 's/^warning\([0-9]+\): [^:]+:[0-9]+:[0-9]+: //' <<<"$line")
        path="${grammarDir[$module]}/$file"
        [[ -f "$path" ]] || path="${grammarDir[key.ncore]}/$file"
        token=$(sed -nE 's/.*implicit definition of token ([A-Z_0-9]+) in parser.*/\1/p' <<<"$msg")
        if [[ -n "$token" ]]; then
            hit=$(grep -rnw "$token" "${grammarDir[$module]}" --include='*Parser*.g4' | head -1)
            if [[ -n "$hit" ]]; then
                path=${hit%%:*}
                lineNo=$(cut -d: -f2 <<<"$hit")
            fi
        fi
        echo "::warning file=$path,line=$lineNo,title=ANTLR ($module)::$msg"
    done < <(grep -E '^warning\(' "$log")
    rm -f "$log"
done

echo "antlr-warnings: $total warning(s)"
if [[ -n "${GITHUB_STEP_SUMMARY:-}" ]]; then
    {
        echo "### ANTLR warnings: $total"
        if ((total > 0)); then
            echo '```'
            cat "$out"
            echo '```'
        fi
    } >>"$GITHUB_STEP_SUMMARY"
fi
rm -f "$out"
