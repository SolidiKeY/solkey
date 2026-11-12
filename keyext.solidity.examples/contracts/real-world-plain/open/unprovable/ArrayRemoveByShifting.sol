// SPDX-License-Identifier: MIT
pragma solidity ^0.8.26;

// Source: https://github.com/Cyfrin/solidity-by-example.github.io/blob/5bcdca0239409d7336a07b66a6fca8d0bcc710e6/contracts/src/array/ArrayRemoveByShifting.sol
// The real-world/open/ArrayRemoveByShifting.sol port with its specification written as require and
// assert. Open, as in real-world/open/: the loop invariant cannot state the shifted prefix.
contract ArrayRemoveByShifting {
    uint256[] public arr;

    /// @custom:key box
    function remove(uint256 _index, uint256 j) public {
        require(_index >= 0);
        uint256 oldLength = arr.length;
        uint256 oldJ = j < arr.length ? arr[j] : 0;
        uint256 oldNext = j + 1 < arr.length ? arr[j + 1] : 0;

        require(_index < arr.length);

        /// @custom:key invariant i + 1 <= arr.length
        for (uint256 i = _index; i < arr.length - 1; i++) {
            arr[i] = arr[i + 1];
        }
        arr.pop();

        assert(arr.length == oldLength - 1);
        assert(j >= _index || j >= arr.length || arr[j] == oldJ);
        assert(j < _index || j >= arr.length || arr[j] == oldNext);
    }
}
