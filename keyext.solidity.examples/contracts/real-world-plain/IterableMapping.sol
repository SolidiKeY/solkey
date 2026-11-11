// SPDX-License-Identifier: MIT
pragma solidity ^0.8.26;

// Source: https://github.com/Cyfrin/solidity-by-example.github.io/blob/5bcdca0239409d7336a07b66a6fca8d0bcc710e6/contracts/src/app/iterable-mapping/IterableMapping.sol
// The real-world/IterableMapping.sol port with its @custom:key specification removed.
contract IterableMapping {
    struct Map {
        address[] keys;
        mapping(address => uint256) values;
        mapping(address => uint256) indexOf;
        mapping(address => bool) inserted;
    }

    Map private map;

    function setInMapping(uint256 val) public {
        address key = msg.sender;
        if (map.inserted[key]) {
            map.values[key] = val;
        } else {
            map.inserted[key] = true;
            map.values[key] = val;
            map.indexOf[key] = map.keys.length;
            map.keys.push(key);
        }
    }

    function getFromMap() public view returns (uint256 v) {
        return map.values[msg.sender];
    }

    function getKeyAtIndex(uint256 index) public view returns (address k) {
        return map.keys[index];
    }

    function sizeOfMapping() public view returns (uint256 s) {
        return map.keys.length;
    }

    function removeFromMapping() public {
        address key = msg.sender;
        if (!map.inserted[key]) {
            return;
        }

        delete map.inserted[key];
        delete map.values[key];

        uint256 index = map.indexOf[key];
        address lastKey = map.keys[map.keys.length - 1];

        map.indexOf[lastKey] = index;
        delete map.indexOf[key];

        map.keys[index] = lastKey;
        map.keys.pop();
    }
}
