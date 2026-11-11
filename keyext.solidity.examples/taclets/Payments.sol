// SPDX-License-Identifier: GPL-2.0-only
pragma solidity ^0.8.0;

// Taclet examples: `transfer`, `send`, low-level calls and `try`.
// Conventions: README.md, "Writing an example".
contract Payments {
    uint total;
    address owner;
    bool flag;

    // ── Taclet coverage: one function per taclet no other example applies ──

    /// @custom:key box
    function transferToOwner() public {
        address payable to = payable(owner);
        to.transfer(5);
    }

    /// @custom:key box
    function transferUnfoldReceiver() public {
        payable(owner).transfer(5);
    }

    /// @custom:key box
    function transferUnfoldArgument() public {
        address payable to = payable(owner);
        uint x = 3;
        to.transfer(x + 2);
    }

    /// @custom:key box
    function sendToOwner() public {
        address payable to = payable(owner);
        bool ok = to.send(5);
        flag = ok;
    }

    /// @custom:key box
    function sendUnfoldReceiver() public {
        bool ok = payable(owner).send(5);
        flag = ok;
    }

    /// @custom:key box
    function sendUnfoldArgument() public {
        address payable to = payable(owner);
        uint x = 3;
        bool ok = to.send(x + 2);
        flag = ok;
    }

    /// @custom:key box
    function callToSender() public {
        uint amount = 5;
        (bool success, ) = payable(msg.sender).call{value: amount}("");
        flag = success;
    }

    /// @custom:key skip
    function tryCalleePing() external pure {
    }

    /// @custom:key box
    function tryCallCatchKeepsState() public {
        total = 1;
        try Payments(owner).tryCalleePing() {
            total = 2;
        } catch {
            assert(total == 1);
        }
        assert(total == 1 || total == 2);
    }

    /// @custom:key skip
    function tryCalleeGet(uint x) external pure returns (uint) {
        return x;
    }

    /// @custom:key box
    function tryCallBindsReturnAndPanicCode() public view {
        uint r = 0;
        try Payments(owner).tryCalleeGet(5) returns (uint v) {
            r = v + 1;
            assert(r == v + 1);
        } catch Error(string memory) {
            r = 2;
            assert(r == 2);
        } catch Panic(uint code) {
            r = code + 3;
            assert(r == code + 3);
        }
    }

    /// @custom:key box
    function tryCallUnmatchedFailureReverts() public {
        total = 3;
        try Payments(owner).tryCalleePing() {
            total = 4;
        } catch Error(string memory) {
            total = 5;
        }
        assert(total == 4 || total == 5);
    }
}
