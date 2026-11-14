// SPDX-License-Identifier: GPL-2.0-only
pragma solidity ^0.8.0;

/// Ports of solc's struct and mapping tests (see `../SolcStructsMappings.sol`) that load but
/// do not close.
contract SolcStructsMappings {
    struct S8 { uint8 x; }

    S8 s8;
    mapping(uint8 => S8[2]) m1s;
    mapping(uint8 => S8[]) m2s;

    /// solc: semanticTests/types/mapping/copy_struct_to_array_stored_in_mapping.sol
    /// @custom:key box
    function structFromCalldataToStaticArrayInMapping(S8 calldata sCalldata) public {
        // open: unsupported: a public function's struct parameter (S8 calldata) has no .key sort, so no obligation is generated
        require(m1s[0][0].x == 0);
        require(sCalldata.x == 8);
        s8.x = 7;
        m1s[0][1] = sCalldata;
        S8[2] memory r = m1s[0];
        assert(r[0].x == 0);
        assert(r[1].x == 8);
    }

    /// solc: semanticTests/types/mapping/copy_struct_to_array_stored_in_mapping.sol
    /// @custom:key box
    function structFromCalldataToDynamicArrayInMapping(S8 calldata sCalldata) public {
        // open: unsupported: a public function's struct parameter (S8 calldata) has no .key sort, so no obligation is generated
        require(m2s[0].length == 0);
        require(sCalldata.x == 8);
        s8.x = 7;
        m2s[0].push();
        m2s[0][0] = sCalldata;
        S8[] memory r = m2s[0];
        assert(r.length == 1);
        assert(r[0].x == 8);
    }
}
