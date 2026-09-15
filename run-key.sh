#!/usr/bin/env bash
# Run a .key problem or a .sol contract with the Solidity CLI, without Gradle in the way.
#
# Usage: ./run-key.sh path/to/file.key [CLI options...]
#        ./run-key.sh path/to/Contract.sol [function] [CLI options...]
#        ./run-key.sh path/to/Contract.sol -f function --open-goals
#
# Every option after the file is forwarded to the CLI verbatim; see `./run-key.sh --help`.
# The fat jar is rebuilt only when it is missing or older than the sources, so a repeat run
# costs no Gradle startup at all.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR="$SCRIPT_DIR/keyext.solidity.core/build/libs/keyext.solidity.core-exe.jar"
EXAMPLES_DIR="$SCRIPT_DIR/keyext.solidity.core/src/test/resources/org/key_project/solidity/examples"
GRAAL_DIR="$SCRIPT_DIR/keyext.solidity.core/build/libs/graal-compiler"

if [ $# -eq 0 ]; then
    echo "Usage: $0 <file.key|file.sol> [function] [CLI options...]"
    echo "Example: $0 keyext.solidity.examples/TestSuite.sol testSimpleAssert"
    echo "         $0 keyext.solidity.examples/TestSuite.sol -f testSimpleAssert --open-goals"
    exit 1
fi

if [ "$1" = "--help" ] || [ "$1" = "-h" ]; then
    set -- "--help"
    FILE=""
else
    # the CLI runs from the examples directory, so a relative path has to be made absolute here
    FILE="$(cd "$(dirname "$1")" && pwd)/$(basename "$1")"
    shift
    # a bare second argument (not an option) is the function name, as it always was
    if [ $# -gt 0 ] && [ "${1#-}" = "$1" ]; then
        set -- --function "$@"
    fi
fi

needs_build() {
    [ ! -f "$JAR" ] && return 0
    [ ! -d "$GRAAL_DIR" ] && return 0
    [ -n "$(find "$SCRIPT_DIR/keyext.solidity.core/src/main" "$SCRIPT_DIR/key.core/src/main" \
        -newer "$JAR" -print -quit 2>/dev/null)" ]
}

if [ "${SOLKEY_REBUILD:-}" = "1" ] || needs_build; then
    echo "Building $(basename "$JAR")..." >&2
    (cd "$SCRIPT_DIR" && ./gradlew -q :keyext.solidity.core:shadowJar) >&2
    # Gradle leaves the jar's timestamp alone when the rebuild is a no-op (a reformat that
    # produces identical bytecode, say), which would make the check above fire on every run.
    touch "$JAR"
fi

# solc runs on the JVM as WebAssembly, which Truffle interprets — about ten times slower —
# unless the Graal compiler is put in the boot layer. Only JDK 21 has the module it upgrades,
# so the flags are tried once and dropped if this JVM will not take them.
JVM_ARGS=()
UPGRADE_PATH="$(find "$GRAAL_DIR" -name '*.jar' 2>/dev/null | paste -sd: -)"
if [ -n "$UPGRADE_PATH" ]; then
    JIT=(-XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI
         --upgrade-module-path="$UPGRADE_PATH")
    if java "${JIT[@]}" -version >/dev/null 2>&1; then
        JVM_ARGS=("${JIT[@]}")
    fi
fi

cd "$EXAMPLES_DIR"
if [ -z "$FILE" ]; then
    exec java "${JVM_ARGS[@]}" -jar "$JAR" "$@"
fi
exec java "${JVM_ARGS[@]}" -jar "$JAR" "$FILE" "$@"
