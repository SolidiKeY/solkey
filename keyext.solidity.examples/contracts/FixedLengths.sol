// SPDX-License-Identifier: GPL-2.0-only
pragma solidity ^0.8.0;

/// The running contract of the storage-shape design (`docs/storage.md`, section 8c): a
/// fixed-size array has no length cell, so its length comes from the shaped empty storage in
/// the constructor and from `wellformed(storage)` in a function.
contract FixedLengths {
    uint[9] d1;
    uint[] d2;
    uint[3][] dyn;

    constructor() {
        d2 = d1;
        assert(d2.length == 9);
        delete d1;
        assert(d1.length == 9);
        dyn.push();
        assert(dyn[dyn.length - 1].length == 3);
    }

    function f() public {
        d2 = d1;
        assert(d2.length == 9);
    }

    function g() public {
        assert(d1.length == 9);
        delete d1;
        assert(d1.length == 9);
        dyn.push();
        assert(dyn[dyn.length - 1].length == 3);
    }
}
