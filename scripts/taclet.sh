#!/usr/bin/env bash
# Print one taclet, the section index, or the list of rule names, without reading a whole .key file.
#
# Usage: scripts/taclet.sh NAME [file.key]   print the taclet called NAME
#        scripts/taclet.sh --index [file]    print the "Rules for ..." section banners
#        scripts/taclet.sh --list [file]     print every rule name with its file:line
#
# Taclets are delimited uniformly: `    <name> {` opens and `    };` closes, both at 4 spaces.

set -u

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RULES_DIR="$SCRIPT_DIR/../keyext.solidity.core/src/main/resources/org/key_project/solidity/proof/rules"

usage() {
    sed -n '2,8p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
}

files() {
    if [ $# -gt 0 ] && [ -n "$1" ]; then
        if [ -f "$1" ]; then echo "$1"; else echo "$RULES_DIR/$1"; fi
    else
        ls "$RULES_DIR"/*.key
    fi
}

case "${1:-}" in
    -h|--help|"")
        usage
        exit 0
        ;;
    --index)
        shift
        for f in $(files "${1:-}"); do
            grep -nE '^\s*// -+ ?Rules for' "$f" /dev/null | sed "s|$RULES_DIR/||"
        done
        ;;
    --list)
        shift
        for f in $(files "${1:-}"); do
            grep -nE '^    [A-Za-z_][A-Za-z0-9_]* \{$' "$f" /dev/null \
                | sed "s|$RULES_DIR/||" | sed 's/ {$//'
        done
        ;;
    *)
        NAME="$1"
        shift
        FOUND=0
        for f in $(files "${1:-}"); do
            LINE=$(grep -nE "^    ${NAME} \{$" "$f" | cut -d: -f1 | head -1)
            [ -z "$LINE" ] && continue
            FOUND=1
            echo "${f#"$RULES_DIR"/}:$LINE"
            awk -v start="$LINE" 'NR >= start { print; if ($0 ~ /^    \};$/) exit }' "$f"
        done
        if [ "$FOUND" -eq 0 ]; then
            echo "No taclet named '$NAME'. Similar names:" >&2
            for f in $(files "${1:-}"); do
                grep -nE '^    [A-Za-z_][A-Za-z0-9_]* \{$' "$f" /dev/null \
                    | sed "s|$RULES_DIR/||" | sed 's/ {$//' | grep -i "$NAME"
            done >&2
            exit 1
        fi
        ;;
esac
