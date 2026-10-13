// SPDX-License-Identifier: GPL-2.0-only
pragma solidity ^0.8.0;

contract FixedLengthsOpen {
    uint[3][] dyn;

    // open: reference-element push changes only the length; wellformed gives no zero tail values
    function emptyPushFixedElementReadsZero() public {
        uint n = dyn.length;
        dyn.push();
        assert(dyn.length == n + 1);
        assert(dyn[n].length == 3);
        assert(dyn[n][0] == 0);
        assert(dyn[n][2] == 0);
    }

    // open: pop refuses arrays containing fixed-size elements
    function popPushResetsFixedElement() public {
        uint n = dyn.length;
        dyn.push();
        dyn[n][0] = 11;
        dyn[n][2] = 22;
        dyn.pop();
        assert(dyn.length == n);
        dyn.push();
        assert(dyn[n].length == 3);
        assert(dyn[n][0] == 0);
        assert(dyn[n][2] == 0);
    }

    // open: delete refuses fixed-size array elements
    function deleteFixedElementKeepsLength() public {
        uint n = dyn.length;
        dyn.push();
        dyn[n][1] = 17;
        delete dyn[n];
        assert(dyn.length == n + 1);
        assert(dyn[n].length == 3);
        assert(dyn[n][1] == 0);
    }

    // open: delete refuses arrays containing fixed-size elements
    function deleteDynamicThenPushFixedElement() public {
        dyn.push();
        dyn[0][2] = 23;
        delete dyn;
        assert(dyn.length == 0);
        dyn.push();
        assert(dyn.length == 1);
        assert(dyn[0].length == 3);
        assert(dyn[0][2] == 0);
    }

    // open: delete refuses fixed-size array elements even through an alias
    function fixedElementStorageAlias() public {
        uint n = dyn.length;
        dyn.push();
        uint[3] storage row = dyn[n];
        assert(row.length == 3);
        row[2] = 31;
        assert(dyn[n][2] == 31);
        delete dyn[n];
        assert(row.length == 3);
        assert(row[2] == 0);
    }

}
