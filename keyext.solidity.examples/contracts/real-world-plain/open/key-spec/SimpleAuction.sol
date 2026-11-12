// SPDX-License-Identifier: GPL-3.0
pragma solidity ^0.8.4;

// Source: https://docs.soliditylang.org/en/latest/solidity-by-example.html#simple-open-auction
// The real-world/SimpleAuction.sol port with its specification written as require and assert. Each
// clause with no Solidity form stays a @custom:key clause; beneficiary != address(this) is one,
// since the prover does not load address(this) in a body.
/// @custom:key invariant beneficiary != address(this)
contract SimpleAuction {
    address payable public beneficiary;
    uint public auctionEndTime;
    address public highestBidder;
    uint public highestBid;
    mapping(address => uint) pendingReturns;
    bool ended;
    uint timeNow;

    /// @custom:key skip
    constructor(uint biddingTime, address payable beneficiaryAddress) {
        beneficiary = beneficiaryAddress;
        auctionEndTime = timeNow + biddingTime;
    }

    /// @custom:key box
    function bid(address a) external payable {
        require(highestBid >= 0);
        require(pendingReturns[a] >= 0);
        require(timeNow <= auctionEndTime && msg.value > highestBid);
        uint oldHighestBid = highestBid;
        address oldHighestBidder = highestBidder;
        uint oldPendingReturn = pendingReturns[highestBidder];

        if (timeNow > auctionEndTime) revert();
        if (msg.value <= highestBid) revert();
        uint previousBid = highestBid;
        if (previousBid != 0) {
            address previousBidder = highestBidder;
            pendingReturns[previousBidder] += previousBid;
        }
        highestBidder = msg.sender;
        highestBid = msg.value;

        assert(highestBidder == msg.sender && highestBid == msg.value);
        assert(oldHighestBid == 0 || pendingReturns[oldHighestBidder] == oldPendingReturn + oldHighestBid);
        assert(highestBid >= 0);
        assert(pendingReturns[a] >= 0);
    }

    /// @custom:key ensures net(msg.sender) == \old(net(msg.sender)) - \old(pendingReturns[msg.sender])
    /// @custom:key box
    function withdraw(address a) external {
        require(highestBid >= 0);
        require(pendingReturns[a] >= 0 && pendingReturns[msg.sender] >= 0);

        uint amount = pendingReturns[msg.sender];
        if (amount > 0) {
            pendingReturns[msg.sender] = 0;
            payable(msg.sender).transfer(amount);
        }

        assert(pendingReturns[msg.sender] == 0);
        assert(highestBid >= 0);
        assert(pendingReturns[a] >= 0);
    }

    /// @custom:key ensures net(beneficiary) == \old(net(beneficiary)) - \old(highestBid)
    /// @custom:key box
    function auctionEnd(address a) external {
        require(highestBid >= 0);
        require(pendingReturns[a] >= 0);
        require(timeNow >= auctionEndTime && !ended);

        if (timeNow < auctionEndTime) revert();
        if (ended) revert();
        ended = true;
        beneficiary.transfer(highestBid);

        assert(ended);
        assert(highestBid >= 0);
        assert(pendingReturns[a] >= 0);
    }
}
