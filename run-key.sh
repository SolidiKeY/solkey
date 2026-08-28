#!/usr/bin/env bash
# Run a .key problem or a .sol contract with the Solidity CLI
# Usage: ./run-key.sh path/to/file.key
#        ./run-key.sh path/to/Contract.sol [function]

if [ -z "$1" ]; then
    echo "Usage: $0 <file.key|file.sol> [function]"
    echo "Example: $0 keyext.solidity.examples/TestSuite.sol testSimpleAssert"
    exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# the CLI runs from the examples directory, so a relative path has to be made absolute here
FILE="$(cd "$(dirname "$1")" && pwd)/$(basename "$1")"

cd "$SCRIPT_DIR"
if [ -n "$2" ]; then
    ./gradlew :keyext.solidity.core:solidityCli -PkeyFile="$FILE" -Pfunction="$2"
else
    ./gradlew :keyext.solidity.core:solidityCli -PkeyFile="$FILE"
fi
