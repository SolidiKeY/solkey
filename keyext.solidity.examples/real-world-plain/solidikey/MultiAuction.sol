// SPDX-License-Identifier: GPL-2.0-only
pragma solidity ^0.8.0;

// The contracts/MultiAuction.sol example with its @custom:key specification removed.
contract MultiAuction {
    address payable private auctionOwner;
    mapping(address => uint) public balances;
    mapping(address => bool) bidded;
    address[] public bidders;

    enum State { AUCTION_OPEN, AUCTION_CLOSED }
    State private state;

    constructor() {
        auctionOwner = payable(msg.sender);
        state = State.AUCTION_OPEN;
    }

    function placeOrIncreaseBid() public payable {
        require(state == State.AUCTION_OPEN);
        address own = auctionOwner;
        require(msg.sender != own);
        require(msg.value > 0);
        uint b = balances[msg.sender];
        balances[msg.sender] = b + msg.value;
        bool bid = bidded[msg.sender];
        if (!bid) {
            bidders.push(msg.sender);
            bidded[msg.sender] = true;
        }
    }

    function withdraw() public {
        require(state == State.AUCTION_OPEN);
        uint tmp = balances[msg.sender];
        balances[msg.sender] = 0;
        payable(msg.sender).transfer(tmp);
    }

    function myTest() public {
    }

    function closeAuction() public {
        address own = auctionOwner;
        require(msg.sender == own);
        require(state == State.AUCTION_OPEN);
        require(bidders.length > 0);
        state = State.AUCTION_CLOSED;
        uint i;
        address winner;
        uint highestBid = 0;
        for (i = 0; i < bidders.length; i = i + 1) {
            address candidate = bidders[i];
            uint candidateBid = balances[candidate];
            if (candidateBid > highestBid) {
                winner = candidate;
                highestBid = candidateBid;
            }
        }
        auctionOwner.transfer(highestBid);
        for (i = 0; i < bidders.length; i = i + 1) {
            address bidder = bidders[i];
            uint tmp = balances[bidder];
            if (bidder != winner && tmp != 0) {
                balances[bidder] = 0;
                payable(bidder).transfer(tmp);
            }
        }
    }
}
