// SPDX-License-Identifier: GPL-2.0-only
pragma solidity ^0.8.0;

// From semantics/SolcArrays.sol. Each claim holds on a chain whose storage beyond every array length
// is zero, and stays open in SolKey, which assumes no such clean tail (README.md).
contract SolcArraysTail {
    uint[] storageArray;

    /// solc: array/array_storage_index_zeroed_test.sol — the direct form of the same
    /// invariant: a freshly pushed slot reads as zero.
    /// @custom:key box
    function pushedSlotIsZeroed() public {
        require(storageArray.length == 0);
        storageArray.push();
        assert(storageArray[0] == 0);
    }
}
