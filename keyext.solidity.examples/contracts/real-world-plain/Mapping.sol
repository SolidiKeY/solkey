// SPDX-License-Identifier: MIT
pragma solidity ^0.8.26;

// Source: https://github.com/Cyfrin/solidity-by-example.github.io/blob/5bcdca0239409d7336a07b66a6fca8d0bcc710e6/contracts/src/mapping/Mapping.sol
// The real-world/Mapping.sol port with its specification written as require and assert.
contract Mapping {
    mapping(address => uint256) public myMap;

    /// @custom:key box
    function get(address _addr, address a) public view returns (uint256 v) {
        require(myMap[a] >= 0);

        v = myMap[_addr];

        assert(v == myMap[_addr]);
        assert(myMap[a] >= 0);
    }

    /// @custom:key box
    function set(address _addr, uint256 _i, address a) public {
        require(myMap[a] >= 0);
        require(_i >= 0);
        uint256 oldA = myMap[a];

        myMap[_addr] = _i;

        assert(myMap[_addr] == _i);
        assert(a == _addr || myMap[a] == oldA);
        assert(myMap[a] >= 0);
    }

    /// @custom:key box
    function remove(address _addr, address a) public {
        require(myMap[a] >= 0);
        uint256 oldA = myMap[a];

        delete myMap[_addr];

        assert(myMap[_addr] == 0);
        assert(a == _addr || myMap[a] == oldA);
        assert(myMap[a] >= 0);
    }
}
