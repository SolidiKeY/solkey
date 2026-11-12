// SPDX-License-Identifier: MIT
pragma solidity ^0.8.26;

// Source: https://github.com/Cyfrin/solidity-by-example.github.io/blob/5bcdca0239409d7336a07b66a6fca8d0bcc710e6/contracts/src/enum/Enum.sol
// The real-world/Enum.sol port with its specification written as require and assert.
contract Enum {
    enum Status {
        Pending,
        Shipped,
        Accepted,
        Rejected,
        Canceled
    }

    Status public status;

    /// @custom:key box
    function get() public view returns (Status s) {
        Status oldStatus = status;

        s = status;

        assert(s == status && status == oldStatus);
    }

    /// @custom:key box
    function set(Status _status) public {
        status = _status;

        assert(status == _status);
    }

    /// @custom:key box
    function cancel() public {
        status = Status.Canceled;

        assert(status == Status.Canceled);
    }

    /// @custom:key box
    function reset() public {
        delete status;

        assert(status == Status.Pending);
    }
}
