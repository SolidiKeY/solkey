// SPDX-License-Identifier: MIT
pragma solidity ^0.8.26;

// Source: https://github.com/Cyfrin/solidity-by-example.github.io/blob/5bcdca0239409d7336a07b66a6fca8d0bcc710e6/contracts/src/app/iterable-mapping/IterableMapping.sol
// The real-world/IterableMapping.sol port with its specification written as require and assert.
// removeFromMapping's early return becomes an if around the removal, so the asserts run before
// the function ends.
contract IterableMapping {
    struct Map {
        address[] keys;
        mapping(address => uint256) values;
        mapping(address => uint256) indexOf;
        mapping(address => bool) inserted;
    }

    Map private map;

    /// @custom:key box
    function setInMapping(uint256 val, address a, uint256 i) public {
        require(map.values[a] >= 0 && (map.inserted[a] || map.values[a] == 0)
            && (!map.inserted[a] || (map.indexOf[a] >= 0 && map.indexOf[a] < map.keys.length && map.keys[map.indexOf[a]] == a)));
        require(map.values[msg.sender] >= 0 && (map.inserted[msg.sender] || map.values[msg.sender] == 0)
            && (!map.inserted[msg.sender] || (map.indexOf[msg.sender] >= 0 && map.indexOf[msg.sender] < map.keys.length && map.keys[map.indexOf[msg.sender]] == msg.sender)));
        require(i >= map.keys.length || (map.inserted[map.keys[i]] && map.indexOf[map.keys[i]] == i));
        require(val >= 0);
        bool oldInserted = map.inserted[msg.sender];
        uint256 oldLength = map.keys.length;

        address key = msg.sender;
        if (map.inserted[key]) {
            map.values[key] = val;
        } else {
            map.inserted[key] = true;
            map.values[key] = val;
            map.indexOf[key] = map.keys.length;
            map.keys.push(key);
        }

        assert(map.values[msg.sender] == val && map.inserted[msg.sender]);
        assert(!oldInserted || map.keys.length == oldLength);
        assert(oldInserted || (map.keys.length == oldLength + 1 && map.keys[oldLength] == msg.sender && map.indexOf[msg.sender] == oldLength));
        assert(map.values[a] >= 0 && (map.inserted[a] || map.values[a] == 0)
            && (!map.inserted[a] || (map.indexOf[a] >= 0 && map.indexOf[a] < map.keys.length && map.keys[map.indexOf[a]] == a)));
        assert(i >= map.keys.length || (map.inserted[map.keys[i]] && map.indexOf[map.keys[i]] == i));
    }

    /// @custom:key box
    function getFromMap(address a, uint256 i) public view returns (uint256 v) {
        require(map.values[a] >= 0 && (map.inserted[a] || map.values[a] == 0)
            && (!map.inserted[a] || (map.indexOf[a] >= 0 && map.indexOf[a] < map.keys.length && map.keys[map.indexOf[a]] == a)));
        require(i >= map.keys.length || (map.inserted[map.keys[i]] && map.indexOf[map.keys[i]] == i));

        v = map.values[msg.sender];

        assert(v == map.values[msg.sender]);
        assert(map.values[a] >= 0 && (map.inserted[a] || map.values[a] == 0)
            && (!map.inserted[a] || (map.indexOf[a] >= 0 && map.indexOf[a] < map.keys.length && map.keys[map.indexOf[a]] == a)));
        assert(i >= map.keys.length || (map.inserted[map.keys[i]] && map.indexOf[map.keys[i]] == i));
    }

    /// @custom:key box
    function getKeyAtIndex(uint256 index, address a, uint256 i) public view returns (address k) {
        require(map.values[a] >= 0 && (map.inserted[a] || map.values[a] == 0)
            && (!map.inserted[a] || (map.indexOf[a] >= 0 && map.indexOf[a] < map.keys.length && map.keys[map.indexOf[a]] == a)));
        require(i >= map.keys.length || (map.inserted[map.keys[i]] && map.indexOf[map.keys[i]] == i));

        k = map.keys[index];

        assert(k == map.keys[index]);
        assert(map.values[a] >= 0 && (map.inserted[a] || map.values[a] == 0)
            && (!map.inserted[a] || (map.indexOf[a] >= 0 && map.indexOf[a] < map.keys.length && map.keys[map.indexOf[a]] == a)));
        assert(i >= map.keys.length || (map.inserted[map.keys[i]] && map.indexOf[map.keys[i]] == i));
    }

    /// @custom:key box
    function sizeOfMapping(address a, uint256 i) public view returns (uint256 s) {
        require(map.values[a] >= 0 && (map.inserted[a] || map.values[a] == 0)
            && (!map.inserted[a] || (map.indexOf[a] >= 0 && map.indexOf[a] < map.keys.length && map.keys[map.indexOf[a]] == a)));
        require(i >= map.keys.length || (map.inserted[map.keys[i]] && map.indexOf[map.keys[i]] == i));

        s = map.keys.length;

        assert(s == map.keys.length);
        assert(map.values[a] >= 0 && (map.inserted[a] || map.values[a] == 0)
            && (!map.inserted[a] || (map.indexOf[a] >= 0 && map.indexOf[a] < map.keys.length && map.keys[map.indexOf[a]] == a)));
        assert(i >= map.keys.length || (map.inserted[map.keys[i]] && map.indexOf[map.keys[i]] == i));
    }

    /// @custom:key box
    function removeFromMapping(address a, uint256 i) public {
        address last = map.keys.length == 0 ? address(0) : map.keys[map.keys.length - 1];
        require(map.values[a] >= 0 && (map.inserted[a] || map.values[a] == 0)
            && (!map.inserted[a] || (map.indexOf[a] >= 0 && map.indexOf[a] < map.keys.length && map.keys[map.indexOf[a]] == a)));
        require(map.values[msg.sender] >= 0 && (map.inserted[msg.sender] || map.values[msg.sender] == 0)
            && (!map.inserted[msg.sender] || (map.indexOf[msg.sender] >= 0 && map.indexOf[msg.sender] < map.keys.length && map.keys[map.indexOf[msg.sender]] == msg.sender)));
        require(map.values[last] >= 0 && (map.inserted[last] || map.values[last] == 0)
            && (!map.inserted[last] || (map.indexOf[last] >= 0 && map.indexOf[last] < map.keys.length && map.keys[map.indexOf[last]] == last)));
        require(i >= map.keys.length || (map.inserted[map.keys[i]] && map.indexOf[map.keys[i]] == i));
        require(map.keys.length == 0 || (map.inserted[map.keys[map.keys.length - 1]] && map.indexOf[map.keys[map.keys.length - 1]] == map.keys.length - 1));
        bool oldInserted = map.inserted[msg.sender];
        uint256 oldLength = map.keys.length;

        address key = msg.sender;
        if (map.inserted[key]) {
            delete map.inserted[key];
            delete map.values[key];

            uint256 index = map.indexOf[key];
            address lastKey = map.keys[map.keys.length - 1];

            map.indexOf[lastKey] = index;
            delete map.indexOf[key];

            map.keys[index] = lastKey;
            map.keys.pop();
        }

        assert(!map.inserted[msg.sender] && map.values[msg.sender] == 0);
        assert(!oldInserted || map.keys.length == oldLength - 1);
        assert(oldInserted || map.keys.length == oldLength);
        assert(map.values[a] >= 0 && (map.inserted[a] || map.values[a] == 0)
            && (!map.inserted[a] || (map.indexOf[a] >= 0 && map.indexOf[a] < map.keys.length && map.keys[map.indexOf[a]] == a)));
        assert(i >= map.keys.length || (map.inserted[map.keys[i]] && map.indexOf[map.keys[i]] == i));
    }
}
