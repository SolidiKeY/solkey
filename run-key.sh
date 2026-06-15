#!/bin/bash
# Run a .key file with the Solidity CLI
# Usage: ./run-key.sh path/to/file.key

if [ -z "$1" ]; then
    echo "Usage: $0 <keyfile>"
    echo "Example: $0 keyext.solidity.core/src/test/resources/org/key_project/solidity/examples/simpleExample1.key"
    exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FILENAME="$(basename "$1")"

cd "$SCRIPT_DIR"
./gradlew :keyext.solidity.core:solidityCli -PkeyFile="$FILENAME"
