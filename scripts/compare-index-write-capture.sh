#!/usr/bin/env bash
# Compare the two `indexWriteCapture` taclet options by proof size.
#
# Proves every function of an example file under both options and prints, per function, the
# node count each option needed, then the mean over the functions the option actually changes
# and over the whole file. See docs/taclets-implementation.md, "Capture partition".
#
# Usage: scripts/compare-index-write-capture.sh [FILE.sol] [-- extra run-key.sh options]

set -u

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$SCRIPT_DIR/.."
FILE="${1:-$ROOT/keyext.solidity.examples/TestSuite.sol}"
shift || true
[ "${1:-}" = "--" ] && shift

CHOICES=(receiverThenIndex allAtOnce)
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT

for choice in "${CHOICES[@]}"; do
    echo "proving under indexWriteCapture:$choice ..." >&2
    "$ROOT/run-key.sh" "$FILE" -s -m 30000 -O "indexWriteCapture:$choice" "$@" \
        >"$OUT/$choice.log" 2>&1
done

python3 - "$OUT" "${CHOICES[@]}" <<'PY'
import re, sys, os

out, choices = sys.argv[1], sys.argv[2:]

def read(choice):
    """function -> nodes, by pairing each Statistics block with the PASS/FAIL line after it."""
    nodes, status, pending = {}, {}, None
    for line in open(os.path.join(out, choice + ".log"), errors="replace"):
        line = line.rstrip("\n")
        m = re.match(r"Statistics for \S+?\.(\w+)", line)
        if m:
            pending = m.group(1)
        m = re.match(r"Nodes: (\d+)", line)
        if m and pending:
            nodes[pending] = int(m.group(1))
        m = re.match(r"(PASS|FAIL) (\w+)", line)
        if m:
            status[m.group(2)] = m.group(1)
    return nodes, status

data = {c: read(c) for c in choices}
common = sorted(set.intersection(*(set(data[c][0]) for c in choices)))
if not common:
    sys.exit("no per-function statistics parsed; did the run fail?")

a, b = choices
changed = [f for f in common if data[a][0][f] != data[b][0][f]]

w = max(len(f) for f in common) + 2
print()
print("Functions whose proof size the option changes")
print(f"{'function':<{w}}{a:>20}{b:>14}{'delta':>10}")
for f in changed:
    x, y = data[a][0][f], data[b][0][f]
    print(f"{f:<{w}}{x:>20}{y:>14}{y - x:>+10}")

def mean(fs, c):
    return sum(data[c][0][f] for f in fs) / len(fs)

print()
print(f"{'set':<{w}}{a:>20}{b:>14}{'delta':>10}")
for label, fs in (("changed functions (mean)", changed), ("whole file (mean)", common)):
    if fs:
        x, y = mean(fs, a), mean(fs, b)
        print(f"{label:<{w}}{x:>20.1f}{y:>14.1f}{y - x:>+10.1f}")
        print(f"{'  (total)':<{w}}{sum(data[a][0][f] for f in fs):>20}"
              f"{sum(data[b][0][f] for f in fs):>14}"
              f"{sum(data[b][0][f] for f in fs) - sum(data[a][0][f] for f in fs):>+10}")

print()
for c in choices:
    st = data[c][1]
    closed = sum(1 for v in st.values() if v == "PASS")
    failed = sorted(f for f, v in st.items() if v == "FAIL")
    print(f"{c}: {closed}/{len(st)} closed" + (f"; FAILED: {', '.join(failed)}" if failed else ""))
PY
