// SPDX-License-Identifier: MIT
pragma solidity ^0.8.26;

// Source: https://github.com/Cyfrin/solidity-by-example.github.io/blob/5bcdca0239409d7336a07b66a6fca8d0bcc710e6/contracts/src/array/ArrayRemoveByShifting.sol
// The real-world/ArrayRemoveByShifting.sol port with its specification written as require and
// assert. The loop keeps its invariant; the full specification of remove is in
// open/unprovable/ArrayRemoveByShifting.sol.
contract ArrayRemoveByShifting {
    uint256[] public arr;

    /// @custom:key box
    function remove(uint256 _index) public {
        require(_index >= 0);
        uint256 oldLength = arr.length;

        require(_index < arr.length);

        /// @custom:key invariant i + 1 <= arr.length
        for (uint256 i = _index; i < arr.length - 1; i++) {
            arr[i] = arr[i + 1];
        }
        arr.pop();

        assert(_index < oldLength);
    }

    function test() external {
        delete arr;
        arr.push(1);
        arr.push(2);
        arr.push(3);
        arr.push(4);
        arr.push(5);
        remove(2);
        assert(arr[0] == 1);
        assert(arr[1] == 2);
        assert(arr[2] == 4);
        assert(arr[3] == 5);
        assert(arr.length == 4);

        delete arr;
        arr.push(1);
        remove(0);
        assert(arr.length == 0);
    }
}
