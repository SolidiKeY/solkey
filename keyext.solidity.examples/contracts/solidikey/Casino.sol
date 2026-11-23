// SPDX-License-Identifier: GPL-2.0-only
pragma solidity ^0.8.0;

// ../solidikey-plain/CasinoNet.sol with the money-conservation specification of its
// casino-*.key obligations carried in @custom:key tags; the obligations' participant pins
// (operator != player, player != address(this)) become invariants. The coin guess stays a
// uint (0 HEADS, 1 TAILS) so it compares with secret % 2. casino-decideBet-withcallback.key
// proves only the invariant, under transferSemantics:withCallback, so it has no @custom:key
// form.
/// @custom:key invariant state != State.BET_PLACED -> net(operator) + net(player) == pot
/// @custom:key invariant state == State.BET_PLACED -> net(operator) + net(player) == pot + bet
/// @custom:key invariant operator != player && player != address(this)
contract Casino {
    enum State { IDLE, GAME_AVAILABLE, BET_PLACED }

    address payable operator;
    address payable player;
    uint pot;
    uint bet;
    uint guess;
    State state;

    /// @custom:key requires _player != msg.sender && _player != address(this)
    constructor(address payable _player) {
        operator = payable(msg.sender);
        player = _player;
        state = State.GAME_AVAILABLE;
    }

    /// @custom:key ensures pot == \old(pot) + msg.value
    function addToPot() public payable {
        address op = operator;
        require(msg.sender == op);
        require(state == State.GAME_AVAILABLE);
        uint p = pot;
        pot = p + msg.value;
    }

    /// @custom:key ensures state == State.BET_PLACED && bet == msg.value
    function placeBet(uint g) public payable {
        address pl = player;
        require(msg.sender == pl);
        require(state == State.GAME_AVAILABLE);
        uint p = pot;
        require(msg.value <= p);
        bet = msg.value;
        guess = g;
        state = State.BET_PLACED;
    }

    /// @custom:key ensures state == State.GAME_AVAILABLE && bet == 0
    function decideBet(uint secret) public {
        address op = operator;
        require(msg.sender == op);
        require(state == State.BET_PLACED);
        state = State.GAME_AVAILABLE;
        uint b = bet;
        bet = 0;
        uint coin = secret % 2;
        uint g = guess;
        if (coin == g) {
            uint p = pot;
            pot = p - b;
            uint win = 2 * b;
            player.transfer(win);
        } else {
            uint p = pot;
            pot = p + b;
        }
    }
}
