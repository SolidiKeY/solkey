#!/usr/bin/env bash
# Run a .key problem or a .sol contract with the Solidity CLI, without Gradle in the way.
#
# Usage: ./run-key.sh path/to/file.key [CLI options...]
#        ./run-key.sh path/to/Contract.sol [function] [CLI options...]
#        ./run-key.sh path/to/Contract.sol -f function --open-goals
#
# Every option after the file is forwarded to the CLI verbatim; see `./run-key.sh --help`.
# The fat jar is rebuilt only when it is missing or older than the Java sources, so a repeat run
# costs no Gradle startup at all. The taclets are read from the source tree, so editing a rule
# file needs no rebuild either.
#
# `./run-key.sh --serve` (run it in the background) starts a prover server that keeps the JVM,
# the JIT and the parsed rules warm: while it runs, every other call is handed to it, which
# saves about ten seconds per call. It reparses the rules when a rule file changes, restarts
# when the jar is rebuilt, and stops after 30 idle minutes or on `./run-key.sh --stop-server`.
# SOLKEY_SERVER=0 bypasses it. The hand-over goes through files, so it works in a sandbox that
# forbids sockets.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR="$SCRIPT_DIR/keyext.solidity.core/build/libs/keyext.solidity.core-exe.jar"
EXAMPLES_DIR="$SCRIPT_DIR/keyext.solidity.core/src/test/resources/org/key_project/solidity/examples"
GRAAL_DIR="$SCRIPT_DIR/keyext.solidity.core/build/libs/graal-compiler"
RULES_DIR="$SCRIPT_DIR/keyext.solidity.core/src/main/resources/org/key_project/solidity/proof/rules"
SPOOL="${SOLKEY_SPOOL:-${TMPDIR:-/tmp}/solkey-server-$(id -u)-$(printf %s "$SCRIPT_DIR" | cksum | cut -d' ' -f1)}"

if [ $# -eq 0 ]; then
    echo "Usage: $0 <file.key|file.sol> [function] [CLI options...]"
    echo "Example: $0 keyext.solidity.examples/TestSuite.sol testSimpleAssert"
    echo "         $0 keyext.solidity.examples/TestSuite.sol -f testSimpleAssert --open-goals"
    exit 1
fi

MODE=""
if [ "$1" = "--stop-server" ] || [ "$1" = "--serve" ] || [ "$1" = "--java-path" ]; then
    MODE="$1"
    FILE=""
    set --
elif [ "$1" = "--help" ] || [ "$1" = "-h" ]; then
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
    # the CLI runs from the examples directory, so path-valued options are made absolute too
    ARGS=()
    while [ $# -gt 0 ]; do
        case "$1" in
            --baseline|--budget|--script-file|-o|--output)
                ARGS+=("$1")
                shift
                if [ $# -gt 0 ]; then
                    case "$1" in /*) ARGS+=("$1") ;; *) ARGS+=("$PWD/$1") ;; esac
                    shift
                fi
                ;;
            *) ARGS+=("$1"); shift ;;
        esac
    done
    set -- "${ARGS[@]}"
fi

needs_build() {
    [ ! -f "$JAR" ] && return 0
    [ ! -d "$GRAAL_DIR" ] && return 0
    [ -n "$(find "$SCRIPT_DIR/keyext.solidity.core/src/main" "$SCRIPT_DIR/key.core/src/main" \
        -path "$RULES_DIR" -prune -o -newer "$JAR" -print -quit 2>/dev/null)" ]
}

server_alive() {
    local beat
    beat="$(stat -c %Y "$SPOOL/alive" 2>/dev/null)" || return 1
    [ $(( $(date +%s) - beat )) -le 5 ]
}

# Hands "$@" to the server and prints its output; returns 75 when the server cannot take it.
via_server() {
    local id="$(date +%s%N)-$$-$RANDOM"
    local base="$SPOOL/$id"
    printf '%s\0' "$@" >"$base.tmp"
    mv "$base.tmp" "$base.req"
    local waited=0
    while [ -e "$base.req" ]; do
        if [ "$waited" -ge 400 ] || ! server_alive; then
            mv "$base.req" "$base.cancel" 2>/dev/null && { rm -f "$base.cancel"; return 75; }
        fi
        sleep 0.005
        waited=$((waited + 1))
    done
    while [ ! -e "$base.exit" ]; do
        if ! server_alive; then
            rm -f "$base".*
            echo "run-key.sh: the solkey server stopped during the run" >&2
            return 1
        fi
        sleep 0.005
    done
    local status
    status="$(cat "$base.exit")"
    if [ "$status" = "stale" ]; then
        rm -f "$base".*
        return 75
    fi
    if [ "$(readlink -f /proc/$$/fd/1 2>/dev/null)" = "$(readlink -f /proc/$$/fd/2 2>/dev/null)" ]; then
        cat "$base.all"
    else
        cat "$base.out"
        cat "$base.err" >&2
    fi
    rm -f "$base".*
    return "$status"
}

# With a live server and a fresh jar, hand the call over before probing for a JDK: the probes
# start JVMs and cost a quarter of a second, the hand-over a few milliseconds.
if [ -z "$MODE" ] && [ -n "$FILE" ] && [ "${SOLKEY_SERVER:-1}" != "0" ] \
    && [ "${SOLKEY_REBUILD:-}" != "1" ] && server_alive && ! needs_build; then
    status=0
    via_server "$FILE" "$@" || status=$?
    [ "$status" -eq 75 ] || exit "$status"
fi

if [ "$MODE" = "--stop-server" ]; then
    :
elif [ "${SOLKEY_REBUILD:-}" = "1" ] || needs_build; then
    echo "Building $(basename "$JAR")..." >&2
    (cd "$SCRIPT_DIR" && ./gradlew -q :keyext.solidity.core:shadowJar 2>&1 | { grep -v '^Picked up' || true; }) >&2
    # Gradle leaves the jar's timestamp alone when the rebuild is a no-op (a reformat that
    # produces identical bytecode, say), which would make the check above fire on every run.
    touch "$JAR"
fi

# solc runs on the JVM as WebAssembly, and the Truffle release that interprets it calls a
# sun.misc.Unsafe method that JDK 24 removed, so a newer `java` on PATH dies on the first
# contract. JDK 21 is also the only one carrying the module the Graal compiler jar upgrades,
# which keeps solc compiled instead of interpreted — about ten times the speed. So: find a 21.
java_major() {
    "$1" -XshowSettings:properties -version 2>&1 \
        | sed -n 's/^ *java\.specification\.version = //p'
}

JAVA=""
for candidate in "${SOLKEY_JAVA_HOME:-}/bin/java" "${JAVA_HOME:-}/bin/java" \
    "$(command -v java || true)" \
    "$HOME"/.jdks/*/bin/java "$HOME"/.gradle/jdks/*/bin/java /usr/lib/jvm/*/bin/java; do
    if [ -x "$candidate" ] && [ "$(java_major "$candidate")" = "21" ]; then
        JAVA="$candidate"
        break
    fi
done

if [ -z "$JAVA" ]; then
    echo "run-key.sh needs a JDK 21: the WebAssembly solc does not run on 24 or newer." >&2
    echo "Install one, or point SOLKEY_JAVA_HOME at an existing JDK 21." >&2
    exit 1
fi

JVM_ARGS=()
UPGRADE_PATH="$(find "$GRAAL_DIR" -name '*.jar' 2>/dev/null | paste -sd: -)"
if [ -n "$UPGRADE_PATH" ]; then
    JIT=(-XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI
         --upgrade-module-path="$UPGRADE_PATH")
    if "$JAVA" "${JIT[@]}" -version >/dev/null 2>&1; then
        JVM_ARGS=("${JIT[@]}")
    fi
fi

# The JVM announces JAVA_TOOL_OPTIONS on stderr in every run; pass them as arguments instead.
if [ -n "${JAVA_TOOL_OPTIONS:-}" ]; then
    read -r -a TOOL_OPTIONS <<<"$JAVA_TOOL_OPTIONS"
    JVM_ARGS+=("${TOOL_OPTIONS[@]}")
    unset JAVA_TOOL_OPTIONS
fi
JVM_ARGS+=("-Dorg.key_project.stdTacletDirectory=$RULES_DIR")

if [ "$MODE" = "--java-path" ]; then
    echo "$JAVA"
    exit 0
fi

if [ "$MODE" = "--stop-server" ]; then
    if server_alive; then via_server --stop || true; else echo "no solkey server running"; fi
    exit 0
fi

cd "$EXAMPLES_DIR"

if [ "$MODE" = "--serve" ]; then
    if server_alive; then
        echo "a solkey server already runs in $SPOOL"
        exit 0
    fi
    mkdir -p "$SPOOL"
    while :; do
        status=0
        "$JAVA" "${JVM_ARGS[@]}" -cp "$JAR" org.key_project.solidity.CLIServer "$SPOOL" || status=$?
        [ "$status" -eq 3 ] || exit "$status"
        if needs_build; then
            (cd "$SCRIPT_DIR" && ./gradlew -q :keyext.solidity.core:shadowJar) >&2
            touch "$JAR"
        fi
    done
fi

if [ -n "$FILE" ]; then
    set -- "$FILE" "$@"
fi

if [ "${SOLKEY_SERVER:-1}" != "0" ] && [ -n "$FILE" ] && server_alive; then
    status=0
    via_server "$@" || status=$?
    [ "$status" -eq 75 ] || exit "$status"
fi

exec "$JAVA" "${JVM_ARGS[@]}" -jar "$JAR" "$@"
