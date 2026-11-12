// SPDX-License-Identifier: MIT
pragma solidity ^0.8.26;

// Source: https://github.com/Cyfrin/solidity-by-example.github.io/blob/5bcdca0239409d7336a07b66a6fca8d0bcc710e6/contracts/src/hacks/self-destruct/PreventForceEther.sol
// The real-world/EtherGame.sol port with its specification written as require and assert. Each
// clause with no Solidity form stays a @custom:key clause.
contract EtherGame {
    uint256 public constant TARGET_AMOUNT = 7 ether;
    uint256 public balance;
    address public winner;

    /// @custom:key ensures net(msg.sender) == \old(net(msg.sender)) - \old(balance)
    /// @custom:key box
    function claimReward() public {
        require(balance >= 0 && balance <= TARGET_AMOUNT);
        address oldWinner = winner;

        require(msg.sender == winner);
        uint256 amount = balance;
        balance = 0;
        (bool sent,) = msg.sender.call{value: amount}("");
        require(sent);

        assert(msg.sender == winner && winner == oldWinner && balance == 0);
        assert(balance >= 0 && balance <= TARGET_AMOUNT);
    }
}
