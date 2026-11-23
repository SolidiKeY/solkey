// SPDX-License-Identifier: GPL-2.0-only
pragma solidity ^0.8.0;

// ../solidikey-plain/AuctionNet.sol, the course transfer-last OneAuction, with the
// specification of its auction-*.key obligations carried in @custom:key tags. The
// obligations' antecedent pins become invariants (bid >= 0, bidder != address(this),
// owner != address(this)) or requires (the fresh caller). auction-makeBid-withcallback.key
// proves only the invariant, under transferSemantics:withCallback, so it has no @custom:key
// form.
/// @custom:key invariant bid == net(bidder) + net(owner)
/// @custom:key invariant net(owner) <= 0
/// @custom:key invariant mode == AuctionMode.Open -> net(owner) == 0
/// @custom:key invariant bid >= 0
/// @custom:key invariant bidder != address(this) && owner != address(this)
contract Auction {
    enum AuctionMode { NeverStarted, Open, Closed }

    AuctionMode mode;
    address payable owner;
    uint closingTime;
    address payable bidder;
    uint bid;
    uint timeNow;

    constructor(uint _closingTime) {
        owner = payable(msg.sender);
        bidder = payable(msg.sender);
        closingTime = _closingTime;
        mode = AuctionMode.Open;
    }

    /// @custom:key requires msg.value >= 0 && msg.sender != address(this)
    /// @custom:key requires msg.sender == bidder || net(msg.sender) == 0
    /// @custom:key ensures bid > \old(bid)
    /// @custom:key ensures net(bidder) == msg.value
    /// @custom:key ensures \old(bidder) != msg.sender -> net(\old(bidder)) == 0
    function makeBid() public payable {
        require(mode == AuctionMode.Open);
        address own = owner;
        require(msg.sender != own);
        uint currentBid = bid;
        require(msg.value > currentBid);
        require(timeNow <= closingTime);
        uint oldBid = bid;
        address payable oldBidder = bidder;
        bid = msg.value;
        bidder = payable(msg.sender);
        oldBidder.transfer(oldBid);
    }

    /// @custom:key ensures net(owner) == -net(bidder)
    /// @custom:key ensures mode == AuctionMode.Closed
    function closeAuction() public {
        require(mode == AuctionMode.Open);
        address own = owner;
        address bdr = bidder;
        require(msg.sender == own || msg.sender == bdr);
        require(timeNow > closingTime);
        mode = AuctionMode.Closed;
        uint tmp = bid;
        bid = 0;
        owner.transfer(tmp);
    }
}
