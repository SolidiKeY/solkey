// SPDX-License-Identifier: MIT
pragma solidity ^0.8.26;

// Source: https://github.com/Cyfrin/solidity-by-example.github.io/blob/5bcdca0239409d7336a07b66a6fca8d0bcc710e6/contracts/src/mapping/Mapping.sol
// The real-world/NestedMapping.sol port with its specification written as require and assert.
contract NestedMapping {
    mapping(address => mapping(uint256 => bool)) public nested;

    /// @custom:key box
    function get(address _addr1, uint256 _i) public view returns (bool v) {
        v = nested[_addr1][_i];

        assert(v == nested[_addr1][_i]);
    }

    /// @custom:key box
    function set(address _addr1, uint256 _i, bool _boo, address a, uint256 j) public {
        bool oldAJ = nested[a][j];

        nested[_addr1][_i] = _boo;

        assert(nested[_addr1][_i] == _boo);
        assert((a == _addr1 && j == _i) || nested[a][j] == oldAJ);
    }

    /// @custom:key box
    function remove(address _addr1, uint256 _i, address a, uint256 j) public {
        bool oldAJ = nested[a][j];

        delete nested[_addr1][_i];

        assert(!nested[_addr1][_i]);
        assert((a == _addr1 && j == _i) || nested[a][j] == oldAJ);
    }
}
