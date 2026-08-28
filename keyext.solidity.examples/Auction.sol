// SPDX-License-Identifier: GPL-2.0-only
pragma solidity ^0.8.0;

/// Auction course examples (SolidityCalculus: SimpleAuction v1-v3, the OneAuction
/// withdrawal pattern, and a loop-free two-bidder fragment of MultiAuction), adapted to
/// the supported fragment: participants are `uint` ids pinned to small literals,
/// `msg.sender` is `caller`, and transfers are outbound counters (`ownerReceived`,
/// `paidOut`) or credit mappings (`refunds`, `withdrawable`). The v1/v2/v3 close-ordering
/// trio states the reentrancy lesson positively: the buggy orderings leave an observable
/// stale bid (v2) or an observably still-open auction at the payout point (v1), while the
/// fixed ordering (v3) clears before paying.
contract Auction {
    uint owner;
    uint bidder1;
    uint bidder2;
    uint caller;
    bool auctionOpen;
    uint currentBid;
    uint currentBidder;
    uint ownerReceived;
    mapping(uint => uint) refunds;
    mapping(uint => uint) withdrawable;
    uint paidOut;
    mapping(uint => uint) bidBalances;
    mapping(uint => bool) hasBid;
    uint bidderCount;

    // ── SimpleAuction: placeBid ──

    /// @custom:key box
    function firstBidHappy() public {
        uint b1 = bidder1;
        uint c = caller;
        require(c == b1);
        require(b1 == 1);
        require(auctionOpen);
        require(currentBid == 0);
        require(refunds[1] == 0);
        uint oldBid = currentBid;
        uint oldBidder = currentBidder;
        currentBid = 5;
        currentBidder = c;
        if (oldBid != 0) {
            refunds[oldBidder] += oldBid;
        }
        uint cb = currentBid;
        uint cbr = currentBidder;
        uint rf = refunds[1];
        assert(cb == 5);
        assert(cbr == 1);
        assert(rf == 0);
    }

    /// @custom:key box
    function overbidRefundsPrevious() public {
        uint b2 = bidder2;
        uint c = caller;
        require(c == b2);
        require(b2 == 2);
        require(auctionOpen);
        require(currentBid == 5);
        require(currentBidder == 1);
        require(refunds[1] == 0);
        require(5 < 8);
        uint oldBid = currentBid;
        uint oldBidder = currentBidder;
        currentBid = 8;
        currentBidder = c;
        if (oldBid != 0) {
            refunds[oldBidder] += oldBid;
        }
        uint cb = currentBid;
        uint cbr = currentBidder;
        uint rf = refunds[1];
        assert(cb == 8);
        assert(cbr == 2);
        assert(rf == 5);
    }

    /// @custom:key box
    function overbidConservation(uint v1, uint v2) public {
        require(v1 > 0 && v1 < v2 && v2 <= 1000);
        uint b2 = bidder2;
        uint c = caller;
        require(c == b2);
        require(b2 == 2);
        require(auctionOpen);
        require(currentBid == v1);
        require(currentBidder == 1);
        require(refunds[1] == 0);
        uint oldBid = currentBid;
        uint oldBidder = currentBidder;
        currentBid = v2;
        currentBidder = c;
        if (oldBid != 0) {
            refunds[oldBidder] += oldBid;
        }
        uint cb = currentBid;
        uint rf = refunds[1];
        uint lhs = cb + rf;
        uint rhs = v1 + v2;
        assert(lhs == rhs);
    }

    /// @custom:key box
    function bidGuardNoOpWhenClosed() public {
        uint b2 = bidder2;
        uint c = caller;
        require(c == b2);
        require(!auctionOpen);
        require(currentBid == 5);
        require(currentBidder == 1);
        bool open = auctionOpen;
        if (open) {
            uint oldBid = currentBid;
            uint oldBidder = currentBidder;
            currentBid = 8;
            currentBidder = c;
            if (oldBid != 0) {
                refunds[oldBidder] += oldBid;
            }
        }
        uint cb = currentBid;
        uint cbr = currentBidder;
        assert(cb == 5);
        assert(cbr == 1);
    }

    // ── SimpleAuction: closeAuction orderings ──

    /// @custom:key box
    function closeFixedClearsBid() public {
        uint ow = owner;
        uint c = caller;
        require(c == ow);
        require(auctionOpen);
        require(currentBid == 8);
        require(ownerReceived == 0);
        auctionOpen = false;
        uint tmp = currentBid;
        currentBid = 0;
        ownerReceived += tmp;
        bool open = auctionOpen;
        uint cb = currentBid;
        uint o = ownerReceived;
        assert(!open);
        assert(cb == 0);
        assert(o == 8);
    }

    /// @custom:key box
    function closeBuggyLeavesBid() public {
        uint ow = owner;
        uint c = caller;
        require(c == ow);
        require(auctionOpen);
        require(currentBid == 8);
        require(ownerReceived == 0);
        auctionOpen = false;
        uint tmp = currentBid;
        ownerReceived += tmp;
        uint cb = currentBid;
        uint o = ownerReceived;
        assert(cb == 8);
        assert(o == 8);
    }

    /// @custom:key box
    function closeV1OpenDuringPayout() public {
        uint ow = owner;
        uint c = caller;
        require(c == ow);
        require(auctionOpen == true);
        require(currentBid == 8);
        require(ownerReceived == 0);
        uint tmp = currentBid;
        bool openAtPayout = auctionOpen;
        ownerReceived += tmp;
        auctionOpen = false;
        bool open = auctionOpen;
        assert(openAtPayout);
        assert(!open);
    }

    // ── OneAuction: withdrawal pattern ──

    /// @custom:key box
    function makeBidCreditsOutbidWithdrawable() public {
        uint b2 = bidder2;
        uint c = caller;
        require(c == b2);
        require(b2 == 2);
        require(auctionOpen);
        require(currentBid == 5);
        require(currentBidder == 1);
        require(withdrawable[1] == 0);
        uint oldBid = currentBid;
        uint oldBidder = currentBidder;
        withdrawable[oldBidder] += oldBid;
        currentBid = 8;
        currentBidder = c;
        uint w = withdrawable[1];
        uint cb = currentBid;
        assert(w == 5);
        assert(cb == 8);
    }

    /// @custom:key box
    function withdrawZeroesBalance() public {
        uint c = caller;
        require(c == 1);
        require(withdrawable[1] == 5);
        require(paidOut == 0);
        uint tmp = withdrawable[c];
        withdrawable[c] = 0;
        paidOut += tmp;
        uint w = withdrawable[1];
        uint p = paidOut;
        assert(w == 0);
        assert(p == 5);
    }

    /// @custom:key box
    function closeCreditsOwnerThenWithdraw() public {
        uint ow = owner;
        require(ow == 3);
        require(auctionOpen);
        require(currentBid == 8);
        require(withdrawable[3] == 0);
        require(paidOut == 0);
        auctionOpen = false;
        uint cb = currentBid;
        withdrawable[ow] += cb;
        uint tmp = withdrawable[ow];
        withdrawable[ow] = 0;
        paidOut += tmp;
        uint w = withdrawable[3];
        uint p = paidOut;
        assert(w == 0);
        assert(p == 8);
    }

    // ── MultiAuction: two-bidder fragment ──

    /// @custom:key box
    function multiFirstBidRegisters() public {
        uint b1 = bidder1;
        require(b1 == 1);
        require(bidBalances[1] == 0);
        require(!hasBid[1]);
        require(bidderCount == 0);
        bidBalances[b1] += 4;
        bool h = hasBid[b1];
        if (!h) {
            hasBid[b1] = true;
            bidderCount++;
        }
        uint bal = bidBalances[1];
        uint n = bidderCount;
        assert(bal == 4);
        assert(n == 1);
    }

    /// @custom:key box
    function multiRepeatBidAccumulates() public {
        uint b1 = bidder1;
        require(b1 == 1);
        require(bidBalances[1] == 0);
        require(!hasBid[1]);
        require(bidderCount == 0);
        bidBalances[b1] += 4;
        bool h = hasBid[b1];
        if (!h) {
            hasBid[b1] = true;
            bidderCount++;
        }
        bidBalances[b1] += 3;
        bool h2 = hasBid[b1];
        if (!h2) {
            hasBid[b1] = true;
            bidderCount++;
        }
        uint bal = bidBalances[1];
        uint n = bidderCount;
        assert(bal == 7);
        assert(n == 1);
    }

    /// @custom:key box
    function multiCloseTwoBiddersUnrolled() public {
        uint b1 = bidder1;
        uint b2 = bidder2;
        require(b1 == 1);
        require(b2 == 2);
        require(bidBalances[1] == 3);
        require(bidBalances[2] == 7);
        require(ownerReceived == 0);
        require(refunds[1] == 0);
        require(refunds[2] == 0);
        uint v1 = bidBalances[b1];
        uint v2 = bidBalances[b2];
        uint highest = v1;
        uint winner = b1;
        uint loser = b2;
        uint loserBal = v2;
        if (v2 > highest) {
            highest = v2;
            winner = b2;
            loser = b1;
            loserBal = v1;
        }
        ownerReceived += highest;
        bidBalances[winner] = 0;
        refunds[loser] += loserBal;
        bidBalances[loser] = 0;
        uint o = ownerReceived;
        uint r1 = refunds[1];
        uint bal1 = bidBalances[1];
        uint bal2 = bidBalances[2];
        uint lhs = o + r1;
        assert(o == 7);
        assert(r1 == 3);
        assert(bal1 == 0);
        assert(bal2 == 0);
        assert(lhs == 10);
    }
}
