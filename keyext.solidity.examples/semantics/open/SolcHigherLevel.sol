// SPDX-License-Identifier: GPL-2.0-only
pragma solidity ^0.8.0;

contract SolcHigherLevelBaseBase {
    constructor() {}

    uint x;
    uint256 m_base = 5;

    function bg() internal pure virtual returns (uint256 r) {
        return 1;
    }

    function init(uint a, uint b) internal virtual {
        x = a;
    }

    function nf(uint256 a, uint256 b, uint256 c) internal pure virtual returns (uint256) {
        return 1 * a + 2 * b + 3 * c;
    }

    function vf() internal pure returns (uint256 i) {
        return vg();
    }

    function vg() internal pure virtual returns (uint256 i) {
        return 1;
    }

    function ivf() internal pure returns (uint256 i) {
        return ivg();
    }

    function ivg() internal pure virtual returns (uint256 i) {
        return 1;
    }
}

contract SolcHigherLevelBase is SolcHigherLevelBaseBase {
    function bg() internal pure virtual override returns (uint256 r) {
        return 2;
    }

    function init(uint a, uint b) internal override {
    }
}

contract SolcHigherLevelDiamondA {
    uint public i;
    uint public k;

    constructor(uint newI, uint newK) {
        i = newI;
        k = newK;
    }
}

abstract contract SolcHigherLevelDiamondB is SolcHigherLevelDiamondA {
    uint public j;

    constructor(uint newJ) {
        j = newJ;
    }
}

contract SolcHigherLevelDiamondC is SolcHigherLevelDiamondA {
    constructor(uint newI, uint newK) SolcHigherLevelDiamondA(newI, newK) {}
}

contract SolcHigherLevelParams {
    uint public pi;
    uint public pk;

    constructor(uint newI, uint newK) {
        pi = newI;
        pk = newK;
    }
}

contract SolcHigherLevelFixedParams {
    uint public qi;
    uint public qk;

    constructor(uint newI, uint newK) {
        qi = newI;
        qk = newK;
    }
}

contract SolcHigherLevelModifiedCtor {
    uint256 data;

    constructor() ctorMod {
        data |= 2;
    }

    modifier ctorMod virtual {
        data |= 1;
        _;
    }
}

contract SolcHigherLevelCreationContext {
    uint256 creationData;

    constructor() creationMod1 {
        creationF1();
    }

    function creationF1() public creationMod2 {
        creationData |= 0x1;
    }

    function creationF2() public {
        creationData |= 0x20;
    }

    function creationF3() public virtual {}

    modifier creationMod1 virtual {
        creationF2();
        _;
    }

    modifier creationMod2 {
        creationF3();
        if (false) _;
    }
}

contract SolcHigherLevel is SolcHigherLevelBase, SolcHigherLevelDiamondB, SolcHigherLevelDiamondC,
        SolcHigherLevelParams, SolcHigherLevelFixedParams(2, 1), SolcHigherLevelModifiedCtor,
        SolcHigherLevelCreationContext {
    uint256 m_derived = 6;

    /// solc: semanticTests/various/inline_member_init_inheritence.sol, semanticTests/inheritance/constructor_with_params_diamond_inheritance.sol, semanticTests/inheritance/constructor_with_params_inheritance.sol, semanticTests/inheritance/constructor_with_params_inheritance_2.sol, semanticTests/modifiers/function_modifier_for_constructor.sol, semanticTests/modifiers/function_modifier_calling_functions_in_creation_context.sol
    // open: inheritance: a derived constructor runs neither base initializers nor base constructors, and the base-constructor calls in its header are stuck
    constructor(uint newI, uint newK) SolcHigherLevelDiamondB(newI) SolcHigherLevelDiamondC(newI, newK + 1)
            SolcHigherLevelParams(newI, newK + 1) {
        uint256 rb = m_base;
        uint256 rd = m_derived;
        assert(rb == 5);
        assert(rd == 6);
        uint ri = i;
        uint rj = j;
        uint rk = k;
        assert(ri == newI);
        assert(rj == newI);
        assert(rk == newK + 1);
        uint rpi = pi;
        uint rpk = pk;
        assert(rpi == newI);
        assert(rpk == newK + 1);
        uint rqi = qi;
        uint rqk = qk;
        assert(rqi == 2);
        assert(rqk == 1);
        uint256 rdata = data;
        assert(rdata == 6);
        uint256 rcreation = creationData;
        assert(rcreation == 0x4300);
    }

    modifier ctorMod override {
        data |= 4;
        _;
    }

    modifier creationMod1 override {
        creationF4();
        _;
    }

    /// solc: semanticTests/modifiers/function_modifier_calling_functions_in_creation_context.sol
    // open: no rule for the compound assignment |=
    function creationF3() public override {
        creationData |= 0x300;
    }

    /// solc: semanticTests/modifiers/function_modifier_calling_functions_in_creation_context.sol
    // open: no rule for the compound assignment |=
    function creationF4() public {
        creationData |= 0x4000;
    }

    function bg() internal pure override returns (uint256 r) {
        return 3;
    }

    function nf(uint256 b, uint256 a, uint256 c) internal pure override returns (uint256) {
        return 2 * b + 3 * a + 4 * c;
    }

    function vg() internal pure override returns (uint256 i) {
        return 2;
    }

    function ivg() internal pure override returns (uint256 i) {
        return 2;
    }

    /// solc: semanticTests/inheritance/explicit_base_class.sol
    // open: a base-qualified call BaseBase.g() is lowered to address.g() and has no rule
    function explicitBaseClass() public pure {
        uint256 g = bg();
        uint256 f = SolcHigherLevelBaseBase.bg();
        assert(g == 3);
        assert(f == 1);
    }

    /// solc: semanticTests/inheritance/inherited_function.sol
    // open: a base-qualified call A.f() is lowered to address.f() and has no rule
    function inheritedFunction() public pure {
        uint256 r = SolcHigherLevelBase.bg();
        assert(r == 2);
    }

    /// solc: semanticTests/inheritance/inherited_function_named_parameters.sol
    // open: a base-qualified call A.f(...) is lowered to address.f(...) and has no rule
    function inheritedNamedBaseOrdered() public pure {
        uint256 r = SolcHigherLevelBaseBase.nf({a: 1, b: 2, c: 3});
        assert(r == 14);
    }

    /// solc: smtCheckerTests/inheritance/overridden_function_static_call_parent.sol
    // open: a base-qualified call BaseBase.init(c, d) is lowered to address.init(c, d) and has no rule
    /// @custom:key box
    function overriddenFunctionStaticCallParent(uint c, uint d) public {
        require(c == 1 && d == 0);
        SolcHigherLevelBaseBase.init(c, d);
        uint r = x;
        assert(r == c);
    }
}
