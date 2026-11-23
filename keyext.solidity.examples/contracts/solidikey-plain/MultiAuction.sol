// SPDX-License-Identifier: GPL-2.0-only
pragma solidity ^0.8.0;

// The ../solidikey/MultiAuction.sol example with the part of its specification that does not
// mention net or address(this) written as require and assert: the invariants
// balances[auctionOwner] == 0 and !(\exists uint i; ... bidders[i] == auctionOwner), the latter
// at a parameter i, and placeOrIncreaseBid's ensures. The whole specification, net and the
// quantified invariants included, is in the generated obligations/MultiAuction-*.key problems.
contract MultiAuction {
    address payable private auctionOwner;
    mapping(address => uint) public balances;
    mapping(address => bool) bidded;
    address[] public bidders;

    enum State { AUCTION_OPEN, AUCTION_CLOSED }
    State private state;

    /// @custom:key skip
    constructor() {
        auctionOwner = payable(msg.sender);
        state = State.AUCTION_OPEN;
    }

    /// @custom:key box
    function placeOrIncreaseBid(uint i) public payable {
        require(balances[auctionOwner] == 0);
        require(i >= bidders.length || bidders[i] != auctionOwner);
        require(state == State.AUCTION_OPEN && msg.sender != auctionOwner && msg.value > 0);
        uint oldBalance = balances[msg.sender];

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

        assert(balances[msg.sender] == oldBalance + msg.value);
        assert(balances[auctionOwner] == 0);
        assert(i >= bidders.length || bidders[i] != auctionOwner);
    }

    /// @custom:key box
    function withdraw(uint i) public {
        require(balances[auctionOwner] == 0);
        require(i >= bidders.length || bidders[i] != auctionOwner);
        require(state == State.AUCTION_OPEN);

        require(state == State.AUCTION_OPEN);
        uint tmp = balances[msg.sender];
        balances[msg.sender] = 0;
        payable(msg.sender).transfer(tmp);

        assert(balances[auctionOwner] == 0);
        assert(i >= bidders.length || bidders[i] != auctionOwner);
    }

    /// @custom:key box
    function myTest(uint i) public {
        require(balances[auctionOwner] == 0);
        require(i >= bidders.length || bidders[i] != auctionOwner);
        require(0 == 0);

        assert(1 == 1);
        assert(balances[auctionOwner] == 0);
        assert(i >= bidders.length || bidders[i] != auctionOwner);
    }

    /// @custom:key skip
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
