// SPDX-License-Identifier: GPL-3.0
pragma solidity ^0.8.4;

// Source: https://docs.soliditylang.org/en/latest/solidity-by-example.html#safe-remote-purchase
// The real-world/Purchase.sol port with its specification written as require and assert. Each
// clause with no Solidity form stays a @custom:key clause.
/// @custom:key invariant state == State.Created -> net(seller) == 2 * value && net(buyer) == 0
/// @custom:key invariant state == State.Locked -> net(seller) == 2 * value && net(buyer) == 2 * value
/// @custom:key invariant state == State.Release -> net(seller) == 2 * value && net(buyer) == value
/// @custom:key invariant state == State.Inactive -> net(seller) + net(buyer) == 0
contract Purchase {
    uint public value;
    address payable public seller;
    address payable public buyer;

    enum State { Created, Locked, Release, Inactive }
    State public state;

    /// @custom:key skip
    constructor() payable {
        seller = payable(msg.sender);
        value = msg.value / 2;
        uint doubled = 2 * value;
        if (doubled != msg.value) revert();
    }

    /// @custom:key ensures net(seller) == 0
    /// @custom:key box
    function abort() external {
        require(value >= 0 && seller != buyer);
        require(msg.sender == seller && state == State.Created);

        address s = seller;
        if (msg.sender != s) revert();
        if (state != State.Created) revert();
        state = State.Inactive;
        uint deposit = 2 * value;
        seller.transfer(deposit);

        assert(state == State.Inactive);
        assert(value >= 0 && seller != buyer);
    }

    /// @custom:key requires net(msg.sender) == 0
    /// @custom:key box
    function confirmPurchase() external payable {
        require(value >= 0 && seller != buyer);
        require(state == State.Created && msg.value == 2 * value);
        require(msg.sender != seller);

        if (state != State.Created) revert();
        uint price = 2 * value;
        require(msg.value == price);
        buyer = payable(msg.sender);
        state = State.Locked;

        assert(state == State.Locked && buyer == msg.sender);
        assert(value >= 0 && seller != buyer);
    }

    /// @custom:key ensures net(buyer) == value
    /// @custom:key box
    function confirmReceived() external {
        require(value >= 0 && seller != buyer);
        require(msg.sender == buyer && state == State.Locked);

        address b = buyer;
        if (msg.sender != b) revert();
        if (state != State.Locked) revert();
        state = State.Release;
        buyer.transfer(value);

        assert(state == State.Release);
        assert(value >= 0 && seller != buyer);
    }

    /// @custom:key ensures net(seller) == -value && net(buyer) == value
    /// @custom:key box
    function refundSeller() external {
        require(value >= 0 && seller != buyer);
        require(msg.sender == seller && state == State.Release);

        address s = seller;
        if (msg.sender != s) revert();
        if (state != State.Release) revert();
        state = State.Inactive;
        uint refund = 3 * value;
        seller.transfer(refund);

        assert(state == State.Inactive);
        assert(value >= 0 && seller != buyer);
    }
}
