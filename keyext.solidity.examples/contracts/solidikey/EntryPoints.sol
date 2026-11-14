// SPDX-License-Identifier: GPL-2.0-only
pragma solidity ^0.8.0;

/// @custom:key invariant limit == 10 && count <= limit
contract EntryPoints {
    uint limit = 10;
    uint count;

    receive() external payable {
        if (count < limit) {
            count = count + 1;
        }
    }

    fallback() external {
        assert(count <= limit);
    }

    /// @custom:key ensures r == result + count
    function offset(uint result) public view returns (uint r) {
        r = result + count;
    }

    /// @custom:key ensures count == \old(count) || count == old
    function reset(uint old) public {
        if (old <= limit) {
            count = old;
        }
    }
}
