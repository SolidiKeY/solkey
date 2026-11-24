// SPDX-License-Identifier: GPL-2.0-only
pragma solidity ^0.8.0;

// Course transfer-last OneAuction (SolidityCalculus/maltaCourseKey/auction-transfer-last)
// in the supported fragment: modifiers are inlined requires, `now` is the timeNow state variable.
// Verified per function by the auction-*.key problems of obligations/ against the course
// invariant
//   bid == net(bidder) + net(owner),
//   net(owner) <= 0,
//   mode == Open -> net(owner) == 0.
// makeBid refunds the outbid bidder with the transfer LAST (ISoLA 2020, Fig. 5), so the
// invariant also holds at the transfer point (auction-makeBid-withcallback.key).
contract AuctionNet {
    enum AuctionMode { NeverStarted, Open, Closed }

    AuctionMode mode;
    address payable owner;
    uint closingTime;
    address payable bidder;
    uint bid;
    uint timeNow;

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
