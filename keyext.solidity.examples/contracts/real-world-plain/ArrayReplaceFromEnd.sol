// SPDX-License-Identifier: MIT
pragma solidity ^0.8.26;

// Source: https://github.com/Cyfrin/solidity-by-example.github.io/blob/5bcdca0239409d7336a07b66a6fca8d0bcc710e6/contracts/src/array/ArrayReplaceFromEnd.sol
// The real-world/ArrayReplaceFromEnd.sol port with its specification written as require and assert.
contract ArrayReplaceFromEnd {
    uint256[] public arr;

    /// @custom:key box
    function remove(uint256 index, uint256 i) public {
        uint256 oldLength = arr.length;
        uint256 oldLast = arr[arr.length - 1];
        uint256 oldI = i < arr.length ? arr[i] : 0;

        arr[index] = arr[arr.length - 1];
        arr.pop();

        assert(arr.length == oldLength - 1);
        assert(index >= arr.length || arr[index] == oldLast);
        assert(i >= arr.length || i == index || arr[i] == oldI);
    }

    function test() public {
        delete arr;
        arr.push(1);
        arr.push(2);
        arr.push(3);
        arr.push(4);

        remove(1, 0);
        assert(arr.length == 3);
        assert(arr[0] == 1);
        assert(arr[1] == 4);
        assert(arr[2] == 3);

        remove(2, 0);
        assert(arr.length == 2);
        assert(arr[0] == 1);
        assert(arr[1] == 4);
    }
}
