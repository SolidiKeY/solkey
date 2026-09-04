#!/usr/bin/env bash
# Install the SolKey External Tools into every IntelliJ IDEA installation of this user.
#
# External Tools live in the IDE configuration directory, not in the project, so they cannot be
# shared through the repository the way .run/ configurations are. This script recreates them on
# any machine that has a clone.
#
# It installs a toolset of its own ("SolKey.xml") and never rewrites another toolset. The one
# exception is the hand-made "Run with solidity" tool this toolset replaces: its file is deleted
# when that tool is all it contains, and otherwise left alone with a note.
#
# Usage: scripts/install-idea-external-tools.sh [--list] [--dry-run] [--all]

set -euo pipefail

TOOLSET_NAME="SolKey"
LEGACY_FILE="External Tools.xml"
LEGACY_TOOL="Run with solidity"
GRADLE="\$ProjectFileDir\$/gradlew"

list_only=false
dry_run=false
all_ides=false

for arg in "$@"; do
    case "$arg" in
        --list) list_only=true ;;
        --dry-run) dry_run=true ;;
        --all) all_ides=true ;;
        -h | --help)
            sed -n '2,12p' "$0" | sed 's/^# \{0,1\}//'
            exit 0
            ;;
        *)
            echo "unknown option: $arg (try --help)" >&2
            exit 2
            ;;
    esac
done

case "$(uname -s)" in
    Darwin) jetbrains_dir="$HOME/Library/Application Support/JetBrains" ;;
    MINGW* | MSYS* | CYGWIN*)
        jetbrains_dir="${APPDATA:-$HOME/AppData/Roaming}/JetBrains"
        GRADLE="\$ProjectFileDir\$/gradlew.bat"
        ;;
    *) jetbrains_dir="${XDG_CONFIG_HOME:-$HOME/.config}/JetBrains" ;;
esac

if [ ! -d "$jetbrains_dir" ]; then
    echo "no JetBrains configuration directory at $jetbrains_dir — is an IDE installed?" >&2
    exit 1
fi

# IDEs that can open this Gradle project. --all lifts the filter.
ide_dirs=()
for dir in "$jetbrains_dir"/*/; do
    name="$(basename "$dir")"
    case "$name" in
        *-backup) continue ;;
    esac
    if $all_ides; then
        ide_dirs+=("$dir")
        continue
    fi
    case "$name" in
        IntelliJIdea* | IdeaIC* | IdeaIU*) ide_dirs+=("$dir") ;;
    esac
done

if [ ${#ide_dirs[@]} -eq 0 ]; then
    echo "no IntelliJ IDEA configuration directory found in $jetbrains_dir" >&2
    echo "(use --all to install into every JetBrains IDE found there)" >&2
    exit 1
fi

if $list_only; then
    echo "IDE configuration directories that would receive $TOOLSET_NAME.xml:"
    for dir in "${ide_dirs[@]}"; do
        marker=" "
        [ -f "$dir/tools/$TOOLSET_NAME.xml" ] && marker="*"
        echo "  $marker $(basename "$dir")"
    done
    echo "(* = already installed)"
    exit 0
fi

read -r -d '' toolset <<EOF || true
<toolSet name="$TOOLSET_NAME">
  <tool name="Verify in KeYther (GUI)" showInMainMenu="true" showInEditor="true" showInProject="true" showInSearchPopup="true" disabled="false" useConsole="true" showConsoleOnStdOut="true" showConsoleOnStdErr="true" synchronizeAfterRun="true">
    <exec>
      <option name="COMMAND" value="$GRADLE" />
      <option name="PARAMETERS" value=":keyext.solidity.gui:solidityGui -PsolFile=\$FilePath\$" />
      <option name="WORKING_DIRECTORY" value="\$ProjectFileDir\$" />
    </exec>
  </tool>
  <tool name="Verify with Solidity CLI" showInMainMenu="true" showInEditor="true" showInProject="true" showInSearchPopup="true" disabled="false" useConsole="true" showConsoleOnStdOut="true" showConsoleOnStdErr="true" synchronizeAfterRun="true">
    <exec>
      <option name="COMMAND" value="$GRADLE" />
      <option name="PARAMETERS" value=":keyext.solidity.core:solidityCli -PkeyFile=\$FilePath\$" />
      <option name="WORKING_DIRECTORY" value="\$ProjectFileDir\$" />
    </exec>
  </tool>
</toolSet>
EOF

# The hand-made predecessor of "Verify with Solidity CLI": same command, own toolset. Two entries
# doing the same thing in the context menu are worse than one, so retire it.
drop_legacy_tool() {
    local legacy="${1%/}/tools/$LEGACY_FILE"
    [ -f "$legacy" ] || return 0
    grep -q "name=\"$LEGACY_TOOL\"" "$legacy" || return 0
    if [ "$(grep -c "<tool " "$legacy")" -gt 1 ]; then
        echo "note: '$LEGACY_TOOL' in $legacy is now a duplicate of '$TOOLSET_NAME -> Verify with"
        echo "      Solidity CLI', but the file holds other tools — remove it in"
        echo "      Settings -> Tools -> External Tools"
        return 0
    fi
    if $dry_run; then
        echo "would remove $legacy (obsoleted by $TOOLSET_NAME)"
    else
        rm "$legacy"
        echo "removed $legacy (obsoleted by $TOOLSET_NAME)"
    fi
}

for dir in "${ide_dirs[@]}"; do
    target="${dir%/}/tools/$TOOLSET_NAME.xml"
    if $dry_run; then
        echo "would write $target"
    else
        mkdir -p "${dir%/}/tools"
        printf '%s\n' "$toolset" > "$target"
        echo "wrote $target"
    fi
    drop_legacy_tool "$dir"
done

if ! $dry_run; then
    echo
    if pgrep -x idea > /dev/null 2>&1 || pgrep -x fsnotifier > /dev/null 2>&1; then
        echo "WARNING: an IDE is running. It holds its own copy of the tools and writes it back on"
        echo "         exit, which can undo what was just done here. Quit the IDE and re-run."
        echo
    fi
    echo "Restart the IDE (the tools are read at startup), then right-click a .sol file:"
    echo "  External Tools -> $TOOLSET_NAME -> Verify in KeYther (GUI)"
fi
