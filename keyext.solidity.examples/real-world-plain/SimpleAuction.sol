// SPDX-License-Identifier: GPL-3.0
pragma solidity ^0.8.4;

// Source: https://docs.soliditylang.org/en/latest/solidity-by-example.html#simple-open-auction
// The real-world/SimpleAuction.sol port with its @custom:key specification removed.
contract SimpleAuction {
    address payable public beneficiary;
    uint public auctionEndTime;
    address public highestBidder;
    uint public highestBid;
    mapping(address => uint) pendingReturns;
    bool ended;
    uint timeNow;

    constructor(uint biddingTime, address payable beneficiaryAddress) {
        beneficiary = beneficiaryAddress;
        auctionEndTime = timeNow + biddingTime;
    }

    function bid() external payable {
        if (timeNow > auctionEndTime) revert();
        if (msg.value <= highestBid) revert();
        uint previousBid = highestBid;
        if (previousBid != 0) {
            address previousBidder = highestBidder;
            pendingReturns[previousBidder] += previousBid;
        }
        highestBidder = msg.sender;
        highestBid = msg.value;
    }

    function withdraw() external {
        uint amount = pendingReturns[msg.sender];
        if (amount > 0) {
            pendingReturns[msg.sender] = 0;
            payable(msg.sender).transfer(amount);
        }
    }

    function auctionEnd() external {
        if (timeNow < auctionEndTime) revert();
        if (ended) revert();
        ended = true;
        beneficiary.transfer(highestBid);
    }
}
