#!/usr/bin/env bash
# Prove every function of the open/ example directories and report the ones whose status changed.
#
# Usage: scripts/open-check.sh [-j JOBS] [FILE.sol ...]
#
# Defaults to keyext.solidity.examples/solc/open/*.sol and real-world/open/*.sol. Proves with the
# budget of OpenExamplesStayOpenTest (50000 steps, 30 s), the CLI form of the same CI check.
# A function whose `// open:` line says "closes in KeY" is a known closer (a proves-false witness
# or a true claim kept out for another reason); every other function is expected to stay open.
#
#   NOW CLOSES  file:function   expected open, now closes: move it to the closing file
#   NOW OPEN    file:function   a known closer stopped closing: a soundness bug may be fixed
#
# The last line counts both. Exit status is 1 when anything changed.

set -u

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUN_KEY="$SCRIPT_DIR/../run-key.sh"
EXAMPLES="$SCRIPT_DIR/../keyext.solidity.examples"

jobs=3
if [ "${1:-}" = "-j" ]; then jobs="$2"; shift 2; fi
if [ $# -gt 0 ]; then files=("$@"); else files=("$EXAMPLES"/solc/open/*.sol "$EXAMPLES"/real-world/open/*.sol); fi

"$RUN_KEY" --help >/dev/null 2>&1

known_closers() {
    awk '
        /\/\/ open:/ { closes = ($0 ~ /\/\/ open: *closes in KeY/) }
        /^[[:space:]]*function[[:space:]]+[A-Za-z_]/ {
            name = $0; sub(/^[[:space:]]*function[[:space:]]+/, "", name); sub(/\(.*/, "", name)
            if (closes) print name; closes = 0
        }
        /^[[:space:]]*constructor[[:space:]]*\(/ { if (closes) print "constructor"; closes = 0 }
    ' "$1"
}

check_file() {
    local file="$1" name out contracts
    name="$(basename "$file")"
    out="$("$RUN_KEY" "$file" -m 50000 -t 30000 2>&1)"
    contracts="$(grep -o 'use --contract with one of: \[.*\]' <<<"$out" | sed 's/.*\[\(.*\)\]/\1/; s/,//g')"
    if [ -n "$contracts" ]; then
        out=""
        for contract in $contracts; do out+="$("$RUN_KEY" "$file" --contract "$contract" -m 50000 -t 30000 2>&1)"$'\n'; done
    fi
    if ! grep -Eq '^[0-9]+/[0-9]+ closed' <<<"$out"; then
        echo "LOAD ERROR  $name"
        return
    fi
    local closers fn
    closers="$(known_closers "$file")"
    for fn in $(sed -n 's/^PASS *\([A-Za-z_0-9]*\).*/\1/p' <<<"$out"); do
        grep -qx "$fn" <<<"$closers" || echo "NOW CLOSES  $name:$fn"
    done
    for fn in $(sed -n 's/^FAIL *\([A-Za-z_0-9]*\).*/\1/p' <<<"$out"); do
        grep -qx "$fn" <<<"$closers" && echo "NOW OPEN    $name:$fn"
    done
}
export -f check_file known_closers
export RUN_KEY

report="$(printf '%s\n' "${files[@]}" | xargs -P "$jobs" -I{} bash -c 'check_file "$1"' _ {})"
[ -n "$report" ] && sort <<<"$report"
changed="$(grep -c . <<<"$report")"
echo "changed: $changed"
[ "$changed" -eq 0 ]
