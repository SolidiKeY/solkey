// SPDX-License-Identifier: GPL-2.0-only
pragma solidity ^0.8.0;

// ../solidikey-plain/AuctionWithdrawNet.sol, the course withdrawal-pattern OneAuction, with
// the specification of its auction-withdraw-*.key obligations carried in @custom:key tags.
// The invariant is over effective_net(a) = net(a) - withdrawableBalances[a]. withdraw keeps
// the obligation's canonical caller (the outbid bidder, distinct from the owner) as a
// requires. closeAuction is skipped as in the course: it credits the owner without
// resetting bid, so the conservation clause provably does not survive it.
/// @custom:key invariant bid == net(bidder) - withdrawableBalances[bidder] + net(owner) - withdrawableBalances[owner]
/// @custom:key invariant net(owner) - withdrawableBalances[owner] <= 0
/// @custom:key invariant mode == AuctionMode.Open -> net(owner) - withdrawableBalances[owner] == 0
/// @custom:key invariant bid >= 0
contract AuctionWithdraw {
    enum AuctionMode { NeverStarted, Open, Closed }

    AuctionMode mode;
    address payable owner;
    uint closingTime;
    address payable bidder;
    uint bid;
    uint timeNow;
    mapping(address => uint) withdrawableBalances;

    /// @custom:key requires msg.sender == bidder || net(msg.sender) - withdrawableBalances[msg.sender] == 0
    /// @custom:key ensures bid > \old(bid)
    /// @custom:key ensures net(bidder) - withdrawableBalances[bidder] == msg.value
    /// @custom:key ensures \old(bidder) != msg.sender -> net(\old(bidder)) - withdrawableBalances[\old(bidder)] == 0
    function makeBid() public payable {
        require(mode == AuctionMode.Open);
        address own = owner;
        require(msg.sender != own);
        uint currentBid = bid;
        require(msg.value > currentBid);
        require(timeNow <= closingTime);
        address bdr = bidder;
        uint wb = withdrawableBalances[bdr];
        withdrawableBalances[bdr] = wb + currentBid;
        bid = msg.value;
        bidder = payable(msg.sender);
    }

    /// @custom:key skip
    function closeAuction() public {
        require(mode == AuctionMode.Open);
        address own = owner;
        address bdr = bidder;
        require(msg.sender == own || msg.sender == bdr);
        require(timeNow > closingTime);
        mode = AuctionMode.Closed;
        uint b = bid;
        uint wb = withdrawableBalances[own];
        withdrawableBalances[own] = wb + b;
    }

    /// @custom:key requires msg.sender == bidder && bidder != owner
    /// @custom:key ensures withdrawableBalances[msg.sender] == 0
    /// @custom:key ensures net(msg.sender) == \old(net(msg.sender) - withdrawableBalances[msg.sender])
    function withdraw() public {
        uint tmp = withdrawableBalances[msg.sender];
        withdrawableBalances[msg.sender] = 0;
        payable(msg.sender).transfer(tmp);
    }
}
