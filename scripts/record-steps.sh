#!/usr/bin/env bash
# Record the deterministic step budgets the example tests prove with.
#
# Usage: scripts/record-steps.sh [DIR | DIR/Contract.sol ...]
#
# Each DIR of keyext.solidity.examples (default: every directory the example tests read) gets a
# steps.txt with one `Contract.function STATUS STEPS STOP_REASON` line per obligation, from one
# run-key.sh run per contract under the generous limits below. The tests then prove with that
# step count and no timeout, so a result never depends on the machine's speed. Re-record after
# a change that makes proofs legitimately longer, and commit the steps.txt files.

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
EXAMPLES="$ROOT/keyext.solidity.examples"
DEFAULT_DIRS=(contracts real-world solc solc/open real-world/open)

limits() {
    case "$1" in
        solc) echo "50000 120000" ;;
        */open) echo "50000 30000" ;;
        *) echo "20000 60000" ;;
    esac
}

record() {
    local file="$1" dir
    dir="$(dirname "${file#"$EXAMPLES"/}")"
    read -r steps timeout <<<"$(limits "$dir")"
    echo "$dir/$(basename "$file")" >&2
    "$ROOT/run-key.sh" "$file" --quiet -m "$steps" -t "$timeout" \
        --baseline "$EXAMPLES/$dir/steps.txt" --baseline-update | grep -E 'closed$|^Error' || true
}

targets=("$@")
[ ${#targets[@]} -gt 0 ] || targets=("${DEFAULT_DIRS[@]}")
for target in "${targets[@]}"; do
    path="$target"
    [ -e "$path" ] || path="$EXAMPLES/$target"
    path="$(cd "$(dirname "$path")" && pwd)/$(basename "$path")"
    if [ -d "$path" ]; then
        for file in "$path"/*.sol; do record "$file"; done
    else
        record "$path"
    fi
done
