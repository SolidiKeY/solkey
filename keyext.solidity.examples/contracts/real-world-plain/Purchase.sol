// SPDX-License-Identifier: GPL-3.0
pragma solidity ^0.8.4;

// Source: https://docs.soliditylang.org/en/latest/solidity-by-example.html#safe-remote-purchase
// The real-world/Purchase.sol port with its @custom:key specification removed.
contract Purchase {
    uint public value;
    address payable public seller;
    address payable public buyer;

    enum State { Created, Locked, Release, Inactive }
    State public state;

    constructor() payable {
        seller = payable(msg.sender);
        value = msg.value / 2;
        uint doubled = 2 * value;
        if (doubled != msg.value) revert();
    }

    function abort() external {
        address s = seller;
        if (msg.sender != s) revert();
        if (state != State.Created) revert();
        state = State.Inactive;
        uint deposit = 2 * value;
        seller.transfer(deposit);
    }

    function confirmPurchase() external payable {
        if (state != State.Created) revert();
        uint price = 2 * value;
        require(msg.value == price);
        buyer = payable(msg.sender);
        state = State.Locked;
    }

    function confirmReceived() external {
        address b = buyer;
        if (msg.sender != b) revert();
        if (state != State.Locked) revert();
        state = State.Release;
        buyer.transfer(value);
    }

    function refundSeller() external {
        address s = seller;
        if (msg.sender != s) revert();
        if (state != State.Release) revert();
        state = State.Inactive;
        uint refund = 3 * value;
        seller.transfer(refund);
    }
}
