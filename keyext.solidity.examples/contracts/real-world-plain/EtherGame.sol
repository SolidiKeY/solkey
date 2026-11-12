// SPDX-License-Identifier: MIT
pragma solidity ^0.8.26;

// Source: https://github.com/Cyfrin/solidity-by-example.github.io/blob/5bcdca0239409d7336a07b66a6fca8d0bcc710e6/contracts/src/hacks/self-destruct/PreventForceEther.sol
// The real-world/EtherGame.sol port with its specification written as require and assert.
// claimReward is in open/key-spec/EtherGame.sol.
contract EtherGame {
    uint256 public constant TARGET_AMOUNT = 7 ether;
    uint256 public balance;
    address public winner;

    /// @custom:key box
    function deposit() public payable {
        require(balance >= 0 && balance <= TARGET_AMOUNT);
        uint256 oldBalance = balance;
        address oldWinner = winner;

        require(msg.value == 1 ether);

        balance += msg.value;
        require(balance <= TARGET_AMOUNT);

        if (balance == TARGET_AMOUNT) {
            winner = msg.sender;
        }

        assert(msg.value == 1 ether && balance == oldBalance + msg.value);
        assert(balance != TARGET_AMOUNT || winner == msg.sender);
        assert(balance >= TARGET_AMOUNT || winner == oldWinner);
        assert(balance >= 0 && balance <= TARGET_AMOUNT);
    }
}
