// SPDX-License-Identifier: GPL-2.0-only
pragma solidity ^0.8.0;

// The ../solidikey/EntryPoints.sol example with its specification written as require and
// assert: the invariant limit == 10 && count <= limit is required at entry and asserted at
// exit of every entry point, and asserted at the end of the constructor.
contract EntryPoints {
    uint limit = 10;
    uint count;

    /// @custom:key box
    constructor() {
        assert(limit == 10 && count <= limit);
    }

    /// @custom:key box
    receive() external payable {
        require(limit == 10 && count <= limit);

        if (count < limit) {
            count = count + 1;
        }

        assert(limit == 10 && count <= limit);
    }

    /// @custom:key box
    fallback() external {
        require(limit == 10 && count <= limit);

        assert(count <= limit);

        assert(limit == 10 && count <= limit);
    }

    /// @custom:key box
    function offset(uint result) public view returns (uint r) {
        require(limit == 10 && count <= limit);

        r = result + count;

        uint expected = result + count;
        assert(r == expected);
        assert(limit == 10 && count <= limit);
    }

    /// @custom:key box
    function reset(uint old) public {
        require(limit == 10 && count <= limit);
        uint oldCount = count;

        if (old <= limit) {
            count = old;
        }

        assert(count == oldCount || count == old);
        assert(limit == 10 && count <= limit);
    }
}
