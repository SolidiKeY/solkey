// SPDX-License-Identifier: GPL-2.0-only
pragma solidity ^0.8.0;

/// Casino course example (SolidityCalculus/SimpleCasino), adapted to the supported
/// fragment: the state enum IDLE/GAME_AVAILABLE/BET_PLACED is a `uint` (0/1/2),
/// participants are `uint` ids, `msg.sender` is `caller`, `block.timestamp` is `timeNow`,
/// `keccak256(secret)` is the stored preimage `storedSecret` whose parity decides the coin
/// (even HEADS = 0, odd TAILS = 1), and transfers are the outbound counters
/// `operatorReceived`/`playerReceived`. The v1/v2 ordering pair states the reentrancy
/// surface positively: a snapshot local taken at the would-be transfer point observes the
/// stale state (v1, payout before `state = 0`) or the fresh one (v2).
contract Casino {
    struct Wager {
        uint bet;
        uint guess;
    }

    uint operator;
    uint player;
    uint caller;
    uint state; // 0 IDLE, 1 GAME_AVAILABLE, 2 BET_PLACED
    uint pot;
    uint storedSecret;
    Wager wager;
    uint timeNow;
    uint betTimestamp;
    uint timeoutVal;
    uint operatorReceived;
    uint playerReceived;

    // ── pot management ──

    /// @custom:key box
    function addToPotIncreases(uint v) public {
        require(v > 0 && v <= 1000);
        uint op = operator;
        require(caller == op);
        require(pot <= 1000);
        uint before = pot;
        pot += v;
        uint r = pot;
        uint expected = before + v;
        assert(r == expected);
    }

    /// @custom:key box
    function removeFromPotWhenIdle() public {
        uint op = operator;
        require(caller == op);
        require(state == 0);
        require(pot == 10);
        require(operatorReceived == 0);
        pot -= 4;
        operatorReceived += 4;
        uint p = pot;
        uint o = operatorReceived;
        assert(p == 6);
        assert(o == 4);
    }

    /// @custom:key box
    function removeFromPotNoOpWhenBetPlaced() public {
        uint op = operator;
        require(caller == op);
        require(state == 2);
        require(pot == 10);
        require(operatorReceived == 0);
        uint s = state;
        if (s == 0 || s == 1) {
            pot -= 4;
            operatorReceived += 4;
        }
        uint p = pot;
        uint o = operatorReceived;
        assert(p == 10);
        assert(o == 0);
    }

    // ── createGame / placeBet ──

    /// @custom:key box
    function createGameHappy() public {
        uint op = operator;
        require(caller == op);
        require(state == 0);
        storedSecret = 4;
        state = 1;
        uint s = state;
        uint sec = storedSecret;
        assert(s == 1);
        assert(sec == 4);
    }

    /// @custom:key box
    function placeBetHappy() public {
        uint pl = player;
        uint op = operator;
        require(caller == pl);
        require(pl != op);
        require(state == 1);
        require(pot == 10);
        require(timeNow == 50);
        require(5 <= pot);
        uint t0 = timeNow;
        state = 2;
        wager.bet = 5;
        wager.guess = 0;
        betTimestamp = t0;
        uint s = state;
        uint b = wager.bet;
        uint t = betTimestamp;
        assert(s == 2);
        assert(b == 5);
        assert(t == 50);
    }

    // ── decideBet ──

    /// @custom:key box
    function decideBetPlayerWins() public {
        uint op = operator;
        require(caller == op);
        require(state == 2);
        require(storedSecret == 4);
        require(wager.guess == 0);
        require(pot == 10);
        require(wager.bet == 5);
        require(playerReceived == 0);
        uint sec = storedSecret;
        uint parity = sec % 2;
        uint coin = parity == 0 ? 0 : 1;
        uint g = wager.guess;
        uint b = wager.bet;
        wager.bet = 0;
        if (coin == g) {
            pot -= b;
            uint win = b * 2;
            playerReceived += win;
        } else {
            pot += b;
        }
        state = 0;
        uint p = pot;
        uint pr = playerReceived;
        uint s = state;
        uint wb = wager.bet;
        assert(p == 5);
        assert(pr == 10);
        assert(s == 0);
        assert(wb == 0);
    }

    /// @custom:key box
    function decideBetOperatorWins() public {
        uint op = operator;
        require(caller == op);
        require(state == 2);
        require(storedSecret == 3);
        require(wager.guess == 0);
        require(pot == 10);
        require(wager.bet == 5);
        require(playerReceived == 0);
        uint sec = storedSecret;
        uint parity = sec % 2;
        uint coin = parity == 0 ? 0 : 1;
        uint g = wager.guess;
        uint b = wager.bet;
        wager.bet = 0;
        if (coin == g) {
            pot -= b;
            uint win = b * 2;
            playerReceived += win;
        } else {
            pot += b;
        }
        state = 0;
        uint p = pot;
        uint pr = playerReceived;
        uint s = state;
        assert(p == 15);
        assert(pr == 0);
        assert(s == 0);
    }

    /// @custom:key box
    function decideBetConservation(uint p0, uint b, uint g) public {
        require(b > 0 && b <= p0 && p0 <= 1000);
        require(g == 0 || g == 1);
        uint op = operator;
        require(caller == op);
        require(state == 2);
        require(storedSecret == 4);
        require(playerReceived == 0);
        pot = p0;
        wager.bet = b;
        wager.guess = g;
        uint sec = storedSecret;
        uint parity = sec % 2;
        uint coin = parity == 0 ? 0 : 1;
        wager.bet = 0;
        if (coin == g) {
            pot -= b;
            uint win = b * 2;
            playerReceived += win;
        } else {
            pot += b;
        }
        state = 0;
        uint p = pot;
        uint pr = playerReceived;
        uint lhs = p + pr;
        uint rhs = p0 + b;
        assert(lhs == rhs);
    }

    // ── timeoutBet ──

    /// @custom:key box
    function timeoutBetPlayerWins() public {
        uint pl = player;
        require(caller == pl);
        require(state == 2);
        require(timeNow == 100 && betTimestamp == 10 && timeoutVal == 30);
        require(timeNow - betTimestamp > timeoutVal);
        require(pot == 10);
        require(wager.bet == 5);
        require(playerReceived == 0);
        uint b = wager.bet;
        wager.bet = 0;
        pot -= b;
        uint win = b * 2;
        playerReceived += win;
        state = 0;
        uint p = pot;
        uint pr = playerReceived;
        uint s = state;
        assert(p == 5);
        assert(pr == 10);
        assert(s == 0);
    }

    // ── reentrancy-ordering pair ──

    /// @custom:key box
    function decideBetV1StaleStateDuringPayout() public {
        uint op = operator;
        require(caller == op);
        require(state == 2);
        require(pot == 10);
        require(wager.bet == 5);
        require(playerReceived == 0);
        uint b = wager.bet;
        wager.bet = 0;
        pot -= b;
        uint stateAtPayout = state;
        uint win = b * 2;
        playerReceived += win;
        state = 0;
        uint s = state;
        assert(stateAtPayout == 2);
        assert(s == 0);
    }

    /// @custom:key box
    function decideBetV2FreshStateDuringPayout() public {
        uint op = operator;
        require(caller == op);
        require(state == 2);
        require(pot == 10);
        require(wager.bet == 5);
        require(playerReceived == 0);
        uint b = wager.bet;
        wager.bet = 0;
        pot -= b;
        state = 0;
        uint stateAtPayout = state;
        uint win = b * 2;
        playerReceived += win;
        uint s = state;
        assert(stateAtPayout == 0);
        assert(s == 0);
    }
}
