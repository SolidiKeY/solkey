// SPDX-License-Identifier: MIT
pragma solidity ^0.8.26;

// Source: https://github.com/Cyfrin/solidity-by-example.github.io/blob/5bcdca0239409d7336a07b66a6fca8d0bcc710e6/contracts/src/first-app/Counter.sol
// The real-world/Counter.sol port with its specification written as require and assert.
contract Counter {
    uint256 public count;

    /// @custom:key box
    function get() public view returns (uint256 c) {
        require(count >= 0);
        uint256 oldCount = count;

        c = count;

        assert(c == count && count == oldCount);
        assert(count >= 0);
    }

    /// @custom:key box
    function inc() public {
        require(count >= 0);
        uint256 oldCount = count;

        count += 1;

        assert(count == oldCount + 1);
        assert(count >= 0);
    }

    /// @custom:key box
    function dec() public {
        require(count >= 0);
        require(count >= 1);
        uint256 oldCount = count;

        count -= 1;

        assert(count == oldCount - 1);
        assert(count >= 0);
    }
}
